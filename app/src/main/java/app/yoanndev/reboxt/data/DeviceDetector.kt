package app.yoanndev.reboxt.data

import android.os.Build
import java.io.BufferedReader
import java.io.InputStreamReader

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val skin: SkinInfo
)

data class SkinInfo(
    val name: String,
    val version: String
)

object DeviceDetector {
    fun detect(): DeviceInfo {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val androidVersion = Build.VERSION.RELEASE
        val sdkInt = Build.VERSION.SDK_INT
        
        val skin = when {
            isMiui() -> SkinInfo("MIUI/HyperOS", getSystemProperty("ro.miui.ui.version.name"))
            isEmui() -> SkinInfo("EMUI", getSystemProperty("ro.build.version.emui"))
            isColorOs() -> SkinInfo("ColorOS", getSystemProperty("ro.build.version.opporom"))
            isOneUi() -> SkinInfo("OneUI", getOneUiVersion())
            isOxygenOs() -> SkinInfo("OxygenOS", getSystemProperty("ro.oxygen.version"))
            else -> SkinInfo("Stock/Unknown", "N/A")
        }
        
        return DeviceInfo(manufacturer, model, androidVersion, sdkInt, skin)
    }

    private fun isMiui() = getSystemProperty("ro.miui.ui.version.name").isNotEmpty()
    private fun isEmui() = getSystemProperty("ro.build.version.emui").isNotEmpty()
    private fun isColorOs() = getSystemProperty("ro.build.version.opporom").isNotEmpty()
    private fun isOxygenOs() = getSystemProperty("ro.oxygen.version").isNotEmpty()
    private fun isOneUi(): Boolean {
        // Samsung Check
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true) && getOneUiVersion().isNotEmpty()
    }

    private fun getOneUiVersion(): String {
        return getSystemProperty("ro.build.version.sep") // Samsung Experience Platform
    }

    private fun getSystemProperty(key: String): String {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val value = reader.readLine()
            reader.close()
            value ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
