package app.yoanndev.reboxt.explorer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingExplorerUI(
    repository: SettingsSnapshotRepository,
    diffEngine: DiffEngine
) {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf<List<SettingEntry>>(emptyList()) }
    var diffs by remember { mutableStateOf<List<SettingDiff>>(emptyList()) }
    var lastSnapshot by remember { mutableStateOf<List<SettingEntry>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                enabled = !isLoading,
                onClick = {
                    isLoading = true
                    val newSnapshot = repository.takeSnapshot()
                    if (lastSnapshot != null) {
                        diffs = diffEngine.compare(lastSnapshot!!, newSnapshot)
                    }
                    lastSnapshot = newSnapshot
                    snapshot = newSnapshot
                    isLoading = false
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (lastSnapshot == null) "Take First Snapshot" else "Compare with New Snapshot")
                }
            }
            
            if (lastSnapshot != null) {
                Button(onClick = { 
                    lastSnapshot = null
                    diffs = emptyList()
                    snapshot = emptyList()
                }) {
                    Text("Reset")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Text("Scanning all system settings... this may take a few seconds.")
        } else if (diffs.isNotEmpty()) {
            Text("${diffs.size} changes detected:", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(diffs) { diff ->
                    DiffItem(diff)
                }
            }
        } else if (lastSnapshot != null) {
            Text("Snapshot taken (${snapshot.size} keys).", color = MaterialTheme.colorScheme.primary)
            Text("1. Change a setting in HyperOS settings.")
            Text("2. Come back and click 'Compare' above.")
            
            if (snapshot.size < 10) {
                Text("Warning: Very few keys detected (${snapshot.size}). The app might lack permissions to read settings properly.", 
                    color = MaterialTheme.colorScheme.error)
            }
        } else {
            Text("Instructions:", style = MaterialTheme.typography.titleMedium)
            Text("This tool helps find which system setting key changes when you toggle an option in HyperOS.")
            Spacer(modifier = Modifier.height(8.dp))
            Text("1. Make sure the option you want to track is OFF.")
            Text("2. Click 'Take First Snapshot'.")
            Text("3. Go to System Settings and turn the option ON.")
            Text("4. Return here and click 'Compare'.")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("MIUI Power Hardware Bridge POC", style = MaterialTheme.typography.titleMedium)
        Text("Trigger internal power-on alarms using discovered intents.", style = MaterialTheme.typography.bodySmall)
        
        Button(
            modifier = Modifier.padding(top = 8.dp),
            onClick = {
                val targetTime = System.currentTimeMillis() + 3600000L
                PowerSchedulePOC.schedulePowerOn(context, targetTime)
            }
        ) {
            Text("Test Power On Schedule (In 1h)")
        }
    }
}

@Composable
fun DiffItem(diff: SettingDiff) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("${diff.namespace}: ${diff.key}", style = MaterialTheme.typography.labelLarge)
            Text("Old: ${diff.oldValue ?: "null"}", style = MaterialTheme.typography.bodySmall)
            Text("New: ${diff.newValue ?: "null"}", color = MaterialTheme.colorScheme.primary)
            Text("Estimated Type: ${diff.type}", style = MaterialTheme.typography.labelSmall)
        }
    }
}
