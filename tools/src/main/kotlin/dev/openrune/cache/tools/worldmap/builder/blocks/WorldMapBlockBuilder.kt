package dev.openrune.cache.tools.worldmap.builder.blocks

import dev.openrune.cache.tools.worldmap.*
import dev.openrune.cache.tools.worldmap.WorldMapDecorationObject.Companion.CENTREPIECE_DIAGONAL_SHAPE
import dev.openrune.cache.tools.worldmap.WorldMapDecorationObject.Companion.CENTREPIECE_STRAIGHT_SHAPE
import dev.openrune.cache.tools.worldmap.WorldMapDecorationObject.Companion.GROUND_DECOR_SHAPE
import dev.openrune.cache.tools.worldmap.WorldMapDecorationObject.Companion.WALL_DIAGONAL_SHAPE
import dev.openrune.cache.tools.worldmap.WorldMapDecorationObject.Companion.WALL_L_SHAPE
import dev.openrune.cache.tools.worldmap.WorldMapDecorationObject.Companion.WALL_SQUARE_CORNER_SHAPE
import dev.openrune.cache.tools.worldmap.WorldMapDecorationObject.Companion.WALL_STRAIGHT_SHAPE
import dev.openrune.cache.tools.worldmap.providers.*
import dev.openrune.cache.tools.worldmap.utils.MapFlags
import dev.openrune.cache.tools.worldmap.utils.Tuple5
import dev.openrune.cache.tools.worldmap.utils.WorldMapConstants
import dev.openrune.filesystem.Cache
import kotlin.math.min

/**
 * @author Kris | 21/08/2022
 */
interface WorldMapBlockBuilder<out T : WorldMapBlock> {

    fun build(mapProvider: MapProvider, objectProvider: ObjectProvider): List<T>

    private fun getViewLevel(z: Int, x: Int, y: Int, landscape: Landscape): Int {
        val visibleBelow = landscape.getFlags(z, x, y) and MapFlags.FLAG_VISIBLE_BELOW != 0
        if (visibleBelow) return 0
        return if (z > 0 && landscape.getFlags(1, x, y) and MapFlags.FLAG_LINK_BELOW != 0) z.dec() else z
    }

    private fun WorldMapObject.accept(level: Int, objectProvider: ObjectProvider, landscape: Landscape): Boolean {
        val mapScene = objectProvider.getMapSceneId(this.id)
        if (mapScene == MAPSCENE_MAP_EXCLUDED) return false
        if (shape == GROUND_DECOR_SHAPE && objectProvider.getMapIconId(id) != -1) return true
        val bridge = landscape.getFlags(level = 1, coordinate.x, coordinate.y) and MapFlags.FLAG_LINK_BELOW != 0
        val locLevel = if (bridge) coordinate.level.dec() else coordinate.level
        if (level != locLevel) return false
        if (shape == WALL_STRAIGHT_SHAPE || shape == WALL_L_SHAPE || shape == WALL_SQUARE_CORNER_SHAPE || shape == WALL_DIAGONAL_SHAPE) {
            return true
        }
        return mapScene != -1 && (shape == CENTREPIECE_STRAIGHT_SHAPE || shape == CENTREPIECE_DIAGONAL_SHAPE || shape == GROUND_DECOR_SHAPE)
    }

