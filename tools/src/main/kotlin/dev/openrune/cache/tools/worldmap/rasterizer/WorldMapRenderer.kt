package dev.openrune.cache.tools.worldmap.rasterizer

import dev.openrune.cache.tools.worldmap.WorldMapArea
import dev.openrune.cache.tools.worldmap.WorldMapAreaBoundaries
import dev.openrune.cache.tools.worldmap.WorldMapDecorationObject
import dev.openrune.cache.tools.worldmap.WorldMapElement
import dev.openrune.cache.tools.worldmap.ground.MapsquareGroundArea
import dev.openrune.cache.tools.worldmap.ground.MapsquareId
import dev.openrune.cache.tools.worldmap.providers.*
import dev.openrune.cache.tools.worldmap.rasterizer.font.WorldMapFontsRepository
import dev.openrune.cache.tools.worldmap.rasterizer.utils.WorldMapLabelSize
import dev.openrune.cache.tools.worldmap.rasterizer.utils.WorldMapTextLabel
import dev.openrune.cache.tools.worldmap.utils.Coordinate
import dev.openrune.cache.tools.worldmap.utils.WorldMapConstants
import dev.openrune.cache.tools.worldmap.rasterizer.sprite.MapSceneSprites
import dev.openrune.cache.tools.worldmap.rasterizer.sprite.ModIconsSprites
import dev.openrune.cache.tools.worldmap.rasterizer.sprite.OverlayRenderer
import dev.openrune.cache.tools.worldmap.rasterizer.sprite.SingleFrameSprite.Companion.toSingleSprite
import java.awt.image.BufferedImage

/**
 * @author Kris | 22/08/2022
 */
object WorldMapRenderer {

    private const val COMPOSITE_TEXTURE_PIXELS_PER_TILE = 1
    private const val COMPOSITE_TEXTURE_DOWNSCALE_FACTOR = 4

    fun generateCompositeTexture(
        providers: Providers,
        mapSceneSprites: MapSceneSprites,
        areaData: WorldMapArea,
        groundAreas: Array<Array<MapsquareGroundArea?>>,
        underlayImages: Map<MapsquareId, BufferedImage>,
        backgroundColour: Int,
        brightness: Double,
    ): BufferedImage {
        val boundaries = areaData.boundaries
        val totalWidth = boundaries.width
        val totalHeight = boundaries.height
        val overlayRenderer = OverlayRenderer(COMPOSITE_TEXTURE_PIXELS_PER_TILE)
        val rasterizers = generateRasterizers(totalWidth, totalHeight, COMPOSITE_TEXTURE_PIXELS_PER_TILE)
        val rasterizer3D = Rasterizer3D(brightness)
        for (x in 0 until totalWidth) {
            for (y in 0 until totalHeight) {
                val current = groundAreas[x][y] ?: continue
                if (current.isEmpty) continue
                val sprite = underlayImages.getValue(current.mapsquareId)
                val rasterizer = rasterizers[x][y]
                drawOverlays(
                    rasterizer3D,
                    rasterizer,
                    providers,
                    mapSceneSprites,
                    overlayRenderer,
                    current,
                    sprite,
                    COMPOSITE_TEXTURE_PIXELS_PER_TILE,
                    backgroundColour,
                    compositeTexture = true
                )
            }
        }

        val fullRasterizer = rasterizers.compose(
            totalWidth,
            totalHeight,
            COMPOSITE_TEXTURE_PIXELS_PER_TILE
        )
        return fullRasterizer.downscale(factor = COMPOSITE_TEXTURE_DOWNSCALE_FACTOR)
    }

