package dev.openrune.cache.tools.worldmap.builder.blocks

import dev.openrune.cache.tools.worldmap.WorldMapZone
import dev.openrune.cache.tools.worldmap.WorldMapZoneData
import dev.openrune.cache.tools.worldmap.WorldMapZoneGeography
import dev.openrune.cache.tools.worldmap.ZoneSingleSection
import dev.openrune.cache.tools.worldmap.providers.MapProvider
import dev.openrune.cache.tools.worldmap.providers.ObjectProvider
import dev.openrune.filesystem.Cache

/**
 * @author Kris | 21/08/2022
 */
class WorldMapSingleZoneBuilder(val cache: org.openrs2.cache.Cache,private val section: ZoneSingleSection) : WorldMapBlockBuilder<WorldMapZone> {
    override fun build(mapProvider: MapProvider, objectProvider: ObjectProvider): List<WorldMapZone> {
        val map = mapProvider.getMap(cache,section.mapsquareSourceX, section.mapsquareSourceY) ?: return emptyList()
        val data = WorldMapZoneData(
            section.level,
            section.levelsCount,
            section.mapsquareSourceX,
            section.mapsquareSourceY,
            section.zoneSourceX,
            section.zoneSourceY,
            section.mapsquareDestinationX,
            section.mapsquareDestinationY,
            section.zoneDestinationX,
            section.zoneDestinationY,
            -1,
            -1
        )
        val minX = section.zoneSourceX shl 3
        val minY = section.zoneSourceY shl 3
        val maxX = minX + 8
        val maxY = minY + 8
        val (underlays, overlays, shapes, rotations, decorations) = map.computeGeography(
            section.level,
            section.levelsCount,
            objectProvider,
            minX until maxX,
            minY until maxY,
            (section.zoneDestinationX shl 3) - minX,
            (section.zoneDestinationY shl 3) - minY
        )
        val geography = WorldMapZoneGeography(
            section.mapsquareDestinationX,
            section.mapsquareDestinationY,
            section.zoneDestinationX,
            section.zoneDestinationY,
            underlays,
            overlays,
            shapes,
            rotations,
            decorations,
        )
        return listOf(WorldMapZone(data, geography))
    }
}
