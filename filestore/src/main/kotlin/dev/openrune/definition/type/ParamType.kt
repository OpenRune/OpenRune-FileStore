package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.util.ScriptVarType

data class ParamType(
    override var id: Int = -1,
    var type: ScriptVarType? = null,
    var isMembers: Boolean = true,
    var defaultInt: Int = 0,
    var defaultString: String? = null,
    //Custom
    override var inherit: Int = -1
) : Definition