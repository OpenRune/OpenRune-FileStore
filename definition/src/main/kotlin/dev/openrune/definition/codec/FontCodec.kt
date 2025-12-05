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
        val glyphAdvances = IntArray(256)
        for (i in glyphAdvances.indices) {
            glyphAdvances[i] = buffer.readUnsignedByte().toInt()
        }
        this.glyphAdvances = glyphAdvances

        if (buffer.readableBytes() == Byte.SIZE_BYTES) {
            val ascent = buffer.readUnsignedByte().toInt()
            this.ascent = ascent
            return
        }

        val glyphHeights = IntArray(256)
        for (i in glyphHeights.indices) {
            glyphHeights[i] = buffer.readUnsignedByte().toInt()
        }

        val bearingY = IntArray(256)
        for (i in bearingY.indices) {
            bearingY[i] = buffer.readUnsignedByte().toInt()
        }

        val rightKern = Array(256) { ByteArray(glyphHeights[it]) }
        for (i in rightKern.indices) {
            var kern: Byte = 0
            for (j in rightKern[i].indices) {
                kern = (kern + buffer.readByte()).toByte()
                rightKern[i][j] = kern
            }
        }

        var tmpPos = 0
        val leftKern = Array(256) { ByteArray(glyphHeights[it]) }
        for (i in leftKern.indices) {
            var kern: Byte = 0
            for (j in leftKern[i].indices) {
                kern = (kern + buffer.getByte(tmpPos++)).toByte()
                leftKern[i][j] = kern
            }
        }

        val kerning = ByteArray(65536)
        for (leftGlyph in 0 until 256) {
            if (leftGlyph == 32 || leftGlyph == 160) {
                continue
            }
            for (rightGlyph in 0 until 256) {
                val computed =
                    computeKerning(
                        rightKern,
                        leftKern,
                        bearingY,
                        glyphAdvances,
                        glyphHeights,
                        leftGlyph,
                        rightGlyph,
                    )
                kerning[(leftGlyph shl 8) or rightGlyph] = computed.toByte()
            }
        }
        this.kerning = kerning

        this.ascent = glyphHeights[32] + bearingY[32]
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


        maxAscent = ascent!! - minTopBearing
        maxDescent = maxBottomBearing - ascent!!

    }

    private fun computeKerning(
        rightKern: Array<ByteArray>,
        leftKern: Array<ByteArray>,
        bearingY: IntArray,
        width: IntArray,
        height: IntArray,
        leftGlyph: Int,
        rightGlyph: Int,
    ): Int {
        val minY1 = bearingY[leftGlyph]
        val maxY1 = minY1 + height[leftGlyph]
        val minY2 = bearingY[rightGlyph]
        val maxY2 = minY2 + height[rightGlyph]

        val minY = minY1.coerceAtLeast(minY2)
        val maxY = maxY1.coerceAtMost(maxY2)

        var kern = width[leftGlyph].coerceAtMost(width[rightGlyph])
        val leftGlyphKern = leftKern[leftGlyph]
        val rightGlyphKern = rightKern[rightGlyph]

        var y1 = minY - minY1
        var y2 = minY - minY2

        for (i in minY until maxY) {
            val total = leftGlyphKern[y1++].toInt() + rightGlyphKern[y2++].toInt()
            if (total < kern) {
                kern = total
            }
        }

        return -kern
    }

    override fun ByteBuf.encode(definition: FontType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = FontType(
        0, intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf(), arrayOf(),
    )

    override fun readLoop(definition: FontType, buffer: ByteBuf) {
        definition.read(-1, buffer)
    }
}