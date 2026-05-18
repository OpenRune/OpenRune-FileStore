@file:Suppress("DuplicatedCode")

package dev.openrune.cache.tools.worldmap.ground

import dev.openrune.cache.tools.worldmap.WORLD_MAP_GROUND_ARCHIVE
import dev.openrune.cache.tools.worldmap.WorldMapArea
import dev.openrune.cache.tools.worldmap.ground.NeighbourPosition.*
import dev.openrune.cache.tools.worldmap.providers.*
import dev.openrune.cache.tools.worldmap.rasterizer.WorldMapRenderer.drawOverlaysAndElements
import dev.openrune.cache.tools.worldmap.rasterizer.WorldMapRenderer.generateCompositeTexture
import dev.openrune.cache.tools.worldmap.toImage
import dev.openrune.cache.tools.worldmap.rasterizer.sprite.MapSceneSprites
import java.awt.image.BufferedImage
import java.util.LinkedHashMap
import kotlin.math.max
import kotlin.math.min

/**
 * @author Kris | 15/08/2022
 */
data class MapsquareGround(val width: Int, val height: Int) {
    private val hue = Array(width) { IntArray(height) }
    private val saturation = Array(width) { IntArray(height) }
    private val lightness = Array(width) { IntArray(height) }
    private val count = Array(width) { IntArray(height) }

