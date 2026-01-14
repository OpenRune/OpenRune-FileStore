package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.util.VarType

data class VarClanType(
    override var id: Int = -1,
    var type: VarType? = null,
    var lifetime : Int = 0,
    var debugName : String = "",
) : Definition