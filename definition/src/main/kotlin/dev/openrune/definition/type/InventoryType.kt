package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class InventoryType(
    override var id: Int = -1,
    override var inherit: Int = 1,
    override var debugName : String = "",
    var size: Int = 0
) : Definition