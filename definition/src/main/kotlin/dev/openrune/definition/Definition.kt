package dev.openrune.definition

interface Definition {
    var id: Int

    val extra: MutableMap<String, Any?>
        get() = mutableMapOf()


    fun setExtraProperty(key: String, value: Any?) {
        extra[key] = value
    }

    fun Definition.getBooleanProperty(key: String): Boolean {
        return key == "true"
    }

    fun Definition.getIntArray2DProperty(key: String): Array<IntArray?> {
        return (extra[key] as? Array<IntArray?>) ?: emptyArray()
    }

    fun Definition.getIntProperty(key: String): Int {
        return (extra[key] as? Int) ?: -1
    }

    fun Definition.getStringProperty(key: String): String {
        return (extra[key] as? String) ?: ""
    }

    fun Definition.getIntArrayProperty(key: String): IntArray {
        return (extra[key] as? IntArray) ?: intArrayOf()
    }

    fun <T> getExtraProperty(key: String): T? {
        return extra[key] as? T
    }

}