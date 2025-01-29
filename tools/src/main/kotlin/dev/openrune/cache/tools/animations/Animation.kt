package dev.openrune.cache.tools.animations

data class Animation(
    var id: Int = -1,
    var lastFrameLength: Int = -1,
    var secondLastFrameLength: Int = -1,
    var forcedPriority: Int = -1,
    val frameIds: ArrayList<Int> = ArrayList()
)