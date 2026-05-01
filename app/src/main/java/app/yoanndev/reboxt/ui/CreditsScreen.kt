package app.yoanndev.reboxt.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Credits") },
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
            Text("Reboxt", style = MaterialTheme.typography.headlineSmall)
            Text("A Native Power Scheduler for Android devices.", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Contributors", style = MaterialTheme.typography.titleMedium)
            Text("- YoannDev", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(16.dp))

            Text("Icon", style = MaterialTheme.typography.titleMedium)
            Text("App icon created by Freepik - Flaticon", style = MaterialTheme.typography.bodySmall)
            Text("License: Flaticon License", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Dependencies", style = MaterialTheme.typography.titleMedium)
            Text("- Jetpack Compose", style = MaterialTheme.typography.bodySmall)
            Text("- Material3", style = MaterialTheme.typography.bodySmall)
        }
    }
}
