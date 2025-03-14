package dev.openrune.definition.type

import dev.openrune.definition.Definition
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class EnumType(
    override var id: Int = -1,
    var keyType : Int = 0,
    var valueType : Int = 0,
    var defaultInt : Int = 0,
    var defaultString : String = "",
    @Contextual
    val values : Int2ObjectOpenHashMap<@Contextual Any> = Int2ObjectOpenHashMap<Any>(),

) : Definition {

    fun getSize() = values.size

    fun getInt(key: Int): Int = values.get(key) as? Int ?: defaultInt

    fun getString(key: Int): String = values.get(key) as? String ?: defaultString
}