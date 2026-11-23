package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered
import dev.openrune.definition.VarDomain

data class Rs2VarBitType(
    override var id: Int = -1,
    var domain: VarDomain? = null,
    var baseVar: Int? = null,
    var startBit: Int? = null,
    var endBit: Int? = null,
    var wikiSync: Boolean = false,
    var pluginApi: Boolean = true,
    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf(),
) : Definition, Rs2OpcodeOrdered
