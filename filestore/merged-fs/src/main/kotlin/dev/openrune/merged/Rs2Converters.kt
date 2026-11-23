package dev.openrune.merged

import dev.openrune.definition.Definition
import dev.openrune.definition.EntityOpsBuilder
import dev.openrune.definition.type.HeadbarType
import dev.openrune.definition.type.HealthBarType
import dev.openrune.definition.type.HitSplatType
import dev.openrune.definition.type.HitmarkType
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.type.NpcType
import dev.openrune.definition.type.Rs2NpcType
import dev.openrune.definition.type.Rs2ObjType
import dev.openrune.definition.type.Rs2SequenceType
import dev.openrune.definition.type.Rs2VarBitType
import dev.openrune.definition.type.SequenceType
import dev.openrune.definition.type.VarBitType

const val RS2_PROPERTY = "rs2def"

val ItemType.rs2: Rs2ObjType? get() = getExtraProperty(RS2_PROPERTY)
val NpcType.rs2: Rs2NpcType? get() = getExtraProperty(RS2_PROPERTY)
val SequenceType.rs2: Rs2SequenceType? get() = getExtraProperty(RS2_PROPERTY)
val VarBitType.rs2: Rs2VarBitType? get() = getExtraProperty(RS2_PROPERTY)
val HitSplatType.rs2: HitmarkType? get() = getExtraProperty(RS2_PROPERTY)
val HealthBarType.rs2: HeadbarType? get() = getExtraProperty(RS2_PROPERTY)

val ItemType.isRs2: Boolean get() = rs2 != null
val NpcType.isRs2: Boolean get() = rs2 != null
val SequenceType.isRs2: Boolean get() = rs2 != null
val VarBitType.isRs2: Boolean get() = rs2 != null
val HitSplatType.isRs2: Boolean get() = rs2 != null
val HealthBarType.isRs2: Boolean get() = rs2 != null

private fun <T : Definition> T.carrying(original: Definition): T =
    also { it.setExtraProperty(RS2_PROPERTY, original) }

fun Rs2NpcType.toNpcType(id: Int): NpcType = NpcType(
    id = id,
    name = name ?: "null",
    size = size ?: 1,
    category = category ?: -1,
    models = models,
    chatheadModels = heads,
    standAnim = readyAnim ?: -1,
    walkAnim = walkAnim ?: -1,
    rotateLeftAnim = turnLeftAnim ?: -1,
    rotateRightAnim = turnRightAnim ?: -1,
    actions = ops.toOps(NPC_OP_FIRST),
    originalColours = originalColours,
    modifiedColours = modifiedColours,
    originalTextureColours = originalTextureColours,
    modifiedTextureColours = modifiedTextureColours,
    multiVarBit = multiVarBit,
    multiVarp = multiVarp,
    multiDefault = multiDefault,
    transforms = transforms,
    isMinimapVisible = minimap,
    combatLevel = visLevel ?: -1,
    widthScale = resizeHorizontal ?: 128,
    heightScale = resizeVertical ?: 128,
    ambient = ambient ?: 0,
    contrast = contrast ?: 0,
    rotation = turnSpeed ?: 32,
    isInteractable = active,
    height = overlayHeight ?: -1,
    params = params,
).carrying(this)

fun Rs2SequenceType.toSequenceType(id: Int): SequenceType = SequenceType(
    id = id,
    frameIDs = frames,
    frameDelays = delays,
    chatFrameIds = iFrames,
    interleaveLeave = walkMerge,
    stretches = stretches,
    priority = priority ?: -1,
    leftHandItem = replaceHeldLeft ?: -1,
    rightHandItem = replaceHeldRight ?: -1,
    maxLoops = maxLoops ?: 99,
    replyMode = duplicateBehaviour ?: 2,
    skeletalId = keyFrameSet ?: -1,
    skeletalRangeBegin = keyFrameRangeFrom ?: -1,
    skeletalRangeEnd = keyFrameRangeTo ?: -1,
).carrying(this)

fun Rs2VarBitType.toVarBitType(id: Int): VarBitType = VarBitType(
    id = id,
    varp = baseVar ?: 0,
    startBit = startBit ?: 0,
    endBit = endBit ?: 0,
).carrying(this)

fun HitmarkType.toHitSplatType(id: Int): HitSplatType = HitSplatType(
    id = id,
    font = damageFont ?: -1,
    textColour = damageColour ?: 16777215,
    icon = classGraphic ?: -1,
    left = leftGraphic ?: -1,
    middle = middleGraphic ?: -1,
    right = rightGraphic ?: -1,
    offsetX = scrollToOffsetX ?: 0,
    offsetY = scrollToOffsetY ?: 0,
    amount = damageFormat ?: "",
    duration = stickTime ?: 70,
    fade = fadeOut ?: -1,
    damageYOfset = damageYOffset ?: 0,
    multiVarBit = multiVarBit,
    multiVarp = multiVarp,
    multiDefault = multiDefault,
    transforms = transforms,
).carrying(this)

fun HeadbarType.toHealthBarType(id: Int): HealthBarType = HealthBarType(
    id = id,
    frontSpriteId = full ?: -1,
    backSpriteId = empty ?: -1,
    int4 = stickTime ?: 70,
).carrying(this)

private fun Map<Int, String>.toOps(first: Int) = EntityOpsBuilder().apply {
    forEach { (opcode, text) -> op(opcode - first, text) }
}.build()

private const val NPC_OP_FIRST = 30
