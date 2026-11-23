package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered

data class BillboardType(
    override var id: Int = -1,
    var material: Int? = null,
    var width: Int? = null,
    var height: Int? = null,
    var unknown3: Int? = null,
    var sourceBlendMode: Int? = null,
    var destinationBlendMode: Int? = null,
    var unknown6: Boolean = false,
    var unknown7: Boolean = false,
    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf(),
) : Definition, Rs2OpcodeOrdered
