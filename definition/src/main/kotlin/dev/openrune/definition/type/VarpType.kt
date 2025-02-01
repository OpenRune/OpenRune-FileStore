package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class VarpType(
    override var id: Int = -1,
    var configType: Int = 0,
    //Custom
    override var inherit: Int = -1
) : Definition