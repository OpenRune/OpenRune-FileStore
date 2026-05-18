package dev.openrune.cache.tools.worldmap.providers

/**
 * @author Kris | 21/08/2022
 */
interface OverlayProvider {
    fun exists(id: Int): Boolean
    fun getMinimapColour(id: Int): Int
    fun getTileColour(id: Int): Int
    fun getTextureId(id: Int): Int
    fun getHue(id: Int): Int
    fun getSaturation(id: Int): Int
    fun getLightness(id: Int): Int
}