    fun Mapsquare.computeGeography(
        baseLevel: Int,
        levelsCount: Int,
        objectProvider: ObjectProvider,
        xRange: IntRange = 0 until WorldMapConstants.MAPSQUARE_SIZE,
        yRange: IntRange = 0 until WorldMapConstants.MAPSQUARE_SIZE,
        xOffset: Int = 0,
        yOffset: Int = 0,
    ): Tuple5<Underlays, Overlays, Shapes, Rotations, Decorations> {
        val levels = min(WorldMapConstants.MAX_LEVELS, levelsCount)
        val (underlays, overlays, shapes, rotations, decorations) = WorldMapGeography.constructGeography(levels)
        for (obj in this.objects) {
            val locX = obj.coordinate.x and 0x3F
            val locY = obj.coordinate.y and 0x3F
            if (locX !in xRange || locY !in yRange) continue
            if (obj.accept(baseLevel, objectProvider, landscape)) {
                val destLevel = obj.coordinate.level - baseLevel
                if (destLevel < 0 || destLevel >= decorations.size) continue
                decorations[destLevel][locX + xOffset][locY + yOffset] += WorldMapDecorationObject(
                    obj.id,
                    obj.shape,
                    obj.rotation
                )
            }
        }
        for (x in xRange) {
            for (y in yRange) {
                val maxLevel = calculateMaxLevel(
                    baseLevel,
                    levelsCount,
                    x,
                    y,
                    landscape,
                    decorations
                )
                // Below is not entirely perfect, 8 tiles in the "main" map end up generating a different underlay id, but it's close enough.
                val hasBridge = landscape.getFlags(1, x, y) and MapFlags.FLAG_LINK_BELOW != 0
                val aboveUnderlay = if (hasBridge) landscape.getUnderlayId(1, x, y) else -1
                val finalUnderlay = if (aboveUnderlay != -1) {
                    aboveUnderlay
                } else {
                    landscape.getUnderlayId(baseLevel, x, y)
                }
                underlays[0][x + xOffset][y + yOffset] = (finalUnderlay + 1).toShort()
                // Overlay logic is a little more flawed than the rest - just shy of 6000 tiles seem to generate a slightly different output.
                // In the end, it doesn't really matter though as almost all the differences are just due to level differences,
                // but as the world map flattens everything onto a single level, it makes no difference in the end.
                for (z in baseLevel..maxLevel) {
                    overlays[z - baseLevel][x + xOffset][y + yOffset] = ((landscape.getOverlayId(z, x, y) + 1) and 0xFF).toShort()
                    shapes[z - baseLevel][x + xOffset][y + yOffset] = landscape.getOverlayShape(z, x, y).toByte()
                    rotations[z - baseLevel][x + xOffset][y + yOffset] = landscape.getOverlayRotation(z, x, y).toByte()
                }
            }
        }
        return Tuple5(underlays, overlays, shapes, rotations, decorations)
    }

    private fun calculateMaxLevel(
        baseLevel: Int,
        levelsCount: Int,
        x: Int,
        y: Int,
        landscape: Landscape,
        decorations: Decorations
    ): Int {
        var maxLevel = baseLevel
        for (z in levelsCount.dec() downTo baseLevel) {
            val hasDecoration = decorations[z][x][y].isNotEmpty()
            if (hasDecoration) {
                maxLevel = z
                break
            }
            val viewLevel = getViewLevel(z, x, y, landscape)
            if (viewLevel <= baseLevel) {
                maxLevel = z
                break
            }
        }
        return maxLevel
    }

    fun generateMapsquare(
        cache: org.openrs2.cache.Cache,
        mapProvider: MapProvider,
        objectProvider: ObjectProvider,
        level: Int,
        levelsCount: Int,
        mapsquareSourceX: Int,
        mapsquareSourceY: Int,
        mapsquareDestinationX: Int,
        mapsquareDestinationY: Int
    ): WorldMapMapsquare? {
        val map = mapProvider.getMap(cache,mapsquareSourceX, mapsquareSourceY) ?: return null
        val data = WorldMapMapsquareData(
            level,
            levelsCount,
            mapsquareSourceX,
            mapsquareSourceY,
            mapsquareDestinationX,
            mapsquareDestinationY,
            -1,
            -1
        )

        val (underlays, overlays, shapes, rotations, decorations) = map.computeGeography(level, levelsCount, objectProvider)
        val geography = WorldMapMapsquareGeography(
            mapsquareDestinationX,
            mapsquareDestinationY,
            underlays,
            overlays,
            shapes,
            rotations,
            decorations,
        )
        return WorldMapMapsquare(data, geography)
    }

    private companion object {
        private const val MAPSCENE_MAP_EXCLUDED = 22
    }
}
