package dev.openrune.cache.worldmap.worldmap.providers

/**
 * @author Kris | 21/08/2022
 */
interface TextureProvider {
    fun getHsl(textureId: Int): Int
}
