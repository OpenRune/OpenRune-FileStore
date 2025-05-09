package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class VarBitType(
    override var id: Int = -1,
    var varp: Int = 0,
    var startBit: Int = 0,
    var endBit: Int = 0,

) : Definition