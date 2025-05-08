package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Parameterized
import dev.openrune.definition.util.Type

data class EnumType(
    override var id: Int = -1,
    override var inherit: Int = 1,
    override var debugName: String = "",
    var keyType: Type = Type.INT,
    var valueType: Type = Type.INT,
    var defaultInt: Int = 0,
    var defaultString: String = "",
    val values: MutableMap<Int, Any> = HashMap()
) : Definition {

    fun getSize() = values.size

    fun getInt(key: Int): Int = values.get(key) as? Int ?: defaultInt

    fun getString(key: Int): String = values.get(key) as? String ?: defaultString
}