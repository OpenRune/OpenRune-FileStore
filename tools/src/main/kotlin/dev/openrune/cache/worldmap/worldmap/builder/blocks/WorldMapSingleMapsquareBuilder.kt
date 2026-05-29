package dev.openrune.cache.worldmap.worldmap.builder.blocks

import dev.openrune.cache.worldmap.worldmap.MapsquareSingleSection
import dev.openrune.cache.worldmap.worldmap.WorldMapMapsquare
import dev.openrune.cache.worldmap.worldmap.providers.MapProvider
import dev.openrune.cache.worldmap.worldmap.providers.ObjectProvider
import dev.openrune.filesystem.Cache

/**
 * @author Kris | 21/08/2022
 */
class WorldMapSingleMapsquareBuilder(val cache: Cache, private val section: MapsquareSingleSection) : WorldMapBlockBuilder<WorldMapMapsquare> {
    override fun build(mapProvider: MapProvider, objectProvider: ObjectProvider): List<WorldMapMapsquare> {
        val mapsquare = generateMapsquare(
            cache = cache,
            mapProvider,
            objectProvider,
            section.level,
            section.levelsCount,
            section.mapsquareSourceX,
            section.mapsquareSourceY,
            section.mapsquareDestinationX,
            section.mapsquareDestinationY
        )
        return if (mapsquare == null) emptyList() else listOf(mapsquare)
    }
}