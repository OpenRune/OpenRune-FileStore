package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class VarpType(
    override var id: Int = -1,
    override var inherit: Int = 1,
    override var debugName : String = "",
    var configType: Int = 0,

) : Definition