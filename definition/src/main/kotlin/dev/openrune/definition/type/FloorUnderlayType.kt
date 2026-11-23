package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered

data class FloorUnderlayType(
    override var id: Int = -1,
    var colour: Int = 0,
    var material: Int? = null,
    var textureScale: Int? = null,
    var hardShadow: Boolean = true,
    var occlude: Boolean = true,
    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf(),
) : Definition, Rs2OpcodeOrdered
