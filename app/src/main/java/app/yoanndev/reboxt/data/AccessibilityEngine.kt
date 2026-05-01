package app.yoanndev.reboxt.data

import android.content.Context
import org.json.JSONObject

data class AccessibilityStep(
    val type: String,
    val value: String,
    val action: String
)

data class AccessibilitySchema(
    val fileName: String,
    val manufacturer: String?,
    val skin: String?,
    var steps: List<AccessibilityStep> = emptyList()
)

object AccessibilityEngine {
    private var schemaEntries: List<AccessibilitySchema> = emptyList()

    fun loadSchemas(context: Context) {
        try {
            val jsonString = context.assets.open("accessibility_schemas.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val array = root.getJSONArray("schemas")
            val loaded = mutableListOf<AccessibilitySchema>()
            
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val match = obj.getJSONObject("match")
                
                loaded.add(AccessibilitySchema(
                    obj.getString("file"),
                    match.optString("manufacturer", null),
                    match.optString("skin", null)
                ))
            }
            schemaEntries = loaded
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun findAndLoadBestSchema(context: Context, deviceInfo: DeviceInfo): AccessibilitySchema? {
        val entry = schemaEntries.find { schema ->
            (schema.manufacturer == null || schema.manufacturer.equals(deviceInfo.manufacturer, ignoreCase = true)) &&
            (schema.skin == null || schema.skin.equals(deviceInfo.skin.name, ignoreCase = true))
        } ?: return null

        return try {
            val jsonString = context.assets.open("schemas/${entry.fileName}").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val stepsArray = root.getJSONArray("steps")
            val steps = mutableListOf<AccessibilityStep>()
            
            for (j in 0 until stepsArray.length()) {
                val stepObj = stepsArray.getJSONObject(j)
                steps.add(AccessibilityStep(
                    stepObj.getString("type"),
                    stepObj.getString("value"),
                    stepObj.getString("action")
                ))
            }
            entry.copy(steps = steps)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
