package app.yoanndev.reboxt.explorer

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings

class SettingWriter(private val context: Context) {
    
    fun writeSetting(namespace: SettingsNamespace, key: String, value: String): Boolean {
        return try {
            when (namespace) {
                SettingsNamespace.SYSTEM -> {
                    if (Settings.System.canWrite(context)) {
                        Settings.System.putString(context.contentResolver, key, value)
                    } else false
                }
                SettingsNamespace.SECURE -> {
                    Settings.Secure.putString(context.contentResolver, key, value)
                }
                SettingsNamespace.GLOBAL -> {
                    Settings.Global.putString(context.contentResolver, key, value)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
