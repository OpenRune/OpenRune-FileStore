package dev.openrune.cache.tools.incremental

/**
 * Records the mapsquares written during a build, so later tasks can limit their work to the squares
 * that actually changed. When incremental packing skips a square it is never written, so it never
 * appears here.
 */
object PackedMapSquares {
    private val squares = LinkedHashSet<Int>()

    val packed: Set<Int> get() = squares.toSet()

    fun record(mapsquareX: Int, mapsquareY: Int) {
        squares += (mapsquareX shl 8) or (mapsquareY and 0xFF)
    }

    fun clear() = squares.clear()
}
