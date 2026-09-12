package dev.openrune.definition.type

import dev.openrune.definition.type.builders.StructTypeBuilder

import dev.openrune.toml.serialization.TomlField
import dev.openrune.definition.Definition
import dev.openrune.definition.Parameterized
import dev.openrune.seralizer.ParamSerializer

data class StructType(
    override var id: Int = -1,
    // Stays MutableMap: ParamSerializer's declared type argument must match the parameter type.
    @param:TomlField(serializer = ParamSerializer::class)
    override val params: MutableMap<Int, Any>? = null,
) : Definition, Parameterized {

    /** A mutable copy of this definition, for tools that need to edit and re-pack it. */
    fun toBuilder(): StructTypeBuilder = StructTypeBuilder.from(this)

    fun getInt(key: Int): Int = params?.get(key) as? Int ?: -1

    fun getString(key: Int): String = params?.get(key) as? String ?: ""

    companion object {
        val EMPTY = StructType()
    }
}
