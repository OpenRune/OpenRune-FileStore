package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.util.VarType

data class ParamType(
    override var id: Int = -1,
    var type: VarType? = null,
    var isMembers: Boolean = true,
    var defaultInt: Int = 0,
    var defaultString: String? = null,

    ) : Definition