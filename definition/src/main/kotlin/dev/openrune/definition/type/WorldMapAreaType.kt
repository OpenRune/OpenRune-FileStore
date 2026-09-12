package dev.openrune.definition.type

import dev.openrune.definition.type.builders.WorldMapAreaTypeBuilder

import dev.openrune.definition.Definition
import dev.openrune.definition.util.Coord
import io.netty.buffer.ByteBuf

/**
 * One rectangle of map data a world map area draws, and where it draws it.
 *
 * The four layouts and the byte that selects them come from the client: the record stores the section's id,
 * which the client maps to a concrete section class (see WorldMapData.method6401). Source coordinates say
 * where the map data is read from, destination coordinates where it is painted, which is what lets an area
 * such as the Zanaris map show data that lives elsewhere in the world.
 */
abstract class WorldMapSectionType {
    /** Id written before the section body, used to pick the layout when reading it back. */
    abstract val sectionId: Int

    abstract fun decode(buffer: ByteBuf)
    abstract fun encode(buffer: ByteBuf)
}

/** Id 0: a rectangle of whole mapsquares copied to another rectangle of mapsquares. */
data class MultiSquare(
    var level: Int = -1,
    var levelsCount: Int = -1,
    var sourceMinX: Int = -1,
    var sourceMinY: Int = -1,
    var sourceMaxX: Int = -1,
    var sourceMaxY: Int = -1,
    var destinationMinX: Int = -1,
    var destinationMinY: Int = -1,
    var destinationMaxX: Int = -1,
    var destinationMaxY: Int = -1
) : WorldMapSectionType() {

    override val sectionId: Int get() = 0

    override fun decode(buffer: ByteBuf) {
        this.level = buffer.readUnsignedByte().toInt()
        this.levelsCount = buffer.readUnsignedByte().toInt()
        this.sourceMinX = buffer.readUnsignedShort()
        this.sourceMinY = buffer.readUnsignedShort()
        this.sourceMaxX = buffer.readUnsignedShort()
        this.sourceMaxY = buffer.readUnsignedShort()
        this.destinationMinX = buffer.readUnsignedShort()
        this.destinationMinY = buffer.readUnsignedShort()
        this.destinationMaxX = buffer.readUnsignedShort()
        this.destinationMaxY = buffer.readUnsignedShort()
    }

    override fun encode(buffer: ByteBuf) {
        buffer.writeByte(level)
        buffer.writeByte(levelsCount)
        buffer.writeShort(sourceMinX)
        buffer.writeShort(sourceMinY)
        buffer.writeShort(sourceMaxX)
        buffer.writeShort(sourceMaxY)
        buffer.writeShort(destinationMinX)
        buffer.writeShort(destinationMinY)
        buffer.writeShort(destinationMaxX)
        buffer.writeShort(destinationMaxY)
    }
}

/** Id 1: a single mapsquare copied to a single mapsquare. */
data class SingleSquare(
    var level: Int = -1,
    var levelsCount: Int = -1,
    var sourceX: Int = -1,
    var sourceY: Int = -1,
    var destinationX: Int = -1,
    var destinationY: Int = -1
) : WorldMapSectionType() {

    override val sectionId: Int get() = 1

    override fun decode(buffer: ByteBuf) {
        this.level = buffer.readUnsignedByte().toInt()
        this.levelsCount = buffer.readUnsignedByte().toInt()
        this.sourceX = buffer.readUnsignedShort()
        this.sourceY = buffer.readUnsignedShort()
        this.destinationX = buffer.readUnsignedShort()
        this.destinationY = buffer.readUnsignedShort()
    }

    override fun encode(buffer: ByteBuf) {
        buffer.writeByte(level)
        buffer.writeByte(levelsCount)
        buffer.writeShort(sourceX)
        buffer.writeShort(sourceY)
        buffer.writeShort(destinationX)
        buffer.writeShort(destinationY)
    }
}

