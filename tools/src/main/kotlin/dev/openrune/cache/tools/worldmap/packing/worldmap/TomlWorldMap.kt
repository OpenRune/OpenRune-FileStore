@file:Suppress("DuplicatedCode", "MemberVisibilityCanBePrivate", "PropertyName")

package dev.openrune.cache.tools.worldmap.packing.worldmap

import dev.openrune.cache.tools.worldmap.MapsquareMultiSection
import dev.openrune.cache.tools.worldmap.MapsquareSingleSection
import dev.openrune.cache.tools.worldmap.WorldMapAreaDetails
import dev.openrune.cache.tools.worldmap.WorldMapElement
import dev.openrune.cache.tools.worldmap.WorldMapSection
import dev.openrune.cache.tools.worldmap.ZoneMultiSection
import dev.openrune.cache.tools.worldmap.ZoneSingleSection
import dev.openrune.cache.tools.worldmap.utils.Coordinate
import dev.openrune.definition.constants.ConstantProvider
import kotlinx.serialization.Serializable

/**
 * @author Kris | 25/08/2022
 */
@Serializable
class TomlWorldMap {
    val display_name: String? = null
    val origin: IntArray = intArrayOf()
    val background_colour: String? = null
    val zoom: Int? = null
    val mapsquare_single_sections: MutableList<TomlMapsquareSingleSection> = mutableListOf()
    val mapsquare_multi_sections: MutableList<TomlMapsquareMultiSection> = mutableListOf()
    val zone_single_sections: MutableList<TomlZoneSingleSection> = mutableListOf()
    val zone_multi_sections: MutableList<TomlZoneMultiSection> = mutableListOf()
    val map_elements: MutableList<TomlMapElement> = mutableListOf()

    fun build(rscmName: String): WorldMapAreaBlock {
        val id = ConstantProvider.getMapping(rscmName)
        val internalName = rscmName.replace("worldmap.", "")
        val displayName = requireNotNull(display_name)
        val backgroundColour = requireNotNull(background_colour)
            .substringAfter("<col=")
            .substringBefore(">")
            .toInt(16)
        val zoom = requireNotNull(zoom)
        val mapsquareSingleSections = mapsquare_single_sections.map { it.toMapsquareSingleSection() }
        val mapsquareMultiSections = mapsquare_multi_sections.map { it.toMapsquareMultiSection() }
        val zoneSingleSections = zone_single_sections.map { it.toZoneSingleSection() }
        val zoneMultiSections = zone_multi_sections.map { it.toZoneMultiSection() }
        val sections = mapsquareSingleSections + mapsquareMultiSections + zoneSingleSections + zoneMultiSections
        val origin = Coordinate(this.origin[0], this.origin[1], this.origin[2])
        val mapElements = map_elements.map { element ->
            val coord = sections.convertCoord(requireNotNull(element.location))
            WorldMapElement(requireNotNull(element.name), coord)
        }
        val details = WorldMapAreaDetails(id, internalName, displayName, origin, backgroundColour, zoom, sections)
        return WorldMapAreaBlock(details, mapElements)
    }

    private fun List<WorldMapSection>.convertCoord(coord: IntArray): Coordinate {
        val (x, y, level) = coord
        if (this.isEmpty()) return Coordinate(x, y, level)
        val section = singleOrNull { it.containsSourceCoord(level, x, y) } ?: error("Coord ${coord.contentToString()} out of bounds.")
        return section.convertToDestinationCoord(Coordinate(x, y, level))
    }
}

data class WorldMapAreaBlock(
    val details: WorldMapAreaDetails,
    val mapElements: List<WorldMapElement>
)

@Serializable
data class TomlMapElement(
    val name: String?,
    val location: IntArray = intArrayOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TomlMapElement

        if (name != other.name) return false
        if (!location.contentEquals(other.location)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + location.contentHashCode()
        return result
    }
}

@Serializable
data class TomlMapsquareSingleSection(
    val level: Int?,
    val levels_count: Int?,
    val mapsquare_source_x: Int?,
    val mapsquare_source_y: Int?,
    val mapsquare_destination_x: Int?,
    val mapsquare_destination_y: Int?
) {
    fun toMapsquareSingleSection(): MapsquareSingleSection {
        val level = requireNotNull(this.level)
        val levelsCount = requireNotNull(this.levels_count)
        val mapsquareSourceX = requireNotNull(this.mapsquare_source_x)
        val mapsquareSourceY = requireNotNull(this.mapsquare_source_y)
        val mapsquareDestinationX = requireNotNull(this.mapsquare_destination_x)
        val mapsquareDestinationY = requireNotNull(this.mapsquare_destination_y)
        return MapsquareSingleSection(
            level,
            levelsCount,
            mapsquareSourceX,
            mapsquareSourceY,
            mapsquareDestinationX,
            mapsquareDestinationY
        )
    }
}

