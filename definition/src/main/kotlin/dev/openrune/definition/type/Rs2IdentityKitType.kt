package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered
import dev.openrune.definition.Rs2Recolourable

data class Rs2IdentityKitType(
    override var id: Int = -1,
    var bodyPart: Int? = null,
    var models: MutableList<Int>? = null,
    var disable: Boolean = false,
    var heads: MutableMap<Int, Int> = LinkedHashMap(),
    override var originalColours: MutableList<Int>? = null,
    override var modifiedColours: MutableList<Int>? = null,
    override var originalTextureColours: MutableList<Int>? = null,
    override var modifiedTextureColours: MutableList<Int>? = null,
    override var recolourMask: Int? = null,
    override var textureMask: Int? = null,
    override val legacyColourSlots: MutableMap<Int, Int> = LinkedHashMap(),
    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf(),
) : Definition, Rs2Recolourable, Rs2OpcodeOrdered
