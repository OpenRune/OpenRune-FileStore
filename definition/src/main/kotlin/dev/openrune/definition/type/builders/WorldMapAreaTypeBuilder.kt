package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

import dev.openrune.definition.util.Coord

class WorldMapAreaTypeBuilder(var id: Int = -1) {

    var backgroundColour: Int = -1
    var fillColour: Int = -16777216
    var zoom: Int = -1
    var origin: Coord? = null
    var regionLowX: Int = Integer.MAX_VALUE
    var regionHighX: Int = 0
    var regionLowY: Int = Integer.MAX_VALUE
    var regionHighY: Int = 0
    var isMain: Boolean = false
    var internalName: String = ""
    var externalName: String = ""
    var sections: MutableList<WorldMapSectionType> = mutableListOf()

    fun build(): WorldMapAreaType = WorldMapAreaType(
        id = id,
        backgroundColour = backgroundColour,
        fillColour = fillColour,
        zoom = zoom,
        origin = origin,
        regionLowX = regionLowX,
        regionHighX = regionHighX,
        regionLowY = regionLowY,
        regionHighY = regionHighY,
        isMain = isMain,
        internalName = internalName,
        externalName = externalName,
        sections = sections.toList(),
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. List contents are copied too. */
        fun from(type: WorldMapAreaType): WorldMapAreaTypeBuilder {
            val builder = WorldMapAreaTypeBuilder(type.id)
            builder.backgroundColour = type.backgroundColour
            builder.fillColour = type.fillColour
            builder.zoom = type.zoom
            builder.origin = type.origin
            builder.regionLowX = type.regionLowX
            builder.regionHighX = type.regionHighX
            builder.regionLowY = type.regionLowY
            builder.regionHighY = type.regionHighY
            builder.isMain = type.isMain
            builder.internalName = type.internalName
            builder.externalName = type.externalName
            builder.sections = type.sections.toMutableList()
            return builder
        }
    }
}
