package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.util.Coord
import io.netty.buffer.ByteBuf

abstract class WorldMapSectionType() {
    abstract fun decode(buffer: ByteBuf)
}

data class MultiSquare(
    var minPlane : Int = -1,
    var planes : Int = -1,
    var regionStartX : Int = -1,
    var regionStartY : Int = -1,
    var regionEndX : Int = -1,
    var regionEndY : Int = -1,
    var field3344 : Int = -1,
    var field3350 : Int = -1,
    var field3353 : Int = -1,
    var field3352 : Int = -1
) : WorldMapSectionType() {
    override fun decode(buffer: ByteBuf) {
        this.minPlane = buffer.readUnsignedByte().toInt()
        this.planes = buffer.readUnsignedByte().toInt()
        this.regionStartX = buffer.readUnsignedShort()
        this.regionStartY = buffer.readUnsignedShort()
        this.regionEndX = buffer.readUnsignedShort()
        this.regionEndY = buffer.readUnsignedShort()
        this.field3344 = buffer.readUnsignedShort()
        this.field3350 = buffer.readUnsignedShort()
        this.field3353 = buffer.readUnsignedShort()
        this.field3352 = buffer.readUnsignedShort()
    }
}

data class SingleSquare(
    var oldZ : Int = -1,
    var newZ : Int = -1,
    var oldX : Int = -1,
    var oldChunkXLow : Int = -1,
    var oldChunkXHigh : Int = -1,
    var oldY : Int = -1,
    var oldChunkYLow : Int = -1,
    var oldChunkYHigh : Int = -1,
    var newX : Int = -1,
    var newChunkXLow : Int = -1,
    var minPlane : Int = -1,
    var newY : Int = -1,
    var newChunkYLow : Int = -1,
    var newChunkXHigh : Int = -1,
    var newChunkYHigh : Int = -1
) : WorldMapSectionType() {
    override fun decode(buffer: ByteBuf) {
        this.oldZ = buffer.readUnsignedByte().toInt()
        this.newZ = buffer.readUnsignedByte().toInt()
        this.oldX = buffer.readUnsignedShort()
        this.oldChunkXLow = buffer.readUnsignedByte().toInt()
        this.oldChunkXHigh = buffer.readUnsignedByte().toInt()
        this.oldY = buffer.readUnsignedShort()
        this.oldChunkYLow = buffer.readUnsignedByte().toInt()
        this.oldChunkYHigh = buffer.readUnsignedByte().toInt()
        this.newX = buffer.readUnsignedShort()
        this.newChunkXLow = buffer.readUnsignedByte().toInt()
        this.newChunkXHigh = buffer.readUnsignedByte().toInt()
        this.newY = buffer.readUnsignedShort()
        this.newChunkYLow = buffer.readUnsignedByte().toInt()
        this.newChunkYHigh = buffer.readUnsignedByte().toInt()
    }

}

data class MultiZone(
    var minPlane : Int = -1,
    var planes : Int = -1,
    var regionStartX : Int = -1,
    var regionStartY : Int = -1,
    var regionEndX : Int = -1,
    var regionEndY : Int = -1
) : WorldMapSectionType() {
    override fun decode(buffer: ByteBuf) {
        this.minPlane = buffer.readUnsignedByte().toInt()
        this.planes = buffer.readUnsignedByte().toInt()
        this.regionStartX = buffer.readUnsignedShort()
        this.regionStartY = buffer.readUnsignedShort()
        this.regionEndX = buffer.readUnsignedShort()
        this.regionEndY = buffer.readUnsignedShort()
    }
}

data class SingleZone(
    var field3412 : Int = -1,
    var field3407 : Int = -1,
    var field3418 : Int = -1,
    var field3408 : Int = -1,
    var field3409 : Int = -1,
    var field3413 : Int = -1,
    var field3410 : Int = -1,
    var field3414 : Int = -1,
    var field3411 : Int = -1,
    var field3415 : Int = -1
) : WorldMapSectionType() {
    override fun decode(buffer: ByteBuf) {
        this.field3412 = buffer.readUnsignedByte().toInt()
        this.field3407 = buffer.readUnsignedByte().toInt()
        this.field3418 = buffer.readUnsignedShort()
        this.field3408 = buffer.readUnsignedByte().toInt()
        this.field3409 = buffer.readUnsignedShort()
        this.field3413 = buffer.readUnsignedByte().toInt()
        this.field3410 = buffer.readUnsignedShort()
        this.field3414 = buffer.readUnsignedByte().toInt()
        this.field3411 = buffer.readUnsignedShort()
        this.field3415 = buffer.readUnsignedByte().toInt()
    }
}

data class WorldMapAreaType(
    override var id: Int = -1,
    var backgroundColour : Int = -1,
    var fillColour : Int = -16777216,
    var zoom : Int = -1,
    var origin : Coord? = null,
    var regionLowX : Int = Integer.MAX_VALUE,
    var regionHighX : Int = 0,
    var regionLowY : Int = Integer.MAX_VALUE,
    var regionHighY : Int = 0,
    var isMain : Boolean = false,
    var internalName : String = "",
    var externalName : String = "",
    var sections : List<WorldMapSectionType> = emptyList()
) : Definition