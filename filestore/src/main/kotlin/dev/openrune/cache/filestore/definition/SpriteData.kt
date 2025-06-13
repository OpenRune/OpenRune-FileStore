package dev.openrune.cache.filestore.definition

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

object SpriteData {

    var spriteWidths: IntArray = intArrayOf()
    var pixels: Array<ByteArray> = arrayOf()
    var spritePalette: IntArray = intArrayOf()
    var xOffsets: IntArray = intArrayOf()
    var spriteHeights: IntArray = intArrayOf()
    var spriteCount: Int = 0
    var yOffsets: IntArray = intArrayOf()
    var spriteWidth: Int = 0
    var spriteHeight: Int = 0

    fun decode(data: ByteArray) {
        val buffer: ByteBuf = Unpooled.wrappedBuffer(data)

        buffer.readerIndex(buffer.writerIndex() - 2)
        spriteCount = buffer.readUnsignedShort()

        xOffsets = IntArray(spriteCount)
        yOffsets = IntArray(spriteCount)
        spriteWidths = IntArray(spriteCount)
        spriteHeights = IntArray(spriteCount)
        pixels = Array(spriteCount) { ByteArray(0) }

        buffer.readerIndex(buffer.writerIndex() - 7 - spriteCount * 8)

        spriteWidth = buffer.readUnsignedShort()
        spriteHeight = buffer.readUnsignedShort()
        val paletteSize = (buffer.readUnsignedByte().toInt() and 0xFF) + 1

        repeat(spriteCount) { i -> xOffsets[i] = buffer.readUnsignedShort() }
        repeat(spriteCount) { i -> yOffsets[i] = buffer.readUnsignedShort() }
        repeat(spriteCount) { i -> spriteWidths[i] = buffer.readUnsignedShort() }
        repeat(spriteCount) { i -> spriteHeights[i] = buffer.readUnsignedShort() }

        buffer.readerIndex(buffer.writerIndex() - 7 - spriteCount * 8 - (paletteSize - 1) * 3)

        spritePalette = IntArray(paletteSize)
        for (i in 1 until paletteSize) {
            spritePalette[i] = buffer.readMedium()
            if (spritePalette[i] == 0) {
                spritePalette[i] = 1
            }
        }

        buffer.readerIndex(0)

        for (i in 0 until spriteCount) {
            val width = spriteWidths[i]
            val height = spriteHeights[i]
            val size = width * height
            val pixelData = ByteArray(size)
            pixels[i] = pixelData

            val encoding = buffer.readUnsignedByte().toInt()
            when (encoding) {
                0 -> {
                    for (j in 0 until size) {
                        pixelData[j] = buffer.readByte()
                    }
                }
                1 -> {
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            pixelData[x + width * y] = buffer.readByte()
                        }
                    }
                }
                else -> {
                    throw IllegalArgumentException("Unsupported encoding: $encoding")
                }
            }
        }
    }
}
