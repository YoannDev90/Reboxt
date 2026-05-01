package app.yoanndev.reboxt.ui

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.yoanndev.reboxt.data.Logger
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val logs = Logger.logs
    var pendingDelete by remember { mutableStateOf<Logger.LogEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Application Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val file = Logger.exportToFile(context)
                        if (file != null) {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                clipData = ClipData.newUri(context.contentResolver, "Reboxt Logs", uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export Logs"))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(
                    items = logs,
                    key = { entry -> "${entry.timestamp}-${entry.tag}-${entry.message.hashCode()}" }
                ) { entry ->
                    DismissibleLogItem(
                        entry = entry,
                        onDeleteRequested = { pendingDelete = entry }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }

            if (pendingDelete != null) {
                AlertDialog(
                    onDismissRequest = { pendingDelete = null },
                    title = { Text("Delete log?") },
                    text = { Text("This log entry will be deleted permanently.") },
                    confirmButton = {
                        TextButton(onClick = {
                            pendingDelete?.let { Logger.remove(it) }
                            pendingDelete = null
                        }) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDelete = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DismissibleLogItem(
    entry: Logger.LogEntry,
    onDeleteRequested: () -> Unit
) {
    var dragOffsetX by remember { mutableStateOf(0f) }
    var rowWidthPx by remember { mutableStateOf(1) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { rowWidthPx = it.width }
            .background(
                if (dragOffsetX != 0f) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
                .pointerInput(entry) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val threshold = rowWidthPx * 0.35f
                            if (abs(dragOffsetX) >= threshold) {
                                onDeleteRequested()
                            }
                            dragOffsetX = 0f
                        },
                        onDragCancel = {
                            dragOffsetX = 0f
                        }
                    ) { _, dragAmount ->
                        dragOffsetX = (dragOffsetX + dragAmount).coerceIn(-rowWidthPx.toFloat(), rowWidthPx.toFloat())
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    LogItem(entry)
                }
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun LogItem(entry: Logger.LogEntry) {
    val color = when (entry.level) {
        "ERROR" -> Color.Red
        "WARN" -> Color(0xFFFFA500)
        "DEBUG" -> Color.Gray
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row {
            Text(
                text = entry.formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = entry.level,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = entry.tag,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
