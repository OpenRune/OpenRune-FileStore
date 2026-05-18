package dev.openrune.cache.tools.worldmap.packing

import dev.openrune.cache.tools.worldmap.providers.WorldMapObject
import dev.openrune.cache.tools.worldmap.utils.Coordinate
import dev.openrune.definition.util.readNullableLargeSmart
import dev.openrune.definition.util.readSmart
import io.netty.buffer.ByteBuf

data class Loc(
    val id: Int,
    val type: Int,
    val orientation: Int,
    val coordinate: Coordinate
)


object MapLocDefinition {

    fun decodeBaseData(buf: ByteBuf) : List<Loc> {
        val result = mutableListOf<Loc>()
        var id = -1
        var idOffset: Int

        while (buf.readSmart().also { idOffset = it } != 0) {
            id += idOffset
            var position = 0
            var positionOffset: Int
            while (buf.readSmart().also { positionOffset = it } != 0) {
                position += positionOffset - 1
                val localY = position and 63
                val localX = position shr 6 and 63
                val height = position shr 12 and 3
                val attributes = buf.readUnsignedByte().toInt()
                val type = attributes shr 2
                val orientation = attributes and 3
                result.add(Loc(id, type, orientation, Coordinate(localX, localY, height)))
            }
        }
        return result
    }
}