package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered

data class BasType(
    override var id: Int = -1,
    var readyAnim: Int? = null,
    var readyAnimSecondary: Int? = null,
    var crawlAnim: Int? = null,
    var crawlAnimBack: Int? = null,
    var crawlAnimLeft: Int? = null,
    var crawlAnimRight: Int? = null,
    var runAnim: Int? = null,
    var runAnimBack: Int? = null,
    var runAnimLeft: Int? = null,
    var runAnimRight: Int? = null,
    var hillRotate: MutableList<Int>? = null,
    var unknown27: MutableList<Int>? = null,
    var unknown28: MutableList<Int>? = null,
    var turnSpeed: Int? = null,
    var turnAcceleration: Int? = null,
    var unknown31: Int? = null,
    var unknown32: Int? = null,
    var unknown33: Int? = null,
    var unknown34: Int? = null,
    var unknown35: Int? = null,
    var unknown36: Int? = null,
    var walkSpeed: Int? = null,
    var readyAnimLeft: Int? = null,
    var readyAnimRight: Int? = null,
    var walkAnimBack: Int? = null,
    var walkAnimLeft: Int? = null,
    var walkAnimRight: Int? = null,
    var unknown43: Int? = null,
    var unknown44: Int? = null,
    var overlayHeight: Int? = null,
    var crawlTurnLeft: Int? = null,
    var crawlTurnRight: Int? = null,
    var runTurnLeft: Int? = null,
    var runTurnRight: Int? = null,
    var walkTurnLeft: Int? = null,
    var walkTurnRight: Int? = null,
    var randomReadyAnims: MutableList<BasRandomReadyAnim>? = null,
    var unknown53: Boolean = true,
    var hillRotateLimit: MutableList<Int>? = null,
    var unknown55: MutableList<Int>? = null,
    var unknown56: MutableList<Int>? = null,
    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf(),
) : Definition, Rs2OpcodeOrdered

data class BasRandomReadyAnim(
    var anim: Int = -1,
    var weight: Int = 0,
    var previousReady: MutableList<Int> = mutableListOf(),
)
