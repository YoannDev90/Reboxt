package app.yoanndev.reboxt.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.yoanndev.reboxt.data.DeviceDetector
import app.yoanndev.reboxt.data.DeviceInfo

@Composable
fun SettingsMenu(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val deviceInfo = remember { DeviceDetector.detect() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        MenuItem(
            title = "Permissions",
            subtitle = "Manage Admin, Accessibility and Notifications",
            icon = Icons.Default.Security,
            onClick = { onNavigate("settings_permissions") }
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        MenuItem(
            title = "Credits",
            subtitle = "Art and dependencies credits",
            icon = Icons.Default.Info,
            onClick = { onNavigate("settings_credits") }
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Device Info", style = MaterialTheme.typography.titleMedium)
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Manufacturer: ${deviceInfo.manufacturer}", style = MaterialTheme.typography.bodySmall)
                Text("Model: ${deviceInfo.model}", style = MaterialTheme.typography.bodySmall)
                Text("Android: ${deviceInfo.androidVersion} (SDK ${deviceInfo.sdkInt})", style = MaterialTheme.typography.bodySmall)
                Text("Skin: ${deviceInfo.skin.name} ${deviceInfo.skin.version}", style = MaterialTheme.typography.bodySmall)
                
                val supported = isDeviceSupported(deviceInfo)
                Text(
                    text = if (supported) "Supported" else "Unsupported",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (supported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

fun isDeviceSupported(deviceInfo: DeviceInfo): Boolean {
    // These should ideally match the "skin" values in DeviceDetector.kt and accessibility_schemas.json
    val supportedSkins = listOf("MIUI/HyperOS", "OxygenOS", "OneUI")
    return supportedSkins.contains(deviceInfo.skin.name)
}

@Composable
fun MenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}
