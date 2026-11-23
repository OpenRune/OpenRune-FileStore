package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered
import dev.openrune.definition.Rs2Recolourable

data class Rs2SpotAnimType(
    override var id: Int = -1,
    var model: Int? = null,
    var anim: Int? = null,
    var hasAlpha: Boolean = false,
    var resizeHorizontal: Int? = null,
    var resizeVertical: Int? = null,
    var rotation: Int? = null,
    var ambient: Int? = null,
    var contrast: Int? = null,
    var allowLoop: Boolean = false,
    var hillChangeRotate: Boolean = false,
    var hillChangeRotateShort: Int? = null,
    var hillChangeRotateInt: Int? = null,
    var castsShadows: Boolean = true,
    override var originalColours: MutableList<Int>? = null,
    override var modifiedColours: MutableList<Int>? = null,
    override var originalTextureColours: MutableList<Int>? = null,
    override var modifiedTextureColours: MutableList<Int>? = null,
    override var recolourMask: Int? = null,
    override var textureMask: Int? = null,
    override val legacyColourSlots: MutableMap<Int, Int> = LinkedHashMap(),
    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf(),
) : Definition, Rs2Recolourable, Rs2OpcodeOrdered
