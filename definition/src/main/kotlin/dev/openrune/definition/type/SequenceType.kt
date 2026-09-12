package dev.openrune.definition.type

import dev.openrune.definition.type.builders.SequenceTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.definition.Definition
import dev.openrune.definition.Sound
import dev.openrune.definition.SoundData
import kotlin.math.ceil

/**
 * A loaded animation definition. Immutable apart from [id]: decoding and packing build one
 * through [SequenceTypeBuilder].
 */
@RsTableHeaders("animation")
data class SequenceType(
    override var id: Int = -1,
    val frameIDs: List<Int>? = null,
    val chatFrameIds: List<Int>? = null,
    val frameDelays: List<Int>? = null,
    val soundEffects: List<SoundData?> = emptyList(),
    val frameStep: Int = -1,
    val interleaveLeave: List<Int>? = null,
    val stretches: Boolean = false,
    val forcedPriority: Int = 5,
    val leftHandItem: Int = -1,
    val rightHandItem: Int = -1,
    val maxLoops: Int = 99,
    val precedenceAnimating: Int = -1,
    val priority: Int = -1,
    val skeletalId: Int = -1,
    val skeletalRangeBegin: Int = -1,
    val skeletalRangeEnd: Int = -1,
    val replyMode: Int = 2,
    val rangeBegin : Int = 0,
    val rangeEnd : Int = 0,
    val verticalOffset : Int = 0,
    val skeletalSounds: Map<Int, SoundData> = emptyMap(),
    val mask: List<Boolean>? = null,
    val debugName : String = ""

) : Definition, Sound {

    /** A mutable copy of this definition, for tools that need to edit and re-pack it. */
    fun toBuilder(): SequenceTypeBuilder = SequenceTypeBuilder.from(this)

    val lengthInCycles: Int
        get() = if (skeletalId >= 0) {
            (getSkeletalLength() / 30.0).toInt()
        } else {
            ceil((cycleLength * 20.0) / 600.0).toInt()
        }

    private val cycleLength: Int
        get() {
            val delays = frameDelays
            return when {
                skeletalId >= 0 || delays == null -> -1
                else -> delays.withIndex().sumOf { (i, d) ->
                    if (i < delays.lastIndex || d < 200) d else 0
                }
            }
        }

    private fun getSkeletalLength(): Int = rangeEnd - rangeBegin
}
