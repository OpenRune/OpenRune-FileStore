package dev.openrune.cache.worldmap.worldmap.ground

import dev.openrune.cache.worldmap.worldmap.WorldMapBlock
import dev.openrune.cache.worldmap.worldmap.WorldMapDecorationObject
import dev.openrune.cache.worldmap.worldmap.providers.UnderlayProvider
import dev.openrune.cache.worldmap.worldmap.utils.WorldMapConstants.MAPSQUARE_SIZE

/**
 * @author Kris | 15/08/2022
 */
interface MapsquareGroundArea {
    val mapsquareDestinationX: Int
    val mapsquareDestinationY: Int
    val isEmpty: Boolean
    val mapsquareId: MapsquareId get() = MapsquareId(mapsquareDestinationX, mapsquareDestinationY)
    val levels: Int
    val groundArea: MapsquareGround

    fun paintGround(neighbours: Array<MapsquareGroundArea?>, bordersSeparate: Boolean, underlayProvider: UnderlayProvider): MapsquareGround
    fun getUnderlayId(x: Int, y: Int): Int
    fun getOverlayId(z: Int, x: Int, y: Int): Int
    fun getShape(z: Int, x: Int, y: Int): Int
    fun getRotation(z: Int, x: Int, y: Int): Int
    fun getDecorations(z: Int, x: Int, y: Int): List<WorldMapDecorationObject>

    fun paintArea(
        minX: Int,
        minY: Int,
        width: Int,
        height: Int,
        area: WorldMapBlock,
        ground: MapsquareGround,
        neighbours: Array<MapsquareGroundArea?>,
        bordersSeparate: Boolean,
        underlayProvider: UnderlayProvider,
    ) {
        for (x in minX until minX + width) {
            for (y in minY until minY + height) {
                val underlayId = area.geography.getUnderlayId(x, y)
                if (underlayId == -1) continue
                val underlay = underlayProvider.getUnderlay(underlayId)
                if (bordersSeparate) {
                    ground.smoothenArea(x, y, AREA_BRUSH_SIZE, underlay)
                } else {
                    ground.smoothenAreaWithNeighbours(x, y, AREA_BRUSH_SIZE, underlay, neighbours)
                }
            }
        }
    }

    fun paintWithSurroundingMapsquares(
        neighbours: Array<MapsquareGroundArea?>,
        ground: MapsquareGround,
        underlayProvider: UnderlayProvider,
    ) {
        for (index in neighbourPositions.indices) {
            val neighbourPosition = neighbourPositions[index]
            val source = neighbours[neighbourPosition.index] ?: continue
            var destinationOffsetX = 0
            var destinationOffsetY = 0
            var width = MAPSQUARE_SIZE
            var length = MAPSQUARE_SIZE
            var sourceOffsetX = 0
            var sourceOffsetY = 0
            when (neighbourPosition) {
                NeighbourPosition.SouthWest -> {
                    sourceOffsetX = MAPSQUARE_SIZE - BORDER_SIZE
                    sourceOffsetY = MAPSQUARE_SIZE - BORDER_SIZE
                    width = BORDER_SIZE
                    length = BORDER_SIZE
                }
                NeighbourPosition.NorthEast -> {
                    destinationOffsetY = MAPSQUARE_SIZE - BORDER_SIZE
                    length = BORDER_SIZE
                    destinationOffsetX = MAPSQUARE_SIZE - BORDER_SIZE
                    width = BORDER_SIZE
                }
                NeighbourPosition.NorthWest -> {
                    destinationOffsetY = MAPSQUARE_SIZE - BORDER_SIZE
                    length = BORDER_SIZE
                    sourceOffsetX = MAPSQUARE_SIZE - BORDER_SIZE
                    width = BORDER_SIZE
                }
                NeighbourPosition.South -> {
                    sourceOffsetY = MAPSQUARE_SIZE - BORDER_SIZE
                    length = BORDER_SIZE
                }
                NeighbourPosition.East -> {
                    destinationOffsetX = MAPSQUARE_SIZE - BORDER_SIZE
                    width = BORDER_SIZE
                }
                NeighbourPosition.North -> {
                    destinationOffsetY = MAPSQUARE_SIZE - BORDER_SIZE
                    length = BORDER_SIZE
                }
                NeighbourPosition.SouthEast -> {
                    sourceOffsetY = MAPSQUARE_SIZE - BORDER_SIZE
                    length = BORDER_SIZE
                    destinationOffsetX = MAPSQUARE_SIZE - BORDER_SIZE
                    width = BORDER_SIZE
                }
                NeighbourPosition.West -> {
                    sourceOffsetX = MAPSQUARE_SIZE - BORDER_SIZE
                    width = BORDER_SIZE
                }
            }
            smoothenMapsquare(
                sourceOffsetX,
                sourceOffsetY,
                destinationOffsetX,
                destinationOffsetY,
                width,
                length,
                source,
                ground,
                underlayProvider
            )
        }
    }

    private fun smoothenMapsquare(
        sourceOffsetX: Int,
        sourceOffsetY: Int,
        destinationOffsetX: Int,
        destinationOffsetY: Int,
        width: Int,
        height: Int,
        source: MapsquareGroundArea,
        destination: MapsquareGround,
        underlayProvider: UnderlayProvider,
    ) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                val underlayId = source.getUnderlayId(x + sourceOffsetX, y + sourceOffsetY)
                if (underlayId == -1) continue
                val underlay = underlayProvider.getUnderlay(underlayId)
                destination.smoothenArea(destinationOffsetX + x, y + destinationOffsetY, BORDER_BRUSH_SIZE, underlay)
            }
        }
    }

    companion object {
        private const val BORDER_SIZE = 5
        private const val BORDER_BRUSH_SIZE = 5
        private const val AREA_BRUSH_SIZE = 5

        private val neighbourPositions = listOf(
            NeighbourPosition.NorthEast,
            NeighbourPosition.SouthEast,
            NeighbourPosition.East,
            NeighbourPosition.North,
            NeighbourPosition.West,
            NeighbourPosition.SouthWest,
            NeighbourPosition.NorthWest,
            NeighbourPosition.South
        )
    }
}
