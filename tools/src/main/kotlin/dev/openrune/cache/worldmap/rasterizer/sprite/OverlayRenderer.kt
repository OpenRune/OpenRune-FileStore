package dev.openrune.cache.worldmap.rasterizer.sprite

import dev.openrune.cache.worldmap.rasterizer.Rasterizer2D

/**
 * @author Kris | 15/08/2022
 */
class OverlayRenderer(private val pixelsPerTile: Int) {
    private var tileTemplates: Array<Array<ByteArray>> = arrayOf(init0(), init1(), init2(), init3(), init4(), init5(), init6(), init7())

    fun drawOverlay(
        rasterizer: Rasterizer2D,
        x: Int,
        y: Int,
        underlayColour: Int,
        overlayColour: Int,
        width: Int,
        height: Int,
        shape: Int,
        rotation: Int
    ) {
        if (shape == 0 || pixelsPerTile == 0) return
        val currentRotation: Int = this.adjustRotation(rotation, shape)
        val currentShape: Int = this.adjustShape(shape)
        rasterizer.drawGradientPixels(
            x,
            y,
            width,
            height,
            underlayColour,
            overlayColour,
            tileTemplates[currentShape - 1][currentRotation],
            pixelsPerTile
        )
    }

    private fun adjustRotation(rotation: Int, shape: Int): Int {
        if (shape == 9) {
            return rotation + 1 and 3
        }
        if (shape == 10) {
            return rotation + 3 and 3
        }
        if (shape == 11) {
            return rotation + 3 and 3
        }
        return rotation
    }

    private fun adjustShape(shape: Int): Int {
        return when (shape) {
            9, 10 -> 1
            11 -> 8
            else -> shape
        }
    }

