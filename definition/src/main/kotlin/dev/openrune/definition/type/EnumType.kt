package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.util.VarType

data class EnumType(
    override var id: Int = -1,
    var keyType: VarType = VarType.INT,
    var valueType: VarType = VarType.INT,
    var defaultInt: Int = 0,
    var defaultString: String = "",
    val values: MutableMap<String, Any> = HashMap()
) : Definition {

    fun getSize() = values.size

    fun getInt(key: Int): Int = values.get(key.toString()) as? Int ?: defaultInt

    fun getString(key: Int): String = values.get(key.toString()) as? String ?: defaultString
}