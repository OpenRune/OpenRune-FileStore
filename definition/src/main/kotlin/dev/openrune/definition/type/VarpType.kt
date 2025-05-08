package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class VarpType(
    override var id: Int = -1,
    var configType: Int = 0,

) : Definition