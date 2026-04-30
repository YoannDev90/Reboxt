package app.yoanndev.reboxt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import app.yoanndev.reboxt.explorer.ShellExecutor

class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "app.yoanndev.reboxt.ACTION_REBOOT") {
            Log.d("Reboxt", "Reboot signal received")
            
            // Try elevated reboot first
            val shellResult = ShellExecutor.exec("reboot")
            if (shellResult.isSuccess) {
                Log.i("Reboxt", "Reboot initiated via Shell")
                return
            }

            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            try {
                // Note: This requires the app to be a system app or have root access on standard Android.
                pm.reboot(null)
            } catch (e: Exception) {
                Log.e("Reboxt", "Reboot failed: ${e.message}. Note: Root or Shizuku permissions are required for direct reboot.")
            }
        }
    }
}
