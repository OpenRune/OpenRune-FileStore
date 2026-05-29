package dev.openrune.cache.worldmap.worldmap

/**
 * World map cache layout changed at client revision 238.
 * @see <a href="file:///C:/Users/chris/Desktop/deober/output/src/deob/WorldMapRenderer.java">WorldMapRenderer</a>
 */
object WorldMapFormat {
    const val REVISION = 238

    fun isLegacy(revision: Int): Boolean = revision < REVISION

    /** Archive 19 group for area details (was string group "details"). */
    fun detailsGroupId(): Int = WORLD_MAP_DETAILS_GROUP_ID

    /** Archive 19 group for compositemap (was string group "compositemap"). */
    fun compositemapGroupId(): Int = WORLD_MAP_COMPOSITEMAP_GROUP_ID

    /** Archive 19 group for compositetexture (was string group "compositetexture"). */
    fun compositetextureGroupId(): Int = WORLD_MAP_COMPOSITETEXTURE_GROUP_ID

    /** Geography / pre-blended ground group key: (regionX << 8) | regionY. */
    fun regionGroupKey(regionX: Int, regionY: Int): Int = (regionX shl 8) or (regionY and 0xFF)
}

/** One geography archive-18 file per region; mapsquare OR concatenated zones (client reads zones sequentially). */
internal fun writeWorldMapGeography(
    cacheProvider: dev.openrune.cache.worldmap.worldmap.providers.CacheProvider,
    areaId: Int,
    mapsquares: List<WorldMapMapsquare>,
    zones: List<WorldMapZone>,
    legacy: Boolean,
) {
    if (legacy) {
        for (mapsquare in mapsquares) {
            val geographyBuffer = io.netty.buffer.Unpooled.buffer(1000)
            mapsquare.geography.encode(geographyBuffer, legacy = true)
            cacheProvider.write(WORLD_MAP_GEOGRAPHY_ARCHIVE, mapsquare.data.groupId, mapsquare.data.fileId, geographyBuffer)
        }
        for (zone in zones) {
            val geographyBuffer = io.netty.buffer.Unpooled.buffer(1000)
            zone.geography.encode(geographyBuffer, legacy = true)
            cacheProvider.write(WORLD_MAP_GEOGRAPHY_ARCHIVE, zone.data.groupId, zone.data.fileId, geographyBuffer)
        }
        return
    }

    val mapsquareRegions = mutableSetOf<Int>()
    for (mapsquare in mapsquares) {
        val groupKey = WorldMapFormat.regionGroupKey(
            mapsquare.data.mapsquareDestinationX,
            mapsquare.data.mapsquareDestinationY,
        )
        mapsquareRegions += groupKey
        val geographyBuffer = io.netty.buffer.Unpooled.buffer(10_000)
        mapsquare.geography.encode(geographyBuffer, legacy = false)
        cacheProvider.write(WORLD_MAP_GEOGRAPHY_ARCHIVE, groupKey, areaId, geographyBuffer)
    }

    val zoneRegions = linkedMapOf<Int, MutableList<WorldMapZone>>()
    for (zone in zones) {
        val groupKey = WorldMapFormat.regionGroupKey(
            zone.data.mapsquareDestinationX,
            zone.data.mapsquareDestinationY,
        )
        if (groupKey in mapsquareRegions) continue
        zoneRegions.getOrPut(groupKey) { mutableListOf() } += zone
    }

    for ((groupKey, regionZones) in zoneRegions) {
        val geographyBuffer = io.netty.buffer.Unpooled.buffer(10_000)
        for (zone in regionZones) {
            zone.geography.encode(geographyBuffer, legacy = false)
        }
        cacheProvider.write(WORLD_MAP_GEOGRAPHY_ARCHIVE, groupKey, areaId, geographyBuffer)
    }
}