    fun drawOverlaysAndElements(
        providers: Providers,
        mapSceneSprites: MapSceneSprites,
        areaData: WorldMapArea,
        groundAreas: Array<Array<MapsquareGroundArea?>>,
        images: Map<MapsquareId, BufferedImage>,
        pixelsPerTile: Int,
        brightness: Double,
    ): BufferedImage {
        val backgroundColour = areaData.details.backgroundColour
        val boundaries = areaData.boundaries
        val totalWidth = boundaries.width
        val totalHeight = boundaries.height
        val overlayRenderer = OverlayRenderer(pixelsPerTile)
        val rasterizers = generateRasterizers(totalWidth, totalHeight, pixelsPerTile)
        val rasterizer3D = Rasterizer3D(brightness)

        for (x in 0 until totalWidth) {
            for (y in 0 until totalHeight) {
                val current = groundAreas[x][y] ?: continue
                if (current.isEmpty) continue
                val sprite = images[current.mapsquareId] ?: continue
                val rasterizer = rasterizers[x][y]
                drawOverlays(
                    rasterizer3D,
                    rasterizer,
                    providers,
                    mapSceneSprites,
                    overlayRenderer,
                    current,
                    sprite,
                    pixelsPerTile,
                    backgroundColour
                )
            }
        }

        val icons = readMapElementsFromObjects(providers.objectProvider, boundaries, groundAreas)

        val fullRasterizer = rasterizers.compose(
            totalWidth,
            totalHeight,
            pixelsPerTile
        )
        val modIconSprites = ModIconsSprites.build(providers.graphicsDefaultsProvider, providers.spriteProvider)
        val fonts = WorldMapFontsRepository.buildWorldMapFonts(providers.spriteProvider, providers.fontMetricsProvider, modIconSprites)
        drawMapElements(
            fullRasterizer,
            providers,
            fonts,
            boundaries.minX shl 6,
            boundaries.minY shl 6,
            boundaries.width shl 6,
            boundaries.height shl 6,
            pixelsPerTile,
            areaData.data.mapElements + icons
        )

        return fullRasterizer.toBufferedImage()
    }

    private fun generateRasterizers(width: Int, height: Int, pixelsPerTile: Int): Array<Array<Rasterizer2D>> {
        return Array(width) {
            Array(height) {
                Rasterizer2D(
                    WorldMapConstants.MAPSQUARE_SIZE * pixelsPerTile,
                    WorldMapConstants.MAPSQUARE_SIZE * pixelsPerTile
                )
            }
        }
    }

    private fun readMapElementsFromObjects(
        objectProvider: ObjectProvider,
        boundaries: WorldMapAreaBoundaries,
        mapRegions: Array<Array<MapsquareGroundArea?>>,
    ): List<WorldMapElement> {
        val totalWidth = boundaries.width
        val totalHeight = boundaries.height
        val icons = mutableListOf<WorldMapElement>()
        for (mapsquareX in 0 until totalWidth) {
            for (mapsquareY in 0 until totalHeight) {
                val current = mapRegions[mapsquareX][mapsquareY] ?: continue
                if (current.isEmpty) continue
                for (x in 0 until WorldMapConstants.MAPSQUARE_SIZE) {
                    for (y in 0 until WorldMapConstants.MAPSQUARE_SIZE) {
                        for (z in 0 until current.levels) {
                            val decorations = current.getDecorations(z, x, y)
                            if (decorations.isEmpty()) continue
                            for (decoration in decorations) {
                                // Supports varbit-transformations too but since this isn't in the context of a player...
                                val mapIconId = objectProvider.getMapIconId(decoration.objectId)
                                if (mapIconId != -1) {
                                    val location = Coordinate(
                                        (boundaries.minX + mapsquareX) * WorldMapConstants.MAPSQUARE_SIZE + x,
                                        (boundaries.minY + mapsquareY) * WorldMapConstants.MAPSQUARE_SIZE + y,
                                        z
                                    )
                                    icons += WorldMapElement(mapIconId, location, false)
                                }
                            }
                        }
                    }
                }
            }
        }
        return icons
    }

    private fun Array<Array<Rasterizer2D>>.compose(totalWidth: Int, totalHeight: Int, pixelsPerTile: Int): Rasterizer2D {
        val fullRasterizerWidth = totalWidth * WorldMapConstants.MAPSQUARE_SIZE * pixelsPerTile
        val fullRasterizerHeight = totalHeight * WorldMapConstants.MAPSQUARE_SIZE * pixelsPerTile
        val fullRasterizer = Rasterizer2D(fullRasterizerWidth, fullRasterizerHeight)
        for (mapsquareX in 0 until totalWidth) {
            for (mapsquareY in 0 until totalHeight) {
                val xPositionInComposite = mapsquareX * WorldMapConstants.MAPSQUARE_SIZE * pixelsPerTile
                val yPositionInComposite = (totalHeight - mapsquareY - 1) * WorldMapConstants.MAPSQUARE_SIZE * pixelsPerTile
                val rasterizer = this[mapsquareX][mapsquareY]
                for (x in 0 until (WorldMapConstants.MAPSQUARE_SIZE * pixelsPerTile)) {
                    for (y in 0 until (WorldMapConstants.MAPSQUARE_SIZE * pixelsPerTile)) {
                        val pixel = rasterizer.pixels[x + (WorldMapConstants.MAPSQUARE_SIZE * pixelsPerTile * y)]
                        fullRasterizer.setPixel(xPositionInComposite + x, yPositionInComposite + y, pixel)
                    }
                }
                rasterizer.dispose()
            }
        }
        return fullRasterizer
    }

