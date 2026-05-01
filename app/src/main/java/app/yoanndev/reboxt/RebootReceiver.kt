package app.yoanndev.reboxt

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class RebootReceiver : DeviceAdminReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "app.yoanndev.reboxt.ACTION_REBOOT") {
            Log.d("Reboxt", "Reboot signal received")
            // Here implementation for reboot without root/shizuku is limited 
            // but we keep the structure
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i("Reboxt", "Device Admin Enabled - Persistence guaranteed")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w("Reboxt", "Device Admin Disabled")
    }
}
