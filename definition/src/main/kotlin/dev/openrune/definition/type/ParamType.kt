package dev.openrune.definition.type

import dev.openrune.definition.type.builders.ParamTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.toml.serialization.TomlField
import dev.openrune.definition.Definition
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.seralizer.CacheVarLiteralSeralizier
import dev.openrune.seralizer.ParamTypeTableHook

@RsTableHeaders(
    "params",
    rowPostDecode = ParamTypeTableHook::class,
)
/**
 * A loaded param definition. Immutable apart from [id] (which the load machinery assigns):
 * decoding builds one through [ParamTypeBuilder], and everything after that only reads (the
 * TOML row hook only validates, it never writes).
 */
data class ParamType(
    override var id: Int = -1,
    @param:TomlField(serializer = CacheVarLiteralSeralizier::class)
    val type: CacheVarLiteral? = null,
    val isMembers: Boolean = true,
    val defaultInt: Int = 0,
    val defaultString: String? = null,
    val defaultLong: Long = 0L
) : Definition {

    /** A mutable copy of this definition, for tools that need to edit and re-pack it. */
    fun toBuilder(): ParamTypeBuilder = ParamTypeBuilder.from(this)
}