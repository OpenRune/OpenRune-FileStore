package dev.openrune.definition.type

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
data class ParamType(
    override var id: Int = -1,
    @param:TomlField(serializer = CacheVarLiteralSeralizier::class)
    var type: CacheVarLiteral? = null,
    var isMembers: Boolean = true,
    var defaultInt: Int = 0,
    var defaultString: String? = null,
    var defaultLong: Long = 0L
) : Definition