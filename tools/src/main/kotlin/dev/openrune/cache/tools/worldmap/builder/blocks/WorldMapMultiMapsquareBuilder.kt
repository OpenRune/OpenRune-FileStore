package dev.openrune.cache.tools.worldmap.builder.blocks

import dev.openrune.cache.tools.worldmap.MapsquareMultiSection
import dev.openrune.cache.tools.worldmap.WorldMapMapsquare
import dev.openrune.cache.tools.worldmap.providers.MapProvider
import dev.openrune.cache.tools.worldmap.providers.ObjectProvider
import dev.openrune.filesystem.Cache

/**
 * @author Kris | 21/08/2022
 */
class WorldMapMultiMapsquareBuilder(val cache : org.openrs2.cache.Cache,private val section: MapsquareMultiSection) : WorldMapBlockBuilder<WorldMapMapsquare> {
    override fun build(mapProvider: MapProvider, objectProvider: ObjectProvider): List<WorldMapMapsquare> {
        val initial = (section.mapsquareSourceMaxX.inc() - section.mapsquareSourceMinX) * (section.mapsquareSourceMaxY.inc() - section.mapsquareSourceMinY)
        try {
            val list = ArrayList<WorldMapMapsquare>(initial)
            for (x in section.mapsquareSourceMinX..section.mapsquareSourceMaxX) {
                for (y in section.mapsquareSourceMinY..section.mapsquareSourceMaxY) {
                    val mapsquare = generateMapsquare(
                        cache = cache,
                        mapProvider,
                        objectProvider,
                        section.level,
                        section.levelsCount,
                        x,
                        y,
                        section.mapsquareDestinationMinX + (x - section.mapsquareSourceMinX),
                        section.mapsquareDestinationMinY + (y - section.mapsquareSourceMinY),
                    )
                    if (mapsquare != null) list += mapsquare
                }
                return list
            }
        }catch (Exception:Exception){}
        return emptyList()
    }
}
