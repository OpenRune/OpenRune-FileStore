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

    /** Outlines the drawn pixels, [thickness] pixels wide. */
    @JvmOverloads
    fun drawBorder(color: Int, thickness: Int = 1) {
        repeat(thickness.coerceAtLeast(1)) { drawBorderPass(color) }
    }

    private fun drawBorderPass(color: Int) {
        val newPixels = pixels.copyOf()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = x + y * width
                if (pixels[index] == 0 &&
                    ((x > 0 && pixels[index - 1] != 0) ||
                            (y > 0 && pixels[index - width] != 0) ||
                            (x < width - 1 && pixels[index + 1] != 0) ||
                            (y < height - 1 && pixels[index + width] != 0))
                ) {
                    newPixels[index] = color
                }
            }
        }

        pixels = newPixels
    }

    /** Casts a shadow [offset] pixels down and to the right. */
    @JvmOverloads
    fun drawShadow(color: Int, offset: Int = 1) {
        val step = offset.coerceAtLeast(1)
        for (y in height - 1 downTo step) {
            val rowOffset = y * width
            for (x in width - 1 downTo step) {
                val index = x + rowOffset
                if (pixels[index] == 0 && pixels[index - step - step * width] != 0) {
                    pixels[index] = color
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
                if (pixels[row + x] == 0) continue
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
        for (y in bounds.y until bounds.y + bounds.height) {
            val targetY = y + shiftY
            if (targetY !in 0 until height) continue
            for (x in bounds.x until bounds.x + bounds.width) {
                val targetX = x + shiftX
                if (targetX !in 0 until width) continue
                shifted[targetX + targetY * width] = pixels[x + y * width]
            }
        }
        pixels = shifted
    }

    fun toBufferedImage(): BufferedImage {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val processedPixels = pixels.map { if (it != 0) it or -0x1000000 else 0 }.toIntArray()
        img.setRGB(0, 0, width, height, processedPixels, 0, width)
        return img
    }

    companion object {
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
