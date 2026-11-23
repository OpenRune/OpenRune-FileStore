package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered

data class WorldAreaType(
    override var id: Int = -1,
    var colour: Int? = null,
    var impostorSquareFrom: Int? = null,
    var impostorSquareTo: Int? = null,
    var impostorZoneCoord: Int? = null,
    var impostorZoneTemplate: Int? = null,
    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf(),
) : Definition, Rs2OpcodeOrdered
