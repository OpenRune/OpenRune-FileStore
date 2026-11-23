package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered

data class ParticleEffectorType(
    override var id: Int = -1,
    var angle: Int? = null,
    var unknown2: Int? = null,
    var forceDirectionX: Int? = null,
    var forceDirectionY: Int? = null,
    var forceDirectionZ: Int? = null,
    var falloffType: Int? = null,
    var falloffValue: Int? = null,
    var forceType: Int? = null,
    var unknown8: Boolean = false,
    var unknown9: Boolean = false,
    var reverseDirection: Boolean = false,
    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf(),
) : Definition, Rs2OpcodeOrdered
