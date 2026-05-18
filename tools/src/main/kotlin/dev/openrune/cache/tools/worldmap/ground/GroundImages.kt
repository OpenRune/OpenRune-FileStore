package dev.openrune.cache.tools.worldmap.ground

import java.awt.image.BufferedImage

/**
 * @author Kris | 15/08/2022
 */
data class GroundImages(val compositeTexture: BufferedImage, val mapsquares: Map<MapsquareId, BufferedImage>)
