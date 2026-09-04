package dev.openrune.definition

interface Definition {
    var id: Int

    /**
     * Loose, codec-specific properties. An interface cannot hold state, so the default is a shared
     * immutable map: reads return null and writes throw. A type that needs [setExtraProperty] to
     * retain anything must override this with its own backing map - see
     * [dev.openrune.definition.type.NpcType].
     */
    val extra: MutableMap<String, Any?>
        get() = NO_EXTRA_PROPERTIES


    fun setExtraProperty(key: String, value: Any?) {
        extra[key] = value
    }

    fun Definition.getBooleanProperty(key: String): Boolean {
        return (extra[key] as? Boolean) ?: false
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

private val NO_EXTRA_PROPERTIES: MutableMap<String, Any?> =
    java.util.Collections.unmodifiableMap(mutableMapOf<String, Any?>())