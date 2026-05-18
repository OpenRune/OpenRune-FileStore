@file:Suppress("DuplicatedCode")

package dev.openrune.cache.tools.worldmap

import dev.openrune.cache.tools.worldmap.utils.Coordinate
import io.netty.buffer.ByteBuf

/**
 * @author Kris | 13/08/2022
 */
sealed interface WorldMapSection {
    fun containsSourceCoord(level: Int, x: Int, y: Int): Boolean
    fun containsDestinationCoord(x: Int, y: Int): Boolean
    fun convertToDestinationCoord(sourceCoordinate: Coordinate): Coordinate
    val type: WorldMapSectionType
    val level: Int
    val levelsCount: Int

    fun verify()
    fun encode(buffer: ByteBuf)

    companion object {
        fun decode(buffer: ByteBuf): WorldMapSection {
            val typeId = buffer.readUnsignedByte().toInt()
            return when (WorldMapSectionType[typeId]) {
                WorldMapSectionType.MultiMapsquare -> MapsquareMultiSection.decode(buffer)
                WorldMapSectionType.SingleMapsquare -> MapsquareSingleSection.decode(buffer)
                WorldMapSectionType.MultiZone -> ZoneMultiSection.decode(buffer)
                WorldMapSectionType.SingleZone -> ZoneSingleSection.decode(buffer)
            }
        }
    }
}

enum class WorldMapSectionType(val type: Int, val id: Int) {
    MultiMapsquare(1, 0),
    SingleMapsquare(3, 1),
    MultiZone(2, 2),
    SingleZone(0, 3);

    companion object {
        operator fun get(id: Int): WorldMapSectionType {
            return values().single { it.id == id }
        }
    }
}

private val levelRange = 0..3
private val levelsCountRange = 1..UByte.MAX_VALUE.toInt()
private val mapsquareSourceXRange = 0 until 100
private val mapsquareSourceYRange = 0..255
private val zoneCoordRange = 0..7
private val mapsquareDestinationRange = 0..255