    private fun drawOverlays(
        rasterizer3D: Rasterizer3D,
        rasterizer2D: Rasterizer2D,
        providers: Providers,
        mapSceneSprites: MapSceneSprites,
        overlayRenderer: OverlayRenderer,
        mapsquare: MapsquareGroundArea,
        image: BufferedImage,
        pixelsPerTile: Int,
        backgroundColour: Int,
        compositeTexture: Boolean = false,
    ) {
        for (x in 0 until WorldMapConstants.MAPSQUARE_SIZE) {
            for (y in 0 until WorldMapConstants.MAPSQUARE_SIZE) {
                drawTileGround(rasterizer3D, rasterizer2D, providers, overlayRenderer, x, y, mapsquare, image, pixelsPerTile, backgroundColour)
                if (!compositeTexture) {
                    drawAboveTiles(rasterizer3D, rasterizer2D, providers, overlayRenderer, x, y, mapsquare, pixelsPerTile, backgroundColour)
                }
            }
        }
        if (!compositeTexture) {
            for (x in 0 until WorldMapConstants.MAPSQUARE_SIZE) {
                for (y in 0 until WorldMapConstants.MAPSQUARE_SIZE) {
                    drawDecorations(rasterizer2D, providers, mapSceneSprites, x, y, mapsquare, pixelsPerTile)
                }
            }
        }
    }

    private fun drawMapElements(
        rasterizer2D: Rasterizer2D,
        providers: Providers,
        fonts: WorldMapFontsRepository,
        minX: Int,
        minY: Int,
        width: Int,
        height: Int,
        pixelsPerTile: Int,
        mapElements: List<WorldMapElement>
    ) {
        val maxX = minX + width
        val maxY = minY + height
        for (mapElement in mapElements) {
            val coord = mapElement.location
            if (coord.x !in minX until maxX || coord.y !in minY until maxY) {
                continue
            }
            val offsetX = coord.x - minX
            val offsetY = coord.y - minY
            drawSpriteElement(rasterizer2D, providers, offsetX, height.dec() - offsetY, pixelsPerTile, mapElement)
        }

        for (mapElement in mapElements) {
            val coord = mapElement.location
            if (coord.x !in minX until maxX || coord.y !in minY until maxY) {
                continue
            }
            val offsetX = coord.x - minX
            val offsetY = coord.y - minY
            drawTextElement(rasterizer2D, providers, fonts, offsetX, height.dec() - offsetY, pixelsPerTile, mapElement)
        }
    }

    private fun drawSpriteElement(
        rasterizer2D: Rasterizer2D,
        providers: Providers,
        x: Int,
        y: Int,
        pixelsPerTile: Int,
        mapElement: WorldMapElement
    ) {
        // positioning only matches up on 8x, slight pixel deviation on others - probably missed something in the pos logic.
        val halfATile = 8 - (pixelsPerTile / 2)
        drawSprite(
            rasterizer2D,
            providers,
            (pixelsPerTile * x) + halfATile,
            (pixelsPerTile * y) - halfATile,
            mapElement
        )
    }

    private fun drawTextElement(
        rasterizer2D: Rasterizer2D,
        providers: Providers,
        fonts: WorldMapFontsRepository,
        x: Int,
        y: Int,
        pixelsPerTile: Int,
        mapElement: WorldMapElement
    ) {
        drawText(
            rasterizer2D,
            providers,
            fonts,
            (pixelsPerTile * x),
            (pixelsPerTile * y),
            pixelsPerTile,
            mapElement
        )
    }

    private fun drawSprite(
        rasterizer2D: Rasterizer2D,
        providers: Providers,
        x: Int,
        y: Int,
        mapElement: WorldMapElement
    ) {
        if (mapElement.elementId < 0) return
        val element = providers.mapElementProvider.getMapElement(mapElement.elementId)
        drawSpriteElement(rasterizer2D, providers, x, y, element)
    }

