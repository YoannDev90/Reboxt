package app.yoanndev.reboxt.explorer

import android.util.Log
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
            if (!Shizuku.pingBinder()) return false
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
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
        return try {
            val process = Shizuku.newProcess(command.split(" ").toTypedArray(), null, null)
            val output = InputStreamReader(process.inputStream).readText()
            val error = InputStreamReader(process.errorStream).readText()
            val exitCode = process.waitFor()
            ShellResult(exitCode, output, error)
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku execution failed", e)
            ShellResult(-1, "", e.message ?: "Unknown Shizuku error")
        }
    }

    private fun execRoot(command: String): ShellResult {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            
            val output = InputStreamReader(process.inputStream).readText()
            val error = InputStreamReader(process.errorStream).readText()
            val exitCode = process.waitFor()
            ShellResult(exitCode, output, error)
        } catch (e: Exception) {
            Log.e(TAG, "Root execution failed", e)
            ShellResult(-1, "", e.message ?: "Unknown Root error")
        }
    }

    data class ShellResult(val exitCode: Int, val output: String, val error: String) {
        val isSuccess get() = exitCode == 0
    }
}
