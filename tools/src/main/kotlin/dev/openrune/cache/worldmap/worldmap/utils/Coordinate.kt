package dev.openrune.cache.worldmap.worldmap.utils

/**
 * @author Kris | 18/08/2022
 */
@JvmInline
value class Coordinate(val packedCoord: Int) {
    constructor(x: Int, y: Int, level: Int = 0) : this((y and 0x3FFF) or ((x and 0x3FFF) shl 14) or ((level and 0x3) shl 28))

    val x: Int get() = (packedCoord shr 14) and 0x3FFF
    val y: Int get() = packedCoord and 0x3FFF
    val level: Int get() = (packedCoord shr 28) and 0x3
    val zoneX: Int get() = x shr 3
    val zoneY: Int get() = y shr 3
    val zoneId: Int get() = zoneX or (zoneY shl 11) or (level shl 22)
    val mapsquareX: Int get() = x shr 6
    val mapsquareY: Int get() = y shr 6
    val mapsquareId: Int get() = (mapsquareX shl 8) or mapsquareY

    operator fun component1(): Int = x
    operator fun component2(): Int = y
    operator fun component3(): Int = level
    override fun toString(): String {
        return "Coordinate(x=$x, y=$y, level=$level, mapsquareId=$mapsquareId, jag=${level}_${mapsquareX}_${mapsquareY}_${x and 0x3F}_${y and 0x3F})"
    }
}