/** Id 2: a rectangle of zones within one mapsquare copied to a rectangle of zones in another. */
data class MultiZone(
    var level: Int = -1,
    var levelsCount: Int = -1,
    var sourceMapsquareX: Int = -1,
    var sourceZoneMinX: Int = -1,
    var sourceZoneMaxX: Int = -1,
    var sourceMapsquareY: Int = -1,
    var sourceZoneMinY: Int = -1,
    var sourceZoneMaxY: Int = -1,
    var destinationMapsquareX: Int = -1,
    var destinationZoneMinX: Int = -1,
    var destinationZoneMaxX: Int = -1,
    var destinationMapsquareY: Int = -1,
    var destinationZoneMinY: Int = -1,
    var destinationZoneMaxY: Int = -1
) : WorldMapSectionType() {

    override val sectionId: Int get() = 2

    override fun decode(buffer: ByteBuf) {
        this.level = buffer.readUnsignedByte().toInt()
        this.levelsCount = buffer.readUnsignedByte().toInt()
        this.sourceMapsquareX = buffer.readUnsignedShort()
        this.sourceZoneMinX = buffer.readUnsignedByte().toInt()
        this.sourceZoneMaxX = buffer.readUnsignedByte().toInt()
        this.sourceMapsquareY = buffer.readUnsignedShort()
        this.sourceZoneMinY = buffer.readUnsignedByte().toInt()
        this.sourceZoneMaxY = buffer.readUnsignedByte().toInt()
        this.destinationMapsquareX = buffer.readUnsignedShort()
        this.destinationZoneMinX = buffer.readUnsignedByte().toInt()
        this.destinationZoneMaxX = buffer.readUnsignedByte().toInt()
        this.destinationMapsquareY = buffer.readUnsignedShort()
        this.destinationZoneMinY = buffer.readUnsignedByte().toInt()
        this.destinationZoneMaxY = buffer.readUnsignedByte().toInt()
    }

    override fun encode(buffer: ByteBuf) {
        buffer.writeByte(level)
        buffer.writeByte(levelsCount)
        buffer.writeShort(sourceMapsquareX)
        buffer.writeByte(sourceZoneMinX)
        buffer.writeByte(sourceZoneMaxX)
        buffer.writeShort(sourceMapsquareY)
        buffer.writeByte(sourceZoneMinY)
        buffer.writeByte(sourceZoneMaxY)
        buffer.writeShort(destinationMapsquareX)
        buffer.writeByte(destinationZoneMinX)
        buffer.writeByte(destinationZoneMaxX)
        buffer.writeShort(destinationMapsquareY)
        buffer.writeByte(destinationZoneMinY)
        buffer.writeByte(destinationZoneMaxY)
    }
}

/** Id 3: a single zone copied to a single zone. */
data class SingleZone(
    var level: Int = -1,
    var levelsCount: Int = -1,
    var sourceMapsquareX: Int = -1,
    var sourceZoneX: Int = -1,
    var sourceMapsquareY: Int = -1,
    var sourceZoneY: Int = -1,
    var destinationMapsquareX: Int = -1,
    var destinationZoneX: Int = -1,
    var destinationMapsquareY: Int = -1,
    var destinationZoneY: Int = -1
) : WorldMapSectionType() {

    override val sectionId: Int get() = 3

    override fun decode(buffer: ByteBuf) {
        this.level = buffer.readUnsignedByte().toInt()
        this.levelsCount = buffer.readUnsignedByte().toInt()
        this.sourceMapsquareX = buffer.readUnsignedShort()
        this.sourceZoneX = buffer.readUnsignedByte().toInt()
        this.sourceMapsquareY = buffer.readUnsignedShort()
        this.sourceZoneY = buffer.readUnsignedByte().toInt()
        this.destinationMapsquareX = buffer.readUnsignedShort()
        this.destinationZoneX = buffer.readUnsignedByte().toInt()
        this.destinationMapsquareY = buffer.readUnsignedShort()
        this.destinationZoneY = buffer.readUnsignedByte().toInt()
    }

    override fun encode(buffer: ByteBuf) {
        buffer.writeByte(level)
        buffer.writeByte(levelsCount)
        buffer.writeShort(sourceMapsquareX)
        buffer.writeByte(sourceZoneX)
        buffer.writeShort(sourceMapsquareY)
        buffer.writeByte(sourceZoneY)
        buffer.writeShort(destinationMapsquareX)
        buffer.writeByte(destinationZoneX)
        buffer.writeShort(destinationMapsquareY)
        buffer.writeByte(destinationZoneY)
    }
}

/**
 * A loaded world map area definition. Immutable apart from [id] (which the load machinery
 * assigns): decoding builds one through [WorldMapAreaTypeBuilder], and everything after that
 * only reads.
 */
data class WorldMapAreaType(
    override var id: Int = -1,
    val backgroundColour : Int = -1,
    val fillColour : Int = -16777216,
    val zoom : Int = -1,
    val origin : Coord? = null,
    val regionLowX : Int = Integer.MAX_VALUE,
    val regionHighX : Int = 0,
    val regionLowY : Int = Integer.MAX_VALUE,
    val regionHighY : Int = 0,
    val isMain : Boolean = false,
    val internalName : String = "",
    val externalName : String = "",
    val sections : List<WorldMapSectionType> = emptyList()
) : Definition {

    /** A mutable copy of this definition, for tools that need to edit and re-pack it. */
    fun toBuilder(): WorldMapAreaTypeBuilder = WorldMapAreaTypeBuilder.from(this)
}
