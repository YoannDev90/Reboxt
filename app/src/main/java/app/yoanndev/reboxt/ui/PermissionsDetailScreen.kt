package app.yoanndev.reboxt.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.yoanndev.reboxt.data.Logger
import app.yoanndev.reboxt.explorer.ShellExecutor
import rikka.shizuku.Shizuku

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsDetailScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var writeSettingsGranted by remember { mutableStateOf(Settings.System.canWrite(context)) }
    var secureSettingsGranted by remember { 
        mutableStateOf(context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) 
    }
    var shizukuGranted by remember { mutableStateOf(ShellExecutor.isShizukuAvailable()) }
    var shizukuRunning by remember { mutableStateOf(ShellExecutor.isShizukuInstalledAndRunning()) }
    var rootAvailable by remember { mutableStateOf(ShellExecutor.isRootAvailable()) }

    // Listener for Shizuku permission result
    val permissionListener = remember {
        Shizuku.OnRequestPermissionResultListener { _, result ->
            shizukuGranted = result == PackageManager.PERMISSION_GRANTED
            Logger.i("Shizuku", "Permission result: $shizukuGranted")
        }
    }

    DisposableEffect(Unit) {
        Shizuku.addRequestPermissionResultListener(permissionListener)
        onDispose {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
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
                },
                actions = {
                    IconButton(onClick = {
                        writeSettingsGranted = Settings.System.canWrite(context)
                        secureSettingsGranted = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
                        shizukuGranted = ShellExecutor.isShizukuAvailable()
                        shizukuRunning = ShellExecutor.isShizukuInstalledAndRunning()
                        rootAvailable = ShellExecutor.isRootAvailable()
                        Logger.i("UI", "Status Refreshed")
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Access Methods", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))

            PermissionItem(
                title = "Write System Settings",
                description = "Required for basic schedule features",
                isGranted = writeSettingsGranted,
                onAction = {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            )

            PermissionItem(
                title = "Shizuku",
                description = if (shizukuRunning) "Service is running" else "Service NOT detected",
                isGranted = shizukuGranted,
                onAction = {
                    if (shizukuRunning) {
                        try {
                            Logger.i("UI", "Triggering Shizuku permission request...")
                            Shizuku.requestPermission(1001)
                        } catch (e: Exception) {
                            Logger.e("UI", "Failed to request Shizuku permission", e)
                        }
                    } else {
                        val intent = context.packageManager.getLaunchIntentForPackage("rikka.shizuku")
                        if (intent != null) context.startActivity(intent)
                        else Logger.e("UI", "Shizuku app not found")
                    }
                },
                actionLabel = if (shizukuGranted) "Granted" else if (shizukuRunning) "Grant Permission" else "Open Shizuku"
            )

            PermissionItem(
                title = "Root Access",
                description = "Direct system execution (su)",
                isGranted = rootAvailable,
                onAction = { 
                    rootAvailable = ShellExecutor.isRootAvailable()
                    Logger.i("UI", "Root check: $rootAvailable")
                },
                actionLabel = "Check Root"
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Secure Settings info", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Currently: ${if(secureSettingsGranted) "Granted" else "Missing"}\n\n" +
                        "To grant via ADB:\nadb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onAction: (() -> Unit)?,
    actionLabel: String = "Fix"
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                             else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall)
                Text(
                    if (isGranted) "Status: GRANTED" else "Status: MISSING",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            if (onAction != null && (!isGranted || actionLabel.startsWith("Check") || actionLabel.contains("Permission"))) {
                Button(onClick = onAction, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
