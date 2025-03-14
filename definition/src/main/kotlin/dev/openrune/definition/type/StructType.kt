package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Parameterized
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class StructType(
    override var id: Int = -1,

    override var params: Map<Int, @Contextual Any>? = Int2ObjectOpenHashMap()
) : Definition, Parameterized {

    fun getInt(key: Int): Int = params?.get(key) as? Int ?: -1

    fun getString(key: Int): String = params?.get(key) as? String ?: ""

    companion object {
        val EMPTY = StructType()
    }
}