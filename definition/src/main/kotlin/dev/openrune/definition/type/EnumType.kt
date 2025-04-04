package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.serialization.Rscm
import dev.openrune.definition.util.Type
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class EnumType(
    override var id: Rscm = -1,
    var keyType : Type = Type.INT,
    var valueType : Type = Type.INT,
    var defaultInt : Int = 0,
    var defaultString : String = "",
    @Contextual
    val values: MutableMap<Int, @Contextual Any> = HashMap(),

    ) : Definition {

    fun getSize() = values.size

    fun getInt(key: Int): Int = values.get(key) as? Int ?: defaultInt

    fun getString(key: Int): String = values.get(key) as? String ?: defaultString
}