package dev.openrune.rs2

import dev.openrune.definition.game.IndexedSprite
import dev.openrune.definition.type.SpriteType
import io.netty.buffer.ByteBuf
import org.openrs2.cache.sprite.Sprite
import java.awt.image.BufferedImage

object Rs2Sprite {

    fun decode(buf: ByteBuf): List<BufferedImage> {
        val header = buf.shortAt(buf.writerIndex() - 2)
        val format = header ushr 15

        return if (format == 0) {
            Sprite.read(buf).toImages()
        } else {
            decodeRaw(buf, header and 0x7FFF)
        }
    }

    fun decodeAsType(id: Int, buf: ByteBuf): SpriteType {
        val frames = decode(buf)
        val sprites = Array(frames.size) { i ->
            val frame = frames[i]
            IndexedSprite(frame.width, frame.height, frame.getRGB(0, 0, frame.width, frame.height, null, 0, frame.width))
        }

        val validSprites = sprites.filter { it.width > 0 && it.height > 0 }
        return SpriteType(
            id = id,
            sprites = sprites,
            width = validSprites.maxOfOrNull { it.width } ?: 1,
            height = validSprites.maxOfOrNull { it.height } ?: 1
        )
    }

    private fun decodeRaw(buf: ByteBuf, count: Int): List<BufferedImage> {
        val base = buf.readerIndex()
        var pos = base

        val version = buf.byteAt(pos)
        pos += 1
        check(version == 0) { "Unsupported raw sprite version $version" }

        val alpha = buf.byteAt(pos) == 1
        pos += 1
        val sizeX = buf.shortAt(pos)
        pos += 2
        val sizeY = buf.shortAt(pos)
        pos += 2

        val images = ArrayList<BufferedImage>(count)
        repeat(count) {
            val rgb = IntArray(sizeX * sizeY)

            for (j in rgb.indices) {
                val color = buf.mediumAt(pos)
                pos += 3
                rgb[j] = if (color == 0xFF00FF) 0 else (0xFF shl 24) or color
            }

            if (alpha) {
                for (j in rgb.indices) {
                    rgb[j] = (rgb[j] and 0xFFFFFF) or (buf.byteAt(pos) shl 24)
                    pos += 1
                }
            }

            val image = BufferedImage(sizeX, sizeY, BufferedImage.TYPE_INT_ARGB)
            image.setRGB(0, 0, sizeX, sizeY, rgb, 0, sizeX)
            images += image
        }
        return images
    }

    private fun ByteBuf.byteAt(index: Int): Int = getUnsignedByte(index).toInt()

    private fun ByteBuf.shortAt(index: Int): Int = getUnsignedShort(index)

    private fun ByteBuf.mediumAt(index: Int): Int = getUnsignedMedium(index)

}
