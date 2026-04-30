package app.yoanndev.reboxt.explorer

import android.content.ContentResolver
import android.provider.Settings

enum class SettingsNamespace {
    SYSTEM, SECURE, GLOBAL
}

data class SettingEntry(
    val namespace: SettingsNamespace,
    val key: String,
    val value: String?
)

class SettingsSnapshotRepository(private val contentResolver: ContentResolver) {

    fun takeSnapshot(): List<SettingEntry> {
        val snapshot = mutableListOf<SettingEntry>()
        
        // Note: Reading ALL settings via ContentResolver query on "content://settings/..." 
        // is the most efficient but might be restricted on newer Android versions.
        // Fallback or common method is to use Cursor on the settings URI.
        
        snapshot.addAll(readNamespace(SettingsNamespace.SYSTEM))
        snapshot.addAll(readNamespace(SettingsNamespace.SECURE))
        snapshot.addAll(readNamespace(SettingsNamespace.GLOBAL))
        
        return snapshot
    }

    private fun readNamespace(namespace: SettingsNamespace): List<SettingEntry> {
        val entries = mutableListOf<SettingEntry>()
        val uri = when (namespace) {
            SettingsNamespace.SYSTEM -> Settings.System.CONTENT_URI
            SettingsNamespace.SECURE -> Settings.Secure.CONTENT_URI
            SettingsNamespace.GLOBAL -> Settings.Global.CONTENT_URI
        }

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                val valueIndex = cursor.getColumnIndex("value")
                
                android.util.Log.d("SettingsSnapshot", "Reading namespace $namespace: found ${cursor.count} rows")

                if (nameIndex != -1 && valueIndex != -1) {
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameIndex)
                        val value = cursor.getString(valueIndex)
                        entries.add(SettingEntry(namespace, name, value))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsSnapshot", "Error reading $namespace", e)
        }
        
        return entries
    }
}
