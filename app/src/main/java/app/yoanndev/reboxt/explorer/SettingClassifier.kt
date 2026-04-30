package app.yoanndev.reboxt.explorer

enum class SettingType {
    BOOLEAN, INTEGER, FLOAT, STRING_STRUCTURED, STRING
}

class SettingClassifier {
    fun inferType(value: String?): SettingType {
        if (value == null || value.isEmpty()) return SettingType.STRING
        
        // Boolean check
        if (value.equals("true", ignoreCase = true) || 
            value.equals("false", ignoreCase = true) ||
            value == "0" || value == "1" ||
            value.equals("enabled", ignoreCase = true) || 
            value.equals("disabled", ignoreCase = true)) {
            return SettingType.BOOLEAN
        }
        
        // Integer check
        if (value.toLongOrNull() != null) {
            return SettingType.INTEGER
        }
        
        // Float check
        if (value.toFloatOrNull() != null && value.contains(".")) {
            return SettingType.FLOAT
        }
        
        // Structured string check (JSON or CSV-like)
        if ((value.startsWith("{") && value.endsWith("}")) || 
            (value.startsWith("[") && value.endsWith("]")) ||
            value.contains(",") || value.contains(":") || value.contains("|")) {
            return SettingType.STRING_STRUCTURED
        }
        
        return SettingType.STRING
    }
}
