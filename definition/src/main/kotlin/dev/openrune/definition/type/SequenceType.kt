package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Sound
import dev.openrune.definition.SoundData
import kotlin.math.ceil

data class SequenceType(
    override var id: Int = -1,
    var frameIDs: MutableList<Int>? = null,
    var chatFrameIds: MutableList<Int>? = null,
    var frameDelays: MutableList<Int>? = null,
    var soundEffects: MutableList<SoundData?> = emptyList<SoundData>().toMutableList(),
    var frameStep: Int = -1,
    var interleaveLeave: MutableList<Int>? = null,
    var stretches: Boolean = false,
    var forcedPriority: Int = 5,
    var leftHandItem: Int = -1,
    var rightHandItem: Int = -1,
    var maxLoops: Int = 99,
    var precedenceAnimating: Int = -1,
    var priority: Int = -1,
    var skeletalId: Int = -1,
    var skeletalRangeBegin: Int = -1,
    var skeletalRangeEnd: Int = -1,
    var replyMode: Int = 2,
    var rangeBegin : Int = 0,
    var rangeEnd : Int = 0,
    var verticalOffset : Int = 0,
    var skeletalSounds: MutableMap<Int, SoundData> = emptyMap<Int, SoundData>().toMutableMap(),
    var mask: MutableList<Boolean>? = null,
    var debugName : String = ""

) : Definition, Sound {
    val lengthInCycles: Int
        get() = if (skeletalId >= 0) {
            (getSkeletalLength() / 30.0).toInt()
        } else {
            ceil((cycleLength * 20.0) / 600.0).toInt()
        }

    private val cycleLength: Int by lazy {
        val delays = frameDelays
        when {
            skeletalId >= 0 || delays == null -> -1
            else -> delays.withIndex().sumOf { (i, d) ->
                if (i < delays.lastIndex || d < 200) d else 0
            }
        }
    }

    private fun getSkeletalLength(): Int = rangeEnd - rangeBegin


}