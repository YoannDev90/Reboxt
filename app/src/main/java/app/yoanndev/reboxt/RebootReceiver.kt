package app.yoanndev.reboxt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "app.yoanndev.reboxt.ACTION_REBOOT") {
            Log.d("Reboxt", "Reboot signal received")
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            try {
                // Note: This requires the app to be a system app or have root access on standard Android.
                // For native "Auto-restart" functions, they are usually hidden in vendor-specific settings (Samsung, Xiaomi, etc.)
                // This app simulates the intent-based trigger if available or provides a way to open system settings.
                pm.reboot(null)
            } catch (e: Exception) {
                Log.e("Reboxt", "Reboot failed: ${e.message}. Note: Root or System permissions are required for direct reboot.")
            }
        }
    }
}