    private fun drawText(
        rasterizer2D: Rasterizer2D,
        providers: Providers,
        fonts: WorldMapFontsRepository,
        x: Int,
        y: Int,
        pixelsPerTile: Int,
        element: WorldMapElement
    ) {
        val mapElement = providers.mapElementProvider.getMapElement(element.elementId)
//        val label = createTextLabel(mapElement, fonts) ?: return
//        if (!label.size.shouldDrawTextLabel(pixelsPerTile.toFloat())) return
//        val font = label.font
//        font.drawLines(
//            rasterizer2D,
//            label.name,
//            x - label.width / 2,
//            y,
//            label.width,
//            label.height,
//            mapElement.textColour or -16777216,
//            0,
//            1,
//            0,
//            font.ascent / 2
//        )
    }

    private fun createTextLabel(element: MapElement, fonts: WorldMapFontsRepository): WorldMapTextLabel? {
        val name = element.text ?: return null
        val textSize = element.textSize
        val labelSize = WorldMapLabelSize.values.firstOrNull { it.textSize == textSize } ?: return null
        val font = fonts[labelSize] ?: return null
        val lineCount = font.lineCount(element.text, 1_000_000)
        val lines = arrayOfNulls<String>(lineCount)
        font.breakLines(element.text, null, lines)
        val totalHeight = lines.size * font.ascent / 2
        val maxWidth = lines.maxOfOrNull { font.getTextWidth(it) } ?: 0
        return WorldMapTextLabel(name, maxWidth, totalHeight, labelSize, font)
    }

    private fun drawSpriteElement(
        rasterizer2D: Rasterizer2D,
        providers: Providers,
        x: Int,
        y: Int,
        element: MapElement
    ) {
        val sprite = providers.spriteProvider.getSpriteSheet(element.graphic) ?: return
        val horizontalOffset = sprite.getHorizontalOffset(element.horizontalAlignment)
        val verticalOffset = sprite.getVerticalOffset(element.verticalAlignment)
        val spritePixels = sprite.toSingleSprite()
        spritePixels.drawTransparentBackgroundSprite(rasterizer2D, x + horizontalOffset, y + verticalOffset)
    }

    private fun drawDecorations(
        rasterizer2D: Rasterizer2D,
        providers: Providers,
        mapSceneSprites: MapSceneSprites,
        x: Int,
        y: Int,
        mapsquare: MapsquareGroundArea,
        pixelsPerTile: Int,
    ) {
        drawDecorativeWalls(rasterizer2D, providers, pixelsPerTile, x, y, mapsquare)
        drawDecorativeSprites(rasterizer2D, providers, mapSceneSprites, pixelsPerTile, x, y, mapsquare)
    }

    private fun drawDecorativeSprites(
        rasterizer2D: Rasterizer2D,
        providers: Providers,
        mapSceneSprites: MapSceneSprites,
        pixelsPerTile: Int,
        x: Int,
        y: Int,
        mapsquare: MapsquareGroundArea
    ) {
        for (level in 0 until mapsquare.levels) {
            val decorations = mapsquare.getDecorations(level, x, y)
            for (decoration in decorations) {
                if (!decoration.isCentrepiece && !decoration.isGroundDecoration) continue
                val mapScene = providers.objectProvider.getMapSceneId(decoration.objectId)
                if (mapScene == -1) continue
                val sprite = mapSceneSprites.indexedSprites[mapScene]
                if (mapScene != 46 && mapScene != 52) {
                    sprite.rasterizeScanLine(
                        rasterizer2D,
                        pixelsPerTile * x,
                        pixelsPerTile * (63 - y),
                        pixelsPerTile * 2,
                        pixelsPerTile * 2
                    )
                } else {
                    sprite.rasterizeScanLine(
                        rasterizer2D,
                        pixelsPerTile * x,
                        pixelsPerTile * (63 - y),
                        pixelsPerTile * 2 + 1,
                        pixelsPerTile * 2 + 1
                    )
                }
            }
        }
    }

