package dev.openrune.cache.worldmap.worldmap.builder.blocks

import dev.openrune.cache.worldmap.worldmap.WorldMapZone
import dev.openrune.cache.worldmap.worldmap.WorldMapZoneData
import dev.openrune.cache.worldmap.worldmap.WorldMapZoneGeography
import dev.openrune.cache.worldmap.worldmap.ZoneMultiSection
import dev.openrune.cache.worldmap.worldmap.providers.MapProvider
import dev.openrune.cache.worldmap.worldmap.providers.ObjectProvider
import dev.openrune.filesystem.Cache

/**
 * @author Kris | 21/08/2022
 */
class WorldMapMultiZoneBuilder(val cache: Cache, private val section: ZoneMultiSection) : WorldMapBlockBuilder<WorldMapZone> {
    override fun build(mapProvider: MapProvider, objectProvider: ObjectProvider): List<WorldMapZone> {
        val map = mapProvider.getMap(cache,section.mapsquareSourceX, section.mapsquareSourceY) ?: return emptyList()
        val initial = (section.zoneSourceMaxX.inc() - section.zoneSourceMinX) * (section.zoneSourceMaxY.inc() - section.zoneSourceMinY)
        val zones = ArrayList<WorldMapZone>(initial)
        val minX = section.zoneSourceMinX shl 3
        val minY = section.zoneSourceMinY shl 3
        val maxX = section.zoneSourceMaxX.inc() shl 3
        val maxY = section.zoneSourceMaxY.inc() shl 3
        val (underlays, overlays, shapes, rotations, decorations) = map.computeGeography(
            section.level,
            section.levelsCount,
            objectProvider,
            minX until maxX,
            minY until maxY,
            (section.zoneDestinationMinX shl 3) - minX,
            (section.zoneDestinationMinY shl 3) - minY
        )
        for (zoneSourceX in section.zoneSourceMinX..section.zoneSourceMaxX) {
            for (zoneSourceY in section.zoneSourceMinY..section.zoneSourceMaxY) {
                val zoneDestinationX = section.zoneDestinationMinX + (zoneSourceX - section.zoneSourceMinX)
                val zoneDestinationY = section.zoneDestinationMinY + (zoneSourceY - section.zoneSourceMinY)
                val data = WorldMapZoneData(
                    section.level,
                    section.levelsCount,
                    section.mapsquareSourceX,
                    section.mapsquareSourceY,
                    zoneSourceX,
                    zoneSourceY,
                    section.mapsquareDestinationX,
                    section.mapsquareDestinationY,
                    zoneDestinationX,
                    zoneDestinationY,
                    -1,
                    -1
                )
                val geography = WorldMapZoneGeography(
                    section.mapsquareDestinationX,
                    section.mapsquareDestinationY,
                    zoneDestinationX,
                    zoneDestinationY,
                    underlays,
                    overlays,
                    shapes,
                    rotations,
                    decorations,
                )
                zones += WorldMapZone(data, geography)
            }
        }
        return zones
    }
}
