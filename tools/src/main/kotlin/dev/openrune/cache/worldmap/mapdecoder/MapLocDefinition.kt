package dev.openrune.cache.worldmap.mapdecoder

import dev.openrune.cache.worldmap.worldmap.utils.Coordinate
import dev.openrune.definition.util.readSmart
import io.netty.buffer.ByteBuf

data class Loc(
    val id: Int,
    val type: Int,
    val orientation: Int,
    val coordinate: Coordinate
)


object MapLocDefinition {

    /**
     * Decodes the loc list of a mapsquare. Returns what was read so far if the buffer runs out, which
     * happens for squares whose loc file is still XTEA encrypted or otherwise not plain loc data.
     */
    fun decodeBaseData(buf: ByteBuf) : List<Loc> {
        val result = mutableListOf<Loc>()
        var id = -1

        while (buf.isReadable) {
            val idOffset = buf.readExtendedSmart()
            if (idOffset == 0) break
            id += idOffset
            var position = 0
            while (buf.isReadable) {
                val positionOffset = buf.readSmart()
                if (positionOffset == 0) break
                if (!buf.isReadable) break
                position += positionOffset - 1
                val localY = position and 63
                val localX = position shr 6 and 63
                val height = position shr 12
                val attributes = buf.readUnsignedByte().toInt()
                val type = attributes shr 2
                val orientation = attributes and 3
                result.add(Loc(id, type, orientation, Coordinate(localX, localY, height)))
            }
        }
        return result
    }

    /**
     * Reads the loc id delta. A value of 32767 means "add 32767 and keep reading", so ids beyond a
     * single smart's range are encoded as a run of them.
     */
    private fun ByteBuf.readExtendedSmart(): Int {
        var total = 0
        while (true) {
            val value = readSmart()
            if (value != 32767) return total + value
            total += 32767
            if (!isReadable) return total
        }
    }
}