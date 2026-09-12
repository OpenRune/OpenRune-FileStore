package dev.openrune.cache.worldmap.worldmap.rasterizer.utils

/**
 * @author Kris | 21/08/2022
 */
enum class WorldMapLabelSize(
    val id: Int,
    val textSize: Int,
    private val pixelsPerTile: Int
) {
    Small(2, 0, 4),
    Medium(0, 1, 2),
    Large(1, 2, 0);

    fun shouldDrawTextLabel(pixelsPerTile: Float): Boolean {
        return pixelsPerTile >= this.pixelsPerTile.toFloat()
    }

    companion object {
        val values = values().toList()
    }
}
