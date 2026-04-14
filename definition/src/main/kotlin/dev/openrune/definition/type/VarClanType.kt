package dev.openrune.definition.type

import dev.openrune.toml.serialization.TomlField
import dev.openrune.definition.Definition
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.seralizer.CacheVarLiteralSeralizier

data class VarClanType(
    override var id: Int = -1,
    @param:TomlField(serializer = CacheVarLiteralSeralizier::class)
    var type: CacheVarLiteral? = null,
    var lifetime : Int = 0,
    var debugName : String = "",
) : Definition