package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

/**
 * The mutable side of [AmbienceType]. Codecs decode into one of these and the packing tools edit
 * one — either fresh or via [AmbienceType.toBuilder] — then [build] produces the immutable
 * definition everything else reads.
 */
class AmbienceTypeBuilder(var id: Int = -1) {

    var sequentialSounds: IntArray? = null
    var randomSounds: RandomSound? = null
    var fade: AmbienceType.SoundFade? = null

    fun build(): AmbienceType = AmbienceType(
        id = id,
        sequentialSounds = sequentialSounds,
        randomSounds = randomSounds,
        fade = fade,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. Nested holders are copied too. */
        fun from(type: AmbienceType): AmbienceTypeBuilder {
            val builder = AmbienceTypeBuilder(type.id)
            builder.sequentialSounds = type.sequentialSounds?.copyOf()
            builder.randomSounds = type.randomSounds?.let { it.copy(soundIds = it.soundIds.toMutableList()) }
            builder.fade = type.fade?.copy()
            return builder
        }
    }
}
