package dev.openrune

// Getters with defaults
fun Map<String, Any>.getAsString(key: String, default: String = "null"): String = this.getOrDefault(key, default) as String
fun Map<String, Any>.getAsInt(key: String, default: Int = -1): Int = this.getOrDefault(key, default) as Int
fun Map<String, Any>.getAsBoolean(key: String, default: Boolean = true): Boolean = this.getOrDefault(key, default) as Boolean
fun Map<String, Any>.getAsListInt(key: String): List<Int>? = this[key] as? List<Int>
fun Map<String, Any>.getAsListString(key: String): List<String?>? = this[key] as? List<String?>
fun Map<String, Any>.getAsListBoolean(key: String): List<Boolean>? = this[key] as? List<Boolean>

// Setters for immutable Map
fun MutableMap<String, Any>.withString(key: String, value: String): MutableMap<String, Any> {
    this[key] = value
    return this
}

fun MutableMap<String, Any>.withInt(key: String, value: Int): MutableMap<String, Any> {
    this[key] = value
    return this
}

fun MutableMap<String, Any>.withBoolean(key: String, value: Boolean): MutableMap<String, Any> {
    this[key] = value
    return this
}

fun MutableMap<String, Any>.withListAny(key: String, value: List<Any>): MutableMap<String, Any> {
    this[key] = value
    return this
}

fun MutableMap<String, Any>.withListInt(key: String, value: List<Int>): MutableMap<String, Any> {
    this[key] = value
    return this
}

fun MutableMap<String, Any>.withListBoolean(key: String, value: List<Boolean>): MutableMap<String, Any> {
    this[key] = value
    return this
}

fun MutableMap<String, Any>.withListString(key: String, value: List<String?>): MutableMap<String, Any> {
    this[key] = value
    return this
}

// Generalized list setter function (supports String?, Int, Boolean)
fun <T> MutableMap<String, Any>.setListValue(key: String, size: Int, value: T, defaultSize: Int) {
    val list = getOrCreateList(key, value, defaultSize)
    list[size] = value
    setUpdatedList(key, value, list)
}

// Create or get existing list, depending on value type
fun <T> MutableMap<String, Any>.getOrCreateList(key: String, value: T, defaultSize: Int): MutableList<Any?> {
    return when (value) {
        is String? -> getAsListString(key)?.toMutableList() ?: MutableList(defaultSize) { null }
        is Int -> getAsListInt(key)?.toMutableList() ?: MutableList(defaultSize) { 0 }
        is Boolean -> getAsListBoolean(key)?.toMutableList() ?: MutableList(defaultSize) { true }
        else -> throw IllegalArgumentException("Unsupported type: ${value!!::class}")
    }
}

// Update the map with the new list, depending on value type
fun <T> MutableMap<String, Any>.setUpdatedList(key: String, value: T, list: MutableList<Any?>) {
    when (value) {
        is String? -> this.withListString(key, list as List<String?>)
        is Int -> this.withListInt(key, list as List<Int>)
        is Boolean -> this.withListBoolean(key, list as List<Boolean>)
        else -> throw IllegalArgumentException("Unsupported type: ${value!!::class}")
    }
}
