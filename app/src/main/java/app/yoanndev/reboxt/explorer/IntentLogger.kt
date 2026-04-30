package app.yoanndev.reboxt.explorer

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Service to capture and log Intents sent by the system or other apps.
 * This is a helper for reverse-engineering.
 */
object IntentLogger {
    private const val TAG = "IntentLogger"

    fun logIntent(intent: Intent?) {
        if (intent == null) return
        
        val sb = StringBuilder()
        sb.append("Action: ").append(intent.action).append("\n")
        sb.append("Component: ").append(intent.component).append("\n")
        sb.append("Flags: ").append(Integer.toHexString(intent.flags)).append("\n")
        
        intent.extras?.let { extras ->
            sb.append("Extras: {\n")
            for (key in extras.keySet()) {
                val value = extras.get(key)
                sb.append("  ").append(key).append(" (").append(value?.javaClass?.simpleName ?: "null").append("): ").append(value).append("\n")
            }
            sb.append("}")
        }
        
        Log.d(TAG, sb.toString())
    }
}
