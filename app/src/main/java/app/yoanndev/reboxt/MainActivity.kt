package app.yoanndev.reboxt

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.yoanndev.reboxt.data.Logger
import app.yoanndev.reboxt.explorer.*
import app.yoanndev.reboxt.ui.SettingsMenu
import app.yoanndev.reboxt.ui.PermissionsDetailScreen
import app.yoanndev.reboxt.ui.LogsDetailScreen
import app.yoanndev.reboxt.ui.CreditsScreen
import app.yoanndev.reboxt.ui.theme.ReboxtTheme
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.init(this)
        val repository = SettingsSnapshotRepository(contentResolver)
        val classifier = SettingClassifier()
        val diffEngine = DiffEngine(classifier)

        setContent {
            ReboxtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("scheduler") }
                    
                    BackHandler(enabled = currentScreen != "scheduler") {
                        if (currentScreen.startsWith("settings_")) {
                            currentScreen = "settings"
                        } else {
                            currentScreen = "scheduler"
                        }
                    }

                    Column {
                        Box(modifier = Modifier.weight(1f)) {
                            when (currentScreen) {
                                "scheduler" -> PowerSchedulerScreen()
                                "explorer" -> SettingExplorerUI(repository, diffEngine)
                                "settings" -> SettingsMenu(onNavigate = { currentScreen = it })
                                "settings_permissions" -> PermissionsDetailScreen(onBack = { currentScreen = "settings" })
                                "settings_logs" -> LogsDetailScreen(onBack = { currentScreen = "settings" })
                                "settings_credits" -> CreditsScreen(onBack = { currentScreen = "settings" })
                            }
                        }
                        
                        if (!currentScreen.startsWith("settings_")) {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Refresh, "Schedule") },
                                    label = { Text("Schedule") },
                                    selected = currentScreen == "scheduler",
                                    onClick = { currentScreen = "scheduler" }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.ContentCopy, "Explorer") },
                                    label = { Text("Explorer") },
                                    selected = currentScreen == "explorer",
                                    onClick = { currentScreen = "explorer" }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Shield, "Settings") },
                                    label = { Text("Settings") },
                                    selected = currentScreen.startsWith("settings"),
                                    onClick = { currentScreen = "settings" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PowerSchedulerScreen() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    var writeSettingsGranted by remember { mutableStateOf(Settings.System.canWrite(context)) }
    var secureSettingsGranted by remember { 
        mutableStateOf(context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) 
    }
    var shellMode by remember { mutableStateOf(ShellExecutor.getAvailableMode()) }
    
    var offTime by remember { mutableStateOf("22:00") }
    var onTime by remember { mutableStateOf("07:00") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Reboxt", style = MaterialTheme.typography.headlineLarge)
        Text(text = "FOSS Native Power Scheduler", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = offTime,
            onValueChange = { offTime = it },
            label = { Text("Power Off Time (HH:mm)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = onTime,
            onValueChange = { onTime = it },
            label = { Text("Power On Time (HH:mm)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val calendar = Calendar.getInstance()
                val parts = onTime.split(":")
                if (parts.size == 2) {
                    calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                    calendar.set(Calendar.MINUTE, parts[1].toInt())
                    calendar.set(Calendar.SECOND, 0)
                    
                    if (calendar.timeInMillis < System.currentTimeMillis()) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    
                    val result = PowerSchedulePOC.schedulePowerOnWithElevated(calendar.timeInMillis)
                    Logger.i("Schedule", "Elevated Boot Result: $result")
                }
                
                val success = tryModifySettings(context, onTime, offTime)
                if (success) {
                    Logger.i("Schedule", "Settings: Set Off $offTime / On $onTime")
                    Toast.makeText(context, "Schedules updated!", Toast.LENGTH_SHORT).show()
                } else {
                    Logger.e("Schedule", "Settings: Modification failed.")
                    Toast.makeText(context, "Failed to update settings.", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = writeSettingsGranted || shellMode != ShellExecutor.Mode.NONE
        ) {
            Text("Apply Schedule Everywhere")
        }
    }
}

@Composable
fun StatusLine(name: String, granted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (granted) "✅" else "❌", modifier = Modifier.width(24.dp))
        Text(name, style = MaterialTheme.typography.bodySmall)
    }
}

fun tryModifySettings(context: Context, onTime: String, offTime: String): Boolean {
    val cr = context.contentResolver
    
    // Attempt standard modification
    var standardSuccess = true
    try {
        val dayMask = 127
        Settings.System.putString(cr, "power_off_alarm_time", offTime)
        Settings.System.putInt(cr, "power_off_alarm_enabled", 1)
        Settings.System.putInt(cr, "power_off_alarm_days", dayMask)
        Settings.Global.putInt(cr, "power_off_alarm_enabled", 1)
        
        Settings.System.putString(cr, "power_on_alarm_time", onTime)
        Settings.System.putInt(cr, "power_on_alarm_enabled", 1)
        Settings.System.putInt(cr, "power_on_alarm_days", dayMask)
        Settings.Global.putInt(cr, "power_on_alarm_enabled", 1)
    } catch (e: Exception) {
        android.util.Log.w("Reboxt", "Standard settings modification failed: ${e.message}")
        standardSuccess = false
    }

    // Attempt elevated modification as fallback or supplement
    val elevatedSuccess = if (ShellExecutor.getAvailableMode() != ShellExecutor.Mode.NONE) {
        val dayMask = "127"
        val results = listOf(
            ShellExecutor.putSettings("system", "power_off_alarm_time", offTime),
            ShellExecutor.putSettings("system", "power_off_alarm_enabled", "1"),
            ShellExecutor.putSettings("system", "power_off_alarm_days", dayMask),
            ShellExecutor.putSettings("global", "power_off_alarm_enabled", "1"),
            ShellExecutor.putSettings("system", "power_on_alarm_time", onTime),
            ShellExecutor.putSettings("system", "power_on_alarm_enabled", "1"),
            ShellExecutor.putSettings("system", "power_on_alarm_days", dayMask),
            ShellExecutor.putSettings("global", "power_on_alarm_enabled", "1")
        )
        results.any { it }
    } else false

    // Broadcast update intents
    val actions = listOf(
        "com.mediatek.schpwronoff.CHANGE_SCHEDULE",
        "android.intent.action.set_pwr_on_off",
        "com.android.settings.action.SCHEDULE_POWER_ON_OFF_CHANGED",
        "com.miui.powercenter.SET_POWER_ON_OFF"
    )
    
    actions.forEach { action ->
        context.sendBroadcast(Intent(action))
    }
    
    return standardSuccess || elevatedSuccess
}

fun openNativeScheduleSettings(context: Context): Boolean {
    val intents = listOf(
        Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.Settings\$SchedulePowerOnOFFActivity")),
        Intent("com.android.settings.SCHEDULE_POWER_ON_OFF_SETTING"),
        Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerShutdownOnTime")),
        Intent().setComponent(ComponentName("com.coloros.settings", "com.coloros.settings.feature.display.poweronoff.PowerOnOffActivity")),
        Intent(Settings.ACTION_SETTINGS)
    )

    for (intent in intents) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return true
        } catch (e: Exception) {}
    }
    return false
}

