@file:Suppress("MemberVisibilityCanBePrivate", "PropertyName", "unused", "DuplicatedCode", "NAME_SHADOWING", "UNUSED_PARAMETER")

package dev.openrune.cache.worldmap.rasterizer

import dev.openrune.cache.worldmap.rasterizer.provider.FontMetrics
import dev.openrune.cache.worldmap.rasterizer.sprite.ModIconsSprites
import dev.openrune.cache.worldmap.rasterizer.sprite.drawTransparentBackgroundSprite
import java.util.*
import kotlin.math.sin

/**
 * @author Kris | 16/08/2022
 */
class Font(
    metrics: FontMetrics,
    xOffsets: IntArray,
    yOffsets: IntArray,
    var widths: IntArray,
    var heights: IntArray,
    var pixels: Array<IntArray>,
    val modIconsSprites: ModIconsSprites,
) {
    var strike = -1
    var underline = -1
    var previousShadow = -1
    var shadow = -1
    var previousColour = -1
    var colour = -1
    var alpha = 256
    var justificationTotal = 0
    var justificationCurrent = 0
    var random: Random = Random()
    var lines: Array<String?> = arrayOfNulls(100)
    var advances: IntArray = metrics.advances
    var leftBearings: IntArray = xOffsets
    var topBearings: IntArray = yOffsets
    var ascent = 0
    var maxAscent = 0
    var maxDescent = 0
    var kerning: ByteArray? = metrics.kerning?.takeIf { it.isNotEmpty() }

    init {
        ascent = metrics.ascent!!
        var var8 = Int.MAX_VALUE
        var var9 = Int.MIN_VALUE
        for (var10 in 0..255) {
            if (topBearings[var10] < var8 && this.heights[var10] != 0) {
                var8 = topBearings[var10]
            }
            if (topBearings[var10] + this.heights[var10] > var9) {
                var9 = topBearings[var10] + this.heights[var10]
            }
        }
        maxAscent = ascent - var8
        maxDescent = var9 - ascent
    }

    fun reset(colour: Int, shadow: Int) {
        strike = -1
        underline = -1
        previousShadow = shadow
        this.shadow = shadow
        previousColour = colour
        this.colour = colour
        alpha = 256
        justificationTotal = 0
        justificationCurrent = 0
    }

    fun stringWidth(string: String?): Int = getTextWidth(string)

    private fun charWidth(char: Char): Int {
        if (char.code == 160) return this.advances[charToByteCp1252(' ').toInt() and 0xFF]
        return this.advances[charToByteCp1252(char).toInt() and 0xFF]
    }

    private fun kerningAdvance(previous: Int, next: Int): Int {
        val table = kerning ?: return 0
        if (previous == -1) return 0
        val index = next + (previous shl 8)
        return if (index in table.indices) table[index].toInt() else 0
    }

    private fun lineWidth(text: String?, width: Int): Int {
        val var3 = breakLines(text, intArrayOf(width), this.lines)
        var var4 = 0
        for (var5 in 0 until var3) {
            val var6 = getTextWidth(this.lines[var5])
            if (var6 > var4) {
                var4 = var6
            }
        }
        return var4
    }

    fun lineCount(text: String?, height: Int, buffer: Array<String?> = this.lines): Int = breakLines(text, intArrayOf(height), buffer)

    fun getTextWidth(text: String?): Int {
        return if (text == null) {
            0
        } else {
            var var2 = -1
            var var3 = -1
            var width = 0
            for (charPos in text.indices) {
                var ch = text[charPos]
                if (ch == '<') {
                    var2 = charPos
                } else {
                    if (ch == '>' && var2 != -1) {
                        val var7 = text.substring(var2 + 1, charPos)
                        var2 = -1
                        when (var7) {
                            "lt" -> ch = '<'
                            "gt" -> ch = '>'
                            else -> {
                                if (var7.startsWith("img=")) {
                                    try {
                                        val var8 = var7.substring(4).toInt()
                                        width += modIconsSprites.indexedSprites[var8].width
                                        var3 = -1
                                    } catch (ignored: Exception) {
                                    }
                                }
                                continue
                            }
                        }
                    }
                    if (ch.code == 160) {
                        ch = ' '
                    }
                    if (var2 == -1) {
                        width += advances[charToByteCp1252(ch).toInt() and 255]
                        width += kerningAdvance(var3, ch.code)
                        var3 = ch.code
                    }
                }
            }
            width
        }
    }

    fun breakLines(text: String?, maxWidthPerLine: IntArray?, buffer: Array<String?> = this.lines): Int {
        if (text == null) return 0
        val lineBuffer = StringBuilder(100)
        var width = 0
        var startOfLine = 0
        var lastWordIndex = -1
        var var8 = 0
        var var9: Byte = 0
        var var10 = -1
        var var11 = 0.toChar()
        var lineIndex = 0
        val length = text.length
        for (index in 0 until length) {
            var ch = text[index]
            if (ch == '<') {
                var10 = index
            } else {
                if (ch == '>' && var10 != -1) {
                    val tag = text.substring(var10 + 1, index)
                    var10 = -1
                    lineBuffer.append('<')
                    lineBuffer.append(tag)
                    lineBuffer.append('>')
                    when {
                        tag == "br" -> {
                            buffer[lineIndex] = lineBuffer.substring(startOfLine, lineBuffer.length)
                            ++lineIndex
                            startOfLine = lineBuffer.length
                            width = 0
                            lastWordIndex = -1
                            var11 = 0.toChar()
                        }
                        tag == "lt" -> {
                            width += charWidth('<')
                            width += kerningAdvance(var11.code, 60)
                            var11 = '<'
                        }
                        tag == "gt" -> {
                            width += charWidth('>')
                            width += kerningAdvance(var11.code, 62)
                            var11 = '>'
                        }
                        tag.startsWith("img=") -> {
                            try {
                                val var17 = tag.substring(4).toInt()
                                width += modIconsSprites.indexedSprites[var17].width
                                var11 = 0.toChar()
                            } catch (ignored: Exception) {
                            }
                        }
                    }
                    ch = 0.toChar()
                }
                if (var10 == -1) {
                    if (ch.code != 0) {
                        lineBuffer.append(ch)
                        width += charWidth(ch)
                        width += kerningAdvance(var11.code, ch.code)
                        var11 = ch
                    }
                    if (ch == ' ') {
                        lastWordIndex = lineBuffer.length
                        var8 = width
                        var9 = 1
                    }
                    if (maxWidthPerLine != null &&
                        width > maxWidthPerLine[if (lineIndex < maxWidthPerLine.size) lineIndex else maxWidthPerLine.size - 1] &&
                        lastWordIndex >= 0
                    ) {
                        buffer[lineIndex] = lineBuffer.substring(startOfLine, lastWordIndex - var9)
                        lineIndex++
                        startOfLine = lastWordIndex
                        lastWordIndex = -1
                        width -= var8
                        var11 = 0.toChar()
                    }
                    if (ch == '-') {
                        lastWordIndex = lineBuffer.length
                        var8 = width
                        var9 = 0
                    }
                }
            }
        }
        val var19 = lineBuffer.toString()
        if (var19.length > startOfLine) buffer[lineIndex++] = var19.substring(startOfLine)
        return lineIndex
    }

    fun calculateLineJustification(string: String, var2: Int) {
        var var3 = 0
        var var4 = false
        for (element in string) {
            if (element == '<') {
                var4 = true
            } else if (element == '>') {
                var4 = false
            } else if (!var4 && element == ' ') {
                ++var3
            }
        }
        if (var3 > 0) {
            justificationTotal = (var2 - this.stringWidth(string) shl 8) / var3
        }
    }

    fun draw(rasterizer2D: Rasterizer2D, var1: String?, var2: Int, var3: Int, var4: Int, var5: Int) {
        if (var1 != null) {
            reset(var4, var5)
            draw0(rasterizer2D, var1, var2, var3)
        }
    }

    fun drawAlpha(rasterizer2D: Rasterizer2D, var1: String?, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int) {
        if (var1 != null) {
            reset(var4, var5)
            alpha = var6
            draw0(rasterizer2D, var1, var2, var3)
        }
    }

    fun drawRightAligned(rasterizer2D: Rasterizer2D, var1: String?, var2: Int, var3: Int, var4: Int, var5: Int) {
        if (var1 != null) {
            reset(var4, var5)
            draw0(rasterizer2D, var1, var2 - stringWidth(var1), var3)
        }
    }

    fun drawCentered(rasterizer2D: Rasterizer2D, var1: String?, var2: Int, var3: Int, var4: Int, var5: Int) {
        if (var1 != null) {
            reset(var4, var5)
            draw0(rasterizer2D, var1, var2 - stringWidth(var1) / 2, var3)
        }
    }

    fun drawCenteredWave(rasterizer2D: Rasterizer2D, var1: String?, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int) {
        if (var1 != null) {
            reset(var4, var5)
            val var7 = IntArray(var1.length)
            for (var8 in var1.indices) {
                var7[var8] = (sin(var8.toDouble() / 2.0 + var6.toDouble() / 5.0) * 5.0).toInt()
            }
            this.drawWithOffsets0(rasterizer2D, var1, var2 - stringWidth(var1) / 2, var3, null as IntArray?, var7)
        }
    }

    fun drawCenteredWave2(rasterizer2D: Rasterizer2D, var1: String?, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int) {
        if (var1 != null) {
            reset(var4, var5)
            val var7 = IntArray(var1.length)
            val var8 = IntArray(var1.length)
            for (var9 in var1.indices) {
                var7[var9] = (sin(var9.toDouble() / 5.0 + var6.toDouble() / 5.0) * 5.0).toInt()
                var8[var9] = (sin(var9.toDouble() / 3.0 + var6.toDouble() / 5.0) * 5.0).toInt()
            }
            this.drawWithOffsets0(rasterizer2D, var1, var2 - stringWidth(var1) / 2, var3, var7, var8)
        }
    }

    fun drawCenteredShake(rasterizer2D: Rasterizer2D, var1: String?, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int, var7: Int) {
        if (var1 != null) {
            reset(var4, var5)
            var var8 = 7.0 - var7.toDouble() / 8.0
            if (var8 < 0.0) {
                var8 = 0.0
            }
            val var10 = IntArray(var1.length)
            for (var11 in var1.indices) {
                var10[var11] = (sin(var11.toDouble() / 1.5 + var6.toDouble() / 1.0) * var8).toInt()
            }
            this.drawWithOffsets0(rasterizer2D, var1, var2 - stringWidth(var1) / 2, var3, null as IntArray?, var10)
        }
    }

    fun drawRandomAlphaAndSpacing(rasterizer2D: Rasterizer2D, var1: String?, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int) {
        if (var1 != null) {
            reset(var4, var5)
            random.setSeed(var6.toLong())
            alpha = 192 + (random.nextInt() and 31)
            val var7 = IntArray(var1.length)
            var var8 = 0
            for (var9 in var1.indices) {
                var7[var9] = var8
                if (random.nextInt() and 3 == 0) {
                    ++var8
                }
            }
            this.drawWithOffsets0(rasterizer2D, var1, var2, var3, var7, null as IntArray?)
        }
    }

    fun drawWithOffsets0(rasterizer2D: Rasterizer2D, var1: String, var2: Int, var3: Int, var4: IntArray?, var5: IntArray?) {
        var var2 = var2
        var var3 = var3
        var3 -= ascent
        var var6 = -1
        var var7 = -1
        var var8 = 0
        for (var9 in var1.indices) {
            if (var1[var9].code != 0) {
                var var10 = (charToByteCp1252(var1[var9]).toInt() and 255).toChar()
                if (var10 == '<') {
                    var6 = var9
                } else {
                    var var12: Int
                    var var13: Int
                    var var14: Int
                    if (var10 == '>' && var6 != -1) {
                        val var11 = var1.substring(var6 + 1, var9)
                        var6 = -1
                        if (var11 == "lt") {
                            var10 = '<'
                        } else {
                            if (var11 != "gt") {
                                if (var11.startsWith("img=")) {
                                    try {
                                        var12 = if (var4 != null) {
                                            var4[var8]
                                        } else {
                                            0
                                        }
                                        var13 = if (var5 != null) {
                                            var5[var8]
                                        } else {
                                            0
                                        }
                                        ++var8
                                        var14 = parseNumber(var11.substring(4), 10)
                                        val var15 = modIconsSprites.indexedSprites[var14]
                                        var15.drawTransparentBackgroundSprite(rasterizer2D, var12 + var2, var13 + (var3 + ascent - var15.height))
                                        var2 += var15.width
                                        var7 = -1
                                    } catch (_: Exception) {
                                    }
                                } else {
                                    decodeTag(var11)
                                }
                                continue
                            }
                            var10 = '>'
                        }
                    }
                    if (var10.code == 160) {
                        var10 = ' '
                    }
                    if (var6 == -1) {
                        var2 += kerningAdvance(var7, var10.code)
                        val var17 = widths[var10.code]
                        var12 = heights[var10.code]
                        var13 = if (var4 != null) {
                            var4[var8]
                        } else {
                            0
                        }
                        var14 = if (var5 != null) {
                            var5[var8]
                        } else {
                            0
                        }
                        ++var8
                        if (var10 != ' ') {
                            if (alpha == 256) {
                                if (shadow != -1) {
                                    drawGlyph2(
                                        rasterizer2D,
                                        pixels[var10.code],
                                        var13 + var2 + leftBearings[var10.code] + 1,
                                        var3 + var14 + topBearings[var10.code] + 1,
                                        var17,
                                        var12,
                                        shadow
                                    )
                                }
                                drawGlyph(
                                    rasterizer2D,
                                    pixels[var10.code],
                                    var13 + var2 + leftBearings[var10.code],
                                    var3 + var14 + topBearings[var10.code],
                                    var17,
                                    var12,
                                    colour
                                )
                            } else {
                                if (shadow != -1) {
                                    drawGlyphAlpha2(
                                        rasterizer2D,
                                        pixels[var10.code],
                                        var13 + var2 + leftBearings[var10.code] + 1,
                                        var3 + var14 + topBearings[var10.code] + 1,
                                        var17,
                                        var12,
                                        shadow,
                                        alpha
                                    )
                                }
                                drawGlyphAlpha(
                                    rasterizer2D,
                                    pixels[var10.code],
                                    var13 + var2 + leftBearings[var10.code],
                                    var3 + var14 + topBearings[var10.code],
                                    var17,
                                    var12,
                                    colour,
                                    alpha
                                )
                            }
                        } else if (justificationTotal > 0) {
                            justificationCurrent += justificationTotal
                            var2 += justificationCurrent shr 8
                            justificationCurrent = justificationCurrent and 255
                        }
                        val var18 = advances[var10.code]
                        if (strike != -1) {
                            rasterizer2D.drawHorizontalLine(
                                var2,
                                var3 + (ascent.toDouble() * 0.7).toInt(),
                                var18,
                                strike
                            )
                        }
                        if (underline != -1) {
                            rasterizer2D.drawHorizontalLine(var2, var3 + ascent, var18, underline)
                        }
                        var2 += var18
                        var7 = var10.code
                    }
                }
            }
        }
    }

    fun drawLines(
        rasterizer2D: Rasterizer2D,
        text: String?,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        colour: Int,
        shadow: Int,
        mode: Int,
        var9: Int,
        var10: Int
    ): Int {
        var var9 = var9
        var var10 = var10
        return if (text == null) {
            0
        } else {
            this.reset(colour, shadow)
            if (var10 == 0) {
                var10 = ascent
            }
            var var11: IntArray? = intArrayOf(width)
            if (height < var10 + maxAscent + maxDescent && height < var10 + var10) {
                var11 = null
            }
            val var12: Int = breakLines(text, var11, lines)
            if (var9 == 3 && var12 == 1) {
                var9 = 1
            }
            var var13: Int
            var var14: Int
            when (var9) {
                0 -> {
                    var13 = y + maxAscent
                }
                1 -> {
                    var13 = y + (height - maxAscent - maxDescent - var10 * (var12 - 1)) / 2 + maxAscent
                }
                2 -> {
                    var13 = y + height - maxDescent - var10 * (var12 - 1)
                }
                else -> {
                    var14 = (height - maxAscent - maxDescent - var10 * (var12 - 1)) / (var12 + 1)
                    if (var14 < 0) {
                        var14 = 0
                    }
                    var13 = y + var14 + maxAscent
                    var10 += var14
                }
            }
            var14 = 0
            while (var14 < var12) {
                val line = lines[var14]!!

                if (mode == 0) {
                    this.draw0(rasterizer2D, line, x, var13)
                } else if (mode == 1) {
                    this.draw0(
                        rasterizer2D,
                        line,
                        x + (width - this.stringWidth(line)) / 2,
                        var13
                    )
                } else if (mode == 2) {
                    this.draw0(rasterizer2D, line, x + width - this.stringWidth(line), var13)
                } else if (var14 == var12 - 1) {
                    this.draw0(rasterizer2D, line, x, var13)
                } else {
                    this.calculateLineJustification(line, width)
                    this.draw0(rasterizer2D, line, x, var13)
                    justificationTotal = 0
                }
                var13 += var10
                ++var14
            }
            var12
        }
    }

    fun parseNumber(text: CharSequence, base: Int, var2: Boolean): Int {
        return if (base in 2..36) {
            var var3 = false
            var var4 = false
            var var5 = 0
            val var6 = text.length
            for (var7 in 0 until var6) {
                val var8 = text[var7]
                if (var7 == 0) {
                    if (var8 == '-') {
                        var3 = true
                        continue
                    }
                    if (var8 == '+') {
                        continue
                    }
                }
                var var10: Int
                var10 = when (var8) {
                    in '0'..'9' -> {
                        var8.code - '0'.code
                    }
                    in 'A'..'Z' -> {
                        var8.code - '7'.code
                    }
                    else -> {
                        if (var8 < 'a' || var8 > 'z') {
                            throw NumberFormatException()
                        }
                        var8.code - 'W'.code
                    }
                }
                if (var10 >= base) {
                    throw NumberFormatException()
                }
                if (var3) {
                    var10 = -var10
                }
                val var9 = var10 + var5 * base
                if (var9 / base != var5) {
                    throw NumberFormatException()
                }
                var5 = var9
                var4 = true
            }
            if (!var4) {
                throw NumberFormatException()
            } else {
                var5
            }
        } else {
            throw IllegalArgumentException("" + base)
        }
    }

    fun parseNumber(var0: CharSequence, var1: Int): Int {
        return parseNumber(var0, var1, true)
    }

    fun decodeTag(var1: String) {
        try {
            if (var1.startsWith("col=")) {
                colour = parseNumber(var1.substring(4), 16)
            } else if (var1 == "/col") {
                colour = previousColour
            } else if (var1.startsWith("str=")) {
                strike = parseNumber(var1.substring(4), 16)
            } else if (var1 == "str") {
                strike = 8388608
            } else if (var1 == "/str") {
                strike = -1
            } else if (var1.startsWith("u=")) {
                underline = parseNumber(var1.substring(2), 16)
            } else if (var1 == "u") {
                underline = 0
            } else if (var1 == "/u") {
                underline = -1
            } else if (var1.startsWith("shad=")) {
                shadow = parseNumber(var1.substring(5), 16)
            } else if (var1 == "shad") {
                shadow = 0
            } else if (var1 == "/shad") {
                shadow = previousShadow
            } else if (var1 == "br") {
                reset(previousColour, previousShadow)
            }
        } catch (_: java.lang.Exception) {
        }
    }

    fun drawGlyph2(rasterizer2D: Rasterizer2D, var0: IntArray, var1: Int, var2: Int, var3: Int, var4: Int, var5: Int) {
        var var1 = var1
        var var2 = var2
        var var3 = var3
        var var4 = var4
        var var6: Int = var1 + var2 * rasterizer2D.width
        var var7: Int = rasterizer2D.width - var3
        var var8 = 0
        var var9 = 0
        var var10: Int
        if (var2 < rasterizer2D.minY) {
            var10 = rasterizer2D.minY - var2
            var4 -= var10
            var2 = rasterizer2D.minY
            var9 += var3 * var10
            var6 += var10 * rasterizer2D.width
        }
        if (var2 + var4 > rasterizer2D.maxY) {
            var4 -= var2 + var4 - rasterizer2D.maxY
        }
        if (var1 < rasterizer2D.minX) {
            var10 = rasterizer2D.minX - var1
            var3 -= var10
            var1 = rasterizer2D.minX
            var9 += var10
            var6 += var10
            var8 += var10
            var7 += var10
        }
        if (var3 + var1 > rasterizer2D.maxX) {
            var10 = var3 + var1 - rasterizer2D.maxX
            var3 -= var10
            var8 += var10
            var7 += var10
        }
        if (var3 > 0 && var4 > 0) {
            placeGlyph(rasterizer2D.pixels, var0, var5, var9, var6, var3, var4, var7, var8)
        }
    }

    fun placeGlyph(var0: IntArray, var1: IntArray, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int, var7: Int, var8: Int) {
        var var3 = var3
        var var4 = var4
        var var5 = var5
        val var9 = -(var5 shr 2)
        var5 = -(var5 and 3)
        for (var10 in -var6..-1) {
            var var11: Int = var9
            while (var11 < 0) {
                if (var1[var3++] != 0) {
                    var0[var4++] = var2
                } else {
                    ++var4
                }
                if (var1[var3++] != 0) {
                    var0[var4++] = var2
                } else {
                    ++var4
                }
                if (var1[var3++] != 0) {
                    var0[var4++] = var2
                } else {
                    ++var4
                }
                if (var1[var3++] != 0) {
                    var0[var4++] = var2
                } else {
                    ++var4
                }
                ++var11
            }
            var11 = var5
            while (var11 < 0) {
                if (var1[var3++] != 0) {
                    var0[var4++] = var2
                } else {
                    ++var4
                }
                ++var11
            }
            var4 += var7
            var3 += var8
        }
    }

    fun draw0(rasterizer2D: Rasterizer2D, var1: String, var2: Int, var3: Int) {
        var var2 = var2
        var var3 = var3
        var3 -= ascent
        var var4 = -1
        var var5 = -1
        for (var6 in var1.indices) {
            if (var1[var6].code != 0) {
                var var7 = (charToByteCp1252(var1[var6]).toInt() and 255).toChar()
                if (var7 == '<') {
                    var4 = var6
                } else {
                    var var9: Int
                    if (var7 == '>' && var4 != -1) {
                        val var8 = var1.substring(var4 + 1, var6)
                        var4 = -1
                        if (var8 == "lt") {
                            var7 = '<'
                        } else {
                            if (var8 != "gt") {
                                if (var8.startsWith("img=")) {
                                    try {
                                        var9 = parseNumber(var8.substring(4), 10)
                                        val var10 = modIconsSprites.indexedSprites[var9]
                                        var10.drawTransparentBackgroundSprite(rasterizer2D, var2, var3 + ascent - var10.height)
                                        var2 += var10.width
                                        var5 = -1
                                    } catch (_: Exception) {
                                    }
                                } else {
                                    this.decodeTag(var8)
                                }
                                continue
                            }
                            var7 = '>'
                        }
                    }
                    if (var7.code == 160) {
                        var7 = ' '
                    }
                    if (var4 == -1) {
                        var2 += kerningAdvance(var5, var7.code)
                        val var12 = widths[var7.code]
                        var9 = heights[var7.code]
                        if (var7 != ' ') {
                            if (alpha == 256) {
                                if (shadow != -1) {
                                    drawGlyph2(
                                        rasterizer2D,
                                        pixels[var7.code],
                                        var2 + leftBearings[var7.code] + 1,
                                        var3 + topBearings[var7.code] + 1,
                                        var12,
                                        var9,
                                        shadow
                                    )
                                }
                                this.drawGlyph(
                                    rasterizer2D,
                                    pixels[var7.code],
                                    var2 + leftBearings[var7.code],
                                    var3 + topBearings[var7.code],
                                    var12,
                                    var9,
                                    colour
                                )
                            } else {
                                if (shadow != -1) {
                                    drawGlyphAlpha2(
                                        rasterizer2D,
                                        pixels[var7.code],
                                        var2 + leftBearings[var7.code] + 1,
                                        var3 + topBearings[var7.code] + 1,
                                        var12,
                                        var9,
                                        shadow,
                                        alpha
                                    )
                                }
                                this.drawGlyphAlpha(
                                    rasterizer2D,
                                    pixels[var7.code],
                                    var2 + leftBearings[var7.code],
                                    var3 + topBearings[var7.code],
                                    var12,
                                    var9,
                                    colour,
                                    alpha
                                )
                            }
                        } else if (justificationTotal > 0) {
                            justificationCurrent += justificationTotal
                            var2 += justificationCurrent shr 8
                            justificationCurrent = justificationCurrent and 255
                        }
                        val var13 = advances[var7.code]
                        if (strike != -1) {
                            rasterizer2D.drawHorizontalLine(
                                var2,
                                var3 + (ascent.toDouble() * 0.7).toInt(),
                                var13,
                                strike
                            )
                        }
                        if (underline != -1) {
                            rasterizer2D.drawHorizontalLine(var2, var3 + ascent + 1, var13, underline)
                        }
                        var2 += var13
                        var5 = var7.code
                    }
                }
            }
        }
    }

    fun drawGlyphAlpha(rasterizer2D: Rasterizer2D, var1: IntArray, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int, var7: Int) {
        var var2 = var2
        var var3 = var3
        var var4 = var4
        var var5 = var5
        var var8: Int = var3 * rasterizer2D.width + var2
        var var9: Int = rasterizer2D.width - var4
        var var10 = 0
        var var11 = 0
        var var12: Int
        if (var3 < rasterizer2D.minY) {
            var12 = rasterizer2D.minY - var3
            var5 -= var12
            var3 = rasterizer2D.minY
            var11 += var12 * var4
            var8 += var12 * rasterizer2D.width
        }
        if (var3 + var5 > rasterizer2D.maxY) {
            var5 -= var3 + var5 - rasterizer2D.maxY
        }
        if (var2 < rasterizer2D.minX) {
            var12 = rasterizer2D.minX - var2
            var4 -= var12
            var2 = rasterizer2D.minX
            var11 += var12
            var8 += var12
            var10 += var12
            var9 += var12
        }
        if (var2 + var4 > rasterizer2D.maxX) {
            var12 = var2 + var4 - rasterizer2D.maxX
            var4 -= var12
            var10 += var12
            var9 += var12
        }
        if (var4 > 0 && var5 > 0) {
            placeGlyphAlpha(rasterizer2D.pixels, var1, var6, var11, var8, var4, var5, var9, var10, var7)
        }
    }

    fun drawGlyph(rasterizer2D: Rasterizer2D, var1: IntArray, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int) {
        var var2 = var2
        var var3 = var3
        var var4 = var4
        var var5 = var5
        var var7: Int = var3 * rasterizer2D.width + var2
        var var8: Int = rasterizer2D.width - var4
        var var9 = 0
        var var10 = 0
        var var11: Int
        if (var3 < rasterizer2D.minY) {
            var11 = rasterizer2D.minY - var3
            var5 -= var11
            var3 = rasterizer2D.minY
            var10 += var11 * var4
            var7 += var11 * rasterizer2D.width
        }
        if (var3 + var5 > rasterizer2D.maxY) {
            var5 -= var3 + var5 - rasterizer2D.maxY
        }
        if (var2 < rasterizer2D.minX) {
            var11 = rasterizer2D.minX - var2
            var4 -= var11
            var2 = rasterizer2D.minX
            var10 += var11
            var7 += var11
            var9 += var11
            var8 += var11
        }
        if (var2 + var4 > rasterizer2D.maxX) {
            var11 = var2 + var4 - rasterizer2D.maxX
            var4 -= var11
            var9 += var11
            var8 += var11
        }
        if (var4 > 0 && var5 > 0) {
            placeGlyph(rasterizer2D.pixels, var1, var6, var10, var7, var4, var5, var8, var9)
        }
    }

    fun drawGlyphAlpha2(rasterizer2D: Rasterizer2D, var0: IntArray, var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int) {
        var var1 = var1
        var var2 = var2
        var var3 = var3
        var var4 = var4
        var var7: Int = var1 + var2 * rasterizer2D.width
        var var8: Int = rasterizer2D.width - var3
        var var9 = 0
        var var10 = 0
        var var11: Int
        if (var2 < rasterizer2D.minY) {
            var11 = rasterizer2D.minY - var2
            var4 -= var11
            var2 = rasterizer2D.minY
            var10 += var3 * var11
            var7 += var11 * rasterizer2D.width
        }
        if (var2 + var4 > rasterizer2D.maxY) {
            var4 -= var2 + var4 - rasterizer2D.maxY
        }
        if (var1 < rasterizer2D.minX) {
            var11 = rasterizer2D.minX - var1
            var3 -= var11
            var1 = rasterizer2D.minX
            var10 += var11
            var7 += var11
            var9 += var11
            var8 += var11
        }
        if (var3 + var1 > rasterizer2D.maxX) {
            var11 = var3 + var1 - rasterizer2D.maxX
            var3 -= var11
            var9 += var11
            var8 += var11
        }
        if (var3 > 0 && var4 > 0) {
            placeGlyphAlpha(rasterizer2D.pixels, var0, var5, var10, var7, var3, var4, var8, var9, var6)
        }
    }

    fun placeGlyphAlpha(var0: IntArray, var1: IntArray, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int, var7: Int, var8: Int, var9: Int) {
        var var2 = var2
        var var3 = var3
        var var4 = var4
        var var9 = var9
        var2 = ((var2 and 65280) * var9 and 16711680) + (var9 * (var2 and 16711935) and -16711936) shr 8
        var9 = 256 - var9
        for (var10 in -var6..-1) {
            for (var11 in -var5..-1) {
                if (var1[var3++] != 0) {
                    val var12 = var0[var4]
                    var0[var4++] = (((var12 and 65280) * var9 and 16711680) + ((var12 and 16711935) * var9 and -16711936) shr 8) + var2
                } else {
                    ++var4
                }
            }
            var4 += var7
            var3 += var8
        }
    }

    private companion object {
        fun charToByteCp1252(var0: Char): Byte = when (var0.code) {
            in 1..127, in 160..255 -> var0.code.toByte()
            8364 -> -128
            8218 -> -126
            402 -> -125
            8222 -> -124
            8230 -> -123
            8224 -> -122
            8225 -> -121
            710 -> -120
            8240 -> -119
            352 -> -118
            8249 -> -117
            338 -> -116
            381 -> -114
            8216 -> -111
            8217 -> -110
            8220 -> -109
            8221 -> -108
            8226 -> -107
            8211 -> -106
            8212 -> -105
            732 -> -104
            8482 -> -103
            353 -> -102
            8250 -> -101
            339 -> -100
            382 -> -98
            376 -> -97
            else -> 63
        }

        fun byteToCharCp1252(
            stringBuffer: CharSequence,
            array: ByteArray,
            startIndex: Int = 0,
            endIndex: Int = stringBuffer.length,
            offset: Int = 0
        ): Int {
            val length = endIndex - startIndex
            for (index in 0 until length) {
                when (val encodedChar = stringBuffer[index + startIndex].code) {
                    in 1..127, in 160..255 -> array[index + offset] = encodedChar.toByte()
                    8364 -> array[index + offset] = -128
                    8218 -> array[index + offset] = -126
                    402 -> array[index + offset] = -125
                    8222 -> array[index + offset] = -124
                    8230 -> array[index + offset] = -123
                    8224 -> array[index + offset] = -122
                    8225 -> array[index + offset] = -121
                    710 -> array[index + offset] = -120
                    8240 -> array[index + offset] = -119
                    352 -> array[index + offset] = -118
                    8249 -> array[index + offset] = -117
                    338 -> array[index + offset] = -116
                    381 -> array[index + offset] = -114
                    8216 -> array[index + offset] = -111
                    8217 -> array[index + offset] = -110
                    8220 -> array[index + offset] = -109
                    8221 -> array[index + offset] = -108
                    8226 -> array[index + offset] = -107
                    8211 -> array[index + offset] = -106
                    8212 -> array[index + offset] = -105
                    732 -> array[index + offset] = -104
                    8482 -> array[index + offset] = -103
                    353 -> array[index + offset] = -102
                    8250 -> array[index + offset] = -101
                    339 -> array[index + offset] = -100
                    382 -> array[index + offset] = -98
                    376 -> array[index + offset] = -97
                    else -> array[index + offset] = 63
                }
            }
            return length
        }
    }
}
