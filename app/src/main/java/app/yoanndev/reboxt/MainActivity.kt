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
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.yoanndev.reboxt.explorer.*
import app.yoanndev.reboxt.ui.theme.ReboxtTheme
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = SettingsSnapshotRepository(contentResolver)
        val classifier = SettingClassifier()
        val diffEngine = DiffEngine(classifier)

        setContent {
            ReboxtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showExplorer by remember { mutableStateOf(false) }
                    
                    Column {
                        if (!showExplorer) {
                            Box(modifier = Modifier.weight(1f)) {
                                PowerSchedulerScreen()
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f)) {
                                SettingExplorerUI(repository, diffEngine)
                            }
                        }
                        
                        Button(
                            onClick = { showExplorer = !showExplorer },
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Text(if (showExplorer) "Back to Scheduler" else "Open Settings Explorer")
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
    var logs by remember { mutableStateOf("App started\n") }

    fun addLog(msg: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs += "[$time] $msg\n"
    }

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

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (writeSettingsGranted && secureSettingsGranted) 
                    MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Permissions Status", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                StatusLine("WRITE_SETTINGS", writeSettingsGranted)
                StatusLine("WRITE_SECURE_SETTINGS", secureSettingsGranted)
                
                val shellStatusText = when (shellMode) {
                    ShellExecutor.Mode.ROOT -> "ROOT (Available)"
                    ShellExecutor.Mode.SHIZUKU -> "SHIZUKU (Available)"
                    ShellExecutor.Mode.NONE -> "No Shell Access (Root or Shizuku)"
                }
                StatusLine(shellStatusText, shellMode != ShellExecutor.Mode.NONE)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = {
                        writeSettingsGranted = Settings.System.canWrite(context)
                        secureSettingsGranted = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
                        shellMode = ShellExecutor.getAvailableMode()
                        addLog("Refresh: WRITE=$writeSettingsGranted, SECURE=$secureSettingsGranted, SHELL=$shellMode")
                    }) {
                        Icon(Icons.Default.Refresh, "Refresh permissions")
                    }
                    if (!writeSettingsGranted) {
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }) {
                            Text("Fix Write")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                    
                    // If time already passed today, schedule for tomorrow
                    if (calendar.timeInMillis < System.currentTimeMillis()) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    
                    val result = PowerSchedulePOC.schedulePowerOnWithElevated(calendar.timeInMillis)
                    addLog("Elevated Boot Result: $result")
                }
                
                val success = tryModifySettings(context, onTime, offTime)
                if (success) {
                    addLog("Settings: Set Off $offTime / On $onTime")
                    Toast.makeText(context, "Schedules updated!", Toast.LENGTH_SHORT).show()
                } else {
                    addLog("Settings: Modification failed.")
                    Toast.makeText(context, "Failed to update settings.", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = writeSettingsGranted || shellMode != ShellExecutor.Mode.NONE
        ) {
            Text("Apply Schedule Everywhere")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Debug Logs", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
        Surface(
            modifier = Modifier.fillMaxWidth().height(150.dp).padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Box {
                SelectionContainer {
                    Text(
                        text = logs,
                        modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                IconButton(
                    onClick = { 
                        clipboardManager.setText(AnnotatedString(logs))
                        Toast.makeText(context, "Logs copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.ContentCopy, "Copy logs", modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { openNativeScheduleSettings(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open OEM Settings UI (Backup)")
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
    return try {
        val dayMask = 127
        Settings.System.putString(cr, "power_off_alarm_time", offTime)
        Settings.System.putInt(cr, "power_off_alarm_enabled", 1)
        Settings.System.putInt(cr, "power_off_alarm_days", dayMask)
        Settings.Global.putInt(cr, "power_off_alarm_enabled", 1)
        
        Settings.System.putString(cr, "power_on_alarm_time", onTime)
        Settings.System.putInt(cr, "power_on_alarm_enabled", 1)
        Settings.System.putInt(cr, "power_on_alarm_days", dayMask)
        Settings.Global.putInt(cr, "power_on_alarm_enabled", 1)

        val actions = listOf(
            "com.mediatek.schpwronoff.CHANGE_SCHEDULE",
            "android.intent.action.set_pwr_on_off",
            "com.android.settings.action.SCHEDULE_POWER_ON_OFF_CHANGED",
            "com.miui.powercenter.SET_POWER_ON_OFF"
        )
        
        actions.forEach { action ->
            context.sendBroadcast(Intent(action))
        }
        
        true
    } catch (e: Exception) {
        android.util.Log.e("Reboxt", "Modification error", e)
        false
    }
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
