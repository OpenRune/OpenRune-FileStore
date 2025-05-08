package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Parameterized

data class StructType(
    override var id: Int = -1,
    override var inherit: Int = 1,
    override var debugName : String = "",
    override var params: MutableMap<Int, Any>? = HashMap()
) : Definition, Parameterized {

    fun getInt(key: Int): Int = params?.get(key) as? Int ?: -1

    fun getString(key: Int): String = params?.get(key) as? String ?: ""

    companion object {
        val EMPTY = StructType()
    }
}