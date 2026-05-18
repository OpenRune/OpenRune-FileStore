package dev.openrune.cache.tools.worldmap.providers

import dev.openrune.cache.tools.worldmap.rasterizer.provider.FontMetricsProvider
import dev.openrune.cache.tools.worldmap.rasterizer.provider.GraphicsDefaultsProvider
import dev.openrune.cache.tools.worldmap.rasterizer.provider.SpriteProvider

/**
 * @author Kris | 21/08/2022
 */
data class Providers(
    val cacheProvider: CacheProvider,
    val textureProvider: TextureProvider,
    val spriteProvider: SpriteProvider,
    val fontMetricsProvider: FontMetricsProvider,
    val objectProvider: ObjectProvider,
    val mapProvider: MapProvider,
    val overlayProvider: OverlayProvider,
    val mapElementProvider: MapElementConfigProvider,
    val graphicsDefaultsProvider: GraphicsDefaultsProvider,
    val underlayProvider: UnderlayProvider,
)
