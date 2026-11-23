package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered

data class HeadbarType(
    override var id: Int = -1,
    var unknown1: Int? = null,
    var showPriority: Int? = null,
    var hidePriority: Int? = null,
    var fadeOut: Int? = null,
    var noFadeOut: Boolean = false,
    var stickTime: Int? = null,
    var unknown6: Int? = null,
    var full: Int? = null,
    var empty: Int? = null,
    var fullPlayerGroup: Int? = null,
    var emptyPlayerGroup: Int? = null,
    var fullPlayerGroupTeam: Int? = null,
    var emptyPlayerGroupTeam: Int? = null,
    var leftCapSprite: Int? = null,
    var rightCapSprite: Int? = null,
    var tileFill: Boolean = false,
    var stackOffset: Int? = null,
    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf(),
) : Definition, Rs2OpcodeOrdered
