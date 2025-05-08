package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.util.Type

data class ParamType(
    override var id: Int = -1,
    override var inherit: Int = 1,
    override var debugName : String = "",
    var type: Type? = null,
    var isMembers: Boolean = true,
    var defaultInt: Int = 0,
    var defaultString: String? = null,

) : Definition