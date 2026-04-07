package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.util.CacheVarLiteral

data class VarClanSettingsType(
    override var id: Int = -1,
    var type: CacheVarLiteral? = null,
    var lifetime : Int = 0,
    var debugName : String = "",
) : Definition