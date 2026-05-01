package app.yoanndev.reboxt.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Logger {
    private const val TAG = "ReboxtLogger"
    private const val MAX_LOGS = 1000
    private var logFile: File? = null
    private val _logs = mutableStateListOf<LogEntry>()
    val logs: List<LogEntry> get() = _logs

    data class LogEntry(
        val timestamp: Long,
        val level: String,
        val tag: String,
        val message: String
    ) {
        val formattedTime: String
            get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        
        override fun toString() = "[$formattedTime] $level/$tag: $message"
    }

    fun i(tag: String, msg: String) = addLog("INFO", tag, msg)
    fun d(tag: String, msg: String) = addLog("DEBUG", tag, msg)
    fun e(tag: String, msg: String, tr: Throwable? = null) = addLog("ERROR", tag, msg + (tr?.let { "\n${it.stackTraceToString()}" } ?: ""))

    fun init(context: Context) {
        logFile = File(context.filesDir, "app_logs.txt")
        loadLogsFromFile()
    }

    private fun loadLogsFromFile() {
        if (logFile?.exists() == true) {
            try {
                val lines = logFile!!.readLines().takeLast(MAX_LOGS)
                lines.forEach { line ->
                    parseLogLine(line)?.let { _logs.add(it) } // Add to end (we show reversed in UI or use reversed)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load logs", e)
            }
        }
    }

    private fun parseLogLine(line: String): LogEntry? {
        val regex = Regex("""\[(\d{2}:\d{2}:\d{2})\] (\w+)/([^:]+): (.*)""")
        val match = regex.find(line) ?: return null
        
        val (timeStr, level, tag, message) = match.destructured
        return try {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(timeStr) ?: Date()
            val cal = Calendar.getInstance()
            val timeCal = Calendar.getInstance().apply { time = date }
            cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
            cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
            cal.set(Calendar.SECOND, timeCal.get(Calendar.SECOND))
            LogEntry(cal.timeInMillis, level, tag, message)
        } catch (e: Exception) {
            null
        }
    }

    private fun addLog(level: String, tag: String, message: String) {
        Log.println(when(level) {
            "ERROR" -> Log.ERROR
            "DEBUG" -> Log.DEBUG
            else -> Log.INFO
        }, tag, message)
        
        val entry = LogEntry(System.currentTimeMillis(), level, tag, message)
        _logs.add(0, entry)
        if (_logs.size > MAX_LOGS) _logs.removeLast()
        
        saveLogToFile(entry)
    }

    private fun saveLogToFile(entry: LogEntry) {
        try {
            logFile?.appendText(entry.toString() + "\n")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save log to file", e)
        }
    }

    fun exportToFile(context: Context): File? {
        return try {
            val fileName = "reboxt_logs_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            val file = File(context.cacheDir, fileName)
            file.writeText(_logs.reversed().joinToString("\n") { it.toString() })
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export logs", e)
            null
        }
    }
}
