package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.Sound

data class AnimDefinition(
    override var id: Int = -1,
    var frameIds: MutableList<Int>? = null,
    var frameLengths: MutableList<Int>? = null,
    var priority: Int = -1,
    //Custom
    override var inherit: Int = -1
) : Definition, Sound {
    var lengthInCycles = 0
    val cycleLength: Int get() = lengthInCycles
}