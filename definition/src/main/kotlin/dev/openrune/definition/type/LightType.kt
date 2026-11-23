package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered

data class LightType(
    override var id: Int = -1,
    var function: Int? = null,
    var frequency: Int? = null,
    var amplitude: Int? = null,
    var offset: Int? = null,
    var unknown5: Int? = null,
    var unknown6: Int? = null,
    var unknown7: Int? = null,
    var swayAmount: Float? = null,
    var swayAmountRandom: Boolean = false,
    var swayDuration: Int? = null,
    var swayDurationRandom: Int? = null,
    var swayEasing: Float? = null,
    var swayFade: Float? = null,
    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf(),
) : Definition, Rs2OpcodeOrdered
