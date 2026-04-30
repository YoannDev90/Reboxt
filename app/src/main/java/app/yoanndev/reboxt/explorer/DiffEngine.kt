package app.yoanndev.reboxt.explorer

data class SettingDiff(
    val namespace: SettingsNamespace,
    val key: String,
    val oldValue: String?,
    val newValue: String?,
    val type: SettingType
)

class DiffEngine(private val classifier: SettingClassifier) {
    fun compare(oldSnapshot: List<SettingEntry>, newSnapshot: List<SettingEntry>): List<SettingDiff> {
        val oldMap = oldSnapshot.associateBy { "${it.namespace}_${it.key}" }
        val newMap = newSnapshot.associateBy { "${it.namespace}_${it.key}" }
        
        val diffs = mutableListOf<SettingDiff>()
        
        // Find changed or added
        newMap.forEach { (uniqueKey, newEntry) ->
            val oldEntry = oldMap[uniqueKey]
            if (oldEntry == null || oldEntry.value != newEntry.value) {
                diffs.add(
                    SettingDiff(
                        namespace = newEntry.namespace,
                        key = newEntry.key,
                        oldValue = oldEntry?.value,
                        newValue = newEntry.value,
                        type = classifier.inferType(newEntry.value)
                    )
                )
            }
        }
        
        // Find deleted (optional, but good for completeness)
        oldMap.forEach { (uniqueKey, oldEntry) ->
            if (!newMap.containsKey(uniqueKey)) {
                diffs.add(
                    SettingDiff(
                        namespace = oldEntry.namespace,
                        key = oldEntry.key,
                        oldValue = oldEntry.value,
                        newValue = null,
                        type = classifier.inferType(oldEntry.value)
                    )
                )
            }
        }
        
        return diffs
    }
}
