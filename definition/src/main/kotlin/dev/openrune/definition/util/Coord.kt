package dev.openrune.definition.util

data class Coord(
    val plane: Int,
    val x: Int,
    val y: Int
) {
    constructor(packed: Int) : this(
        packed shr 28 and 3,
        packed shr 14 and 16383,
        packed and 16383
    )
}