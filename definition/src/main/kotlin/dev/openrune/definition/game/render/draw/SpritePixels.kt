package dev.openrune.definition.game.render.draw

import java.awt.Rectangle
import java.awt.image.BufferedImage

class SpritePixels(
    var pixels: IntArray,
    var width: Int,
    var height: Int,
    var offsetX: Int = 0,
    var offsetY: Int = 0
) {

    constructor(width: Int, height: Int) : this(IntArray(width * height), width, height)

    /** Per-pixel opacity 0..255, or null when a pixel counts as drawn purely by being non-zero. */
    var alphaPixels: IntArray? = null

    private fun isDrawn(index: Int): Boolean {
        val alphas = alphaPixels ?: return pixels[index] != 0
        return alphas[index] != 0
    }

    /** Outlines the drawn pixels, [thickness] pixels wide. */
    @JvmOverloads
    fun drawBorder(color: Int, thickness: Int = 1) {
        repeat(thickness.coerceAtLeast(1)) { drawBorderPass(color) }
    }

    private fun drawBorderPass(color: Int) {
        val newPixels = pixels.copyOf()
        val newAlphas = alphaPixels?.copyOf()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = x + y * width
                if (!isDrawn(index) &&
                    ((x > 0 && isDrawn(index - 1)) ||
                            (y > 0 && isDrawn(index - width)) ||
                            (x < width - 1 && isDrawn(index + 1)) ||
                            (y < height - 1 && isDrawn(index + width)))
                ) {
                    newPixels[index] = color
                    newAlphas?.set(index, 255)
                }
            }
        }

        pixels = newPixels
        if (newAlphas != null) alphaPixels = newAlphas
    }

    /** Casts a shadow [offset] pixels down and to the right. */
    @JvmOverloads
    fun drawShadow(color: Int, offset: Int = 1) {
        val step = offset.coerceAtLeast(1)
        val alphas = alphaPixels
        for (y in height - 1 downTo step) {
            val rowOffset = y * width
            for (x in width - 1 downTo step) {
                val index = x + rowOffset
                if (!isDrawn(index) && isDrawn(index - step - step * width)) {
                    pixels[index] = color
                    alphas?.set(index, 255)
                }
            }
        }
    }

    fun drawAtOn(graphics: Rasterizer2D, x: Int, y: Int) {
        var x = x + offsetX
        var y = y + offsetY
        var pixelIndex = x + y * graphics.graphicsPixelsWidth
        var deltaIndex = 0
        var drawWidth = width
        var drawHeight = height
        var stride = graphics.graphicsPixelsWidth - width
        var extraOffset = 0

        if (y < graphics.drawingAreaTop) {
            val deltaY = graphics.drawingAreaTop - y
            drawHeight -= deltaY
            y = graphics.drawingAreaTop
            deltaIndex += deltaY * width
            pixelIndex += deltaY * graphics.graphicsPixelsWidth
        }

        if (y + drawHeight > graphics.drawingAreaBottom) {
            drawHeight -= (y + drawHeight) - graphics.drawingAreaBottom
        }

        if (x < graphics.drawRegionX) {
            val deltaX = graphics.drawRegionX - x
            drawWidth -= deltaX
            x = graphics.drawRegionX
            deltaIndex += deltaX
            pixelIndex += deltaX
            extraOffset += deltaX
            stride += deltaX
        }

        if (x + drawWidth > graphics.drawingAreaRight) {
            val deltaX = (x + drawWidth) - graphics.drawingAreaRight
            drawWidth -= deltaX
            extraOffset += deltaX
            stride += deltaX
        }

        if (drawWidth > 0 && drawHeight > 0) {
            drawPixels(graphics.graphicsPixels, pixels, deltaIndex, pixelIndex, drawWidth, drawHeight, stride, extraOffset)
        }
    }

    /** Smallest rectangle holding every drawn pixel, or null when nothing was drawn. */
    fun contentBounds(): Rectangle? {
        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                if (!isDrawn(row + x)) continue
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }

        return if (maxX < 0) null else Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1)
    }

    /** Centres the drawn pixels by whole-pixel translation, so nothing is resampled. */
    fun centerContent() {
        val bounds = contentBounds() ?: return
        val shiftX = (width - bounds.width) / 2 - bounds.x
        val shiftY = (height - bounds.height) / 2 - bounds.y
        if (shiftX == 0 && shiftY == 0) return

        val shifted = IntArray(pixels.size)
        val alphas = alphaPixels
        val shiftedAlphas = if (alphas == null) null else IntArray(alphas.size)
        for (y in bounds.y until bounds.y + bounds.height) {
            val targetY = y + shiftY
            if (targetY !in 0 until height) continue
            for (x in bounds.x until bounds.x + bounds.width) {
                val targetX = x + shiftX
                if (targetX !in 0 until width) continue
                shifted[targetX + targetY * width] = pixels[x + y * width]
                if (alphas != null) shiftedAlphas!![targetX + targetY * width] = alphas[x + y * width]
            }
        }
        pixels = shifted
        if (shiftedAlphas != null) alphaPixels = shiftedAlphas
    }

    fun toBufferedImage(): BufferedImage {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val alphas = alphaPixels
        val processedPixels = if (alphas == null) {
            IntArray(pixels.size) { if (pixels[it] != 0) pixels[it] or -0x1000000 else 0 }
        } else {
            IntArray(pixels.size) {
                val alpha = alphas[it]
                if (alpha == 0) 0 else (alpha shl 24) or (pixels[it] and 0xffffff)
            }
        }
        img.setRGB(0, 0, width, height, processedPixels, 0, width)
        return img
    }

    companion object {
        /**
         * Recovers per-pixel opacity from two renders of the same model, one over black and one
         * over white. A pixel of colour `C` at opacity `a` lands as `a*C` in the black pass and
         * `a*C + (1-a)*255` in the white pass, so the difference gives the transparency and the
         * black pass divided by the recovered opacity undoes the blend.
         */
        @JvmStatic
        fun fromOpaquePasses(overBlack: SpritePixels, overWhite: SpritePixels): SpritePixels {
            val black = overBlack.pixels
            val white = overWhite.pixels
            val colors = IntArray(black.size)
            val alphas = IntArray(black.size)

            for (i in black.indices) {
                val blended = black[i]
                val lifted = white[i]
                val transparency = maxOf(
                    (lifted ushr 16 and 0xff) - (blended ushr 16 and 0xff),
                    (lifted ushr 8 and 0xff) - (blended ushr 8 and 0xff),
                    (lifted and 0xff) - (blended and 0xff)
                ).coerceIn(0, 255)

                val alpha = 255 - transparency
                if (alpha == 0) continue
                alphas[i] = alpha
                colors[i] = unblend(blended, alpha)
            }

            return SpritePixels(colors, overBlack.width, overBlack.height, overBlack.offsetX, overBlack.offsetY)
                .also { it.alphaPixels = alphas }
        }

        private fun unblend(color: Int, alpha: Int): Int {
            if (alpha >= 255) return color and 0xffffff
            val red = ((color ushr 16 and 0xff) * 255 / alpha).coerceAtMost(255)
            val green = ((color ushr 8 and 0xff) * 255 / alpha).coerceAtMost(255)
            val blue = ((color and 0xff) * 255 / alpha).coerceAtMost(255)
            return (red shl 16) or (green shl 8) or blue
        }

        @JvmStatic
        fun drawPixels(
            rasterizerPixels: IntArray,
            spritePixels: IntArray,
            spriteOffset: Int,
            rasterizerOffset: Int,
            width: Int,
            height: Int,
            stride: Int,
            extraOffset: Int
        ) {
            var srcIndex = spriteOffset
            var destIndex = rasterizerOffset

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = spritePixels[srcIndex++]
                    if (pixel != 0) {
                        rasterizerPixels[destIndex] = pixel
                    }
                    destIndex++
                }
                destIndex += stride
                srcIndex += extraOffset
            }
        }
    }

}