    private fun drawDecorativeWalls(
        rasterizer: Rasterizer2D,
        providers: Providers,
        pixelsPerTile: Int,
        x: Int,
        y: Int,
        mapsquare: MapsquareGroundArea
    ) {
        for (level in 0 until mapsquare.levels) {
            val decorations = mapsquare.getDecorations(level, x, y)
            for (decoration in decorations) {
                val isWall = decoration.isWall
                if (!isWall) continue
                val boundaryType = providers.objectProvider.getBoundaryType(decoration.objectId)
                val rgb = if (boundaryType != 0) -3407872 else -3355444
                if (decoration.shape == WorldMapDecorationObject.WALL_STRAIGHT_SHAPE) {
                    drawLine(rasterizer, pixelsPerTile, x, y, decoration.rotation, rgb)
                }
                if (decoration.shape == WorldMapDecorationObject.WALL_L_SHAPE) {
                    drawLine(rasterizer, pixelsPerTile, x, y, decoration.rotation, -3355444)
                    drawLine(rasterizer, pixelsPerTile, x, y, decoration.rotation + 1, rgb)
                }
                if (decoration.shape == WorldMapDecorationObject.WALL_SQUARE_CORNER_SHAPE) {
                    if (decoration.rotation == 0) {
                        rasterizer.drawHorizontalLine(pixelsPerTile * x, pixelsPerTile * (63 - y), 1, rgb)
                    }
                    if (decoration.rotation == 1) {
                        rasterizer.drawHorizontalLine(pixelsPerTile + pixelsPerTile * x - 1, pixelsPerTile * (63 - y), 1, rgb)
                    }
                    if (decoration.rotation == 2) {
                        rasterizer.drawHorizontalLine(
                            pixelsPerTile * x + pixelsPerTile - 1,
                            pixelsPerTile * (63 - y) + pixelsPerTile - 1,
                            1,
                            rgb
                        )
                    }

                    if (decoration.rotation == 3) {
                        rasterizer.drawHorizontalLine(pixelsPerTile * x, pixelsPerTile * (63 - y) + pixelsPerTile - 1, 1, rgb)
                    }
                }
                if (decoration.shape == WorldMapDecorationObject.WALL_DIAGONAL_SHAPE) {
                    if (decoration.rotation % 2 == 0) {
                        for (index in 0 until pixelsPerTile) {
                            rasterizer.drawHorizontalLine(
                                index + pixelsPerTile * x,
                                (64 - y) * pixelsPerTile - 1 - index,
                                1,
                                rgb
                            )
                        }
                    } else {
                        for (index in 0 until pixelsPerTile) {
                            rasterizer.drawHorizontalLine(
                                index + pixelsPerTile * x,
                                index + pixelsPerTile * (63 - y),
                                1,
                                rgb
                            )
                        }
                    }
                }
            }
        }
    }

    private fun drawLine(rasterizer: Rasterizer2D, pixelsPerTile: Int, x: Int, y: Int, wallRotation: Int, colour: Int) {
        val rotation = wallRotation % 4
        if (rotation == 0) {
            rasterizer.drawVerticalLine(pixelsPerTile * x, pixelsPerTile * (63 - y), pixelsPerTile, colour)
        }
        if (rotation == 1) {
            rasterizer.drawHorizontalLine(pixelsPerTile * x, pixelsPerTile * (63 - y), pixelsPerTile, colour)
        }
        if (rotation == 2) {
            rasterizer.drawVerticalLine(
                pixelsPerTile * x + pixelsPerTile - 1,
                pixelsPerTile * (63 - y),
                pixelsPerTile,
                colour
            )
        }
        if (rotation == 3) {
            rasterizer.drawHorizontalLine(
                pixelsPerTile * x,
                pixelsPerTile * (63 - y) + pixelsPerTile - 1,
                pixelsPerTile,
                colour
            )
        }
    }

