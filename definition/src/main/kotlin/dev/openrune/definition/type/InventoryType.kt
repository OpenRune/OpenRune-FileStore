package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Parameterized

data class InventoryType(
    override var id: Int = -1,
    var size: Int = 0,
    override var params: MutableMap<String, Any>? = null,
) : Parameterized,Definition