@Serializable
data class TomlMapsquareMultiSection(
    val level: Int?,
    val levels_count: Int?,
    val mapsquare_source_min_x: Int?,
    val mapsquare_source_min_y: Int?,
    val mapsquare_source_max_x: Int?,
    val mapsquare_source_max_y: Int,
    val mapsquare_destination_min_x: Int?,
    val mapsquare_destination_min_y: Int?,
    val mapsquare_destination_max_x: Int?,
    val mapsquare_destination_max_y: Int?,
) {
    fun toMapsquareMultiSection(): MapsquareMultiSection {
        val level = requireNotNull(this.level)
        val levelsCount = requireNotNull(this.levels_count)
        val mapsquareSourceMinX = requireNotNull(this.mapsquare_source_min_x)
        val mapsquareSourceMinY = requireNotNull(this.mapsquare_source_min_y)
        val mapsquareSourceMaxX = requireNotNull(this.mapsquare_source_max_x)
        val mapsquareSourceMaxY = requireNotNull(this.mapsquare_source_max_y)
        val mapsquareDestinationMinX = requireNotNull(this.mapsquare_destination_min_x)
        val mapsquareDestinationMinY = requireNotNull(this.mapsquare_destination_min_y)
        val mapsquareDestinationMaxX = requireNotNull(this.mapsquare_destination_max_x)
        val mapsquareDestinationMaxY = requireNotNull(this.mapsquare_destination_max_y)
        return MapsquareMultiSection(
            level,
            levelsCount,
            mapsquareSourceMinX,
            mapsquareSourceMinY,
            mapsquareSourceMaxX,
            mapsquareSourceMaxY,
            mapsquareDestinationMinX,
            mapsquareDestinationMinY,
            mapsquareDestinationMaxX,
            mapsquareDestinationMaxY
        )
    }
}

@Serializable
data class TomlZoneSingleSection(
    val level: Int?,
    val levels_count: Int?,
    val mapsquare_source_x: Int?,
    val zone_source_x: Int?,
    val mapsquare_source_y: Int?,
    val zone_source_y: Int?,
    val mapsquare_destination_x: Int?,
    val zone_destination_x: Int?,
    val mapsquare_destination_y: Int?,
    val zone_destination_y: Int?
) {
    fun toZoneSingleSection(): ZoneSingleSection {
        val level = requireNotNull(this.level)
        val levelsCount = requireNotNull(this.levels_count)
        val mapsquareSourceX = requireNotNull(this.mapsquare_source_x)
        val zoneSourceX = requireNotNull(this.zone_source_x)
        val mapsquareSourceY = requireNotNull(this.mapsquare_source_y)
        val zoneSourceY = requireNotNull(this.zone_source_y)
        val mapsquareDestinationX = requireNotNull(this.mapsquare_destination_x)
        val zoneDestinationX = requireNotNull(this.zone_destination_x)
        val mapsquareDestinationY = requireNotNull(this.mapsquare_destination_y)
        val zoneDestinationY = requireNotNull(this.zone_destination_y)
        return ZoneSingleSection(
            level,
            levelsCount,
            mapsquareSourceX,
            zoneSourceX,
            mapsquareSourceY,
            zoneSourceY,
            mapsquareDestinationX,
            zoneDestinationX,
            mapsquareDestinationY,
            zoneDestinationY
        )
    }
}

@Serializable
data class TomlZoneMultiSection(
    val level: Int?,
    val levels_count: Int?,
    val mapsquare_source_x: Int?,
    val zone_source_min_x: Int?,
    val zone_source_max_x: Int?,
    val mapsquare_source_y: Int?,
    val zone_source_min_y: Int?,
    val zone_source_max_y: Int?,
    val mapsquare_destination_x: Int?,
    val zone_destination_min_x: Int?,
    val zone_destination_max_x: Int?,
    val mapsquare_destination_y: Int?,
    val zone_destination_min_y: Int?,
    val zone_destination_max_y: Int?
) {
    fun toZoneMultiSection(): ZoneMultiSection {
        val level = requireNotNull(this.level)
        val levelsCount = requireNotNull(this.levels_count)
        val mapsquareSourceX = requireNotNull(this.mapsquare_source_x)
        val zoneSourceMinX = requireNotNull(this.zone_source_min_x)
        val zoneSourceMaxX = requireNotNull(this.zone_source_max_x)
        val mapsquareSourceY = requireNotNull(this.mapsquare_source_y)
        val zoneSourceMinY = requireNotNull(this.zone_source_min_y)
        val zoneSourceMaxY = requireNotNull(this.zone_source_max_y)
        val mapsquareDestinationX = requireNotNull(this.mapsquare_destination_x)
        val zoneDestinationMinX = requireNotNull(this.zone_destination_min_x)
        val zoneDestinationMaxX = requireNotNull(this.zone_destination_max_x)
        val mapsquareDestinationY = requireNotNull(this.mapsquare_destination_y)
        val zoneDestinationMinY = requireNotNull(this.zone_destination_min_y)
        val zoneDestinationMaxY = requireNotNull(this.zone_destination_max_y)
        return ZoneMultiSection(
            level,
            levelsCount,
            mapsquareSourceX,
            zoneSourceMinX,
            zoneSourceMaxX,
            mapsquareSourceY,
            zoneSourceMinY,
            zoneSourceMaxY,
            mapsquareDestinationX,
            zoneDestinationMinX,
            zoneDestinationMaxX,
            mapsquareDestinationY,
            zoneDestinationMinY,
            zoneDestinationMaxY
        )
    }
}