    fun toImage(): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                image.setRGB(x, y, calculateAverageRgb(x, y))
            }
        }
        return image
    }

    fun calculateAverageRgb(x: Int, y: Int): Int {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) return 0
        if (lightness[x][y] == 0) return 0
        val hue = hue[x][y] / count[x][y]
        val saturation = saturation[x][y] / count[x][y]
        val lightness = lightness[x][y] / count[x][y]
        return hslToRgb(
            hue.toDouble() / MAX_PIXEL_VALUE,
            saturation.toDouble() / MAX_PIXEL_VALUE,
            lightness.toDouble() / MAX_PIXEL_VALUE
        )
    }

    fun smoothenAreaWithNeighbours(
        x: Int,
        y: Int,
        radius: Int,
        underlay: Underlay?,
        neighbours: Array<MapsquareGroundArea?>
    ) {
        if (underlay == null) return
        val minX = x - radius
        val maxX = x + radius
        val minY = y - radius
        val maxY = y + radius
        for (xInMapsquare in minX until maxX) {
            for (yInMapsquare in minY until maxY) {
                val position = when {
                    xInMapsquare < 0 && yInMapsquare < 0 -> SouthWest
                    xInMapsquare < 0 && yInMapsquare >= 64 -> NorthWest
                    xInMapsquare >= 64 && yInMapsquare < 0 -> SouthEast
                    xInMapsquare >= 64 && yInMapsquare >= 64 -> NorthEast
                    xInMapsquare < 0 -> West
                    xInMapsquare >= 64 -> East
                    yInMapsquare < 0 -> South
                    yInMapsquare >= 64 -> North
                    else -> null
                }
                if (position == null) {
                    setColour(xInMapsquare, yInMapsquare, underlay)
                } else {
                    neighbours[position.index]?.groundArea
                        ?.setColour(xInMapsquare and 0x3F, yInMapsquare and 0x3F, underlay)
                }
            }
        }
    }

    fun smoothenArea(x: Int, y: Int, radius: Int, underlay: Underlay?) {
        if (underlay == null) return
        if (x + radius < 0 || y + radius < 0) return
        if (x - radius > this.width || y - radius > this.height) return
        val minX = max(0, x - radius)
        val maxX = min(this.width, x + radius)
        val minY = max(0, y - radius)
        val maxY = min(this.height, y + radius)
        for (xInMapsquare in minX until maxX) {
            for (yInMapsquare in minY until maxY) {
                hue[xInMapsquare][yInMapsquare] += underlay.hue * MAX_PIXEL_VALUE / underlay.hueMultiplier
                saturation[xInMapsquare][yInMapsquare] += underlay.saturation
                lightness[xInMapsquare][yInMapsquare] += underlay.lightness
                count[xInMapsquare][yInMapsquare]++
            }
        }
    }

    companion object {
        private const val MAX_PIXEL_VALUE = 256
        private const val NEIGHBOURS_COUNT = 8

        private fun MapsquareGround.setColour(xInMapsquare: Int, yInMapsquare: Int, underlay: Underlay) {
            hue[xInMapsquare][yInMapsquare] += underlay.hue * MAX_PIXEL_VALUE / underlay.hueMultiplier
            saturation[xInMapsquare][yInMapsquare] += underlay.saturation
            lightness[xInMapsquare][yInMapsquare] += underlay.lightness
            count[xInMapsquare][yInMapsquare]++
        }

        fun generateSprites(
            providers: Providers,
            areaData: WorldMapArea,
            bordersSeparate: Boolean,
            backgroundColour: Int,
            brightness: Double,
        ): Pair<Map<MapsquareId, BufferedImage>, BufferedImage> {
            val boundaries = areaData.boundaries
            val totalWidth = boundaries.width
            val totalHeight = boundaries.height
            val groundAreas = Array<Array<MapsquareGroundArea?>>(totalWidth) { arrayOfNulls(totalHeight) }
            val underlayImages = buildUnderlayImages(groundAreas, areaData, bordersSeparate, providers.underlayProvider)
            val mapSceneSprites = MapSceneSprites.build(providers.graphicsDefaultsProvider, providers.spriteProvider)
            val composite = generateCompositeTexture(
                providers,
                mapSceneSprites,
                areaData,
                groundAreas,
                underlayImages,
                backgroundColour,
                brightness,
            )
            return underlayImages to composite
        }

        fun generateMapImage(
            providers: Providers,
            areaData: WorldMapArea,
            bordersSeparate: Boolean,
            pixelsPerTile: Int,
            brightness: Double,
            generateUnderlays: Boolean,
        ): BufferedImage {
            val boundaries = areaData.boundaries
            val totalWidth = boundaries.width
            val totalHeight = boundaries.height
            val groundAreas = Array<Array<MapsquareGroundArea?>>(totalWidth) { arrayOfNulls(totalHeight) }
            val images = if (generateUnderlays) {
                buildUnderlayImages(groundAreas, areaData, bordersSeparate, providers.underlayProvider)
            } else {
                parseUnderlayImages(providers.cacheProvider, groundAreas, areaData)
            }
            val mapSceneSprites = MapSceneSprites.build(providers.graphicsDefaultsProvider, providers.spriteProvider)
            return drawOverlaysAndElements(
                providers,
                mapSceneSprites,
                areaData,
                groundAreas,
                images,
                pixelsPerTile,
                brightness,
            )
        }

        private fun parseUnderlayImages(
            cacheProvider: CacheProvider,
            mapRegions: Array<Array<MapsquareGroundArea?>>,
            areaData: WorldMapArea,
        ): Map<MapsquareId, BufferedImage> {
            val boundaries = areaData.boundaries
            val minX = boundaries.minX
            val minY = boundaries.minY
            val totalWidth = boundaries.width
            val totalHeight = boundaries.height
            val mapsquareImages = mutableMapOf<MapsquareId, BufferedImage>()
            for (mapsquare in areaData.data.mapsquares) {
                val region = MapsquareBasedMapsquareGroundArea(
                    mapsquare.data.mapsquareDestinationX,
                    mapsquare.data.mapsquareDestinationY,
                    mapsquare,
                )
                mapRegions[mapsquare.data.mapsquareDestinationX - minX][mapsquare.data.mapsquareDestinationY - minY] = region
                if (!cacheProvider.exists(WORLD_MAP_GROUND_ARCHIVE, mapsquare.data.groupId, mapsquare.data.fileId)) continue
                val buffer = cacheProvider.read(WORLD_MAP_GROUND_ARCHIVE, mapsquare.data.groupId, mapsquare.data.fileId)
                mapsquareImages[region.mapsquareId] = buffer.toImage()
                buffer.release()
            }

            for (x in 0 until totalWidth) {
                for (y in 0 until totalHeight) {
                    val current = mapRegions[x][y]
                    if (current != null) continue
                    val mapsquareDestinationX = minX + x
                    val mapsquareDestinationY = minY + y
                    val zones = areaData.data.zones.filter {
                        it.geography.mapsquareDestinationX == mapsquareDestinationX &&
                            it.geography.mapsquareDestinationY == mapsquareDestinationY
                    }
                    val new = ZoneBasedMapsquareGroundArea(
                        mapsquareDestinationX,
                        mapsquareDestinationY,
                        zones,
                    )
                    mapRegions[x][y] = new
                    val mapsquareId = new.mapsquareId
                    if (zones.isEmpty() || mapsquareImages.containsKey(mapsquareId)) continue
                    val zone = zones.first()
                    if (!cacheProvider.exists(WORLD_MAP_GROUND_ARCHIVE, zone.data.groupId, 0)) continue
                    val buffer = cacheProvider.read(WORLD_MAP_GROUND_ARCHIVE, zone.data.groupId, 0)
                    mapsquareImages[mapsquareId] = buffer.toImage()
                    buffer.release()
                }
            }
            return mapsquareImages
        }

        private fun buildUnderlayImages(
            mapRegions: Array<Array<MapsquareGroundArea?>>,
            areaData: WorldMapArea,
            bordersSeparate: Boolean,
            underlayProvider: UnderlayProvider,
        ): Map<MapsquareId, BufferedImage> {
            val boundaries = areaData.boundaries
            val minX = boundaries.minX
            val minY = boundaries.minY
            val totalWidth = boundaries.width
            val totalHeight = boundaries.height
            for (mapsquare in areaData.data.mapsquares) {
                val region = MapsquareBasedMapsquareGroundArea(
                    mapsquare.data.mapsquareDestinationX,
                    mapsquare.data.mapsquareDestinationY,
                    mapsquare,
                )
                mapRegions[mapsquare.data.mapsquareDestinationX - minX][mapsquare.data.mapsquareDestinationY - minY] = region
            }

            for (x in 0 until totalWidth) {
                for (y in 0 until totalHeight) {
                    val current = mapRegions[x][y]
                    if (current != null) continue
                    val mapsquareDestinationX = minX + x
                    val mapsquareDestinationY = minY + y
                    val zones = areaData.data.zones.filter {
                        it.geography.mapsquareDestinationX == mapsquareDestinationX &&
                            it.geography.mapsquareDestinationY == mapsquareDestinationY
                    }
                    val new = ZoneBasedMapsquareGroundArea(
                        mapsquareDestinationX,
                        mapsquareDestinationY,
                        zones,
                    )
                    mapRegions[x][y] = new
                }
            }
            val images = LinkedHashMap<MapsquareId, BufferedImage>()
            val repository = mutableMapOf<MapsquareId, MapsquareGround>()
            val neighbours = arrayOfNulls<MapsquareGroundArea>(NEIGHBOURS_COUNT)
            for (x in 0 until totalWidth) {
                for (y in 0 until totalHeight) {
                    val current = mapRegions[x][y]
                    if (current == null || current.isEmpty) {
                        continue
                    }
                    fillNeighbouringZones(mapRegions, x, y, neighbours)
                    val ground = current.paintGround(neighbours, bordersSeparate, underlayProvider)
                    repository[current.mapsquareId] = ground
                }
            }

            for ((mapsquare, ground) in repository) {
                val sprite = ground.toImage()
                images[mapsquare] = sprite
            }
            return images
        }

        private fun fillNeighbouringZones(
            mapRegions: Array<Array<MapsquareGroundArea?>>,
            mapsquareDestinationX: Int,
            mapsquareDestinationY: Int,
            neighbours: Array<MapsquareGroundArea?>
        ) {
            val isWest = mapsquareDestinationX <= 0
            val isEast = mapsquareDestinationX >= mapRegions.size - 1
            val isSouth = mapsquareDestinationY <= 0
            val isNorth = mapsquareDestinationY >= mapRegions.first().size - 1
            neighbours[North] = if (isNorth) null else mapRegions[mapsquareDestinationX][mapsquareDestinationY + 1]
            neighbours[NorthEast] = if (!isNorth && !isEast) mapRegions[mapsquareDestinationX + 1][mapsquareDestinationY + 1] else null
            neighbours[NorthWest] = if (!isNorth && !isWest) mapRegions[mapsquareDestinationX - 1][mapsquareDestinationY + 1] else null
            neighbours[East] = if (isEast) null else mapRegions[mapsquareDestinationX + 1][mapsquareDestinationY]
            neighbours[West] = if (isWest) null else mapRegions[mapsquareDestinationX - 1][mapsquareDestinationY]
            neighbours[South] = if (isSouth) null else mapRegions[mapsquareDestinationX][mapsquareDestinationY - 1]
            neighbours[SouthEast] = if (!isSouth && !isEast) mapRegions[mapsquareDestinationX + 1][mapsquareDestinationY - 1] else null
            neighbours[SouthWest] = if (!isSouth && !isWest) mapRegions[mapsquareDestinationX - 1][mapsquareDestinationY - 1] else null
        }

        private operator fun Array<MapsquareGroundArea?>.set(position: NeighbourPosition, value: MapsquareGroundArea?) {
            this[position.index] = value
        }

        private fun hslToRgb(hue: Double, saturation: Double, lightness: Double): Int {
            var redPercentage = lightness
            var greenPercentage = lightness
            var bluePercentage = lightness
            if (saturation != 0.0) {
                val a = if (lightness < 0.5) {
                    lightness * (1.0 + saturation)
                } else {
                    saturation + lightness - lightness * saturation
                }
                val b = lightness * 2.0 - a
                var c = hue + 0.3333333333333333
                if (c > 1.0) {
                    --c
                }
                var var20 = hue - 0.3333333333333333
                if (var20 < 0.0) {
                    ++var20
                }
                redPercentage = if (c * 6.0 < 1.0) {
                    b + 6.0 * (a - b) * c
                } else if (2.0 * c < 1.0) {
                    a
                } else if (c * 3.0 < 2.0) {
                    b + (0.6666666666666666 - c) * (a - b) * 6.0
                } else {
                    b
                }
                greenPercentage = if (6.0 * hue < 1.0) {
                    b + hue * (a - b) * 6.0
                } else if (2.0 * hue < 1.0) {
                    a
                } else if (hue * 3.0 < 2.0) {
                    b + 6.0 * (0.6666666666666666 - hue) * (a - b)
                } else {
                    b
                }
                bluePercentage = if (6.0 * var20 < 1.0) {
                    b + var20 * 6.0 * (a - b)
                } else if (2.0 * var20 < 1.0) {
                    a
                } else if (var20 * 3.0 < 2.0) {
                    b + 6.0 * (a - b) * (0.6666666666666666 - var20)
                } else {
                    b
                }
            }
            val red = (redPercentage * MAX_PIXEL_VALUE).toInt()
            val green = (greenPercentage * MAX_PIXEL_VALUE).toInt()
            val blue = (bluePercentage * MAX_PIXEL_VALUE).toInt()
            return blue + (green shl 8) + (red shl 16)
        }
    }
}
