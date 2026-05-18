@file:Suppress("PrivatePropertyName", "FunctionName", "PropertyName", "unused", "MemberVisibilityCanBePrivate", "DuplicatedCode", "NAME_SHADOWING")

package dev.openrune.cache.tools.worldmap.rasterizer

import java.awt.image.BufferedImage
import kotlin.math.floor

/**
 * @author Kris | 15/08/2022
 */
class Rasterizer2D(width: Int, height: Int) {
    var pixels: IntArray = IntArray(width * height)
    var width = 0
    var height = 0
    var minY = 0
    var maxY = 0
    var minX = 0
    var maxX = 0

    init {
        replace(pixels, width, height)
    }

    fun toBufferedImage(): BufferedImage {
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in minX until maxX) {
            for (y in minY until maxY) {
                bufferedImage.setRGB(x, y, pixels[x + width * y])
            }
        }
        return bufferedImage
    }

    fun downscale(factor: Int): BufferedImage {
        val bufferedImage = BufferedImage(width / factor, height / factor, BufferedImage.TYPE_INT_RGB)
        for (x in minX until maxX step factor) {
            for (y in minY until maxY step factor) {
                var red = 0
                var green = 0
                var blue = 0
                for (subX in 0 until factor) {
                    for (subY in 0 until factor) {
                        val value = pixels[(x + subX) + width * (y + subY)]
                        red += value and 0xFF
                        green += value shr 8 and 0xFF
                        blue += value shr 16 and 0xFF
                    }
                }
                red = red shr factor
                green = green shr factor
                blue = blue shr factor
                val result = (red and 0xFF) or (green and 0xFF shl 8) or (blue and 0xFF shl 16)
                bufferedImage.setRGB(x / factor, y / factor, result)
            }
        }
        return bufferedImage
    }

    fun dispose() {
        pixels = emptyIntArray
        width = 0
        height = 0
        minY = 0
        maxY = 0
        minX = 0
        maxX = 0
    }

    fun replace(var0: IntArray, var1: Int, var2: Int) {
        pixels = var0
        width = var1
        height = var2
        setClip(0, 0, var1, var2)
    }

    fun resetClip() {
        minX = 0
        minY = 0
        maxX = width
        maxY = height
    }

    fun setClip(startX: Int, startY: Int, endX: Int, endY: Int) {
        var var0 = startX
        var var1 = startY
        var var2 = endX
        var var3 = endY
        if (var0 < 0) {
            var0 = 0
        }
        if (var1 < 0) {
            var1 = 0
        }
        if (var2 > width) {
            var2 = width
        }
        if (var3 > height) {
            var3 = height
        }
        minX = var0
        minY = var1
        maxX = var2
        maxY = var3
    }

    fun expandClip(var0: Int, var1: Int, var2: Int, var3: Int) {
        if (minX < var0) {
            minX = var0
        }
        if (minY < var1) {
            minY = var1
        }
        if (maxX > var2) {
            maxX = var2
        }
        if (maxY > var3) {
            maxY = var3
        }
    }

    fun getClipBounds(var0: IntArray) {
        var0[0] = minX
        var0[1] = minY
        var0[2] = maxX
        var0[3] = maxY
    }

    fun setClipBounds(var0: IntArray) {
        minX = var0[0]
        minY = var0[1]
        maxX = var0[2]
        maxY = var0[3]
    }

    fun clear() {
        var var0 = 0
        var var1: Int = width * height - 7
        while (var0 < var1) {
            pixels[var0++] = 0
            pixels[var0++] = 0
            pixels[var0++] = 0
            pixels[var0++] = 0
            pixels[var0++] = 0
            pixels[var0++] = 0
            pixels[var0++] = 0
            pixels[var0++] = 0
        }
        var1 += 7
        while (var0 < var1) {
            pixels[var0++] = 0
        }
    }

    fun setPixel(x: Int, y: Int, var2: Int) {
        if (x >= minX && y >= minY && x < maxX && y < maxY) {
            pixels[x + width * y] = var2
        }
    }

    fun drawCircle(x: Int, y: Int, var2: Int, rgb: Int) {
        var curY = y
        var var2 = var2
        if (var2 == 0) {
            setPixel(x, curY, rgb)
        } else {
            if (var2 < 0) {
                var2 = -var2
            }
            var var4 = curY - var2
            if (var4 < minY) {
                var4 = minY
            }
            var var5 = var2 + curY + 1
            if (var5 > maxY) {
                var5 = maxY
            }
            var var6 = var4
            val var7 = var2 * var2
            var var8 = 0
            var var9 = curY - var4
            var var10 = var9 * var9
            var var11 = var10 - var9
            if (curY > var5) {
                curY = var5
            }
            var var12: Int
            var var13: Int
            var var14: Int
            var var15: Int
            while (var6 < curY) {
                while (var11 <= var7 || var10 <= var7) {
                    var10 += var8 + var8
                    var11 += var8++ + var8
                }
                var12 = x - var8 + 1
                if (var12 < minX) {
                    var12 = minX
                }
                var13 = x + var8
                if (var13 > maxX) {
                    var13 = maxX
                }
                var14 = var12 + var6 * width
                var15 = var12
                while (var15 < var13) {
                    pixels[var14++] = rgb
                    ++var15
                }
                ++var6
                var10 -= var9-- + var9
                var11 -= var9 + var9
            }
            var8 = var2
            var9 = var6 - curY
            var11 = var7 + var9 * var9
            var10 = var11 - var2
            var11 -= var9
            while (var6 < var5) {
                while (var11 > var7 && var10 > var7) {
                    var11 -= var8-- + var8
                    var10 -= var8 + var8
                }
                var12 = x - var8
                if (var12 < minX) {
                    var12 = minX
                }
                var13 = x + var8
                if (var13 > maxX - 1) {
                    var13 = maxX - 1
                }
                var14 = var12 + var6 * width
                var15 = var12
                while (var15 <= var13) {
                    pixels[var14++] = rgb
                    ++var15
                }
                ++var6
                var11 += var9 + var9
                var10 += var9++ + var9
            }
        }
    }

    fun drawCircleAlpha(var0: Int, var1: Int, var2: Int, var3: Int, var4: Int) {
        var var1 = var1
        var var2 = var2
        if (var4 != 0) {
            if (var4 == 256) {
                drawCircle(var0, var1, var2, var3)
            } else {
                if (var2 < 0) {
                    var2 = -var2
                }
                val var5 = 256 - var4
                val var6 = (var3 shr 16 and 255) * var4
                val var7 = (var3 shr 8 and 255) * var4
                val var8 = var4 * (var3 and 255)
                var var12 = var1 - var2
                if (var12 < minY) {
                    var12 = minY
                }
                var var13 = var2 + var1 + 1
                if (var13 > maxY) {
                    var13 = maxY
                }
                var var14 = var12
                val var15 = var2 * var2
                var var16 = 0
                var var17 = var1 - var12
                var var18 = var17 * var17
                var var19 = var18 - var17
                if (var1 > var13) {
                    var1 = var13
                }
                var var9: Int
                var var10: Int
                var var11: Int
                var var20: Int
                var var21: Int
                var var22: Int
                var var23: Int
                var var24: Int
                while (var14 < var1) {
                    while (var19 <= var15 || var18 <= var15) {
                        var18 += var16 + var16
                        var19 += var16++ + var16
                    }
                    var20 = var0 - var16 + 1
                    if (var20 < minX) {
                        var20 = minX
                    }
                    var21 = var0 + var16
                    if (var21 > maxX) {
                        var21 = maxX
                    }
                    var22 = var20 + var14 * width
                    var23 = var20
                    while (var23 < var21) {
                        var9 = var5 * (pixels[var22] shr 16 and 255)
                        var10 = (pixels[var22] shr 8 and 255) * var5
                        var11 = var5 * (pixels[var22] and 255)
                        var24 = (var8 + var11 shr 8) + (var6 + var9 shr 8 shl 16) + (var7 + var10 shr 8 shl 8)
                        pixels[var22++] = var24
                        ++var23
                    }
                    ++var14
                    var18 -= var17-- + var17
                    var19 -= var17 + var17
                }
                var16 = var2
                var17 = -var17
                var19 = var15 + var17 * var17
                var18 = var19 - var2
                var19 -= var17
                while (var14 < var13) {
                    while (var19 > var15 && var18 > var15) {
                        var19 -= var16-- + var16
                        var18 -= var16 + var16
                    }
                    var20 = var0 - var16
                    if (var20 < minX) {
                        var20 = minX
                    }
                    var21 = var0 + var16
                    if (var21 > maxX - 1) {
                        var21 = maxX - 1
                    }
                    var22 = var20 + var14 * width
                    var23 = var20
                    while (var23 <= var21) {
                        var9 = var5 * (pixels[var22] shr 16 and 255)
                        var10 = (pixels[var22] shr 8 and 255) * var5
                        var11 = var5 * (pixels[var22] and 255)
                        var24 = (var8 + var11 shr 8) + (var6 + var9 shr 8 shl 16) + (var7 + var10 shr 8 shl 8)
                        pixels[var22++] = var24
                        ++var23
                    }
                    ++var14
                    var19 += var17 + var17
                    var18 += var17++ + var17
                }
            }
        }
    }

    fun fillRectangleAlpha(var0: Int, var1: Int, var2: Int, var3: Int, var4: Int, var5: Int) {
        var var0 = var0
        var var1 = var1
        var var2 = var2
        var var3 = var3
        var var4 = var4
        if (var0 < minX) {
            var2 -= minX - var0
            var0 = minX
        }
        if (var1 < minY) {
            var3 -= minY - var1
            var1 = minY
        }
        if (var0 + var2 > maxX) {
            var2 = maxX - var0
        }
        if (var3 + var1 > maxY) {
            var3 = maxY - var1
        }
        var4 = (var5 * (var4 and 16711935) shr 8 and 16711935) + (var5 * (var4 and 65280) shr 8 and 65280)
        val var6 = 256 - var5
        val var7: Int = width - var2
        var var8: Int = var0 + width * var1
        for (var9 in 0 until var3) {
            for (var10 in -var2..-1) {
                var var11: Int = pixels[var8]
                var11 = ((var11 and 16711935) * var6 shr 8 and 16711935) + (var6 * (var11 and 65280) shr 8 and 65280)
                pixels[var8++] = var11 + var4
            }
            var8 += var7
        }
    }

    fun fillRectangle(var0: Int, var1: Int, var2: Int, var3: Int, var4: Int) {
        var var0 = var0
        var var1 = var1
        var var2 = var2
        var var3 = var3
        if (var0 < minX) {
            var2 -= minX - var0
            var0 = minX
        }
        if (var1 < minY) {
            var3 -= minY - var1
            var1 = minY
        }
        if (var0 + var2 > maxX) {
            var2 = maxX - var0
        }
        if (var3 + var1 > maxY) {
            var3 = maxY - var1
        }
        val var5: Int = width - var2
        var var6: Int = var0 + width * var1
        for (var7 in -var3..-1) {
            for (var8 in -var2..-1) {
                pixels[var6++] = var4
            }
            var6 += var5
        }
    }

    fun fillRectangleGradient(var0: Int, var1: Int, var2: Int, var3: Int, var4: Int, var5: Int) {
        var var0 = var0
        var var1 = var1
        var var2 = var2
        var var3 = var3
        if (var2 > 0 && var3 > 0) {
            var var6 = 0
            val var7 = 65536 / var3
            if (var0 < minX) {
                var2 -= minX - var0
                var0 = minX
            }
            if (var1 < minY) {
                var6 += (minY - var1) * var7
                var3 -= minY - var1
                var1 = minY
            }
            if (var0 + var2 > maxX) {
                var2 = maxX - var0
            }
            if (var3 + var1 > maxY) {
                var3 = maxY - var1
            }
            val var8: Int = width - var2
            var var9: Int = var0 + width * var1
            for (var10 in -var3..-1) {
                val var11 = 65536 - var6 shr 8
                val var12 = var6 shr 8
                val var13 = (var12 * (var5 and 16711935) + var11 * (var4 and 16711935) and -16711936) +
                    (var12 * (var5 and 65280) + var11 * (var4 and 65280) and 16711680) ushr 8
                for (var14 in -var2..-1) {
                    pixels[var9++] = var13
                }
                var9 += var8
                var6 += var7
            }
        }
    }

    fun fillRectangleGradientAlpha(var0: Int, var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int, var7: Int) {
        var var0 = var0
        var var1 = var1
        var var2 = var2
        var var3 = var3
        if (var2 > 0 && var3 > 0) {
            var var8 = 0
            val var9 = 65536 / var3
            if (var0 < minX) {
                var2 -= minX - var0
                var0 = minX
            }
            if (var1 < minY) {
                var8 += (minY - var1) * var9
                var3 -= minY - var1
                var1 = minY
            }
            if (var0 + var2 > maxX) {
                var2 = maxX - var0
            }
            if (var3 + var1 > maxY) {
                var3 = maxY - var1
            }
            val var10: Int = width - var2
            var var11: Int = var0 + width * var1
            for (var12 in -var3..-1) {
                val var13 = 65536 - var8 shr 8
                val var14 = var8 shr 8
                val var15 = var13 * var6 + var14 * var7 and 65280 ushr 8
                if (var15 == 0) {
                    var11 += width
                    var8 += var9
                } else {
                    val var16 = (var14 * (var5 and 16711935) + var13 * (var4 and 16711935) and -16711936) +
                        (var14 * (var5 and 65280) + var13 * (var4 and 65280) and 16711680) ushr 8
                    val var17 = 255 - var15
                    val var18 = ((var16 and 16711935) * var15 shr 8 and 16711935) + (var15 * (var16 and 65280) shr 8 and 65280)
                    for (var19 in -var2..-1) {
                        var var20: Int = pixels[var11]
                        if (var20 == 0) {
                            pixels[var11++] = var18
                        } else {
                            var20 = ((var20 and 16711935) * var17 shr 8 and 16711935) + (var17 * (var20 and 65280) shr 8 and 65280)
                            pixels[var11++] = var18 + var20
                        }
                    }
                    var11 += var10
                    var8 += var9
                }
            }
        }
    }

    fun drawGradientPixels(var0: Int, var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, var6: ByteArray, var7: Int) {
        var var2 = var2
        var var3 = var3
        if (var0 + var2 >= 0 && var3 + var1 >= 0) {
            if (var0 < width && var1 < height) {
                var var8 = 0
                var var9 = 0
                if (var0 < 0) {
                    var8 -= var0
                    var2 += var0
                }
                if (var1 < 0) {
                    var9 -= var1
                    var3 += var1
                }
                if (var0 + var2 > width) {
                    var2 = width - var0
                }
                if (var3 + var1 > height) {
                    var3 = height - var1
                }
                val var10 = var6.size / var7
                val var11: Int = width - var2
                val var12 = var4 ushr 24
                val var13 = var5 ushr 24
                var var14: Int
                var var15: Int
                var var16: Int
                var var17: Int
                var var18: Int
                if (var12 == 255 && var13 == 255) {
                    var14 = var0 + var8 + (var9 + var1) * width
                    var15 = var9 + var1
                    while (var15 < var3 + var9 + var1) {
                        var16 = var0 + var8
                        while (var16 < var0 + var8 + var2) {
                            var17 = (var15 - var1) % var10
                            var18 = (var16 - var0) % var7
                            if (var6[var18 + var17 * var7].toInt() != 0) {
                                pixels[var14++] = var5
                            } else {
                                pixels[var14++] = var4
                            }
                            ++var16
                        }
                        var14 += var11
                        ++var15
                    }
                } else {
                    var14 = var0 + var8 + (var9 + var1) * width
                    var15 = var9 + var1
                    while (var15 < var3 + var9 + var1) {
                        var16 = var0 + var8
                        while (var16 < var0 + var8 + var2) {
                            var17 = (var15 - var1) % var10
                            var18 = (var16 - var0) % var7
                            var var19 = var4
                            if (var6[var18 + var17 * var7].toInt() != 0) {
                                var19 = var5
                            }
                            val var20 = var19 ushr 24
                            val var21 = 255 - var20
                            val var22: Int = pixels[var14]
                            val var23 = ((var19 and 16711935) * var20 + (var22 and 16711935) * var21 and -16711936) +
                                (var20 * (var19 and 65280) + var21 * (var22 and 65280) and 16711680) shr 8
                            pixels[var14++] = var23
                            ++var16
                        }
                        var14 += var11
                        ++var15
                    }
                }
            }
        }
    }

    fun drawRectangle(var0: Int, var1: Int, var2: Int, var3: Int, var4: Int) {
        drawHorizontalLine(var0, var1, var2, var4)
        drawHorizontalLine(var0, var3 + var1 - 1, var2, var4)
        drawVerticalLine(var0, var1, var3, var4)
        drawVerticalLine(var0 + var2 - 1, var1, var3, var4)
    }

    fun drawRectangleAlpha(var0: Int, var1: Int, var2: Int, var3: Int, var4: Int, var5: Int) {
        drawHorizontalLineAlpha(var0, var1, var2, var4, var5)
        drawHorizontalLineAlpha(var0, var3 + var1 - 1, var2, var4, var5)
        if (var3 >= 3) {
            drawVerticalLineAlpha(var0, var1 + 1, var3 - 2, var4, var5)
            drawVerticalLineAlpha(var0 + var2 - 1, var1 + 1, var3 - 2, var4, var5)
        }
    }

    fun drawHorizontalLine(var0: Int, var1: Int, var2: Int, var3: Int) {
        var var0 = var0
        var var2 = var2
        if (var1 in minY until maxY) {
            if (var0 < minX) {
                var2 -= minX - var0
                var0 = minX
            }
            if (var0 + var2 > maxX) {
                var2 = maxX - var0
            }
            val var4: Int = var0 + width * var1
            for (var5 in 0 until var2) {
                pixels[var4 + var5] = var3
            }
        }
    }

    fun drawHorizontalLineAlpha(var0: Int, var1: Int, var2: Int, var3: Int, var4: Int) {
        var var0 = var0
        var var2 = var2
        if (var1 in minY until maxY) {
            if (var0 < minX) {
                var2 -= minX - var0
                var0 = minX
            }
            if (var0 + var2 > maxX) {
                var2 = maxX - var0
            }
            val var5 = 256 - var4
            val var6 = (var3 shr 16 and 255) * var4
            val var7 = (var3 shr 8 and 255) * var4
            val var8 = var4 * (var3 and 255)
            var var12: Int = var0 + width * var1
            for (var13 in 0 until var2) {
                val var9: Int = var5 * (pixels[var12] shr 16 and 255)
                val var10: Int = (pixels[var12] shr 8 and 255) * var5
                val var11: Int = var5 * (pixels[var12] and 255)
                val var14 = (var8 + var11 shr 8) + (var6 + var9 shr 8 shl 16) + (var7 + var10 shr 8 shl 8)
                pixels[var12++] = var14
            }
        }
    }

    fun drawVerticalLine(var0: Int, var1: Int, var2: Int, var3: Int) {
        var var1 = var1
        var var2 = var2
        if (var0 in minX until maxX) {
            if (var1 < minY) {
                var2 -= minY - var1
                var1 = minY
            }
            if (var2 + var1 > maxY) {
                var2 = maxY - var1
            }
            val var4: Int = var0 + width * var1
            for (var5 in 0 until var2) {
                pixels[var4 + var5 * width] = var3
            }
        }
    }

    fun drawVerticalLineAlpha(var0: Int, var1: Int, var2: Int, var3: Int, var4: Int) {
        var var1 = var1
        var var2 = var2
        if (var0 in minX until maxX) {
            if (var1 < minY) {
                var2 -= minY - var1
                var1 = minY
            }
            if (var2 + var1 > maxY) {
                var2 = maxY - var1
            }
            val var5 = 256 - var4
            val var6 = (var3 shr 16 and 255) * var4
            val var7 = (var3 shr 8 and 255) * var4
            val var8 = var4 * (var3 and 255)
            var var12: Int = var0 + width * var1
            for (var13 in 0 until var2) {
                val var9: Int = var5 * (pixels[var12] shr 16 and 255)
                val var10: Int = (pixels[var12] shr 8 and 255) * var5
                val var11: Int = var5 * (pixels[var12] and 255)
                val var14 = (var8 + var11 shr 8) + (var6 + var9 shr 8 shl 16) + (var7 + var10 shr 8 shl 8)
                pixels[var12] = var14
                var12 += width
            }
        }
    }

    fun drawLine(var0: Int, var1: Int, var2: Int, var3: Int, var4: Int) {
        var var0 = var0
        var var1 = var1
        var var2 = var2
        var var3 = var3
        var2 -= var0
        var3 -= var1
        if (var3 == 0) {
            if (var2 >= 0) {
                drawHorizontalLine(var0, var1, var2 + 1, var4)
            } else {
                drawHorizontalLine(var0 + var2, var1, -var2 + 1, var4)
            }
        } else if (var2 == 0) {
            if (var3 >= 0) {
                drawVerticalLine(var0, var1, var3 + 1, var4)
            } else {
                drawVerticalLine(var0, var3 + var1, -var3 + 1, var4)
            }
        } else {
            if (var3 + var2 < 0) {
                var0 += var2
                var2 = -var2
                var1 += var3
                var3 = -var3
            }
            val var5: Int
            var var6: Int
            if (var2 > var3) {
                var1 = var1 shl 16
                var1 += 32768
                var3 = var3 shl 16
                var5 = floor(var3.toDouble() / var2.toDouble() + 0.5).toInt()
                var2 += var0
                if (var0 < minX) {
                    var1 += var5 * (minX - var0)
                    var0 = minX
                }
                if (var2 >= maxX) {
                    var2 = maxX - 1
                }
                while (var0 <= var2) {
                    var6 = var1 shr 16
                    if (var6 in minY until maxY) {
                        pixels[var0 + var6 * width] = var4
                    }
                    var1 += var5
                    ++var0
                }
            } else {
                var0 = var0 shl 16
                var0 += 32768
                var2 = var2 shl 16
                var5 = floor(var2.toDouble() / var3.toDouble() + 0.5).toInt()
                var3 += var1
                if (var1 < minY) {
                    var0 += (minY - var1) * var5
                    var1 = minY
                }
                if (var3 >= maxY) {
                    var3 = maxY - 1
                }
                while (var1 <= var3) {
                    var6 = var0 shr 16
                    if (var6 in minX until maxX) {
                        pixels[var6 + width * var1] = var4
                    }
                    var0 += var5
                    ++var1
                }
            }
        }
    }

    fun fillMaskedRectangle(var0: Int, var1: Int, var2: Int, var3: IntArray, var4: IntArray) {
        var var0 = var0
        var var1 = var1
        var var5: Int = var0 + width * var1
        var1 = 0
        while (var1 < var3.size) {
            var var6 = var5 + var3[var1]
            var0 = -var4[var1]
            while (var0 < 0) {
                pixels[var6++] = var2
                ++var0
            }
            var5 += width
            ++var1
        }
    }

    private companion object {
        private val emptyIntArray = IntArray(0)
    }
}
