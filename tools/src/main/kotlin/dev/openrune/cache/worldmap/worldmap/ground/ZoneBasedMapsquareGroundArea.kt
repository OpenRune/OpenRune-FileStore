package dev.openrune.cache.worldmap.worldmap.ground

import dev.openrune.cache.worldmap.worldmap.WorldMapDecorationObject
import dev.openrune.cache.worldmap.worldmap.WorldMapZone
import dev.openrune.cache.worldmap.worldmap.providers.UnderlayProvider
import dev.openrune.cache.worldmap.worldmap.utils.WorldMapConstants

/**
 * @author Kris | 15/08/2022
 */
data class ZoneBasedMapsquareGroundArea(
    override val mapsquareDestinationX: Int,
    override val mapsquareDestinationY: Int,
    private val zones: List<WorldMapZone>,
) : MapsquareGroundArea {
    override val isEmpty: Boolean get() = zones.isEmpty()
    override val levels: Int get() = zones.firstOrNull()?.geography?.getLevelsCount() ?: 1
    override val groundArea: MapsquareGround = MapsquareGround(WorldMapConstants.MAPSQUARE_SIZE, WorldMapConstants.MAPSQUARE_SIZE)
    override fun paintGround(neighbours: Array<MapsquareGroundArea?>, bordersSeparate: Boolean, underlayProvider: UnderlayProvider): MapsquareGround {
        for (zone in zones) {
            paintArea(
                zone.geography.zoneDestinationX shl 3,
                zone.geography.zoneDestinationY shl 3,
                WorldMapConstants.ZONE_SIZE,
                WorldMapConstants.ZONE_SIZE,
                zone,
                groundArea,
                neighbours,
                bordersSeparate,
                underlayProvider,
            )
        }
        if (bordersSeparate) paintWithSurroundingMapsquares(neighbours, groundArea, underlayProvider)
        return groundArea
    }

    override fun getUnderlayId(x: Int, y: Int): Int {
        return zones.firstOrNull { it.contains(x, y) }?.geography?.getUnderlayId(x, y) ?: -1
    }

    override fun getOverlayId(z: Int, x: Int, y: Int): Int {
        return zones.firstOrNull { it.contains(x, y) }?.geography?.getOverlayId(z, x, y) ?: -1
    }

    override fun getShape(z: Int, x: Int, y: Int): Int {
        return zones.firstOrNull { it.contains(x, y) }?.geography?.getOverlayShape(z, x, y) ?: -1
    }

    override fun getRotation(z: Int, x: Int, y: Int): Int {
        return zones.firstOrNull { it.contains(x, y) }?.geography?.getOverlayRotation(z, x, y) ?: -1
    }

    override fun getDecorations(z: Int, x: Int, y: Int): List<WorldMapDecorationObject> {
        return zones.firstOrNull { it.contains(x, y) }?.geography?.getDecorations(z, x, y) ?: emptyList()
    }

    private companion object {
        private fun WorldMapZone.contains(x: Int, y: Int): Boolean {
            return x >= (this.geography.zoneDestinationX shl 3) &&
                y >= (this.geography.zoneDestinationY shl 3) &&
                x < ((this.geography.zoneDestinationX shl 3) + WorldMapConstants.ZONE_SIZE) &&
                y < ((this.geography.zoneDestinationY shl 3) + WorldMapConstants.ZONE_SIZE)
        }
    }
}
