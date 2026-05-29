package dev.openrune.cache.worldmap.worldmap.ground

import dev.openrune.cache.worldmap.worldmap.WorldMapDecorationObject
import dev.openrune.cache.worldmap.worldmap.WorldMapMapsquare
import dev.openrune.cache.worldmap.worldmap.providers.UnderlayProvider
import dev.openrune.cache.worldmap.worldmap.utils.WorldMapConstants

/**
 * @author Kris | 15/08/2022
 */
data class MapsquareBasedMapsquareGroundArea(
    override val mapsquareDestinationX: Int,
    override val mapsquareDestinationY: Int,
    private val parent: WorldMapMapsquare,
) : MapsquareGroundArea {
    override val isEmpty: Boolean get() = false
    override val levels: Int get() = parent.geography.getLevelsCount()
    override val groundArea: MapsquareGround = MapsquareGround(WorldMapConstants.MAPSQUARE_SIZE, WorldMapConstants.MAPSQUARE_SIZE)
    override fun paintGround(
        neighbours: Array<MapsquareGroundArea?>,
        bordersSeparate: Boolean,
        underlayProvider: UnderlayProvider
    ): MapsquareGround {
        paintArea(
            0,
            0,
            WorldMapConstants.MAPSQUARE_SIZE,
            WorldMapConstants.MAPSQUARE_SIZE,
            parent,
            groundArea,
            neighbours,
            bordersSeparate,
            underlayProvider
        )
        if (bordersSeparate) paintWithSurroundingMapsquares(neighbours, groundArea, underlayProvider)
        return groundArea
    }

    override fun getUnderlayId(x: Int, y: Int): Int {
        return parent.geography.getUnderlayId(x, y)
    }

    override fun getOverlayId(z: Int, x: Int, y: Int): Int {
        return parent.geography.getOverlayId(z, x, y)
    }

    override fun getShape(z: Int, x: Int, y: Int): Int {
        return parent.geography.getOverlayShape(z, x, y)
    }

    override fun getRotation(z: Int, x: Int, y: Int): Int {
        return parent.geography.getOverlayRotation(z, x, y)
    }

    override fun getDecorations(z: Int, x: Int, y: Int): List<WorldMapDecorationObject> {
        return parent.geography.getDecorations(z, x, y)
    }
}
