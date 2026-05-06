package dev.openrune.definition.type


import dev.openrune.definition.Definition

data class AmbienceType(
    override var id: Int = -1,
    var sequentialSounds: IntArray? = null,
    var randomSounds: RandomSound? = null,
    var fade: SoundFade? = null
) : Definition {

    data class SoundFade(
        var inSpeed: Int? = null,
        var inDuration: Int? = null,
        var outSpeed: Int? = null,
        var outDuration: Int? = null
    )
}

data class BgSound(
    var id: Int = -1,
    var range: Int = 0,
    var volume: Int = 0
)

data class BgSoundFade(
    var dropoffEasing: Int = 0,
    var easeInType: Int = 0,
    var easeInDuration: Int = 0,
    var easeOutType: Int = 0,
    var easeOutDuration: Int = 0
)

data class RandomSound(
    var minDelay: Int = 0,
    var maxDelay: Int = 0,
    var minVolume: Int = 0,
    var maxVolume: Int = 0,
    var soundIds: MutableList<Int> = mutableListOf()
)
