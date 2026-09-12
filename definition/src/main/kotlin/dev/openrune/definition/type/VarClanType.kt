package dev.openrune.definition.type

import dev.openrune.definition.type.builders.VarClanTypeBuilder

import dev.openrune.toml.serialization.TomlField
import dev.openrune.definition.Definition
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.seralizer.CacheVarLiteralSeralizier

data class VarClanType(
    override var id: Int = -1,
    @param:TomlField(serializer = CacheVarLiteralSeralizier::class)
    val type: CacheVarLiteral? = null,
    val lifetime : Int = 0,
    val debugName : String = "",
) : Definition {

    fun toBuilder(): VarClanTypeBuilder = VarClanTypeBuilder.from(this)
}
