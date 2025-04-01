package dev.openrune.definition.game.render.draw

import dev.openrune.definition.game.render.util.JagexColor
import dev.openrune.definition.type.SpriteType
import dev.openrune.definition.type.TextureType
import kotlin.math.cos
import kotlin.math.sin

class Rasterizer3D(
    val textures : Map<Int,TextureType>,
    val sprites : Map<Int,SpriteType>
) : Rasterizer2D() {
    var isRasterClippingEnabled: Boolean = false
    var field1909: Boolean = false
    var isLowMem: Boolean = false
    var isGouraudShadingLowRes: Boolean = true
    var alpha: Int = 0
	var zoom: Int = 512
    var centerX: Int = 0
    var centerY: Int = 0
    var clippingOffsetX: Int = 0
    var clippingHeight: Int = 0
    var clippingMidXNegative: Int = 0
    var clippingMidXPositive: Int = 0
    var clippingMidYNegative: Int = 0
    var clippingMidYPositive: Int = 0
    var clippingOffsetsY: IntArray = IntArray(1024)
	lateinit var colorPalette: IntArray

    fun setRasterClipping() {
        setRasterClipping(drawRegionX, drawingAreaTop, drawingAreaRight, drawingAreaBottom)
    }

    fun setRasterClipping(x1: Int, y1: Int, x2: Int, y2: Int) {
        clippingOffsetX = x2 - x1
        clippingHeight = y2 - y1
        updateClippingBounds()

        if (clippingOffsetsY.size < clippingHeight) {
            val newSize = 2.let { size ->
                generateSequence(size) { it * 2 }.first { it >= clippingHeight }
            }
            clippingOffsetsY = IntArray(newSize)
        }

        var offset = x1 + graphicsPixelsWidth * y1
        repeat(clippingHeight) {
            clippingOffsetsY[it] = offset
            offset += graphicsPixelsWidth
        }
    }


    private fun updateClippingBounds() {
        centerX = clippingOffsetX / 2
        centerY = clippingHeight / 2
        clippingMidXNegative = -centerX
        clippingMidXPositive = clippingOffsetX - centerX
        clippingMidYNegative = -centerY
        clippingMidYPositive = clippingHeight - centerY
    }

    fun setOffset(x: Int, y: Int) {
        val firstOffset = clippingOffsetsY.first()
        val yOffset = firstOffset / graphicsPixelsWidth
        val xOffset = firstOffset - yOffset * graphicsPixelsWidth
        centerX = x - xOffset
        centerY = y - yOffset
        clippingMidXNegative = -centerX
        clippingMidXPositive = clippingOffsetX - centerX
        clippingMidYNegative = -centerY
        clippingMidYPositive = clippingHeight - centerY
    }

    fun setBrightness(brightness: Double) {
        colorPalette = JagexColor.createPalette(brightness)
    }

    fun rasterGouraud(
        vertex0X: Int,
        vertex0Y: Int,
        vertex1X: Int,
        var3: Int,
        var4: Int,
        var5: Int,
        r: Int,
        g: Int,
        b: Int
    ) {
        var var0 = vertex0X
        var var1 = vertex0Y
        var var2 = vertex1X
        var var3 = var3
        var var4 = var4
        var var5 = var5
        var r = r
        var g = g
        var b = b
        val var9 = var4 - var3
        val var10 = var1 - var0
        val var11 = var5 - var3
        val var12 = var2 - var0
        val gsr = g - r
        val bsr = b - r
        val var15 = if (var2 != var1) {
            (var5 - var4 shl 14) / (var2 - var1)
        } else {
            0
        }
        val var16 = if (var0 != var1) {
            (var9 shl 14) / var10
        } else {
            0
        }
        val var17 = if (var0 != var2) {
            (var11 shl 14) / var12
        } else {
            0
        }

        val var18 = var9 * var12 - var11 * var10
        if (var18 != 0) {
            val var19 = (gsr * var12 - bsr * var10 shl 8) / var18
            val var20 = (bsr * var9 - gsr * var11 shl 8) / var18
            if (var0 <= var1 && var0 <= var2) {
                if (var0 < clippingHeight) {
                    if (var1 > clippingHeight) {
                        var1 = clippingHeight
                    }

                    if (var2 > clippingHeight) {
                        var2 = clippingHeight
                    }

                    r = var19 + ((r shl 8) - var3 * var19)
                    if (var1 < var2) {
                        var3 = var3 shl 14
                        var5 = var3
                        if (var0 < 0) {
                            var5 -= var0 * var17
                            var3 -= var0 * var16
                            r -= var0 * var20
                            var0 = 0
                        }

                        var4 = var4 shl 14
                        if (var1 < 0) {
                            var4 -= var15 * var1
                            var1 = 0
                        }

                        if (var0 != var1 && var17 < var16 || var0 == var1 && var17 > var15) {
                            var2 -= var1
                            var1 -= var0
                            var0 = clippingOffsetsY[var0]

                            while (true) {
                                --var1
                                if (var1 < 0) {
                                    while (true) {
                                        --var2
                                        if (var2 < 0) {
                                            return
                                        }

                                        drawTriangleScanline(graphicsPixels, var0, 0, 0, var5 shr 14, var4 shr 14, r, var19)
                                        var5 += var17
                                        var4 += var15
                                        r += var20
                                        var0 += graphicsPixelsWidth
                                    }
                                }

                                drawTriangleScanline(graphicsPixels, var0, 0, 0, var5 shr 14, var3 shr 14, r, var19)
                                var5 += var17
                                var3 += var16
                                r += var20
                                var0 += graphicsPixelsWidth
                            }
                        } else {
                            var2 -= var1
                            var1 -= var0
                            var0 = clippingOffsetsY[var0]

                            while (true) {
                                --var1
                                if (var1 < 0) {
                                    while (true) {
                                        --var2
                                        if (var2 < 0) {
                                            return
                                        }

                                        drawTriangleScanline(graphicsPixels, var0, 0, 0, var4 shr 14, var5 shr 14, r, var19)
                                        var5 += var17
                                        var4 += var15
                                        r += var20
                                        var0 += graphicsPixelsWidth
                                    }
                                }

                                drawTriangleScanline(graphicsPixels, var0, 0, 0, var3 shr 14, var5 shr 14, r, var19)
                                var5 += var17
                                var3 += var16
                                r += var20
                                var0 += graphicsPixelsWidth
                            }
                        }
                    } else {
                        var3 = var3 shl 14
                        var4 = var3
                        if (var0 < 0) {
                            var4 -= var0 * var17
                            var3 -= var0 * var16
                            r -= var0 * var20
                            var0 = 0
                        }

                        var5 = var5 shl 14
                        if (var2 < 0) {
                            var5 -= var15 * var2
                            var2 = 0
                        }

                        if (var0 != var2 && var17 < var16 || var0 == var2 && var15 > var16) {
                            var1 -= var2
                            var2 -= var0
                            var0 = clippingOffsetsY[var0]

                            while (true) {
                                --var2
                                if (var2 < 0) {
                                    while (true) {
                                        --var1
                                        if (var1 < 0) {
                                            return
                                        }

                                        drawTriangleScanline(graphicsPixels, var0, 0, 0, var5 shr 14, var3 shr 14, r, var19)
                                        var5 += var15
                                        var3 += var16
                                        r += var20
                                        var0 += graphicsPixelsWidth
                                    }
                                }

                                drawTriangleScanline(graphicsPixels, var0, 0, 0, var4 shr 14, var3 shr 14, r, var19)
                                var4 += var17
                                var3 += var16
                                r += var20
                                var0 += graphicsPixelsWidth
                            }
                        } else {
                            var1 -= var2
                            var2 -= var0
                            var0 = clippingOffsetsY[var0]

                            while (true) {
                                --var2
                                if (var2 < 0) {
                                    while (true) {
                                        --var1
                                        if (var1 < 0) {
                                            return
                                        }

                                        drawTriangleScanline(graphicsPixels, var0, 0, 0, var3 shr 14, var5 shr 14, r, var19)
                                        var5 += var15
                                        var3 += var16
                                        r += var20
                                        var0 += graphicsPixelsWidth
                                    }
                                }

                                drawTriangleScanline(graphicsPixels, var0, 0, 0, var3 shr 14, var4 shr 14, r, var19)
                                var4 += var17
                                var3 += var16
                                r += var20
                                var0 += graphicsPixelsWidth
                            }
                        }
                    }
                }
            } else if (var1 <= var2) {
                if (var1 < clippingHeight) {
                    if (var2 > clippingHeight) {
                        var2 = clippingHeight
                    }

                    if (var0 > clippingHeight) {
                        var0 = clippingHeight
                    }

                    g = var19 + ((g shl 8) - var19 * var4)
                    if (var2 < var0) {
                        var4 = var4 shl 14
                        var3 = var4
                        if (var1 < 0) {
                            var3 -= var16 * var1
                            var4 -= var15 * var1
                            g -= var20 * var1
                            var1 = 0
                        }

                        var5 = var5 shl 14
                        if (var2 < 0) {
                            var5 -= var17 * var2
                            var2 = 0
                        }

                        if (var2 != var1 && var16 < var15 || var2 == var1 && var16 > var17) {
                            var0 -= var2
                            var2 -= var1
                            var1 = clippingOffsetsY[var1]

                            while (true) {
                                --var2
                                if (var2 < 0) {
                                    while (true) {
                                        --var0
                                        if (var0 < 0) {
                                            return
                                        }

                                        drawTriangleScanline(graphicsPixels, var1, 0, 0, var3 shr 14, var5 shr 14, g, var19)
                                        var3 += var16
                                        var5 += var17
                                        g += var20
                                        var1 += graphicsPixelsWidth
                                    }
                                }

                                drawTriangleScanline(graphicsPixels, var1, 0, 0, var3 shr 14, var4 shr 14, g, var19)
                                var3 += var16
                                var4 += var15
                                g += var20
                                var1 += graphicsPixelsWidth
                            }
                        } else {
                            var0 -= var2
                            var2 -= var1
                            var1 = clippingOffsetsY[var1]

                            while (true) {
                                --var2
                                if (var2 < 0) {
                                    while (true) {
                                        --var0
                                        if (var0 < 0) {
                                            return
                                        }

                                        drawTriangleScanline(graphicsPixels, var1, 0, 0, var5 shr 14, var3 shr 14, g, var19)
                                        var3 += var16
                                        var5 += var17
                                        g += var20
                                        var1 += graphicsPixelsWidth
                                    }
                                }

                                drawTriangleScanline(graphicsPixels, var1, 0, 0, var4 shr 14, var3 shr 14, g, var19)
                                var3 += var16
                                var4 += var15
                                g += var20
                                var1 += graphicsPixelsWidth
                            }
                        }
                    } else {
                        var4 = var4 shl 14
                        var5 = var4
                        if (var1 < 0) {
                            var5 -= var16 * var1
                            var4 -= var15 * var1
                            g -= var20 * var1
                            var1 = 0
                        }

                        var3 = var3 shl 14
                        if (var0 < 0) {
                            var3 -= var0 * var17
                            var0 = 0
                        }

                        if (var16 < var15) {
                            var2 -= var0
                            var0 -= var1
                            var1 = clippingOffsetsY[var1]

                            while (true) {
                                --var0
                                if (var0 < 0) {
                                    while (true) {
                                        --var2
                                        if (var2 < 0) {
                                            return
                                        }

                                        drawTriangleScanline(graphicsPixels, var1, 0, 0, var3 shr 14, var4 shr 14, g, var19)
                                        var3 += var17
                                        var4 += var15
                                        g += var20
                                        var1 += graphicsPixelsWidth
                                    }
                                }

                                drawTriangleScanline(graphicsPixels, var1, 0, 0, var5 shr 14, var4 shr 14, g, var19)
                                var5 += var16
                                var4 += var15
                                g += var20
                                var1 += graphicsPixelsWidth
                            }
                        } else {
                            var2 -= var0
                            var0 -= var1
                            var1 = clippingOffsetsY[var1]

                            while (true) {
                                --var0
                                if (var0 < 0) {
                                    while (true) {
                                        --var2
                                        if (var2 < 0) {
                                            return
                                        }

                                        drawTriangleScanline(graphicsPixels, var1, 0, 0, var4 shr 14, var3 shr 14, g, var19)
                                        var3 += var17
                                        var4 += var15
                                        g += var20
                                        var1 += graphicsPixelsWidth
                                    }
                                }

                                drawTriangleScanline(graphicsPixels, var1, 0, 0, var4 shr 14, var5 shr 14, g, var19)
                                var5 += var16
                                var4 += var15
                                g += var20
                                var1 += graphicsPixelsWidth
                            }
                        }
                    }
                }
            } else if (var2 < clippingHeight) {
                if (var0 > clippingHeight) {
                    var0 = clippingHeight
                }

                if (var1 > clippingHeight) {
                    var1 = clippingHeight
                }

                b = var19 + ((b shl 8) - var5 * var19)
                if (var0 < var1) {
                    var5 = var5 shl 14
                    var4 = var5
                    if (var2 < 0) {
                        var4 -= var15 * var2
                        var5 -= var17 * var2
                        b -= var20 * var2
                        var2 = 0
                    }

                    var3 = var3 shl 14
                    if (var0 < 0) {
                        var3 -= var0 * var16
                        var0 = 0
                    }

                    if (var15 < var17) {
                        var1 -= var0
                        var0 -= var2
                        var2 = clippingOffsetsY[var2]

                        while (true) {
                            --var0
                            if (var0 < 0) {
                                while (true) {
                                    --var1
                                    if (var1 < 0) {
                                        return
                                    }

                                    drawTriangleScanline(graphicsPixels, var2, 0, 0, var4 shr 14, var3 shr 14, b, var19)
                                    var4 += var15
                                    var3 += var16
                                    b += var20
                                    var2 += graphicsPixelsWidth
                                }
                            }

                            drawTriangleScanline(graphicsPixels, var2, 0, 0, var4 shr 14, var5 shr 14, b, var19)
                            var4 += var15
                            var5 += var17
                            b += var20
                            var2 += graphicsPixelsWidth
                        }
                    } else {
                        var1 -= var0
                        var0 -= var2
                        var2 = clippingOffsetsY[var2]

                        while (true) {
                            --var0
                            if (var0 < 0) {
                                while (true) {
                                    --var1
                                    if (var1 < 0) {
                                        return
                                    }

                                    drawTriangleScanline(graphicsPixels, var2, 0, 0, var3 shr 14, var4 shr 14, b, var19)
                                    var4 += var15
                                    var3 += var16
                                    b += var20
                                    var2 += graphicsPixelsWidth
                                }
                            }

                            drawTriangleScanline(graphicsPixels, var2, 0, 0, var5 shr 14, var4 shr 14, b, var19)
                            var4 += var15
                            var5 += var17
                            b += var20
                            var2 += graphicsPixelsWidth
                        }
                    }
                } else {
                    var5 = var5 shl 14
                    var3 = var5
                    if (var2 < 0) {
                        var3 -= var15 * var2
                        var5 -= var17 * var2
                        b -= var20 * var2
                        var2 = 0
                    }

                    var4 = var4 shl 14
                    if (var1 < 0) {
                        var4 -= var16 * var1
                        var1 = 0
                    }

                    if (var15 < var17) {
                        var0 -= var1
                        var1 -= var2
                        var2 = clippingOffsetsY[var2]

                        while (true) {
                            --var1
                            if (var1 < 0) {
                                while (true) {
                                    --var0
                                    if (var0 < 0) {
                                        return
                                    }

                                    drawTriangleScanline(graphicsPixels, var2, 0, 0, var4 shr 14, var5 shr 14, b, var19)
                                    var4 += var16
                                    var5 += var17
                                    b += var20
                                    var2 += graphicsPixelsWidth
                                }
                            }

                            drawTriangleScanline(graphicsPixels, var2, 0, 0, var3 shr 14, var5 shr 14, b, var19)
                            var3 += var15
                            var5 += var17
                            b += var20
                            var2 += graphicsPixelsWidth
                        }
                    } else {
                        var0 -= var1
                        var1 -= var2
                        var2 = clippingOffsetsY[var2]

                        while (true) {
                            --var1
                            if (var1 < 0) {
                                while (true) {
                                    --var0
                                    if (var0 < 0) {
                                        return
                                    }

                                    drawTriangleScanline(graphicsPixels, var2, 0, 0, var5 shr 14, var4 shr 14, b, var19)
                                    var4 += var16
                                    var5 += var17
                                    b += var20
                                    var2 += graphicsPixelsWidth
                                }
                            }

                            drawTriangleScanline(graphicsPixels, var2, 0, 0, var5 shr 14, var3 shr 14, b, var19)
                            var3 += var15
                            var5 += var17
                            b += var20
                            var2 += graphicsPixelsWidth
                        }
                    }
                }
            }
        }
    }


    fun drawTriangleScanline(var0: IntArray, var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int, var7: Int) {
        var var1 = var1
        var var2 = var2
        var var3 = var3
        var var4 = var4
        var var5 = var5
        var var6 = var6
        var var7 = var7
        if (isRasterClippingEnabled) {
            if (var5 > clippingOffsetX) {
                var5 = clippingOffsetX
            }

            if (var4 < 0) {
                var4 = 0
            }
        }

        if (var4 < var5) {
            var1 += var4
            var6 += var4 * var7
            val var8: Int
            val var9: Int
            var var10: Int
            if (isGouraudShadingLowRes) {
                var3 = var5 - var4 shr 2
                var7 = var7 shl 2
                if (alpha == 0) {
                    if (var3 > 0) {
                        do {
                            var2 = colorPalette[var6 shr 8]
                            var6 += var7
                            var0[var1++] = var2
                            var0[var1++] = var2
                            var0[var1++] = var2
                            var0[var1++] = var2
                            --var3
                        } while (var3 > 0)
                    }

                    var3 = var5 - var4 and 3
                    if (var3 > 0) {
                        var2 = colorPalette[var6 shr 8]

                        do {
                            var0[var1++] = var2
                            --var3
                        } while (var3 > 0)
                    }
                } else {
                    var8 = alpha
                    var9 = 256 - alpha
                    if (var3 > 0) {
                        do {
                            var2 = colorPalette[var6 shr 8]
                            var6 += var7
                            var2 =
                                (var9 * (var2 and 65280) shr 8 and 65280) + (var9 * (var2 and 16711935) shr 8 and 16711935)
                            var10 = var0[var1]
                            var0[var1++] =
                                ((var10 and 16711935) * var8 shr 8 and 16711935) + var2 + (var8 * (var10 and 65280) shr 8 and 65280)
                            var10 = var0[var1]
                            var0[var1++] =
                                ((var10 and 16711935) * var8 shr 8 and 16711935) + var2 + (var8 * (var10 and 65280) shr 8 and 65280)
                            var10 = var0[var1]
                            var0[var1++] =
                                ((var10 and 16711935) * var8 shr 8 and 16711935) + var2 + (var8 * (var10 and 65280) shr 8 and 65280)
                            var10 = var0[var1]
                            var0[var1++] =
                                ((var10 and 16711935) * var8 shr 8 and 16711935) + var2 + (var8 * (var10 and 65280) shr 8 and 65280)
                            --var3
                        } while (var3 > 0)
                    }

                    var3 = var5 - var4 and 3
                    if (var3 > 0) {
                        var2 = colorPalette[var6 shr 8]
                        var2 =
                            (var9 * (var2 and 65280) shr 8 and 65280) + (var9 * (var2 and 16711935) shr 8 and 16711935)

                        do {
                            var10 = var0[var1]
                            var0[var1++] =
                                ((var10 and 16711935) * var8 shr 8 and 16711935) + var2 + (var8 * (var10 and 65280) shr 8 and 65280)
                            --var3
                        } while (var3 > 0)
                    }
                }
            } else {
                var3 = var5 - var4
                if (alpha == 0) {
                    do {
                        var0[var1++] = colorPalette[var6 shr 8]
                        var6 += var7
                        --var3
                    } while (var3 > 0)
                } else {
                    var8 = alpha
                    var9 = 256 - alpha

                    do {
                        var2 = colorPalette[var6 shr 8]
                        var6 += var7
                        var2 =
                            (var9 * (var2 and 65280) shr 8 and 65280) + (var9 * (var2 and 16711935) shr 8 and 16711935)
                        var10 = var0[var1]
                        var0[var1++] =
                            ((var10 and 16711935) * var8 shr 8 and 16711935) + var2 + (var8 * (var10 and 65280) shr 8 and 65280)
                        --var3
                    } while (var3 > 0)
                }
            }
        }
    }


    fun rasterFlat(var0: Int, var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int) {
        var var0 = var0
        var var1 = var1
        var var2 = var2
        var var3 = var3
        var var4 = var4
        var var5 = var5
        var var7 = 0
        if (var0 != var1) {
            var7 = (var4 - var3 shl 14) / (var1 - var0)
        }

        var var8 = 0
        if (var2 != var1) {
            var8 = (var5 - var4 shl 14) / (var2 - var1)
        }

        var var9 = 0
        if (var0 != var2) {
            var9 = (var3 - var5 shl 14) / (var0 - var2)
        }

        if (var0 <= var1 && var0 <= var2) {
            if (var0 < clippingHeight) {
                if (var1 > clippingHeight) {
                    var1 = clippingHeight
                }

                if (var2 > clippingHeight) {
                    var2 = clippingHeight
                }

                if (var1 < var2) {
                    var3 = var3 shl 14
                    var5 = var3
                    if (var0 < 0) {
                        var5 -= var0 * var9
                        var3 -= var0 * var7
                        var0 = 0
                    }

                    var4 = var4 shl 14
                    if (var1 < 0) {
                        var4 -= var8 * var1
                        var1 = 0
                    }

                    if ((var0 == var1 || var9 >= var7) && (var0 != var1 || var9 <= var8)) {
                        var2 -= var1
                        var1 -= var0
                        var0 = clippingOffsetsY[var0]

                        while (true) {
                            --var1
                            if (var1 < 0) {
                                while (true) {
                                    --var2
                                    if (var2 < 0) {
                                        return
                                    }

                                    method2842(graphicsPixels, var0, var6, 0, var4 shr 14, var5 shr 14)
                                    var5 += var9
                                    var4 += var8
                                    var0 += graphicsPixelsWidth
                                }
                            }

                            method2842(graphicsPixels, var0, var6, 0, var3 shr 14, var5 shr 14)
                            var5 += var9
                            var3 += var7
                            var0 += graphicsPixelsWidth
                        }
                    } else {
                        var2 -= var1
                        var1 -= var0
                        var0 = clippingOffsetsY[var0]

                        while (true) {
                            --var1
                            if (var1 < 0) {
                                while (true) {
                                    --var2
                                    if (var2 < 0) {
                                        return
                                    }

                                    method2842(graphicsPixels, var0, var6, 0, var5 shr 14, var4 shr 14)
                                    var5 += var9
                                    var4 += var8
                                    var0 += graphicsPixelsWidth
                                }
                            }

                            method2842(graphicsPixels, var0, var6, 0, var5 shr 14, var3 shr 14)
                            var5 += var9
                            var3 += var7
                            var0 += graphicsPixelsWidth
                        }
                    }
                } else {
                    var3 = var3 shl 14
                    var4 = var3
                    if (var0 < 0) {
                        var4 -= var0 * var9
                        var3 -= var0 * var7
                        var0 = 0
                    }

                    var5 = var5 shl 14
                    if (var2 < 0) {
                        var5 -= var8 * var2
                        var2 = 0
                    }

                    if (var0 != var2 && var9 < var7 || var0 == var2 && var8 > var7) {
                        var1 -= var2
                        var2 -= var0
                        var0 = clippingOffsetsY[var0]

                        while (true) {
                            --var2
                            if (var2 < 0) {
                                while (true) {
                                    --var1
                                    if (var1 < 0) {
                                        return
                                    }

                                    method2842(graphicsPixels, var0, var6, 0, var5 shr 14, var3 shr 14)
                                    var5 += var8
                                    var3 += var7
                                    var0 += graphicsPixelsWidth
                                }
                            }

                            method2842(graphicsPixels, var0, var6, 0, var4 shr 14, var3 shr 14)
                            var4 += var9
                            var3 += var7
                            var0 += graphicsPixelsWidth
                        }
                    } else {
                        var1 -= var2
                        var2 -= var0
                        var0 = clippingOffsetsY[var0]

                        while (true) {
                            --var2
                            if (var2 < 0) {
                                while (true) {
                                    --var1
                                    if (var1 < 0) {
                                        return
                                    }

                                    method2842(graphicsPixels, var0, var6, 0, var3 shr 14, var5 shr 14)
                                    var5 += var8
                                    var3 += var7
                                    var0 += graphicsPixelsWidth
                                }
                            }

                            method2842(graphicsPixels, var0, var6, 0, var3 shr 14, var4 shr 14)
                            var4 += var9
                            var3 += var7
                            var0 += graphicsPixelsWidth
                        }
                    }
                }
            }
        } else if (var1 <= var2) {
            if (var1 < clippingHeight) {
                if (var2 > clippingHeight) {
                    var2 = clippingHeight
                }

                if (var0 > clippingHeight) {
                    var0 = clippingHeight
                }

                if (var2 < var0) {
                    var4 = var4 shl 14
                    var3 = var4
                    if (var1 < 0) {
                        var3 -= var7 * var1
                        var4 -= var8 * var1
                        var1 = 0
                    }

                    var5 = var5 shl 14
                    if (var2 < 0) {
                        var5 -= var9 * var2
                        var2 = 0
                    }

                    if ((var2 == var1 || var7 >= var8) && (var2 != var1 || var7 <= var9)) {
                        var0 -= var2
                        var2 -= var1
                        var1 = clippingOffsetsY[var1]

                        while (true) {
                            --var2
                            if (var2 < 0) {
                                while (true) {
                                    --var0
                                    if (var0 < 0) {
                                        return
                                    }

                                    method2842(graphicsPixels, var1, var6, 0, var5 shr 14, var3 shr 14)
                                    var3 += var7
                                    var5 += var9
                                    var1 += graphicsPixelsWidth
                                }
                            }

                            method2842(graphicsPixels, var1, var6, 0, var4 shr 14, var3 shr 14)
                            var3 += var7
                            var4 += var8
                            var1 += graphicsPixelsWidth
                        }
                    } else {
                        var0 -= var2
                        var2 -= var1
                        var1 = clippingOffsetsY[var1]

                        while (true) {
                            --var2
                            if (var2 < 0) {
                                while (true) {
                                    --var0
                                    if (var0 < 0) {
                                        return
                                    }

                                    method2842(graphicsPixels, var1, var6, 0, var3 shr 14, var5 shr 14)
                                    var3 += var7
                                    var5 += var9
                                    var1 += graphicsPixelsWidth
                                }
                            }

                            method2842(graphicsPixels, var1, var6, 0, var3 shr 14, var4 shr 14)
                            var3 += var7
                            var4 += var8
                            var1 += graphicsPixelsWidth
                        }
                    }
                } else {
                    var4 = var4 shl 14
                    var5 = var4
                    if (var1 < 0) {
                        var5 -= var7 * var1
                        var4 -= var8 * var1
                        var1 = 0
                    }

                    var3 = var3 shl 14
                    if (var0 < 0) {
                        var3 -= var0 * var9
                        var0 = 0
                    }

                    if (var7 < var8) {
                        var2 -= var0
                        var0 -= var1
                        var1 = clippingOffsetsY[var1]

                        while (true) {
                            --var0
                            if (var0 < 0) {
                                while (true) {
                                    --var2
                                    if (var2 < 0) {
                                        return
                                    }

                                    method2842(graphicsPixels, var1, var6, 0, var3 shr 14, var4 shr 14)
                                    var3 += var9
                                    var4 += var8
                                    var1 += graphicsPixelsWidth
                                }
                            }

                            method2842(graphicsPixels, var1, var6, 0, var5 shr 14, var4 shr 14)
                            var5 += var7
                            var4 += var8
                            var1 += graphicsPixelsWidth
                        }
                    } else {
                        var2 -= var0
                        var0 -= var1
                        var1 = clippingOffsetsY[var1]

                        while (true) {
                            --var0
                            if (var0 < 0) {
                                while (true) {
                                    --var2
                                    if (var2 < 0) {
                                        return
                                    }

                                    method2842(graphicsPixels, var1, var6, 0, var4 shr 14, var3 shr 14)
                                    var3 += var9
                                    var4 += var8
                                    var1 += graphicsPixelsWidth
                                }
                            }

                            method2842(graphicsPixels, var1, var6, 0, var4 shr 14, var5 shr 14)
                            var5 += var7
                            var4 += var8
                            var1 += graphicsPixelsWidth
                        }
                    }
                }
            }
        } else if (var2 < clippingHeight) {
            if (var0 > clippingHeight) {
                var0 = clippingHeight
            }

            if (var1 > clippingHeight) {
                var1 = clippingHeight
            }

            if (var0 < var1) {
                var5 = var5 shl 14
                var4 = var5
                if (var2 < 0) {
                    var4 -= var8 * var2
                    var5 -= var9 * var2
                    var2 = 0
                }

                var3 = var3 shl 14
                if (var0 < 0) {
                    var3 -= var0 * var7
                    var0 = 0
                }

                if (var8 < var9) {
                    var1 -= var0
                    var0 -= var2
                    var2 = clippingOffsetsY[var2]

                    while (true) {
                        --var0
                        if (var0 < 0) {
                            while (true) {
                                --var1
                                if (var1 < 0) {
                                    return
                                }

                                method2842(graphicsPixels, var2, var6, 0, var4 shr 14, var3 shr 14)
                                var4 += var8
                                var3 += var7
                                var2 += graphicsPixelsWidth
                            }
                        }

                        method2842(graphicsPixels, var2, var6, 0, var4 shr 14, var5 shr 14)
                        var4 += var8
                        var5 += var9
                        var2 += graphicsPixelsWidth
                    }
                } else {
                    var1 -= var0
                    var0 -= var2
                    var2 = clippingOffsetsY[var2]

                    while (true) {
                        --var0
                        if (var0 < 0) {
                            while (true) {
                                --var1
                                if (var1 < 0) {
                                    return
                                }

                                method2842(graphicsPixels, var2, var6, 0, var3 shr 14, var4 shr 14)
                                var4 += var8
                                var3 += var7
                                var2 += graphicsPixelsWidth
                            }
                        }

                        method2842(graphicsPixels, var2, var6, 0, var5 shr 14, var4 shr 14)
                        var4 += var8
                        var5 += var9
                        var2 += graphicsPixelsWidth
                    }
                }
            } else {
                var5 = var5 shl 14
                var3 = var5
                if (var2 < 0) {
                    var3 -= var8 * var2
                    var5 -= var9 * var2
                    var2 = 0
                }

                var4 = var4 shl 14
                if (var1 < 0) {
                    var4 -= var7 * var1
                    var1 = 0
                }

                if (var8 < var9) {
                    var0 -= var1
                    var1 -= var2
                    var2 = clippingOffsetsY[var2]

                    while (true) {
                        --var1
                        if (var1 < 0) {
                            while (true) {
                                --var0
                                if (var0 < 0) {
                                    return
                                }

                                method2842(graphicsPixels, var2, var6, 0, var4 shr 14, var5 shr 14)
                                var4 += var7
                                var5 += var9
                                var2 += graphicsPixelsWidth
                            }
                        }

                        method2842(graphicsPixels, var2, var6, 0, var3 shr 14, var5 shr 14)
                        var3 += var8
                        var5 += var9
                        var2 += graphicsPixelsWidth
                    }
                } else {
                    var0 -= var1
                    var1 -= var2
                    var2 = clippingOffsetsY[var2]

                    while (true) {
                        --var1
                        if (var1 < 0) {
                            while (true) {
                                --var0
                                if (var0 < 0) {
                                    return
                                }

                                method2842(graphicsPixels, var2, var6, 0, var5 shr 14, var4 shr 14)
                                var4 += var7
                                var5 += var9
                                var2 += graphicsPixelsWidth
                            }
                        }

                        method2842(graphicsPixels, var2, var6, 0, var5 shr 14, var3 shr 14)
                        var3 += var8
                        var5 += var9
                        var2 += graphicsPixelsWidth
                    }
                }
            }
        }
    }


    fun method2842(var0: IntArray, var1: Int, var2: Int, var3: Int, var4: Int, var5: Int) {
        var var1 = var1
        var var2 = var2
        var var3 = var3
        var var4 = var4
        var var5 = var5
        if (isRasterClippingEnabled) {
            if (var5 > clippingOffsetX) {
                var5 = clippingOffsetX
            }

            if (var4 < 0) {
                var4 = 0
            }
        }

        if (var4 < var5) {
            var1 += var4
            var3 = var5 - var4 shr 2
            if (alpha != 0) {
                if (alpha == 254) {
                    while (true) {
                        --var3
                        if (var3 < 0) {
                            var3 = var5 - var4 and 3

                            while (true) {
                                --var3
                                if (var3 < 0) {
                                    return
                                }

                                var0[var1++] = var0[var1]
                            }
                        }

                        var0[var1++] = var0[var1]
                        var0[var1++] = var0[var1]
                        var0[var1++] = var0[var1]
                        var0[var1++] = var0[var1]
                    }
                } else {
                    val var6 = alpha
                    val var7 = 256 - alpha
                    var2 = (var7 * (var2 and 65280) shr 8 and 65280) + (var7 * (var2 and 16711935) shr 8 and 16711935)

                    while (true) {
                        --var3
                        var var8: Int
                        if (var3 < 0) {
                            var3 = var5 - var4 and 3

                            while (true) {
                                --var3
                                if (var3 < 0) {
                                    return
                                }

                                var8 = var0[var1]
                                var0[var1++] =
                                    ((var8 and 16711935) * var6 shr 8 and 16711935) + var2 + (var6 * (var8 and 65280) shr 8 and 65280)
                            }
                        }

                        var8 = var0[var1]
                        var0[var1++] =
                            ((var8 and 16711935) * var6 shr 8 and 16711935) + var2 + (var6 * (var8 and 65280) shr 8 and 65280)
                        var8 = var0[var1]
                        var0[var1++] =
                            ((var8 and 16711935) * var6 shr 8 and 16711935) + var2 + (var6 * (var8 and 65280) shr 8 and 65280)
                        var8 = var0[var1]
                        var0[var1++] =
                            ((var8 and 16711935) * var6 shr 8 and 16711935) + var2 + (var6 * (var8 and 65280) shr 8 and 65280)
                        var8 = var0[var1]
                        var0[var1++] =
                            ((var8 and 16711935) * var6 shr 8 and 16711935) + var2 + (var6 * (var8 and 65280) shr 8 and 65280)
                    }
                }
            } else {
                while (true) {
                    --var3
                    if (var3 < 0) {
                        var3 = var5 - var4 and 3

                        while (true) {
                            --var3
                            if (var3 < 0) {
                                return
                            }

                            var0[var1++] = var2
                        }
                    }

                    var0[var1++] = var2
                    var0[var1++] = var2
                    var0[var1++] = var2
                    var0[var1++] = var2
                }
            }
        }
    }


    fun rasterTextureAffine(
        var0: Int,
        var1: Int,
        var2: Int,
        var3: Int,
        var4: Int,
        var5: Int,
        var6: Int,
        var7: Int,
        var8: Int,
        var9: Int,
        var10: Int,
        var11: Int,
        var12: Int,
        var13: Int,
        var14: Int,
        var15: Int,
        var16: Int,
        var17: Int,
        var18: Int
    ) {
        var var0 = var0
        var var1 = var1
        var var2 = var2
        var var3 = var3
        var var4 = var4
        var var5 = var5
        var var6 = var6
        var var7 = var7
        var var8 = var8
        var var10 = var10
        var var11 = var11
        var var13 = var13
        var var14 = var14
        var var16 = var16
        var var17 = var17

        val textureType = textures[var18]
        val textureRenderPixels = textures[var18]?.load(sprites)

        val var20: Int
        if (textureRenderPixels == null) {
            var20 = textureType?.averageRgb?: 0
            rasterGouraud(
                var0,
                var1,
                var2,
                var3,
                var4,
                var5,
                method2794(var20, var6),
                method2794(var20, var7),
                method2794(var20, var8)
            )
        } else {
            isLowMem = textureType!!.isLowMem()
            field1909 = textureType.isTransparent
            var20 = var4 - var3
            val var21 = var1 - var0
            val var22 = var5 - var3
            val var23 = var2 - var0
            val var24 = var7 - var6
            val var25 = var8 - var6
            var var26 = 0
            if (var0 != var1) {
                var26 = (var4 - var3 shl 14) / (var1 - var0)
            }

            var var27 = 0
            if (var2 != var1) {
                var27 = (var5 - var4 shl 14) / (var2 - var1)
            }

            var var28 = 0
            if (var0 != var2) {
                var28 = (var3 - var5 shl 14) / (var0 - var2)
            }

            val var29 = var20 * var23 - var22 * var21
            if (var29 != 0) {
                val var30 = (var24 * var23 - var25 * var21 shl 9) / var29
                val var31 = (var25 * var20 - var24 * var22 shl 9) / var29
                var10 = var9 - var10
                var13 = var12 - var13
                var16 = var15 - var16
                var11 -= var9
                var14 -= var12
                var17 -= var15
                var var32 = var11 * var12 - var9 * var14 shl 14
                val var33 =
                    (((var15 * var14 - var17 * var12).toLong() shl 3 shl 14) / zoom.toLong()).toInt()
                val var34 = (((var17 * var9 - var11 * var15).toLong() shl 14) / zoom.toLong()).toInt()
                var var35 = var10 * var12 - var13 * var9 shl 14
                val var36 =
                    (((var13 * var15 - var16 * var12).toLong() shl 3 shl 14) / zoom.toLong()).toInt()
                val var37 = (((var16 * var9 - var10 * var15).toLong() shl 14) / zoom.toLong()).toInt()
                var var38 = var13 * var11 - var10 * var14 shl 14
                val var39 =
                    (((var16 * var14 - var13 * var17).toLong() shl 3 shl 14) / zoom.toLong()).toInt()
                val var40 = (((var17 * var10 - var11 * var16).toLong() shl 14) / zoom.toLong()).toInt()
                val var41: Int
                if (var0 <= var1 && var0 <= var2) {
                    if (var0 < clippingHeight) {
                        if (var1 > clippingHeight) {
                            var1 = clippingHeight
                        }

                        if (var2 > clippingHeight) {
                            var2 = clippingHeight
                        }

                        var6 = var30 + ((var6 shl 9) - var3 * var30)
                        if (var1 < var2) {
                            var3 = var3 shl 14
                            var5 = var3
                            if (var0 < 0) {
                                var5 -= var0 * var28
                                var3 -= var0 * var26
                                var6 -= var0 * var31
                                var0 = 0
                            }

                            var4 = var4 shl 14
                            if (var1 < 0) {
                                var4 -= var27 * var1
                                var1 = 0
                            }

                            var41 = var0 - centerY
                            var32 += var34 * var41
                            var35 += var37 * var41
                            var38 += var40 * var41
                            if ((var0 == var1 || var28 >= var26) && (var0 != var1 || var28 <= var27)) {
                                var2 -= var1
                                var1 -= var0
                                var0 = clippingOffsetsY[var0]

                                while (true) {
                                    --var1
                                    if (var1 < 0) {
                                        while (true) {
                                            --var2
                                            if (var2 < 0) {
                                                return
                                            }

                                            method2791(
                                                graphicsPixels,
                                                textureRenderPixels,
                                                0,
                                                0,
                                                var0,
                                                var4 shr 14,
                                                var5 shr 14,
                                                var6,
                                                var30,
                                                var32,
                                                var35,
                                                var38,
                                                var33,
                                                var36,
                                                var39
                                            )
                                            var5 += var28
                                            var4 += var27
                                            var6 += var31
                                            var0 += graphicsPixelsWidth
                                            var32 += var34
                                            var35 += var37
                                            var38 += var40
                                        }
                                    }

                                    method2791(
                                        graphicsPixels,
                                        textureRenderPixels,
                                        0,
                                        0,
                                        var0,
                                        var3 shr 14,
                                        var5 shr 14,
                                        var6,
                                        var30,
                                        var32,
                                        var35,
                                        var38,
                                        var33,
                                        var36,
                                        var39
                                    )
                                    var5 += var28
                                    var3 += var26
                                    var6 += var31
                                    var0 += graphicsPixelsWidth
                                    var32 += var34
                                    var35 += var37
                                    var38 += var40
                                }
                            } else {
                                var2 -= var1
                                var1 -= var0
                                var0 = clippingOffsetsY[var0]

                                while (true) {
                                    --var1
                                    if (var1 < 0) {
                                        while (true) {
                                            --var2
                                            if (var2 < 0) {
                                                return
                                            }

                                            method2791(
                                                graphicsPixels,
                                                textureRenderPixels,
                                                0,
                                                0,
                                                var0,
                                                var5 shr 14,
                                                var4 shr 14,
                                                var6,
                                                var30,
                                                var32,
                                                var35,
                                                var38,
                                                var33,
                                                var36,
                                                var39
                                            )
                                            var5 += var28
                                            var4 += var27
                                            var6 += var31
                                            var0 += graphicsPixelsWidth
                                            var32 += var34
                                            var35 += var37
                                            var38 += var40
                                        }
                                    }

                                    method2791(
                                        graphicsPixels,
                                        textureRenderPixels,
                                        0,
                                        0,
                                        var0,
                                        var5 shr 14,
                                        var3 shr 14,
                                        var6,
                                        var30,
                                        var32,
                                        var35,
                                        var38,
                                        var33,
                                        var36,
                                        var39
                                    )
                                    var5 += var28
                                    var3 += var26
                                    var6 += var31
                                    var0 += graphicsPixelsWidth
                                    var32 += var34
                                    var35 += var37
                                    var38 += var40
                                }
                            }
                        } else {
                            var3 = var3 shl 14
                            var4 = var3
                            if (var0 < 0) {
                                var4 -= var0 * var28
                                var3 -= var0 * var26
                                var6 -= var0 * var31
                                var0 = 0
                            }

                            var5 = var5 shl 14
                            if (var2 < 0) {
                                var5 -= var27 * var2
                                var2 = 0
                            }

                            var41 = var0 - centerY
                            var32 += var34 * var41
                            var35 += var37 * var41
                            var38 += var40 * var41
                            if (var0 != var2 && var28 < var26 || var0 == var2 && var27 > var26) {
                                var1 -= var2
                                var2 -= var0
                                var0 = clippingOffsetsY[var0]

                                while (true) {
                                    --var2
                                    if (var2 < 0) {
                                        while (true) {
                                            --var1
                                            if (var1 < 0) {
                                                return
                                            }

                                            method2791(
                                                graphicsPixels,
                                                textureRenderPixels,
                                                0,
                                                0,
                                                var0,
                                                var5 shr 14,
                                                var3 shr 14,
                                                var6,
                                                var30,
                                                var32,
                                                var35,
                                                var38,
                                                var33,
                                                var36,
                                                var39
                                            )
                                            var5 += var27
                                            var3 += var26
                                            var6 += var31
                                            var0 += graphicsPixelsWidth
                                            var32 += var34
                                            var35 += var37
                                            var38 += var40
                                        }
                                    }

                                    method2791(
                                        graphicsPixels,
                                        textureRenderPixels,
                                        0,
                                        0,
                                        var0,
                                        var4 shr 14,
                                        var3 shr 14,
                                        var6,
                                        var30,
                                        var32,
                                        var35,
                                        var38,
                                        var33,
                                        var36,
                                        var39
                                    )
                                    var4 += var28
                                    var3 += var26
                                    var6 += var31
                                    var0 += graphicsPixelsWidth
                                    var32 += var34
                                    var35 += var37
                                    var38 += var40
                                }
                            } else {
                                var1 -= var2
                                var2 -= var0
                                var0 = clippingOffsetsY[var0]

                                while (true) {
                                    --var2
                                    if (var2 < 0) {
                                        while (true) {
                                            --var1
                                            if (var1 < 0) {
                                                return
                                            }

                                            method2791(
                                                graphicsPixels,
                                                textureRenderPixels,
                                                0,
                                                0,
                                                var0,
                                                var3 shr 14,
                                                var5 shr 14,
                                                var6,
                                                var30,
                                                var32,
                                                var35,
                                                var38,
                                                var33,
                                                var36,
                                                var39
                                            )
                                            var5 += var27
                                            var3 += var26
                                            var6 += var31
                                            var0 += graphicsPixelsWidth
                                            var32 += var34
                                            var35 += var37
                                            var38 += var40
                                        }
                                    }

                                    method2791(
                                        graphicsPixels,
                                        textureRenderPixels,
                                        0,
                                        0,
                                        var0,
                                        var3 shr 14,
                                        var4 shr 14,
                                        var6,
                                        var30,
                                        var32,
                                        var35,
                                        var38,
                                        var33,
                                        var36,
                                        var39
                                    )
                                    var4 += var28
                                    var3 += var26
                                    var6 += var31
                                    var0 += graphicsPixelsWidth
                                    var32 += var34
                                    var35 += var37
                                    var38 += var40
                                }
                            }
                        }
                    }
                } else if (var1 <= var2) {
                    if (var1 < clippingHeight) {
                        if (var2 > clippingHeight) {
                            var2 = clippingHeight
                        }

                        if (var0 > clippingHeight) {
                            var0 = clippingHeight
                        }

                        var7 = var30 + ((var7 shl 9) - var30 * var4)
                        if (var2 < var0) {
                            var4 = var4 shl 14
                            var3 = var4
                            if (var1 < 0) {
                                var3 -= var26 * var1
                                var4 -= var27 * var1
                                var7 -= var31 * var1
                                var1 = 0
                            }

                            var5 = var5 shl 14
                            if (var2 < 0) {
                                var5 -= var28 * var2
                                var2 = 0
                            }

                            var41 = var1 - centerY
                            var32 += var34 * var41
                            var35 += var37 * var41
                            var38 += var40 * var41
                            if ((var2 == var1 || var26 >= var27) && (var2 != var1 || var26 <= var28)) {
                                var0 -= var2
                                var2 -= var1
                                var1 = clippingOffsetsY[var1]

                                while (true) {
                                    --var2
                                    if (var2 < 0) {
                                        while (true) {
                                            --var0
                                            if (var0 < 0) {
                                                return
                                            }

                                            method2791(
                                                graphicsPixels,
                                                textureRenderPixels,
                                                0,
                                                0,
                                                var1,
                                                var5 shr 14,
                                                var3 shr 14,
                                                var7,
                                                var30,
                                                var32,
                                                var35,
                                                var38,
                                                var33,
                                                var36,
                                                var39
                                            )
                                            var3 += var26
                                            var5 += var28
                                            var7 += var31
                                            var1 += graphicsPixelsWidth
                                            var32 += var34
                                            var35 += var37
                                            var38 += var40
                                        }
                                    }

                                    method2791(
                                        graphicsPixels,
                                        textureRenderPixels,
                                        0,
                                        0,
                                        var1,
                                        var4 shr 14,
                                        var3 shr 14,
                                        var7,
                                        var30,
                                        var32,
                                        var35,
                                        var38,
                                        var33,
                                        var36,
                                        var39
                                    )
                                    var3 += var26
                                    var4 += var27
                                    var7 += var31
                                    var1 += graphicsPixelsWidth
                                    var32 += var34
                                    var35 += var37
                                    var38 += var40
                                }
                            } else {
                                var0 -= var2
                                var2 -= var1
                                var1 = clippingOffsetsY[var1]

                                while (true) {
                                    --var2
                                    if (var2 < 0) {
                                        while (true) {
                                            --var0
                                            if (var0 < 0) {
                                                return
                                            }

                                            method2791(
                                                graphicsPixels,
                                                textureRenderPixels,
                                                0,
                                                0,
                                                var1,
                                                var3 shr 14,
                                                var5 shr 14,
                                                var7,
                                                var30,
                                                var32,
                                                var35,
                                                var38,
                                                var33,
                                                var36,
                                                var39
                                            )
                                            var3 += var26
                                            var5 += var28
                                            var7 += var31
                                            var1 += graphicsPixelsWidth
                                            var32 += var34
                                            var35 += var37
                                            var38 += var40
                                        }
                                    }

                                    method2791(
                                        graphicsPixels,
                                        textureRenderPixels,
                                        0,
                                        0,
                                        var1,
                                        var3 shr 14,
                                        var4 shr 14,
                                        var7,
                                        var30,
                                        var32,
                                        var35,
                                        var38,
                                        var33,
                                        var36,
                                        var39
                                    )
                                    var3 += var26
                                    var4 += var27
                                    var7 += var31
                                    var1 += graphicsPixelsWidth
                                    var32 += var34
                                    var35 += var37
                                    var38 += var40
                                }
                            }
                        } else {
                            var4 = var4 shl 14
                            var5 = var4
                            if (var1 < 0) {
                                var5 -= var26 * var1
                                var4 -= var27 * var1
                                var7 -= var31 * var1
                                var1 = 0
                            }

                            var3 = var3 shl 14
                            if (var0 < 0) {
                                var3 -= var0 * var28
                                var0 = 0
                            }

                            var41 = var1 - centerY
                            var32 += var34 * var41
                            var35 += var37 * var41
                            var38 += var40 * var41
                            if (var26 < var27) {
                                var2 -= var0
                                var0 -= var1
                                var1 = clippingOffsetsY[var1]

                                while (true) {
                                    --var0
                                    if (var0 < 0) {
                                        while (true) {
                                            --var2
                                            if (var2 < 0) {
                                                return
                                            }

                                            method2791(
                                                graphicsPixels,
                                                textureRenderPixels,
                                                0,
                                                0,
                                                var1,
                                                var3 shr 14,
                                                var4 shr 14,
                                                var7,
                                                var30,
                                                var32,
                                                var35,
                                                var38,
                                                var33,
                                                var36,
                                                var39
                                            )
                                            var3 += var28
                                            var4 += var27
                                            var7 += var31
                                            var1 += graphicsPixelsWidth
                                            var32 += var34
                                            var35 += var37
                                            var38 += var40
                                        }
                                    }

                                    method2791(
                                        graphicsPixels,
                                        textureRenderPixels,
                                        0,
                                        0,
                                        var1,
                                        var5 shr 14,
                                        var4 shr 14,
                                        var7,
                                        var30,
                                        var32,
                                        var35,
                                        var38,
                                        var33,
                                        var36,
                                        var39
                                    )
                                    var5 += var26
                                    var4 += var27
                                    var7 += var31
                                    var1 += graphicsPixelsWidth
                                    var32 += var34
                                    var35 += var37
                                    var38 += var40
                                }
                            } else {
                                var2 -= var0
                                var0 -= var1
                                var1 = clippingOffsetsY[var1]

                                while (true) {
                                    --var0
                                    if (var0 < 0) {
                                        while (true) {
                                            --var2
                                            if (var2 < 0) {
                                                return
                                            }

                                            method2791(
                                                graphicsPixels,
                                                textureRenderPixels,
                                                0,
                                                0,
                                                var1,
                                                var4 shr 14,
                                                var3 shr 14,
                                                var7,
                                                var30,
                                                var32,
                                                var35,
                                                var38,
                                                var33,
                                                var36,
                                                var39
                                            )
                                            var3 += var28
                                            var4 += var27
                                            var7 += var31
                                            var1 += graphicsPixelsWidth
                                            var32 += var34
                                            var35 += var37
                                            var38 += var40
                                        }
                                    }

                                    method2791(
                                        graphicsPixels,
                                        textureRenderPixels,
                                        0,
                                        0,
                                        var1,
                                        var4 shr 14,
                                        var5 shr 14,
                                        var7,
                                        var30,
                                        var32,
                                        var35,
                                        var38,
                                        var33,
                                        var36,
                                        var39
                                    )
                                    var5 += var26
                                    var4 += var27
                                    var7 += var31
                                    var1 += graphicsPixelsWidth
                                    var32 += var34
                                    var35 += var37
                                    var38 += var40
                                }
                            }
                        }
                    }
                } else if (var2 < clippingHeight) {
                    if (var0 > clippingHeight) {
                        var0 = clippingHeight
                    }

                    if (var1 > clippingHeight) {
                        var1 = clippingHeight
                    }

                    var8 = (var8 shl 9) - var5 * var30 + var30
                    if (var0 < var1) {
                        var5 = var5 shl 14
                        var4 = var5
                        if (var2 < 0) {
                            var4 -= var27 * var2
                            var5 -= var28 * var2
                            var8 -= var31 * var2
                            var2 = 0
                        }

                        var3 = var3 shl 14
                        if (var0 < 0) {
                            var3 -= var0 * var26
                            var0 = 0
                        }

                        var41 = var2 - centerY
                        var32 += var34 * var41
                        var35 += var37 * var41
                        var38 += var40 * var41
                        if (var27 < var28) {
                            var1 -= var0
                            var0 -= var2
                            var2 = clippingOffsetsY[var2]

                            while (true) {
                                --var0
                                if (var0 < 0) {
                                    while (true) {
                                        --var1
                                        if (var1 < 0) {
                                            return
                                        }

                                        method2791(
                                            graphicsPixels,
                                            textureRenderPixels,
                                            0,
                                            0,
                                            var2,
                                            var4 shr 14,
                                            var3 shr 14,
                                            var8,
                                            var30,
                                            var32,
                                            var35,
                                            var38,
                                            var33,
                                            var36,
                                            var39
                                        )
                                        var4 += var27
                                        var3 += var26
                                        var8 += var31
                                        var2 += graphicsPixelsWidth
                                        var32 += var34
                                        var35 += var37
                                        var38 += var40
                                    }
                                }

                                method2791(
                                    graphicsPixels,
                                    textureRenderPixels,
                                    0,
                                    0,
                                    var2,
                                    var4 shr 14,
                                    var5 shr 14,
                                    var8,
                                    var30,
                                    var32,
                                    var35,
                                    var38,
                                    var33,
                                    var36,
                                    var39
                                )
                                var4 += var27
                                var5 += var28
                                var8 += var31
                                var2 += graphicsPixelsWidth
                                var32 += var34
                                var35 += var37
                                var38 += var40
                            }
                        } else {
                            var1 -= var0
                            var0 -= var2
                            var2 = clippingOffsetsY[var2]

                            while (true) {
                                --var0
                                if (var0 < 0) {
                                    while (true) {
                                        --var1
                                        if (var1 < 0) {
                                            return
                                        }

                                        method2791(
                                            graphicsPixels,
                                            textureRenderPixels,
                                            0,
                                            0,
                                            var2,
                                            var3 shr 14,
                                            var4 shr 14,
                                            var8,
                                            var30,
                                            var32,
                                            var35,
                                            var38,
                                            var33,
                                            var36,
                                            var39
                                        )
                                        var4 += var27
                                        var3 += var26
                                        var8 += var31
                                        var2 += graphicsPixelsWidth
                                        var32 += var34
                                        var35 += var37
                                        var38 += var40
                                    }
                                }

                                method2791(
                                    graphicsPixels,
                                    textureRenderPixels,
                                    0,
                                    0,
                                    var2,
                                    var5 shr 14,
                                    var4 shr 14,
                                    var8,
                                    var30,
                                    var32,
                                    var35,
                                    var38,
                                    var33,
                                    var36,
                                    var39
                                )
                                var4 += var27
                                var5 += var28
                                var8 += var31
                                var2 += graphicsPixelsWidth
                                var32 += var34
                                var35 += var37
                                var38 += var40
                            }
                        }
                    } else {
                        var5 = var5 shl 14
                        var3 = var5
                        if (var2 < 0) {
                            var3 -= var27 * var2
                            var5 -= var28 * var2
                            var8 -= var31 * var2
                            var2 = 0
                        }

                        var4 = var4 shl 14
                        if (var1 < 0) {
                            var4 -= var26 * var1
                            var1 = 0
                        }

                        var41 = var2 - centerY
                        var32 += var34 * var41
                        var35 += var37 * var41
                        var38 += var40 * var41
                        if (var27 < var28) {
                            var0 -= var1
                            var1 -= var2
                            var2 = clippingOffsetsY[var2]

                            while (true) {
                                --var1
                                if (var1 < 0) {
                                    while (true) {
                                        --var0
                                        if (var0 < 0) {
                                            return
                                        }

                                        method2791(
                                            graphicsPixels,
                                            textureRenderPixels,
                                            0,
                                            0,
                                            var2,
                                            var4 shr 14,
                                            var5 shr 14,
                                            var8,
                                            var30,
                                            var32,
                                            var35,
                                            var38,
                                            var33,
                                            var36,
                                            var39
                                        )
                                        var4 += var26
                                        var5 += var28
                                        var8 += var31
                                        var2 += graphicsPixelsWidth
                                        var32 += var34
                                        var35 += var37
                                        var38 += var40
                                    }
                                }

                                method2791(
                                    graphicsPixels,
                                    textureRenderPixels,
                                    0,
                                    0,
                                    var2,
                                    var3 shr 14,
                                    var5 shr 14,
                                    var8,
                                    var30,
                                    var32,
                                    var35,
                                    var38,
                                    var33,
                                    var36,
                                    var39
                                )
                                var3 += var27
                                var5 += var28
                                var8 += var31
                                var2 += graphicsPixelsWidth
                                var32 += var34
                                var35 += var37
                                var38 += var40
                            }
                        } else {
                            var0 -= var1
                            var1 -= var2
                            var2 = clippingOffsetsY[var2]

                            while (true) {
                                --var1
                                if (var1 < 0) {
                                    while (true) {
                                        --var0
                                        if (var0 < 0) {
                                            return
                                        }

                                        method2791(
                                            graphicsPixels,
                                            textureRenderPixels,
                                            0,
                                            0,
                                            var2,
                                            var5 shr 14,
                                            var4 shr 14,
                                            var8,
                                            var30,
                                            var32,
                                            var35,
                                            var38,
                                            var33,
                                            var36,
                                            var39
                                        )
                                        var4 += var26
                                        var5 += var28
                                        var8 += var31
                                        var2 += graphicsPixelsWidth
                                        var32 += var34
                                        var35 += var37
                                        var38 += var40
                                    }
                                }

                                method2791(
                                    graphicsPixels,
                                    textureRenderPixels,
                                    0,
                                    0,
                                    var2,
                                    var5 shr 14,
                                    var3 shr 14,
                                    var8,
                                    var30,
                                    var32,
                                    var35,
                                    var38,
                                    var33,
                                    var36,
                                    var39
                                )
                                var3 += var27
                                var5 += var28
                                var8 += var31
                                var2 += graphicsPixelsWidth
                                var32 += var34
                                var35 += var37
                                var38 += var40
                            }
                        }
                    }
                }
            }
        }
    }


    fun method2791(
        var0: IntArray,
        var1: IntArray,
        var2: Int,
        var3: Int,
        var4: Int,
        var5: Int,
        var6: Int,
        var7: Int,
        var8: Int,
        var9: Int,
        var10: Int,
        var11: Int,
        var12: Int,
        var13: Int,
        var14: Int
    ) {
        var var2 = var2
        var var3 = var3
        var var4 = var4
        var var5 = var5
        var var6 = var6
        var var7 = var7
        var var8 = var8
        var var9 = var9
        var var10 = var10
        var var11 = var11
        if (isRasterClippingEnabled) {
            if (var6 > clippingOffsetX) {
                var6 = clippingOffsetX
            }

            if (var5 < 0) {
                var5 = 0
            }
        }

        if (var5 < var6) {
            var4 += var5
            var7 += var5 * var8
            var var17 = var6 - var5
            var var15: Int
            var var16: Int
            var var10000: Int
            var var18: Int
            var var19: Int
            var var20: Int
            var var21: Int
            var var22: Int
            val var23: Int
            if (isLowMem) {
                var23 = var5 - centerX
                var9 += var23 * (var12 shr 3)
                var10 += (var13 shr 3) * var23
                var11 += var23 * (var14 shr 3)
                var22 = var11 shr 12
                if (var22 != 0) {
                    var18 = var9 / var22
                    var19 = var10 / var22
                    if (var18 < 0) {
                        var18 = 0
                    } else if (var18 > 4032) {
                        var18 = 4032
                    }
                } else {
                    var18 = 0
                    var19 = 0
                }

                var9 += var12
                var10 += var13
                var11 += var14
                var22 = var11 shr 12
                if (var22 != 0) {
                    var20 = var9 / var22
                    var21 = var10 / var22
                    if (var20 < 0) {
                        var20 = 0
                    } else if (var20 > 4032) {
                        var20 = 4032
                    }
                } else {
                    var20 = 0
                    var21 = 0
                }

                var2 = (var18 shl 20) + var19
                var16 = (var21 - var19 shr 3) + (var20 - var18 shr 3 shl 20)
                var17 = var17 shr 3
                var8 = var8 shl 3
                var15 = var7 shr 8
                if (field1909) {
                    if (var17 > 0) {
                        do {
                            var3 = var1[(var2 ushr 26) + (var2 and 4032)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var2 += var16
                            var3 = var1[(var2 ushr 26) + (var2 and 4032)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var2 += var16
                            var3 = var1[(var2 ushr 26) + (var2 and 4032)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var2 += var16
                            var3 = var1[(var2 ushr 26) + (var2 and 4032)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var2 += var16
                            var3 = var1[(var2 ushr 26) + (var2 and 4032)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var2 += var16
                            var3 = var1[(var2 ushr 26) + (var2 and 4032)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var2 += var16
                            var3 = var1[(var2 ushr 26) + (var2 and 4032)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var2 += var16
                            var3 = var1[(var2 ushr 26) + (var2 and 4032)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var10000 = var16 + var2
                            var18 = var20
                            var19 = var21
                            var9 += var12
                            var10 += var13
                            var11 += var14
                            var22 = var11 shr 12
                            if (var22 != 0) {
                                var20 = var9 / var22
                                var21 = var10 / var22
                                if (var20 < 0) {
                                    var20 = 0
                                } else if (var20 > 4032) {
                                    var20 = 4032
                                }
                            } else {
                                var20 = 0
                                var21 = 0
                            }

                            var2 = (var18 shl 20) + var19
                            var16 = (var21 - var19 shr 3) + (var20 - var18 shr 3 shl 20)
                            var7 += var8
                            var15 = var7 shr 8
                            --var17
                        } while (var17 > 0)
                    }

                    var17 = var6 - var5 and 7
                    if (var17 > 0) {
                        do {
                            var3 = var1[(var2 ushr 26) + (var2 and 4032)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var2 += var16
                            --var17
                        } while (var17 > 0)
                    }
                } else {
                    if (var17 > 0) {
                        do {
                            if ((var1[(var2 ushr 26) + (var2 and 4032)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var2 += var16
                            if ((var1[(var2 ushr 26) + (var2 and 4032)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var2 += var16
                            if ((var1[(var2 ushr 26) + (var2 and 4032)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var2 += var16
                            if ((var1[(var2 ushr 26) + (var2 and 4032)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var2 += var16
                            if ((var1[(var2 ushr 26) + (var2 and 4032)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var2 += var16
                            if ((var1[(var2 ushr 26) + (var2 and 4032)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var2 += var16
                            if ((var1[(var2 ushr 26) + (var2 and 4032)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var2 += var16
                            if ((var1[(var2 ushr 26) + (var2 and 4032)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var10000 = var16 + var2
                            var18 = var20
                            var19 = var21
                            var9 += var12
                            var10 += var13
                            var11 += var14
                            var22 = var11 shr 12
                            if (var22 != 0) {
                                var20 = var9 / var22
                                var21 = var10 / var22
                                if (var20 < 0) {
                                    var20 = 0
                                } else if (var20 > 4032) {
                                    var20 = 4032
                                }
                            } else {
                                var20 = 0
                                var21 = 0
                            }

                            var2 = (var18 shl 20) + var19
                            var16 = (var21 - var19 shr 3) + (var20 - var18 shr 3 shl 20)
                            var7 += var8
                            var15 = var7 shr 8
                            --var17
                        } while (var17 > 0)
                    }

                    var17 = var6 - var5 and 7
                    if (var17 > 0) {
                        do {
                            if ((var1[(var2 ushr 26) + (var2 and 4032)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var2 += var16
                            --var17
                        } while (var17 > 0)
                    }
                }
            } else {
                var23 = var5 - centerX
                var9 += var23 * (var12 shr 3)
                var10 += (var13 shr 3) * var23
                var11 += var23 * (var14 shr 3)
                var22 = var11 shr 14
                if (var22 != 0) {
                    var18 = var9 / var22
                    var19 = var10 / var22
                    if (var18 < 0) {
                        var18 = 0
                    } else if (var18 > 16256) {
                        var18 = 16256
                    }
                } else {
                    var18 = 0
                    var19 = 0
                }

                var9 += var12
                var10 += var13
                var11 += var14
                var22 = var11 shr 14
                if (var22 != 0) {
                    var20 = var9 / var22
                    var21 = var10 / var22
                    if (var20 < 0) {
                        var20 = 0
                    } else if (var20 > 16256) {
                        var20 = 16256
                    }
                } else {
                    var20 = 0
                    var21 = 0
                }

                var2 = (var18 shl 18) + var19
                var16 = (var21 - var19 shr 3) + (var20 - var18 shr 3 shl 18)
                var17 = var17 shr 3
                var8 = var8 shl 3
                var15 = var7 shr 8
                if (field1909) {
                    if (var17 > 0) {
                        do {
                            var3 = var1[(var2 and 16256) + (var2 ushr 25)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var2 += var16
                            var3 = var1[(var2 and 16256) + (var2 ushr 25)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var2 += var16
                            var3 = var1[(var2 and 16256) + (var2 ushr 25)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var2 += var16
                            var3 = var1[(var2 and 16256) + (var2 ushr 25)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var2 += var16
                            var3 = var1[(var2 and 16256) + (var2 ushr 25)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var2 += var16
                            var3 = var1[(var2 and 16256) + (var2 ushr 25)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var2 += var16
                            var3 = var1[(var2 and 16256) + (var2 ushr 25)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var2 += var16
                            var3 = var1[(var2 and 16256) + (var2 ushr 25)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var10000 = var16 + var2
                            var18 = var20
                            var19 = var21
                            var9 += var12
                            var10 += var13
                            var11 += var14
                            var22 = var11 shr 14
                            if (var22 != 0) {
                                var20 = var9 / var22
                                var21 = var10 / var22
                                if (var20 < 0) {
                                    var20 = 0
                                } else if (var20 > 16256) {
                                    var20 = 16256
                                }
                            } else {
                                var20 = 0
                                var21 = 0
                            }

                            var2 = (var18 shl 18) + var19
                            var16 = (var21 - var19 shr 3) + (var20 - var18 shr 3 shl 18)
                            var7 += var8
                            var15 = var7 shr 8
                            --var17
                        } while (var17 > 0)
                    }

                    var17 = var6 - var5 and 7
                    if (var17 > 0) {
                        do {
                            var3 = var1[(var2 and 16256) + (var2 ushr 25)]
                            var0[var4++] =
                                (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            var2 += var16
                            --var17
                        } while (var17 > 0)
                    }
                } else {
                    if (var17 > 0) {
                        do {
                            if ((var1[(var2 and 16256) + (var2 ushr 25)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var2 += var16
                            if ((var1[(var2 and 16256) + (var2 ushr 25)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var2 += var16
                            if ((var1[(var2 and 16256) + (var2 ushr 25)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var2 += var16
                            if ((var1[(var2 and 16256) + (var2 ushr 25)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var2 += var16
                            if ((var1[(var2 and 16256) + (var2 ushr 25)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var2 += var16
                            if ((var1[(var2 and 16256) + (var2 ushr 25)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var2 += var16
                            if ((var1[(var2 and 16256) + (var2 ushr 25)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var2 += var16
                            if ((var1[(var2 and 16256) + (var2 ushr 25)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var10000 = var16 + var2
                            var18 = var20
                            var19 = var21
                            var9 += var12
                            var10 += var13
                            var11 += var14
                            var22 = var11 shr 14
                            if (var22 != 0) {
                                var20 = var9 / var22
                                var21 = var10 / var22
                                if (var20 < 0) {
                                    var20 = 0
                                } else if (var20 > 16256) {
                                    var20 = 16256
                                }
                            } else {
                                var20 = 0
                                var21 = 0
                            }

                            var2 = (var18 shl 18) + var19
                            var16 = (var21 - var19 shr 3) + (var20 - var18 shr 3 shl 18)
                            var7 += var8
                            var15 = var7 shr 8
                            --var17
                        } while (var17 > 0)
                    }

                    var17 = var6 - var5 and 7
                    if (var17 > 0) {
                        do {
                            if ((var1[(var2 and 16256) + (var2 ushr 25)].also { var3 = it }) != 0) {
                                var0[var4] =
                                    (var15 * (var3 and 65280) and 16711680) + ((var3 and 16711935) * var15 and -16711936) shr 8
                            }

                            ++var4
                            var2 += var16
                            --var17
                        } while (var17 > 0)
                    }
                }
            }
        }
    }

    companion object {
        private const val UNIT = Math.PI / 1024.0 // How much of the circle each unit of SINE/COSINE is

        @JvmField
		val SINE: IntArray = IntArray(2048) // sine angles for each of the 2048 units, * 65536 and stored as an int
        @JvmField
		val COSINE: IntArray = IntArray(2048) // cosine

        init {
            for (i in 0..2047) {
                SINE[i] = (65536.0 * sin(i.toDouble() * UNIT)).toInt()
                COSINE[i] = (65536.0 * cos(i.toDouble() * UNIT)).toInt()
            }
        }

        fun method2794(var0: Int, var1: Int): Int {
            var var1 = var1
            var1 = (var0 and 127) * var1 shr 7
            if (var1 < 2) {
                var1 = 2
            } else if (var1 > 126) {
                var1 = 126
            }

            return (var0 and 65408) + var1
        }
    }
}
