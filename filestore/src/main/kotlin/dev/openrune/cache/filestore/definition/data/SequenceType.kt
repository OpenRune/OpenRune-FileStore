package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.Sound
import dev.openrune.cache.filestore.definition.SoundData
import dev.openrune.cache.filestore.serialization.toIntWithMaxCheck

import kotlinx.serialization.Serializable

@Serializable
data class SequenceType(
    override var id: Int = -1,
    var frameIDs: MutableList<Int>? = null,
    var chatFrameIds: MutableList<Int>? = null,
    var frameDelays: MutableList<Int>? = null,
    var soundEffects: MutableList<SoundData?> = emptyList<SoundData>().toMutableList(),
    var frameStep: UShort = UShort.MAX_VALUE,
    var interleaveLeave: MutableList<Int>? = null,
    var stretches: Boolean = false,
    var forcedPriority: UByte = 5u,
    var leftHandItem: UShort = UShort.MAX_VALUE,
    var rightHandItem: UShort = UShort.MAX_VALUE,
    var maxLoops: UByte = 99u,
    var precedenceAnimating: UByte = UByte.MAX_VALUE,
    var priority: UByte = UByte.MAX_VALUE,
    var skeletalId: Int = -1,
    var replyMode: UByte = 2u,
    var rangeBegin : Short = 0,
    var rangeEnd : Short = 0,
    var skeletalSounds: MutableMap<Int, SoundData> = emptyMap<Int, SoundData>().toMutableMap(),
    var mask: MutableList<Boolean>? = null,
    //Custom
    override var inherit: Int = -1
) : Definition, Sound {
    var lengthInCycles = 0
    val cycleLength: Int get() = lengthInCycles

    fun getForcedPriority(): Int = forcedPriority.toIntWithMaxCheck()
    fun getMaxLoops(): Int = maxLoops.toIntWithMaxCheck()
    fun getPrecedenceAnimating(): Int = precedenceAnimating.toIntWithMaxCheck()
    fun getPriority(): Int = priority.toIntWithMaxCheck()

    fun getReplyMode(): Int = replyMode.toIntWithMaxCheck()

    fun getRangeBegin(): Int = rangeBegin.toIntWithMaxCheck()
    fun getRangeEnd(): Int = rangeEnd.toIntWithMaxCheck()

    fun getFrameStep(): Int = frameStep.toIntWithMaxCheck()
    fun getLeftHandItem(): Int = leftHandItem.toIntWithMaxCheck()
    fun getRightHandItem(): Int = rightHandItem.toIntWithMaxCheck()

}