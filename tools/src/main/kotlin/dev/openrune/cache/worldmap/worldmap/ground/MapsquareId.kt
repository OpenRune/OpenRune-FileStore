package dev.openrune.cache.worldmap.worldmap.ground

/**
 * @author Kris | 15/08/2022
 */
@JvmInline
value class MapsquareId(val packed: Int) {
    constructor(x: Int, y: Int) : this(((x and 0xFF) shl 8) or (y and 0xFF))
    val x: Int get() = packed shr 8
    val y: Int get() = packed and 0xFF

    operator fun component1(): Int = x
    operator fun component2(): Int = y
}
