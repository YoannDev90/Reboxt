package app.yoanndev.reboxt.explorer

import app.yoanndev.reboxt.data.Logger
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object ShellExecutor {
    private const val TAG = "ShellExecutor"

    enum class Mode {
        ROOT, SHIZUKU, NONE
    }

    fun getAvailableMode(): Mode {
        if (isShizukuAvailable()) return Mode.SHIZUKU
        if (isRootAvailable()) return Mode.ROOT
        return Mode.NONE
    }

    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su -c id")
            val output = BufferedReader(InputStreamReader(process.inputStream)).readLine()
            process.destroy()
            output != null && output.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            if (!Shizuku.pingBinder()) {
                Logger.d(TAG, "Shizuku Binder not reachable")
                return false
            }
            val granted = Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            Logger.d(TAG, "Shizuku Permission Status: $granted")
            granted
        } catch (e: Exception) {
            Logger.e(TAG, "Shizuku availability check failed", e)
            false
        }
    }

    /**
     * Checks if Shizuku is installed and running, regardless of permission.
     */
    fun isShizukuInstalledAndRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun exec(command: String): ShellResult {
        val mode = getAvailableMode()
        return when (mode) {
            Mode.SHIZUKU -> execShizuku(command)
            Mode.ROOT -> execRoot(command)
            Mode.NONE -> ShellResult(-1, "", "No elevated permissions (Root or Shizuku) available")
        }
    }

    private fun execShizuku(command: String): ShellResult {
        Logger.d(TAG, "Exec Shizuku: $command")
        return try {
            // Use 'sh' to execute the full command string, and wrap with '--user 0' or similar if it's a settings call
            // Many system settings require --user 0 to be visible to the system
            val cmd = if (command.startsWith("settings") && !command.contains("--user")) {
                command.replaceFirst("settings", "settings --user 0")
            } else {
                command
            }
            
            val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
            val output = InputStreamReader(process.inputStream).readText()
            val error = InputStreamReader(process.errorStream).readText()
            val exitCode = process.waitFor()
            Logger.d(TAG, "Results - Exit: $exitCode, Out: ${output.take(50)}, Err: ${error.take(50)}")
            ShellResult(exitCode, output, error)
        } catch (e: Exception) {
            Logger.e(TAG, "Shizuku execution failed", e)
            ShellResult(-1, "", e.message ?: "Unknown Shizuku error")
        }
    }

    private fun execRoot(command: String): ShellResult {
        Logger.d(TAG, "Exec Root: $command")
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            
            val output = InputStreamReader(process.inputStream).readText()
            val error = InputStreamReader(process.errorStream).readText()
            val exitCode = process.waitFor()
            Logger.d(TAG, "Results - Exit: $exitCode, Out: ${output.take(50)}, Err: ${error.take(50)}")
            ShellResult(exitCode, output, error)
        } catch (e: Exception) {
            Logger.e(TAG, "Root execution failed", e)
            ShellResult(-1, "", e.message ?: "Unknown Root error")
        }
    }

    data class ShellResult(val exitCode: Int, val output: String, val error: String) {
        val isSuccess get() = exitCode == 0
    }
}
