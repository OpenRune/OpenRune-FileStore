package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered
import dev.openrune.definition.MutableTransforms

data class HitmarkType(
    override var id: Int = -1,
    var damageFont: Int? = null,
    var damageColour: Int? = null,
    var classGraphic: Int? = null,
    var leftGraphic: Int? = null,
    var middleGraphic: Int? = null,
    var rightGraphic: Int? = null,
    var scrollToOffsetX: Int? = null,
    var damageFormat: String? = null,
    var stickTime: Int? = null,
    var scrollToOffsetY: Int? = null,
    var fadeOut: Int? = null,
    var noFadeOut: Boolean = false,
    var replaceMode: Int? = null,
    var damageYOffset: Int? = null,
    var graphicOffsetX: Int? = null,
    var graphicOffsetY: Int? = null,
    var damageScaleTo: Int? = null,
    var damageScaleFrom: Int? = null,
    override var multiVarBit: Int = -1,
    override var multiVarp: Int = -1,
    override var multiDefault: Int = -1,
    override var transforms: MutableList<Int>? = null,
    var transformsWideVarBit: Boolean = false,
    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf(),
) : Definition, MutableTransforms, Rs2OpcodeOrdered
