package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

import dev.openrune.definition.util.CacheVarLiteral

class EnumTypeBuilder(var id: Int = -1) {

    var keyType: CacheVarLiteral = CacheVarLiteral.INT
    var valueType: CacheVarLiteral = CacheVarLiteral.INT
    var defaultInt: Int = 0
    var defaultString: String = ""
    val values: MutableMap<Int, Any> = HashMap()

    fun build(): EnumType = EnumType(
        id = id,
        keyType = keyType,
        valueType = valueType,
        defaultInt = defaultInt,
        defaultString = defaultString,
        values = values,
    )

    companion object {
        fun from(type: EnumType): EnumTypeBuilder {
            val builder = EnumTypeBuilder(type.id)
            builder.keyType = type.keyType
            builder.valueType = type.valueType
            builder.defaultInt = type.defaultInt
            builder.defaultString = type.defaultString
            builder.values.putAll(type.values)
            return builder
        }
    }
}
