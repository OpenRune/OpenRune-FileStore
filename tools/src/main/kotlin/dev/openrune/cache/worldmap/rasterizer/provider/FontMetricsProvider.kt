package dev.openrune.cache.worldmap.rasterizer.provider

/**
 * @author Kris | 21/08/2022
 */
interface FontMetricsProvider {
    val verdana11FontId: Int get() = 1442
    val verdana13FontId: Int get() = 1445
    val verdana15FontId: Int get() = 1447
    fun getFont(id: Int): FontMetrics?
}

interface FontMetrics {
    val advances: IntArray
    val kerning: ByteArray?
    val ascent: Int?
}
