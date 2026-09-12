package dev.openrune.definition.type

import dev.openrune.toml.serialization.TomlField
import dev.openrune.definition.Definition
import dev.openrune.definition.MutableParameterized
import dev.openrune.seralizer.ParamSerializer

data class StructType(
    override var id: Int = -1,
    @param:TomlField(serializer = ParamSerializer::class)
    override var params: MutableMap<Int, Any>? = null,
) : Definition, MutableParameterized {

    fun getInt(key: Int): Int = params?.get(key) as? Int ?: -1

    fun getString(key: Int): String = params?.get(key) as? String ?: ""

    companion object {
        val EMPTY = StructType()
    }
}