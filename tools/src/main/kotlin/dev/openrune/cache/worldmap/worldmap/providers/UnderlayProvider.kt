package dev.openrune.cache.worldmap.worldmap.providers

/**
 * @author Kris | 22/08/2022
 */
interface UnderlayProvider {
    fun getUnderlay(id: Int): Underlay?
}

interface Underlay {
    val hue: Int
    val hueMultiplier: Int
    val saturation: Int
    val lightness: Int
}
