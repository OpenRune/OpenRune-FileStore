package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.util.CacheVarLiteral

data class ParamType(
    override var id: Int = -1,
    var type: CacheVarLiteral? = null,
    var isMembers: Boolean = true,
    var defaultInt: Int = 0,
    var defaultString: String? = null,
    var defaultLong: Long = 0L
) : Definition