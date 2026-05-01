package app.yoanndev.reboxt

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.yoanndev.reboxt.data.Logger
import app.yoanndev.reboxt.data.AccessibilityEngine
import app.yoanndev.reboxt.data.DeviceDetector
import app.yoanndev.reboxt.data.ReboxtAccessibilityService
import app.yoanndev.reboxt.ui.SettingsMenu
import app.yoanndev.reboxt.ui.PermissionsDetailScreen
import app.yoanndev.reboxt.ui.CreditsScreen
import app.yoanndev.reboxt.ui.theme.ReboxtTheme
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.init(this)
        AccessibilityEngine.loadSchemas(this)

        setContent {
            ReboxtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("scheduler") }
                    
                    BackHandler(enabled = currentScreen != "scheduler") {
                        currentScreen = "scheduler"
                    }

                    Column {
                        Box(modifier = Modifier.weight(1f)) {
                            when (currentScreen) {
                                "scheduler" -> PowerSchedulerScreen()
                                "settings" -> SettingsMenu(onNavigate = { currentScreen = it })
                                "settings_permissions" -> PermissionsDetailScreen(onBack = { currentScreen = "settings" })
                                "settings_credits" -> CreditsScreen(onBack = { currentScreen = "settings" })
                            }
                        }
                        
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Refresh, "Schedule") },
                                label = { Text("Schedule") },
                                selected = currentScreen == "scheduler",
                                onClick = { currentScreen = "scheduler" }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Shield, "Settings") },
                                label = { Text("Settings") },
                                selected = currentScreen == "settings",
                                onClick = { currentScreen = "settings" }
                            )
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
    
    val deviceInfo = remember { DeviceDetector.detect() }
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
        Text(text = "Native Power Scheduler", style = MaterialTheme.typography.bodyMedium)

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
                val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
                val isServiceEnabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
                    .any { it.resolveInfo.serviceInfo.packageName == context.packageName }

                if (!isServiceEnabled) {
                    Toast.makeText(context, "Please enable Accessibility Service first!", Toast.LENGTH_LONG).show()
                    return@Button
                }

                AccessibilityEngine.loadSchemas(context)
                val schema = AccessibilityEngine.findAndLoadBestSchema(context, deviceInfo)
                
                if (schema != null) {
                    val service = ReboxtAccessibilityService.instance
                    if (service != null) {
                        Toast.makeText(context, "Starting automation...", Toast.LENGTH_SHORT).show()
                        service.startAutomation(schema)
                    } else {
                        Toast.makeText(context, "Service is enabled but not active. Try toggling it.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "No schema found for your device.", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply Schedule")
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
        true
    } catch (e: Exception) {
        android.util.Log.w("Reboxt", "Standard settings modification failed: ${e.message}")
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

