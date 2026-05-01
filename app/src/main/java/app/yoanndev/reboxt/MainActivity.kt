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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.yoanndev.reboxt.data.Logger
import app.yoanndev.reboxt.data.AccessibilityEngine
import app.yoanndev.reboxt.data.DeviceDetector
import app.yoanndev.reboxt.data.ReboxtAccessibilityService
import app.yoanndev.reboxt.ui.SettingsMenu
import app.yoanndev.reboxt.ui.PermissionsDetailScreen
import app.yoanndev.reboxt.ui.CreditsScreen
import app.yoanndev.reboxt.ui.LogsScreen
import app.yoanndev.reboxt.ui.theme.ReboxtTheme
import java.util.Calendar
import java.util.Locale

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
                                "settings_logs" -> LogsScreen(onBack = { currentScreen = "settings" })
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
    var scheduleStatus by remember { mutableStateOf("Tap a time field to edit the schedule.") }
    var showOffPicker by remember { mutableStateOf(false) }
    var showOnPicker by remember { mutableStateOf(false) }

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

        TimeSelectorCard(
            label = "Power Off Time",
            value = offTime,
            onClick = { showOffPicker = true }
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        TimeSelectorCard(
            label = "Power On Time",
            value = onTime,
            onClick = { showOnPicker = true }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                Logger.i("Schedule", "Apply pressed with on=$onTime off=$offTime")
                val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
                val isServiceEnabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
                    .any { it.resolveInfo.serviceInfo.packageName == context.packageName }

                Logger.d("Schedule", "Accessibility enabled for app = $isServiceEnabled")

                if (!isServiceEnabled) {
                    Logger.w("Schedule", "Accessibility service is not enabled")
                    Toast.makeText(context, "Please enable Accessibility Service first!", Toast.LENGTH_LONG).show()
                    return@Button
                }

                AccessibilityEngine.loadSchemas(context)
                Logger.d("Schedule", "Schemas loaded from assets")
                val schema = AccessibilityEngine.findAndLoadBestSchema(context, deviceInfo)
                
                if (schema != null) {
                    Logger.i("Schedule", "Schema selected: ${schema.fileName} with ${schema.steps.size} steps")
                    val service = ReboxtAccessibilityService.instance
                    if (service != null) {
                        Logger.i("Schedule", "Accessibility service instance available, starting automation")
                        Toast.makeText(context, "Starting automation...", Toast.LENGTH_SHORT).show()
                        service.startAutomation(schema)
                        scheduleStatus = "Automation started for ${schema.fileName}"
                    } else {
                        Logger.e("Schedule", "Accessibility service is enabled but instance is null")
                        Toast.makeText(context, "Service is enabled but not active. Try toggling it.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Logger.e("Schedule", "No schema matched for device ${deviceInfo.manufacturer} / ${deviceInfo.skin.name}")
                    Toast.makeText(context, "No schema found for your device.", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply Schedule")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = scheduleStatus, style = MaterialTheme.typography.bodySmall)
    }

    if (showOffPicker) {
        AppTimePickerDialog(
            title = "Select Power Off Time",
            initialTime = offTime,
            onDismiss = { showOffPicker = false },
            onConfirm = { selectedTime ->
                offTime = selectedTime
                scheduleStatus = "Power off time set to $selectedTime"
                Logger.i("Schedule", scheduleStatus)
                showOffPicker = false
            }
        )
    }

    if (showOnPicker) {
        AppTimePickerDialog(
            title = "Select Power On Time",
            initialTime = onTime,
            onDismiss = { showOnPicker = false },
            onConfirm = { selectedTime ->
                onTime = selectedTime
                scheduleStatus = "Power on time set to $selectedTime"
                Logger.i("Schedule", scheduleStatus)
                showOnPicker = false
            }
        )
    }
}

@Composable
fun TimeSelectorCard(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun AppTimePickerDialog(
    title: String,
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val context = LocalContext.current
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    DisposableEffect(title, initialTime) {
        val dialog = android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                onConfirm(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute))
            },
            initialHour,
            initialMinute,
            true
        ).apply {
            setTitle(title)
            setOnDismissListener { onDismiss() }
        }

        dialog.show()
        onDispose { dialog.dismiss() }
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