    private fun init0(): Array<ByteArray> {
        val byteArrays = arrayOfNulls<ByteArray>(4)
        var pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var var2 = 0
        var var4: Int
        var var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var4 <= var3) {
                    pixels[var2] = -1
                }
                ++var2
                ++var4
            }
            ++var3
        }
        byteArrays[0] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var2 = 0
        var3 = pixelsPerTile - 1
        while (var3 >= 0) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var4 <= var3) {
                    pixels[var2] = -1
                }
                ++var2
                ++var4
            }
            --var3
        }
        byteArrays[1] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var2 = 0
        var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var4 >= var3) {
                    pixels[var2] = -1
                }
                ++var2
                ++var4
            }
            ++var3
        }
        byteArrays[2] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var2 = 0
        var3 = pixelsPerTile - 1
        while (var3 >= 0) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var4 >= var3) {
                    pixels[var2] = -1
                }
                ++var2
                ++var4
            }
            --var3
        }
        byteArrays[3] = pixels
        return byteArrays.requireNoNulls()
    }

    private fun init1(): Array<ByteArray> {
        val byteArrays = arrayOfNulls<ByteArray>(4)
        var pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var var2 = 0
        var var4: Int
        var var3: Int = pixelsPerTile - 1
        while (var3 >= 0) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var4 <= (var3 shr 1)) {
                    pixels[var2] = -1
                }
                ++var2
                ++var4
            }
            --var3
        }
        byteArrays[0] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var2 = 0
        var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var2 >= 0 && var2 < pixels.size) {
                    if (var4 >= ((var3 shl 1))) {
                        pixels[var2] = -1
                    }
                    ++var2
                } else {
                    ++var2
                }
                ++var4
            }
            ++var3
        }
        byteArrays[1] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var2 = 0
        var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = pixelsPerTile - 1
            while (var4 >= 0) {
                if (var4 <= (var3 shr 1)) {
                    pixels[var2] = -1
                }
                ++var2
                --var4
            }
            ++var3
        }
        byteArrays[2] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var2 = 0
        var3 = pixelsPerTile - 1
        while (var3 >= 0) {
            var4 = pixelsPerTile - 1
            while (var4 >= 0) {
                if (var4 >= ((var3 shl 1))) {
                    pixels[var2] = -1
                }
                ++var2
                --var4
            }
            --var3
        }
        byteArrays[3] = pixels
        return byteArrays.requireNoNulls()
    }

    private fun init2(): Array<ByteArray> {
        val byteArrays = arrayOfNulls<ByteArray>(4)
        var pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var var2 = 0
        var var4: Int
        var var3: Int = pixelsPerTile - 1
        while (var3 >= 0) {
            var4 = pixelsPerTile - 1
            while (var4 >= 0) {
                if (var4 <= (var3 shr 1)) {
                    pixels[var2] = -1
                }
                ++var2
                --var4
            }
            --var3
        }
        byteArrays[0] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var2 = 0
        var3 = pixelsPerTile - 1
        while (var3 >= 0) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var4 >= (var3 shl 1)) {
                    pixels[var2] = -1
                }
                ++var2
                ++var4
            }
            --var3
        }
        byteArrays[1] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var2 = 0
        var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var4 <= var3 shr 1) {
                    pixels[var2] = -1
                }
                ++var2
                ++var4
            }
            ++var3
        }
        byteArrays[2] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var2 = 0
        var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = pixelsPerTile - 1
            while (var4 >= 0) {
                if (var4 >= (var3 shl 1)) {
                    pixels[var2] = -1
                }
                ++var2
                --var4
            }
            ++var3
        }
        byteArrays[3] = pixels
        return byteArrays.requireNoNulls()
    }

    private fun init3(): Array<ByteArray> {
        val byteArrays = arrayOfNulls<ByteArray>(4)
        var pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var var2 = 0
        var var4: Int
        var var3: Int = pixelsPerTile - 1
        while (var3 >= 0) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var4 >= var3 shr 1) {
                    pixels[var2] = -1
                }
                ++var2
                ++var4
            }
            --var3
        }
        byteArrays[0] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var2 = 0
        var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var4 <= (var3 shl 1)) {
                    pixels[var2] = -1
                }
                ++var2
                ++var4
            }
            ++var3
        }
        byteArrays[1] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var2 = 0
        var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = pixelsPerTile - 1
            while (var4 >= 0) {
                if (var4 >= var3 shr 1) {
                    pixels[var2] = -1
                }
                ++var2
                --var4
            }
            ++var3
        }
        byteArrays[2] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var2 = 0
        var3 = pixelsPerTile - 1
        while (var3 >= 0) {
            var4 = pixelsPerTile - 1
            while (var4 >= 0) {
                if (var4 <= (var3 shl 1)) {
                    pixels[var2] = -1
                }
                ++var2
                --var4
            }
            --var3
        }
        byteArrays[3] = pixels
        return byteArrays.requireNoNulls()
    }

    private fun init4(): Array<ByteArray> {
        val byteArrays = arrayOfNulls<ByteArray>(4)
        var pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var var2 = 0
        var var4: Int
        var var3: Int = pixelsPerTile - 1
        while (var3 >= 0) {
            var4 = pixelsPerTile - 1
            while (var4 >= 0) {
                if (var4 >= var3 shr 1) {
                    pixels[var2] = -1
                }
                ++var2
                --var4
            }
            --var3
        }
        byteArrays[0] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var2 = 0
        var3 = pixelsPerTile - 1
        while (var3 >= 0) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var4 <= (var3 shl 1)) {
                    pixels[var2] = -1
                }
                ++var2
                ++var4
            }
            --var3
        }
        byteArrays[1] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var2 = 0
        var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var4 >= var3 shr 1) {
                    pixels[var2] = -1
                }
                ++var2
                ++var4
            }
            ++var3
        }
        byteArrays[2] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var2 = 0
        var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = pixelsPerTile - 1
            while (var4 >= 0) {
                if (var4 <= (var3 shl 1)) {
                    pixels[var2] = -1
                }
                ++var2
                --var4
            }
            ++var3
        }
        byteArrays[3] = pixels
        return byteArrays.requireNoNulls()
    }

    private fun init5(): Array<ByteArray> {
        val byteArrays = arrayOfNulls<ByteArray>(4)
        var pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var var5 = 0
        var var4: Int
        var var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var4 <= pixelsPerTile / 2) {
                    pixels[var5] = -1
                }
                ++var5
                ++var4
            }
            ++var3
        }
        byteArrays[0] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var5 = 0
        var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var3 <= pixelsPerTile / 2) {
                    pixels[var5] = -1
                }
                ++var5
                ++var4
            }
            ++var3
        }
        byteArrays[1] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var5 = 0
        var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var4 >= pixelsPerTile / 2) {
                    pixels[var5] = -1
                }
                ++var5
                ++var4
            }
            ++var3
        }
        byteArrays[2] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var5 = 0
        var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var3 >= pixelsPerTile / 2) {
                    pixels[var5] = -1
                }
                ++var5
                ++var4
            }
            ++var3
        }
        byteArrays[3] = pixels
        return byteArrays.requireNoNulls()
    }

    private fun init6(): Array<ByteArray> {
        val byteArrays = arrayOfNulls<ByteArray>(4)
        var pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var var5 = 0
        var var4: Int
        var var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var4 <= var3 - pixelsPerTile / 2) {
                    pixels[var5] = -1
                }
                ++var5
                ++var4
            }
            ++var3
        }
        byteArrays[0] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var5 = 0
        var3 = pixelsPerTile - 1
        while (var3 >= 0) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var4 <= var3 - pixelsPerTile / 2) {
                    pixels[var5] = -1
                }
                ++var5
                ++var4
            }
            --var3
        }
        byteArrays[1] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var5 = 0
        var3 = pixelsPerTile - 1
        while (var3 >= 0) {
            var4 = pixelsPerTile - 1
            while (var4 >= 0) {
                if (var4 <= var3 - pixelsPerTile / 2) {
                    pixels[var5] = -1
                }
                ++var5
                --var4
            }
            --var3
        }
        byteArrays[2] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var5 = 0
        var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = pixelsPerTile - 1
            while (var4 >= 0) {
                if (var4 <= var3 - pixelsPerTile / 2) {
                    pixels[var5] = -1
                }
                ++var5
                --var4
            }
            ++var3
        }
        byteArrays[3] = pixels
        return byteArrays.requireNoNulls()
    }

    private fun init7(): Array<ByteArray> {
        val byteArrays = arrayOfNulls<ByteArray>(4)
        var pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var var5 = 0
        var var4: Int
        var var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var4 >= var3 - pixelsPerTile / 2) {
                    pixels[var5] = -1
                }
                ++var5
                ++var4
            }
            ++var3
        }
        byteArrays[0] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var5 = 0
        var3 = pixelsPerTile - 1
        while (var3 >= 0) {
            var4 = 0
            while (var4 < pixelsPerTile) {
                if (var4 >= var3 - pixelsPerTile / 2) {
                    pixels[var5] = -1
                }
                ++var5
                ++var4
            }
            --var3
        }
        byteArrays[1] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var5 = 0
        var3 = pixelsPerTile - 1
        while (var3 >= 0) {
            var4 = pixelsPerTile - 1
            while (var4 >= 0) {
                if (var4 >= var3 - pixelsPerTile / 2) {
                    pixels[var5] = -1
                }
                ++var5
                --var4
            }
            --var3
        }
        byteArrays[2] = pixels
        pixels = ByteArray(pixelsPerTile * pixelsPerTile)
        var5 = 0
        var3 = 0
        while (var3 < pixelsPerTile) {
            var4 = pixelsPerTile - 1
            while (var4 >= 0) {
                if (var4 >= var3 - pixelsPerTile / 2) {
                    pixels[var5] = -1
                }
                ++var5
                --var4
            }
            ++var3
        }
        byteArrays[3] = pixels
        return byteArrays.requireNoNulls()
    }
}
