package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

import dev.openrune.definition.util.CacheVarLiteral

class EnumTypeBuilder(var id: Int = -1) {

    var keyType: CacheVarLiteral = CacheVarLiteral.INT
    var valueType: CacheVarLiteral = CacheVarLiteral.INT
    var defaultInt: Int = 0
    var defaultLong: Long = 0L
    var defaultString: String = ""
    val values: MutableMap<Int, Any> = LinkedHashMap()
    var dense: Boolean = false
    var capacity: Int? = null
    var typesFromId: Boolean = false
    var pluginApi: Boolean = true

    fun build(): EnumType = EnumType(
        id = id,
        keyType = keyType,
        valueType = valueType,
        defaultInt = defaultInt,
        defaultLong = defaultLong,
        defaultString = defaultString,
        values = values,
    ).also { it.rs2 = rs2OrNull() }

    private fun rs2OrNull(): EnumType.Rs2? =
        EnumType.Rs2(dense, capacity, typesFromId, pluginApi)
            .takeIf { it != DEFAULT_RS2 }

    companion object {
        fun from(type: EnumType): EnumTypeBuilder {
            val builder = EnumTypeBuilder(type.id)
            builder.keyType = type.keyType
            builder.valueType = type.valueType
            builder.defaultInt = type.defaultInt
            builder.defaultLong = type.defaultLong
            builder.defaultString = type.defaultString
            builder.values.putAll(type.values)
            type.rs2?.let {
                builder.dense = it.dense
                builder.capacity = it.capacity
                builder.typesFromId = it.typesFromId
                builder.pluginApi = it.pluginApi
            }
            return builder
        }

        private val DEFAULT_RS2 = EnumType.Rs2()
    }
}
