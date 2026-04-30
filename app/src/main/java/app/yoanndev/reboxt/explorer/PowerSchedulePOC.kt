package app.yoanndev.reboxt.explorer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import rikka.shizuku.Shizuku
import java.io.InputStreamReader

/**
 * Utility to schedule Power On/Off events on Xiaomi/HyperOS devices.
 * Uses Shizuku to bypass Permission Denial for non-exported system services.
 */
object PowerSchedulePOC {
    private const val TAG = "PowerSchedulePOC"
    
    // MIUI SecurityCenter / PowerCenter constants
    private const val PACKAGE_SECURITY_CENTER = "com.miui.securitycenter"
    private const val SERVICE_BOOT_ALARM = "com.miui.powercenter.bootshutdown.BootAlarmIntentService"
    private const val ACTION_RESET_BOOT_TIME = "com.miui.powercenter.RESET_BOOT_TIME"
    private const val EXTRA_BOOT_TIME = "boot_time"

    /**
     * Checks if Shizuku is available and has permissions.
     */
    fun isShizukuReady(): Boolean {
        if (!Shizuku.pingBinder()) return false
        return Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Schedules a "Power On" event at the specified epoch time using Shizuku.
     * 
     * @param epochMillis The desired wake-up time in milliseconds
     */
    fun schedulePowerOnWithShizuku(epochMillis: Long): String {
        Log.d(TAG, "Attempting to schedule Power On via Shizuku at: $epochMillis")
        
        if (!isShizukuReady()) {
            return "Error: Shizuku not ready or permission denied"
        }

        val adbCmd = "am start-service -a $ACTION_RESET_BOOT_TIME --el $EXTRA_BOOT_TIME $epochMillis $PACKAGE_SECURITY_CENTER/$SERVICE_BOOT_ALARM"
        
        return try {
            val process = Shizuku.newProcess(adbCmd.split(" ").toTypedArray(), null, null)
            val output = InputStreamReader(process.inputStream).readText()
            val error = InputStreamReader(process.errorStream).readText()
            process.waitFor()
            
            if (error.isNotEmpty()) {
                Log.e(TAG, "Shizuku error: $error")
                "Error: $error"
            } else {
                Log.i(TAG, "Shizuku result: $output")
                output
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute Shizuku command", e)
            "Exception: ${e.message}"
        }
    }

    /**
     * Legacy method for direct service call (will fail on non-root/non-system).
     */
    fun schedulePowerOn(context: Context, epochMillis: Long) {
        Log.w(TAG, "Standard startService call triggered (likely to fail without system UID)")
        try {
            val intent = Intent(ACTION_RESET_BOOT_TIME).apply {
                component = ComponentName(PACKAGE_SECURITY_CENTER, SERVICE_BOOT_ALARM)
                putExtra(EXTRA_BOOT_TIME, epochMillis)
            }
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send Power On intent: ${e.message}")
        }
    }

    fun getAdbCommand(epochMillis: Long): String {
        return "adb shell am start-service -a $ACTION_RESET_BOOT_TIME --el $EXTRA_BOOT_TIME $epochMillis $PACKAGE_SECURITY_CENTER/$SERVICE_BOOT_ALARM"
    }
}