    private fun determineOverlayColour(
        rasterizer3D: Rasterizer3D,
        overlayProvider: OverlayProvider,
        textureProvider: TextureProvider,
        overlayId: Int,
        backgroundColour: Int
    ): Int {
        if (overlayId == -1) return 16711935
        return if (!overlayProvider.exists(overlayId)) {
            backgroundColour
        } else if (overlayProvider.getMinimapColour(overlayId) >= 0) {
            overlayProvider.getMinimapColour(overlayId) or -16777216
        } else if (overlayProvider.getTextureId(overlayId) >= 0) {
            val averageRgb = textureProvider.getHsl(overlayProvider.getTextureId(overlayId))
            val var12 = adjustLightness(averageRgb, 96)
            rasterizer3D.colourPalette[var12] or -16777216
        } else if (overlayProvider.getTileColour(overlayId) == 16711935) {
            backgroundColour
        } else {
            val hue = overlayProvider.getHue(overlayId)
            var saturation = overlayProvider.getSaturation(overlayId)
            val lightness = overlayProvider.getLightness(overlayId)
            if (lightness > 179) {
                saturation /= 2
            }
            if (lightness > 192) {
                saturation /= 2
            }
            if (lightness > 217) {
                saturation /= 2
            }
            if (lightness > 243) {
                saturation /= 2
            }
            val var16 = (saturation / 32 shl 7) + lightness / 2 + (hue / 4 shl 10)
            val var17 = adjustLightness(var16, 96)
            rasterizer3D.colourPalette[var17] or -16777216
        }
    }

    private fun drawTileGround(
        rasterizer3D: Rasterizer3D,
        rasterizer2D: Rasterizer2D,
        providers: Providers,
        overlayRenderer: OverlayRenderer,
        x: Int,
        y: Int,
        mapsquare: MapsquareGroundArea,
        image: BufferedImage,
        pixelsPerTile: Int,
        backgroundColour: Int,
    ) {
        val underlay = mapsquare.getUnderlayId(x, y)
        val overlayId = mapsquare.getOverlayId(0, x, y)
        val overlayProvider = providers.overlayProvider
        if (underlay == -1 && overlayId == -1) {
            rasterizer2D.fillRectangle(pixelsPerTile * x, pixelsPerTile * (63 - y), pixelsPerTile, pixelsPerTile, backgroundColour)
        }
        val overlayColour = determineOverlayColour(rasterizer3D, overlayProvider, providers.textureProvider, overlayId, backgroundColour)
        val tileShape = mapsquare.getShape(0, x, y)
        val tileRotation = mapsquare.getRotation(0, x, y)

        if (overlayId > -1 && tileShape == 0) {
            return rasterizer2D.fillRectangle(pixelsPerTile * x, pixelsPerTile * (63 - y), pixelsPerTile, pixelsPerTile, overlayColour)
        }
        val underlayColour = if (underlay == -1) backgroundColour else image.getRGB(x, y)
        if (overlayId == -1) {
            return rasterizer2D.fillRectangle(pixelsPerTile * x, pixelsPerTile * (63 - y), pixelsPerTile, pixelsPerTile, underlayColour)
        }
        overlayRenderer.drawOverlay(
            rasterizer2D,
            pixelsPerTile * x,
            pixelsPerTile * (63 - y),
            underlayColour,
            overlayColour,
            pixelsPerTile,
            pixelsPerTile,
            tileShape,
            tileRotation
        )
    }

    private fun drawAboveTiles(
        rasterizer3D: Rasterizer3D,
        rasterizer: Rasterizer2D,
        providers: Providers,
        overlayRenderer: OverlayRenderer,
        x: Int,
        y: Int,
        mapsquare: MapsquareGroundArea,
        pixelsPerTile: Int,
        backgroundColour: Int,
    ) {
        val overlayProvider = providers.overlayProvider
        for (level in 1 until mapsquare.levels) {
            val overlayId = mapsquare.getOverlayId(level, x, y)
            if (overlayId < 0) continue
            val overlayColour = determineOverlayColour(rasterizer3D, overlayProvider, providers.textureProvider, overlayId, backgroundColour)
            val shape = mapsquare.getShape(level, x, y)
            if (shape == 0) {
                rasterizer.fillRectangle(pixelsPerTile * x, pixelsPerTile * (63 - y), pixelsPerTile, pixelsPerTile, overlayColour)
            } else {
                overlayRenderer.drawOverlay(
                    rasterizer,
                    pixelsPerTile * x,
                    pixelsPerTile * (63 - y),
                    0,
                    overlayColour,
                    pixelsPerTile,
                    pixelsPerTile,
                    shape,
                    mapsquare.getRotation(level, x, y)
                )
            }
        }
    }

    private fun adjustLightness(hsl: Int, @Suppress("SameParameterValue") lightness: Int): Int {
        return when (hsl) {
            -2 -> 12345678
            -1 -> 127 - lightness.coerceIn(0..127)
            else -> (hsl and 65408) + ((hsl and 127) * lightness / 128).coerceIn(2..126)
        }
    }
}
