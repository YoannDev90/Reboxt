package app.yoanndev.reboxt.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.unit.dp
import app.yoanndev.reboxt.RebootReceiver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsDetailScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var isAdminEnabled by remember { mutableStateOf(checkAdminStatus(context)) }
    var isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var isNotificationEnabled by remember { mutableStateOf(areNotificationsEnabled(context)) }

    // Update states when returning to foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAdminEnabled = checkAdminStatus(context)
                isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
                isNotificationEnabled = areNotificationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            PermissionToggle(
                title = "Device Administrator",
                description = "Prevents MIUI from killing the app process.",
                enabled = isAdminEnabled,
                onToggle = {
                    if (!isAdminEnabled) {
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(context, RebootReceiver::class.java))
                            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for app persistence.")
                        }
                        context.startActivity(intent)
                    } else {
                        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                        dpm.removeActiveAdmin(ComponentName(context, RebootReceiver::class.java))
                        isAdminEnabled = false
                    }
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            PermissionToggle(
                title = "Accessibility Service",
                description = "Used to automate the native power schedule UI.",
                enabled = isAccessibilityEnabled,
                onToggle = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            PermissionToggle(
                title = "Notifications",
                description = "Show status of the scheduler.",
                enabled = isNotificationEnabled,
                onToggle = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun PermissionToggle(
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = enabled, onCheckedChange = { onToggle() })
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
    return enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
}

private fun checkAdminStatus(context: Context): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val adminName = ComponentName(context, RebootReceiver::class.java)
    return dpm.isAdminActive(adminName)
}

private fun areNotificationsEnabled(context: Context): Boolean {
    return androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
}
