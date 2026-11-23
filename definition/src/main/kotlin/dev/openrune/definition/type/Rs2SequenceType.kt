package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered
import dev.openrune.definition.MutableParameterized

data class Rs2SequenceType(
    override var id: Int = -1,
    var frames: MutableList<Int>? = null,
    var delays: MutableList<Int>? = null,
    var legacyFrames: Boolean = false,
    var legacyIFrames: MutableList<Int>? = null,
    var loops: Int? = null,
    var walkMerge: MutableList<Int>? = null,
    var unknown4: Int? = null,
    var stretches: Boolean = false,
    var priority: Int? = null,
    var replaceHeldLeft: Int? = null,
    var replaceHeldRight: Int? = null,
    var maxLoops: Int? = null,
    var preAnimMove: Int? = null,
    var postAnimMove: Int? = null,
    var duplicateBehaviour: Int? = null,
    var iFrames: MutableList<Int>? = null,
    var iFramesWideCount: Boolean = false,
    var sounds: MutableList<Rs2SequenceSound>? = null,
    var unknown14: Boolean = false,
    var unknown15: Boolean = false,
    var unknown16: Boolean = false,
    var unknown17: Int? = null,
    var soundVorbis: Boolean = false,
    var soundVolume: Int? = null,
    var soundVolumeSecondary: Int? = null,
    var soundVolumeWide: Boolean = false,
    var soundRandomRate: MutableList<Int>? = null,
    var soundRandomRateWide: Boolean = false,
    var tertiaryVolumeFactor: Int? = null,
    var unknown23: Int? = null,
    var group: Int? = null,
    var keyFrameSet: Int? = null,
    var keyFrameRangeFrom: Int? = null,
    var keyFrameRangeTo: Int? = null,
    var unknown27: Int? = null,
    override var params: MutableMap<Int, Any>? = null,
    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf(),
) : Definition, MutableParameterized, Rs2OpcodeOrdered

data class Rs2SequenceSound(
    var packed: Int? = null,
    var extra: MutableList<Int> = mutableListOf(),
)
