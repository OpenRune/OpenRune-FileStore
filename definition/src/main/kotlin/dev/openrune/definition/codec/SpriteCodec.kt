package dev.openrune.definition.codec

import dev.openrune.buffer.*
import io.netty.buffer.ByteBuf
import dev.openrune.buffer.Writer
import dev.openrune.buffer.arrayRD
import dev.openrune.buffer.positionRD
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.SpriteType
import dev.openrune.definition.game.IndexedSprite

class SpriteCodec : DefinitionCodec<SpriteType> {
    override fun SpriteType.read(opcode: Int, buffer: ByteBuf) {
        buffer.positionRD(buffer.arrayRD().size - 2)
        val size: Int = buffer.readShortRD()
        buffer.positionRD(buffer.arrayRD().size - 7 - size * 8)

        val offsetX: Int = buffer.readShortRD()
        val offsetY: Int = buffer.readShortRD()

        val paletteSize: Int = buffer.readUnsignedByteRD() + 1

        val sprites = Array(size) { IndexedSprite() }
        for (index in 0 until size) {
            sprites[index].offsetX = buffer.readShortRD()
        }
        for (index in 0 until size) {
            sprites[index].offsetY = buffer.readShortRD()
        }
        for (index in 0 until size) {
            sprites[index].width = buffer.readShortRD()
        }
        for (index in 0 until size) {
            sprites[index].height = buffer.readShortRD()
        }
        for (index in 0 until size) {
            val sprite = sprites[index]
            sprite.subWidth = offsetX - sprite.width - sprite.offsetX
            sprite.subHeight = offsetY - sprite.height - sprite.offsetY
        }

        buffer.positionRD(buffer.arrayRD().size - 7 - size * 8 - (paletteSize - 1) * 3)
        val palette = IntArray(paletteSize)
        for (index in 1 until paletteSize) {
            palette[index] = buffer.readUnsignedMediumRD()
            if (palette[index] == 0) {
                palette[index] = 1
            }
        }
        for (index in 0 until size) {
            sprites[index].palette = palette
        }

        buffer.positionRD(0)
        for (index in 0 until size) {
            val sprite = sprites[index]
            val area = sprite.width * sprite.height

            sprite.raster = ByteArray(area)

            val setting: Int = buffer.readUnsignedByteRD()
            if (setting and 0x2 == 0) {
                if (setting and 0x1 == 0) {
                    for (pixel in 0 until area) {
                        sprite.raster[pixel] = buffer.readByteRD().toByte()
                    }
                } else {
                    for (x in 0 until sprite.width) {
                        for (y in 0 until sprite.height) {
                            sprite.raster[x + y * sprite.width] = buffer.readByteRD().toByte()
                        }
                    }
                }
            } else {
                var transparent = false
                val alpha = ByteArray(area)
                if (setting and 0x1 == 0) {
                    for (pixel in 0 until area) {
                        sprite.raster[pixel] = buffer.readByteRD().toByte()
                    }
                    for (pixel in 0 until area) {
                        alpha[pixel] = buffer.readByteRD().toByte()
                        val p = alpha[pixel].toInt()
                        transparent = transparent or (p != -1)
                    }
                } else {
                    for (x in 0 until sprite.width) {
                        for (y in 0 until sprite.height) {
                            sprite.raster[x + y * sprite.width] = buffer.readByteRD().toByte()
                        }
                    }
                    for (x in 0 until sprite.width) {
                        for (y in 0 until sprite.height) {
                            alpha[x + y * sprite.width] = buffer.readByteRD().toByte()
                            val pixel = alpha[x + y * sprite.width].toInt()
                            transparent = transparent or (pixel != -1)
                        }
                    }
                }
                if (transparent) {
                    sprite.alpha = alpha
                }
            }
        }
        this.sprites = sprites
    }

    override fun Writer.encode(definition: SpriteType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = SpriteType(0)

    override fun readLoop(definition: SpriteType, buffer: ByteBuf) {
        definition.read(-1, buffer)
    }
}