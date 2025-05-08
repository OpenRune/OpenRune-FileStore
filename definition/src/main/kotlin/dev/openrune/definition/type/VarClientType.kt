package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class VarClientType(
    override var id: Int = -1,
    override var inherit: Int = 1,
    override var debugName : String = "",
    var persist: Boolean = false
) : Definition