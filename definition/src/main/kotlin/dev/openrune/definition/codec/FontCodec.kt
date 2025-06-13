package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.game.IndexedSprite
import dev.openrune.definition.type.FontType
import dev.openrune.definition.type.SpriteType
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class FontCodec(
    var leftBearingsSprite: IntArray,
    var topBearingsSprite: IntArray,
    var widthsSprite: IntArray,
    var heightsSprite: IntArray,
    var spritePaletteSprite: IntArray,
    var pixelsSprite: Array<ByteArray>,
) : DefinitionCodec<FontType> {

    override fun FontType.read(opcode: Int, buffer: ByteBuf) {
        this.leftBearings = leftBearingsSprite
        this.topBearings = topBearingsSprite
        this.widths = widthsSprite
        this.heights = heightsSprite
        this.spritePalette = spritePaletteSprite
        readMetrics(buffer.array())
        this.pixels = pixelsSprite

        var minTopBearing = Int.MAX_VALUE
        var maxBottomBearing = Int.MIN_VALUE

        for (index in 0 until 256) {
            if (topBearings[index] < minTopBearing && heights[index] != 0) {
                minTopBearing = topBearings[index]
            }
            val bottom = topBearings[index] + heights[index]
            if (bottom > maxBottomBearing) {
                maxBottomBearing = bottom
            }
        }

        maxAscent = ascent - minTopBearing
        maxDescent = maxBottomBearing - ascent
    }

    private fun FontType.readMetrics(data: ByteArray) {
        advances = IntArray(256)
        var index = 0
        if (data.size == 257) {
            for (i in advances.indices) {
                advances[i] = data[i].toInt() and 0xFF
            }
            ascent = data[256].toInt() and 0xFF
        } else {
            for (i in 0 until 256) {
                advances[i] = data[index++].toInt() and 0xFF
            }
            val charWidth = IntArray(256)
            val charHeights = IntArray(256)

            for (i in 0 until 256) {
                charWidth[i] = data[index++].toInt() and 0xFF
            }
            for (i in 0 until 256) {
                charHeights[i] = data[index++].toInt() and 0xFF
            }

            val charBitmapWidth = Array(256) { ByteArray(charWidth[it]) }
            for (i in 0 until 256) {
                var sum = 0
                for (j in charBitmapWidth[i].indices) {
                    sum += data[index++]
                    charBitmapWidth[i][j] = sum.toByte()
                }
            }

            val charBitmapHeights = Array(256) { ByteArray(charWidth[it]) }
            for (i in 0 until 256) {
                var sum: Byte = 0
                for (j in charBitmapHeights[i].indices) {
                    sum = (sum + data[index++]).toByte()
                    charBitmapHeights[i][j] = sum
                }
            }

            kerning = ByteArray(65536)
            for (i in 0 until 256) {
                if (i != 32 && i != 160) {
                    for (j in 0 until 256) {
                        if (j != 32 && j != 160) {
                            kerning[j + (i shl 8)] = computeKerning(
                                charBitmapWidth,
                                charBitmapHeights,
                                charHeights,
                                advances,
                                charWidth,
                                i,
                                j
                            ).toByte()
                        }
                    }
                }
            }

            ascent = charHeights[32] + charWidth[32]
        }
    }

    private fun computeKerning(
        firstGlyphs: Array<ByteArray>,
        secondGlyphs: Array<ByteArray>,
        firstWidths: IntArray,
        secondHeights: IntArray,
        secondWidths: IntArray,
        firstIndex: Int,
        secondIndex: Int
    ): Int {
        val firstStart = firstWidths[firstIndex]
        val firstEnd = firstStart + secondHeights[firstIndex]
        val secondStart = secondWidths[secondIndex]
        val secondEnd = secondStart + secondHeights[secondIndex]
        val commonStart = maxOf(firstStart, secondStart)
        val commonEnd = minOf(firstEnd, secondEnd)

        var minimum = firstWidths[firstIndex].coerceAtMost(secondWidths[secondIndex])
        val firstArray = firstGlyphs[firstIndex]
        val secondArray = secondGlyphs[secondIndex]

        var firstArrayIndex = commonStart - firstStart
        var secondArrayIndex = commonStart - secondStart

        for (i in commonStart until commonEnd) {
            val combinedValue =
                (firstArray[firstArrayIndex++].toInt() and 0xFF) + (secondArray[secondArrayIndex++].toInt() and 0xFF)
            if (combinedValue < minimum) {
                minimum = combinedValue
            }
        }

        return -minimum
    }

    override fun ByteBuf.encode(definition: FontType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = FontType(
        0, intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf(), arrayOf(),
    )

    override fun readLoop(definition: FontType, buffer: ByteBuf) {
        definition.read(-1, buffer)
    }
}