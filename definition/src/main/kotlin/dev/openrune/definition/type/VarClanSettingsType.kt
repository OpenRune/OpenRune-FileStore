package dev.openrune.definition.type

import dev.openrune.definition.type.builders.VarClanSettingsTypeBuilder

import dev.openrune.toml.serialization.TomlField
import dev.openrune.definition.Definition
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.seralizer.CacheVarLiteralSeralizier

/**
 * A loaded var clan settings definition. Immutable apart from [id] (which the load machinery
 * assigns): decoding builds one through [VarClanSettingsTypeBuilder], and everything after that
 * only reads.
 */
data class VarClanSettingsType(
    override var id: Int = -1,
    @param:TomlField(serializer = CacheVarLiteralSeralizier::class)
    val type: CacheVarLiteral? = null,
    val lifetime : Int = 0,
    val debugName : String = "",
) : Definition {

    /** A mutable copy of this definition, for tools that need to edit and re-pack it. */
    fun toBuilder(): VarClanSettingsTypeBuilder = VarClanSettingsTypeBuilder.from(this)
}
