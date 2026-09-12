package dev.openrune.cache.tools.worldmap.packing

import io.netty.buffer.ByteBuf


class MapTiles {

    val X: Int = 64
    val Y: Int = 64
    val Z: Int = 4


    val tileHeights = Array<Array<IntArray>>(Z) { Array<IntArray>(X) { IntArray(Y) } }
    val tileSettings = Array(Z) { Array<ByteArray>(X) { ByteArray(Y) } }
    val overlayIds = Array<Array<ShortArray>>(Z) { Array<ShortArray>(X) { ShortArray(Y) } }
    val overlayPaths = Array<Array<ByteArray>>(Z) { Array<ByteArray>(X) { ByteArray(Y) } }
    val overlayRotations = Array<Array<ByteArray>>(Z) { Array<ByteArray>(X) { ByteArray(Y) } }
    val underlayIds = Array<Array<ShortArray>>(Z) { Array<ShortArray>(X) { ShortArray(Y) } }
}


object FullMapDefinition {

    fun decode(buf : ByteBuf, x : Int, y : Int) : MapTiles {
        val mapTiles = MapTiles()
        for (z in 0 until 4) {
            for (x in 0 until 64) {
                for (y in 0 until 64) {

                    while (true) {
                        val attribute = buf.readUnsignedShort()
                        if (attribute == 0) {
                            break
                        }
                        if (attribute == 1) {
                            val height = buf.readUnsignedByte()
                            mapTiles.tileHeights[z][x][y] = height.toInt()
                            break
                        }
                        if (attribute <= 49) {
                            val attrOpcode = attribute
                            mapTiles.overlayIds[z][x][y] = buf.readShort()
                            mapTiles.overlayPaths[z][x][y] = ((attribute - 2) / 4).toByte()
                            mapTiles.overlayRotations[z][x][y] = (attribute - 2 and 3).toByte()
                        } else if (attribute <= 81) {
                            mapTiles.tileSettings[z][x][y] = (attribute - 49).toByte()
                        } else {
                            mapTiles.underlayIds[z][x][y] = (attribute - 81).toShort()
                        }
                    }
                }
            }
        }


        return mapTiles
    }

}