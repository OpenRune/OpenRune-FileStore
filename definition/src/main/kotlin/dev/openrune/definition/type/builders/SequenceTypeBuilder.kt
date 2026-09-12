package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

import dev.openrune.definition.Sound
import dev.openrune.definition.SoundData
import dev.openrune.definition.util.IntListPool

class SequenceTypeBuilder(var id: Int = -1) : Sound {

    var frameIDs: MutableList<Int>? = null
    var chatFrameIds: MutableList<Int>? = null
    var frameDelays: MutableList<Int>? = null
    var soundEffects: MutableList<SoundData?> = mutableListOf()
    var frameStep: Int = -1
    var interleaveLeave: MutableList<Int>? = null
    var stretches: Boolean = false
    var forcedPriority: Int = 5
    var leftHandItem: Int = -1
    var rightHandItem: Int = -1
    var maxLoops: Int = 99
    var precedenceAnimating: Int = -1
    var priority: Int = -1
    var skeletalId: Int = -1
    var skeletalRangeBegin: Int = -1
    var skeletalRangeEnd: Int = -1
    var replyMode: Int = 2
    var rangeBegin: Int = 0
    var rangeEnd: Int = 0
    var verticalOffset: Int = 0
    val skeletalSounds: MutableMap<Int, SoundData> = mutableMapOf()
    var mask: MutableList<Boolean>? = null
    var debugName: String = ""

    fun build(): SequenceType = SequenceType(
        id = id,
        frameIDs = IntListPool.of(frameIDs),
        chatFrameIds = IntListPool.of(chatFrameIds),
        frameDelays = IntListPool.of(frameDelays),
        soundEffects = soundEffects,
        frameStep = frameStep,
        interleaveLeave = IntListPool.of(interleaveLeave),
        stretches = stretches,
        forcedPriority = forcedPriority,
        leftHandItem = leftHandItem,
        rightHandItem = rightHandItem,
        maxLoops = maxLoops,
        precedenceAnimating = precedenceAnimating,
        priority = priority,
        skeletalId = skeletalId,
        skeletalRangeBegin = skeletalRangeBegin,
        skeletalRangeEnd = skeletalRangeEnd,
        replyMode = replyMode,
        rangeBegin = rangeBegin,
        rangeEnd = rangeEnd,
        verticalOffset = verticalOffset,
        skeletalSounds = skeletalSounds,
        mask = mask,
        debugName = debugName,
    )

    companion object {
        fun from(type: SequenceType): SequenceTypeBuilder {
            val builder = SequenceTypeBuilder(type.id)
            builder.frameIDs = type.frameIDs?.toMutableList()
            builder.chatFrameIds = type.chatFrameIds?.toMutableList()
            builder.frameDelays = type.frameDelays?.toMutableList()
            builder.soundEffects = type.soundEffects.toMutableList()
            builder.frameStep = type.frameStep
            builder.interleaveLeave = type.interleaveLeave?.toMutableList()
            builder.stretches = type.stretches
            builder.forcedPriority = type.forcedPriority
            builder.leftHandItem = type.leftHandItem
            builder.rightHandItem = type.rightHandItem
            builder.maxLoops = type.maxLoops
            builder.precedenceAnimating = type.precedenceAnimating
            builder.priority = type.priority
            builder.skeletalId = type.skeletalId
            builder.skeletalRangeBegin = type.skeletalRangeBegin
            builder.skeletalRangeEnd = type.skeletalRangeEnd
            builder.replyMode = type.replyMode
            builder.rangeBegin = type.rangeBegin
            builder.rangeEnd = type.rangeEnd
            builder.verticalOffset = type.verticalOffset
            builder.skeletalSounds.putAll(type.skeletalSounds)
            builder.mask = type.mask?.toMutableList()
            builder.debugName = type.debugName
            return builder
        }
    }
}