data class ZoneSingleSection(
    /**
     * The base level of the area.
     */
    override val level: Int,

    /**
     * The number of levels this area should render (a value of 1 to 4).
     */
    override val levelsCount: Int,

    /**
     * The source mapsquare x coordinate.
     */
    val mapsquareSourceX: Int,

    /**
     * The source zone x coordinate.
     */
    val zoneSourceX: Int,

    /**
     * The source mapsquare y coordinate.
     */
    val mapsquareSourceY: Int,

    /**
     * The source zone y coordinate.
     */
    val zoneSourceY: Int,

    /**
     * The destination mapsquare x coordinate.
     */
    val mapsquareDestinationX: Int,

    /**
     * The destination zone x coordinate.
     */
    val zoneDestinationX: Int,

    /**
     * The destination mapsquare y coordinate.
     */
    val mapsquareDestinationY: Int,

    /**
     * The destination zone y coordinate.
     */
    val zoneDestinationY: Int
) : WorldMapSection {

    override val type: WorldMapSectionType get() = WorldMapSectionType.SingleZone

    override fun verify() {
        require(level in levelRange) { "Level outside of boundaries: $level" }
        require(levelsCount in levelsCountRange) { "Levels count outside of boundaries: $levelsCount" }
        require(mapsquareSourceX in mapsquareSourceXRange) { "Mapsquare source x outside of boundaries: $mapsquareSourceX" }
        require(mapsquareSourceY in mapsquareSourceYRange) { "Mapsquare source y outside of boundaries: $mapsquareSourceY" }
        require(zoneSourceX in zoneCoordRange) { "Zone source x outside of boundaries: $zoneSourceX" }
        require(zoneSourceY in zoneCoordRange) { "Zone source y outside of boundaries: $zoneSourceY" }
        require(mapsquareDestinationX in mapsquareDestinationRange) { "Mapsquare destination x outside of boundaries: $mapsquareDestinationX" }
        require(mapsquareDestinationY in mapsquareDestinationRange) { "Mapsquare destination y outside of boundaries: $mapsquareDestinationY" }
    }

    override fun encode(buffer: ByteBuf) {
        buffer.writeByte(level)
        buffer.writeByte(levelsCount)
        buffer.writeShort(mapsquareSourceX)
        buffer.writeByte(zoneSourceX)
        buffer.writeShort(mapsquareSourceY)
        buffer.writeByte(zoneSourceY)
        buffer.writeShort(mapsquareDestinationX)
        buffer.writeByte(zoneDestinationX)
        buffer.writeShort(mapsquareDestinationY)
        buffer.writeByte(zoneDestinationY)
    }

    override fun convertToDestinationCoord(sourceCoordinate: Coordinate): Coordinate {
        val swSourceX = (mapsquareSourceX shl 6) or (zoneSourceX shl 3)
        val swSourceY = (mapsquareSourceY shl 6) or (zoneSourceY shl 3)
        val swDestinationX = (mapsquareDestinationX shl 6) or (zoneDestinationX shl 3)
        val swDestinationY = (mapsquareDestinationY shl 6) or (zoneDestinationY shl 3)
        val deltaX = sourceCoordinate.x - swSourceX
        val deltaY = sourceCoordinate.y - swSourceY
        return Coordinate(swDestinationX + deltaX, swDestinationY + deltaY, sourceCoordinate.level)
    }

    override fun containsSourceCoord(level: Int, x: Int, y: Int): Boolean {
        if (level < this.level || level >= (this.level + levelsCount)) return false
        return x >= (mapsquareSourceX shl 6) + (zoneSourceX shl 3) && x <= (mapsquareSourceX shl 6) + (zoneSourceX shl 3) + 7 &&
            y >= (mapsquareSourceY shl 6) + (zoneSourceY shl 3) && y <= (mapsquareSourceY shl 6) + (zoneSourceY shl 3) + 7
    }

    override fun containsDestinationCoord(x: Int, y: Int): Boolean {
        return x >= (mapsquareDestinationX shl 6) + (zoneDestinationX shl 3) && x <= (mapsquareDestinationX shl 6) + (zoneDestinationX shl 3) + 7 &&
            y >= (mapsquareDestinationY shl 6) + (zoneDestinationY shl 3) && y <= (mapsquareDestinationY shl 6) + (zoneDestinationY shl 3) + 7
    }

    override fun toString(): String {
        val builder = StringBuilder(100)
        builder.append("ZoneSingleSection:").appendLine()
        builder.append("\tlevel: $level").appendLine()
        builder.append("\tlevelsCount: $levelsCount").appendLine()
        builder.append("\tmapsquareSourceX: $mapsquareSourceX").appendLine()
        builder.append("\tzoneSourceX: $zoneSourceX").appendLine()
        builder.append("\tmapsquareSourceY: $mapsquareSourceY").appendLine()
        builder.append("\tzoneSourceY: $zoneSourceY").appendLine()
        builder.append("\tmapsquareDestinationX: $mapsquareDestinationX").appendLine()
        builder.append("\tzoneDestinationX: $zoneDestinationX").appendLine()
        builder.append("\tmapsquareDestinationY: $mapsquareDestinationY").appendLine()
        builder.append("\tzoneDestinationY: $zoneDestinationY").appendLine()
        return builder.toString()
    }

    companion object {
        fun decode(buffer: ByteBuf): ZoneSingleSection {
            val level = buffer.readUnsignedByte().toInt()
            val levelsCount = buffer.readUnsignedByte().toInt()
            val mapsquareSourceX = buffer.readUnsignedShort()
            val zoneSourceX = buffer.readUnsignedByte().toInt()
            val mapsquareSourceY = buffer.readUnsignedShort()
            val zoneSourceY = buffer.readUnsignedByte().toInt()
            val mapsquareDestinationX = buffer.readUnsignedShort()
            val zoneDestinationX = buffer.readUnsignedByte().toInt()
            val mapsquareDestinationY = buffer.readUnsignedShort()
            val zoneDestinationY = buffer.readUnsignedByte().toInt()
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
}

data class MapsquareMultiSection(
    /**
     * The base level of the area.
     */
    override val level: Int,

    /**
     * The number of levels this area should render (a value of 1 to 4).
     */
    override val levelsCount: Int,

    /**
     * The minimum source mapsquare x coordinate.
     */
    val mapsquareSourceMinX: Int,

    /**
     * The minimum source mapsquare y coordinate.
     */
    val mapsquareSourceMinY: Int,

    /**
     * The maximum source mapsquare x coordinate(inclusive).
     */
    val mapsquareSourceMaxX: Int,

    /**
     * The maximum source mapsquare y coordinate(inclusive).
     */
    val mapsquareSourceMaxY: Int,

    /**
     * The minimum destination mapsquare x coordinate.
     */
    val mapsquareDestinationMinX: Int,

    /**
     * The minimum destination mapsquare y coordinate.
     */
    val mapsquareDestinationMinY: Int,

    /**
     * The maximum destination mapsquare x coordinate(inclusive).
     */
    val mapsquareDestinationMaxX: Int,

    /**
     * The maximum destination mapsquare y coordinate(inclusive).
     */
    val mapsquareDestinationMaxY: Int
) : WorldMapSection {

    override val type: WorldMapSectionType get() = WorldMapSectionType.MultiMapsquare

    override fun verify() {
        require(level in levelRange) { "Level outside of boundaries: $level" }
        require(levelsCount in levelsCountRange) { "Levels count outside of boundaries: $levelsCount" }
        require(mapsquareSourceMinX in mapsquareSourceXRange) { "Mapsquare source min x outside of boundaries: $mapsquareSourceMinX" }
        require(mapsquareSourceMinY in mapsquareSourceYRange) { "Mapsquare source min y outside of boundaries: $mapsquareSourceMinY" }
        require(mapsquareSourceMaxX in mapsquareSourceXRange) { "Mapsquare source max x outside of boundaries: $mapsquareSourceMaxX" }
        require(mapsquareSourceMaxY in mapsquareSourceYRange) { "Mapsquare source max y outside of boundaries: $mapsquareSourceMaxY" }
        require(mapsquareDestinationMinX in mapsquareSourceXRange) { "Mapsquare destination min x outside of boundaries: $mapsquareDestinationMinX" }
        require(mapsquareDestinationMinY in mapsquareSourceYRange) { "Mapsquare destination min y outside of boundaries: $mapsquareDestinationMinY" }
        require(mapsquareDestinationMaxX in mapsquareSourceXRange) { "Mapsquare destination max x outside of boundaries: $mapsquareDestinationMaxX" }
        require(mapsquareDestinationMaxY in mapsquareSourceYRange) { "Mapsquare destination max y outside of boundaries: $mapsquareDestinationMaxY" }
        require(mapsquareSourceMaxX - mapsquareSourceMinX == mapsquareDestinationMaxX - mapsquareDestinationMinX) {
            "Mismatching rectangle width: $mapsquareSourceMinX, $mapsquareSourceMaxX, $mapsquareDestinationMinX, $mapsquareDestinationMaxX"
        }
        require(mapsquareSourceMaxY - mapsquareSourceMinY == mapsquareDestinationMaxY - mapsquareDestinationMinY) {
            "Mismatching rectangle height: $mapsquareSourceMinY, $mapsquareSourceMaxY, $mapsquareDestinationMinY, $mapsquareDestinationMaxY"
        }
    }

    override fun encode(buffer: ByteBuf) {
        buffer.writeByte(level)
        buffer.writeByte(levelsCount)
        buffer.writeShort(mapsquareSourceMinX)
        buffer.writeShort(mapsquareSourceMinY)
        buffer.writeShort(mapsquareSourceMaxX)
        buffer.writeShort(mapsquareSourceMaxY)
        buffer.writeShort(mapsquareDestinationMinX)
        buffer.writeShort(mapsquareDestinationMinY)
        buffer.writeShort(mapsquareDestinationMaxX)
        buffer.writeShort(mapsquareDestinationMaxY)
    }

    override fun convertToDestinationCoord(sourceCoordinate: Coordinate): Coordinate {
        val swSourceX = (mapsquareSourceMinX shl 6)
        val swSourceY = (mapsquareSourceMinY shl 6)
        val swDestinationX = (mapsquareDestinationMinX shl 6)
        val swDestinationY = (mapsquareDestinationMinY shl 6)
        val deltaX = sourceCoordinate.x - swSourceX
        val deltaY = sourceCoordinate.y - swSourceY
        return Coordinate(swDestinationX + deltaX, swDestinationY + deltaY, sourceCoordinate.level)
    }

    override fun containsSourceCoord(level: Int, x: Int, y: Int): Boolean {
        if (level < this.level || level >= this.level + levelsCount) return false
        return x shr 6 in mapsquareSourceMinX..mapsquareSourceMaxX &&
            y shr 6 in mapsquareSourceMinY..mapsquareSourceMaxY
    }

    override fun containsDestinationCoord(x: Int, y: Int): Boolean {
        return x shr 6 in mapsquareDestinationMinX..mapsquareDestinationMaxX &&
            y shr 6 in mapsquareDestinationMinY..mapsquareDestinationMaxY
    }

    override fun toString(): String {
        val builder = StringBuilder(100)
        builder.append("MapsquareMultiSection:").appendLine()
        builder.append("\tlevel: $level").appendLine()
        builder.append("\tlevelsCount: $levelsCount").appendLine()
        builder.append("\tmapsquareSourceMinX: $mapsquareSourceMinX").appendLine()
        builder.append("\tmapsquareSourceMinY: $mapsquareSourceMinY").appendLine()
        builder.append("\tmapsquareSourceMaxX: $mapsquareSourceMaxX").appendLine()
        builder.append("\tmapsquareSourceMaxY: $mapsquareSourceMaxY").appendLine()
        builder.append("\tmapsquareDestinationMinX: $mapsquareDestinationMinX").appendLine()
        builder.append("\tmapsquareDestinationMinY: $mapsquareDestinationMinY").appendLine()
        builder.append("\tmapsquareDestinationMaxX: $mapsquareDestinationMaxX").appendLine()
        builder.append("\tmapsquareDestinationMaxY: $mapsquareDestinationMaxY").appendLine()
        return builder.toString()
    }

    companion object {
        fun decode(buffer: ByteBuf): MapsquareMultiSection {
            val level = buffer.readUnsignedByte().toInt()
            val levelsCount = buffer.readUnsignedByte().toInt()
            val mapsquareSourceMinX = buffer.readUnsignedShort()
            val mapsquareSourceMinY = buffer.readUnsignedShort()
            val mapsquareSourceMaxX = buffer.readUnsignedShort()
            val mapsquareSourceMaxY = buffer.readUnsignedShort()
            val mapsquareDestinationMinX = buffer.readUnsignedShort()
            val mapsquareDestinationMinY = buffer.readUnsignedShort()
            val mapsquareDestinationMaxX = buffer.readUnsignedShort()
            val mapsquareDestinationMaxY = buffer.readUnsignedShort()
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
}

data class ZoneMultiSection(
    /**
     * The base level of the area.
     */
    override val level: Int,

    /**
     * The number of levels this area should render (a value of 1 to 4).
     */
    override val levelsCount: Int,

    /**
     * The source mapsquare x coordinate.
     */
    val mapsquareSourceX: Int,

    /**
     * The minimum source zone x coordinate.
     */
    val zoneSourceMinX: Int,

    /**
     * The maximum source zone x coordinate(inclusive).
     */
    val zoneSourceMaxX: Int,

    /**
     * The source mapsquare y coordinate.
     */
    val mapsquareSourceY: Int,

    /**
     * The minimum source zone y coordinate.
     */
    val zoneSourceMinY: Int,

    /**
     * The maximum source zone y coordinate(inclusive).
     */
    val zoneSourceMaxY: Int,

    /**
     * The destination mapsquare x coordinate.
     */
    val mapsquareDestinationX: Int,

    /**
     * The minimum destination zone x coordinate.
     */
    val zoneDestinationMinX: Int,

    /**
     * The maximum destination zone x coordinate(inclusive).
     */
    val zoneDestinationMaxX: Int,

    /**
     * The destination mapsquare y coordinate.
     */
    val mapsquareDestinationY: Int,

    /**
     * The minimum destination zone y coordinate.
     */
    val zoneDestinationMinY: Int,

    /**
     * The maximum destination zone y coordinate(inclusive).
     */
    val zoneDestinationMaxY: Int,
) : WorldMapSection {

    override val type: WorldMapSectionType get() = WorldMapSectionType.MultiZone

    override fun verify() {
        require(level in levelRange) { "Level outside of boundaries: $level" }
        require(levelsCount in levelsCountRange) { "Levels count outside of boundaries: $levelsCount" }
        require(mapsquareSourceX in mapsquareSourceXRange) { "Mapsquare source x outside of boundaries: $mapsquareSourceX" }
        require(zoneSourceMinX in zoneCoordRange) { "Zone source min x outside of boundaries: $zoneSourceMinX" }
        require(zoneSourceMaxX in zoneCoordRange) { "Zone source max x outside of boundaries: $zoneSourceMaxX" }
        require(mapsquareSourceY in mapsquareSourceYRange) { "Mapsquare source y outside of boundaries: $mapsquareSourceY" }
        require(zoneSourceMinY in zoneCoordRange) { "Zone source min y outside of boundaries: $zoneSourceMinY" }
        require(zoneSourceMaxY in zoneCoordRange) { "Zone source max y outside of boundaries: $zoneSourceMaxY" }
        require(mapsquareDestinationX in mapsquareDestinationRange) { "Mapsquare destination x outside of boundaries: $mapsquareDestinationX" }
        require(zoneDestinationMinX in zoneCoordRange) { "Zone destination min x outside of boundaries: $zoneDestinationMinX" }
        require(zoneDestinationMaxX in zoneCoordRange) { "Zone destination max x outside of boundaries: $zoneDestinationMaxX" }
        require(mapsquareDestinationY in mapsquareDestinationRange) { "Mapsquare destination y outside of boundaries: $mapsquareDestinationY" }
        require(zoneDestinationMinY in zoneCoordRange) { "Zone destination min y outside of boundaries: $zoneDestinationMinY" }
        require(zoneDestinationMaxY in zoneCoordRange) { "Zone destination max y outside of boundaries: $zoneDestinationMaxY" }
        require(zoneSourceMaxX - zoneSourceMinX == zoneDestinationMaxX - zoneDestinationMinX) {
            "Mismatching rectangle width: $zoneSourceMinX, $zoneSourceMaxX, $zoneDestinationMinX, $zoneDestinationMaxX"
        }
        require(zoneSourceMaxY - zoneSourceMinY == zoneDestinationMaxY - zoneDestinationMinY) {
            "Mismatching rectangle height: $zoneSourceMinY, $zoneSourceMaxY, $zoneDestinationMinY, $zoneDestinationMaxY"
        }
    }

    override fun encode(buffer: ByteBuf) {
        buffer.writeByte(level)
        buffer.writeByte(levelsCount)
        buffer.writeShort(mapsquareSourceX)
        buffer.writeByte(zoneSourceMinX)
        buffer.writeByte(zoneSourceMaxX)
        buffer.writeShort(mapsquareSourceY)
        buffer.writeByte(zoneSourceMinY)
        buffer.writeByte(zoneSourceMaxY)
        buffer.writeShort(mapsquareDestinationX)
        buffer.writeByte(zoneDestinationMinX)
        buffer.writeByte(zoneDestinationMaxX)
        buffer.writeShort(mapsquareDestinationY)
        buffer.writeByte(zoneDestinationMinY)
        buffer.writeByte(zoneDestinationMaxY)
    }

    override fun convertToDestinationCoord(sourceCoordinate: Coordinate): Coordinate {
        val swSourceX = (mapsquareSourceX shl 6) or (zoneSourceMinX shl 3)
        val swSourceY = (mapsquareSourceY shl 6) or (zoneSourceMinY shl 3)
        val swDestinationX = (mapsquareDestinationX shl 6) or (zoneDestinationMinX shl 3)
        val swDestinationY = (mapsquareDestinationY shl 6) or (zoneDestinationMinY shl 3)
        val deltaX = sourceCoordinate.x - swSourceX
        val deltaY = sourceCoordinate.y - swSourceY
        return Coordinate(swDestinationX + deltaX, swDestinationY + deltaY, sourceCoordinate.level)
    }

    override fun containsSourceCoord(level: Int, x: Int, y: Int): Boolean {
        if (level < this.level || level >= (this.level + levelsCount)) return false
        val minX = (mapsquareSourceX shl 6) + (zoneSourceMinX shl 3)
        val maxX = (mapsquareSourceX shl 6) + (zoneSourceMaxX shl 3) + 7
        val minY = (mapsquareSourceY shl 6) + (zoneSourceMinY shl 3)
        val maxY = (mapsquareSourceY shl 6) + (zoneSourceMaxY shl 3) + 7
        return x in minX..maxX && y in minY..maxY
    }

    override fun containsDestinationCoord(x: Int, y: Int): Boolean {
        val minX = (mapsquareDestinationX shl 6) + (zoneDestinationMinX shl 3)
        val maxX = (mapsquareDestinationX shl 6) + (zoneDestinationMaxX shl 3) + 7
        val minY = (mapsquareDestinationY shl 6) + (zoneDestinationMinY shl 3)
        val maxY = (mapsquareDestinationY shl 6) + (zoneDestinationMaxY shl 3) + 7
        return x in minX..maxX && y in minY..maxY
    }

    override fun toString(): String {
        val builder = StringBuilder(100)
        builder.append("ZoneMultiSection:").appendLine()
        builder.append("\tlevel: $level").appendLine()
        builder.append("\tlevelsCount: $levelsCount").appendLine()
        builder.append("\tmapsquareSourceX: $mapsquareSourceX").appendLine()
        builder.append("\tzoneSourceMinX: $zoneSourceMinX").appendLine()
        builder.append("\tzoneSourceMaxX: $zoneSourceMaxX").appendLine()
        builder.append("\tmapsquareSourceY: $mapsquareSourceY").appendLine()
        builder.append("\tzoneSourceMinY: $zoneSourceMinY").appendLine()
        builder.append("\tzoneSourceMaxY: $zoneSourceMaxY").appendLine()
        builder.append("\tmapsquareDestinationX: $mapsquareDestinationX").appendLine()
        builder.append("\tzoneDestinationMinX: $zoneDestinationMinX").appendLine()
        builder.append("\tzoneDestinationMaxX: $zoneDestinationMaxX").appendLine()
        builder.append("\tmapsquareDestinationY: $mapsquareDestinationY").appendLine()
        builder.append("\tzoneDestinationMinY: $zoneDestinationMinY").appendLine()
        builder.append("\tzoneDestinationMaxY: $zoneDestinationMaxY").appendLine()
        return builder.toString()
    }

    companion object {
        fun decode(buffer: ByteBuf): ZoneMultiSection {
            val level = buffer.readUnsignedByte().toInt()
            val levelsCount = buffer.readUnsignedByte().toInt()
            val mapsquareSourceX = buffer.readUnsignedShort()
            val zoneSourceMinX = buffer.readUnsignedByte().toInt()
            val zoneSourceMaxX = buffer.readUnsignedByte().toInt()
            val mapsquareSourceY = buffer.readUnsignedShort()
            val zoneSourceMinY = buffer.readUnsignedByte().toInt()
            val zoneSourceMaxY = buffer.readUnsignedByte().toInt()
            val mapsquareDestinationX = buffer.readUnsignedShort()
            val zoneDestinationMinX = buffer.readUnsignedByte().toInt()
            val zoneDestinationMaxX = buffer.readUnsignedByte().toInt()
            val mapsquareDestinationY = buffer.readUnsignedShort()
            val zoneDestinationMinY = buffer.readUnsignedByte().toInt()
            val zoneDestinationMaxY = buffer.readUnsignedByte().toInt()
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
}

data class MapsquareSingleSection(
    /**
     * The base level of the area.
     */
    override val level: Int,

    /**
     * The number of levels this area should render (a value of 1 to 4).
     */
    override val levelsCount: Int,

    /**
     * The source mapsquare x coordinate.
     */
    val mapsquareSourceX: Int,

    /**
     * The source mapsquare y coordinate.
     */
    val mapsquareSourceY: Int,

    /**
     * The destination mapsquare x coordinate.
     */
    val mapsquareDestinationX: Int,

    /**
     * The destination mapsquare y coordinate.
     */
    val mapsquareDestinationY: Int
) : WorldMapSection {

    override val type: WorldMapSectionType get() = WorldMapSectionType.SingleMapsquare

    override fun verify() {
        require(level in levelRange) { "Level outside of boundaries: $level" }
        require(levelsCount in levelsCountRange) { "Levels count outside of boundaries: $levelsCount" }
        require(mapsquareSourceX in mapsquareSourceXRange) { "Mapsquare source x outside of boundaries: $mapsquareSourceX" }
        require(mapsquareSourceY in mapsquareSourceYRange) { "Mapsquare source y outside of boundaries: $mapsquareSourceY" }
        require(mapsquareDestinationX in mapsquareDestinationRange) { "Mapsquare destination x outside of boundaries: $mapsquareDestinationX" }
        require(mapsquareDestinationY in mapsquareDestinationRange) { "Mapsquare destination y outside of boundaries: $mapsquareDestinationY" }
    }

    override fun encode(buffer: ByteBuf) {
        buffer.writeByte(level)
        buffer.writeByte(levelsCount)
        buffer.writeShort(mapsquareSourceX)
        buffer.writeShort(mapsquareSourceY)
        buffer.writeShort(mapsquareDestinationX)
        buffer.writeShort(mapsquareDestinationY)
    }

    override fun convertToDestinationCoord(sourceCoordinate: Coordinate): Coordinate {
        val swSourceX = (mapsquareSourceX shl 6)
        val swSourceY = (mapsquareSourceY shl 6)
        val swDestinationX = (mapsquareDestinationX shl 6)
        val swDestinationY = (mapsquareDestinationY shl 6)
        val deltaX = sourceCoordinate.x - swSourceX
        val deltaY = sourceCoordinate.y - swSourceY
        return Coordinate(swDestinationX + deltaX, swDestinationY + deltaY, sourceCoordinate.level)
    }

    override fun containsSourceCoord(level: Int, x: Int, y: Int): Boolean {
        if (level < this.level || level >= (this.level + levelsCount)) return false
        return x shr 6 == mapsquareSourceX && y shr 6 == mapsquareSourceY
    }

    override fun containsDestinationCoord(x: Int, y: Int): Boolean {
        return x shr 6 == mapsquareDestinationX && y shr 6 == mapsquareDestinationY
    }

    override fun toString(): String {
        val builder = StringBuilder(100)
        builder.append("MapsquareSingleSection:").appendLine()
        builder.append("\tlevel: $level").appendLine()
        builder.append("\tlevelsCount: $levelsCount").appendLine()
        builder.append("\tmapsquareSourceX: $mapsquareSourceX").appendLine()
        builder.append("\tmapsquareSourceY: $mapsquareSourceY").appendLine()
        builder.append("\tmapsquareDestinationX: $mapsquareDestinationX").appendLine()
        builder.append("\tmapsquareDestinationY: $mapsquareDestinationY").appendLine()
        return builder.toString()
    }

    companion object {
        fun decode(buffer: ByteBuf): MapsquareSingleSection {
            val level = buffer.readUnsignedByte().toInt()
            val levelsCount = buffer.readUnsignedByte().toInt()
            val mapsquareSourceX = buffer.readUnsignedShort()
            val mapsquareSourceY = buffer.readUnsignedShort()
            val mapsquareDestinationX = buffer.readUnsignedShort()
            val mapsquareDestinationY = buffer.readUnsignedShort()
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
}
