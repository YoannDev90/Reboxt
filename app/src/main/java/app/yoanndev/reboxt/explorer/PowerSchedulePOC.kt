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
     * Schedules a "Power On" event at the specified epoch time using available elevated permissions.
     * 
     * @param epochMillis The desired wake-up time in milliseconds
     */
    fun schedulePowerOnWithElevated(epochMillis: Long): String {
        Log.d(TAG, "Attempting to schedule Power On via elevated shell at: $epochMillis")
        
        // Use 'am broadcast' because SET_POWER_ON_OFF is a receiver, or targets the power center directly.
        // Also, com.miui.powercenter.SET_POWER_ON_OFF is often used to trigger the schedule update.
        // We try both the service reset and the direct PowerCenter broadcast.
        
        val adbCmd = "am broadcast -a com.miui.powercenter.SET_POWER_ON_OFF --el boot_time $epochMillis --ei power_on_enabled 1"
        
        val result = ShellExecutor.exec(adbCmd)
        if (result.isSuccess) {
            return "Broadcast Success: ${result.output}"
        }

        // Fallback or secondary: Try direct service call with the older intent but as a broadcast if it was meant to be one
        val fallbackCmd = "am broadcast --user 0 -a $ACTION_RESET_BOOT_TIME --el $EXTRA_BOOT_TIME $epochMillis $PACKAGE_SECURITY_CENTER"
        val fallbackResult = ShellExecutor.exec(fallbackCmd)

        return if (fallbackResult.isSuccess) {
            Log.i(TAG, "Success via fallback: ${fallbackResult.output}")
            fallbackResult.output.ifEmpty { "Success (Fallback)" }
        } else {
            Log.e(TAG, "Error: ${fallbackResult.error}")
            "Error: ${fallbackResult.error}"
        }
    }

    /**
     * Checks if Shizuku is available and has permissions.
     * @deprecated Use ShellExecutor.isShizukuAvailable()
     */
    fun isShizukuReady(): Boolean = ShellExecutor.isShizukuAvailable()

    /**
     * Schedules a "Power On" event at the specified epoch time using Shizuku.
     * @deprecated Use schedulePowerOnWithElevated
     */
    fun schedulePowerOnWithShizuku(epochMillis: Long): String = schedulePowerOnWithElevated(epochMillis)

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
