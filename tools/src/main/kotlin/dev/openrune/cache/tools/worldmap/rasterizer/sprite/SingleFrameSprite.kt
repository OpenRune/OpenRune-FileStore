package dev.openrune.cache.tools.worldmap.rasterizer.sprite

import dev.openrune.cache.tools.worldmap.rasterizer.provider.SpriteSheet
import dev.openrune.cache.tools.worldmap.rasterizer.Rasterizer2D

/**
 * @author Kris | 21/08/2022
 */
data class SingleFrameSprite(
    val width: Int,
    val height: Int,
    val xOffset: Int,
    val yOffset: Int,
    val subWidth: Int,
    val subHeight: Int,
    val pixels: IntArray,
) {

    fun rasterizeScanLine(
        rasterizer: Rasterizer2D,
        x: Int,
        y: Int,
        var3: Int,
        var4: Int
    ) {
        var var1 = x
        var var2 = y
        var var3 = var3
        var var4 = var4
        val frameWidth = subWidth
        val frameHeight = subHeight
        var var7 = 0
        var var8 = 0
        val spriteWidth = width
        val spriteHeight = height
        val var11 = (spriteWidth shl 16) / var3
        val var12 = (spriteHeight shl 16) / var4
        var var13: Int
        if (xOffset > 0) {
            var13 = (var11 + (xOffset shl 16) - 1) / var11
            var1 += var13
            var7 += var13 * var11 - (xOffset shl 16)
        }
        if (yOffset > 0) {
            var13 = (var12 + (yOffset shl 16) - 1) / var12
            var2 += var13
            var8 += var13 * var12 - (yOffset shl 16)
        }
        if (frameWidth < spriteWidth) {
            var3 = (var11 + ((frameWidth shl 16) - var7) - 1) / var11
        }
        if (frameHeight < spriteHeight) {
            var4 = (var12 + ((frameHeight shl 16) - var8) - 1) / var12
        }
        var13 = var1 + var2 * rasterizer.width
        var var14: Int = rasterizer.width - var3
        if (var2 + var4 > rasterizer.maxY) {
            var4 -= var2 + var4 - rasterizer.maxY
        }
        var var15: Int
        if (var2 < rasterizer.minY) {
            var15 = rasterizer.minY - var2
            var4 -= var15
            var13 += var15 * rasterizer.width
            var8 += var12 * var15
        }
        if (var3 + var1 > rasterizer.maxX) {
            var15 = var3 + var1 - rasterizer.maxX
            var3 -= var15
            var14 += var15
        }
        if (var1 < rasterizer.minX) {
            var15 = rasterizer.minX - var1
            var3 -= var15
            var13 += var15
            var7 += var11 * var15
            var14 += var15
        }
        drawScanLine(
            rasterizer.pixels,
            pixels,
            var7,
            var8,
            var13,
            var14,
            var3,
            var4,
            var11,
            var12,
            frameWidth
        )
    }

    private fun drawScanLine(
        rasterizerPixels: IntArray,
        pixels: IntArray,
        var3: Int,
        var4: Int,
        var5: Int,
        var6: Int,
        var7: Int,
        var8: Int,
        var9: Int,
        var10: Int,
        var11: Int
    ) {
        var var3 = var3
        var var4 = var4
        var var5 = var5
        val var12 = var3
        for (var13 in -var8..-1) {
            val var14 = var11 * (var4 shr 16)
            for (var15 in -var7..-1) {
                val pixel = pixels[(var3 shr 16) + var14]
                if (pixel != 0) {
                    rasterizerPixels[var5++] = pixel
                } else {
                    ++var5
                }
                var3 += var9
            }
            var4 += var10
            var3 = var12
            var5 += var6
        }
    }

    fun drawTransparentBackgroundSprite(rasterizer2D: Rasterizer2D, x: Int, y: Int) {
        var startX = x + xOffset
        var startY = y + yOffset
        var rasterizerPos: Int = startX + startY * rasterizer2D.width
        var spritePos = 0
        var spriteHeight: Int = subHeight
        var spriteWidth: Int = subWidth
        var rasterizerOffset: Int = rasterizer2D.width - spriteWidth
        var spriteOffset = 0
        if (startY < rasterizer2D.minY) {
            val subtractedHeight = rasterizer2D.minY - startY
            spriteHeight -= subtractedHeight
            startY = rasterizer2D.minY
            spritePos += subtractedHeight * spriteWidth
            rasterizerPos += subtractedHeight * rasterizer2D.width
        }
        if (spriteHeight + startY > rasterizer2D.maxY) {
            spriteHeight -= spriteHeight + startY - rasterizer2D.maxY
        }
        if (startX < rasterizer2D.minX) {
            val subtractedWidth = rasterizer2D.minX - startX
            spriteWidth -= subtractedWidth
            startX = rasterizer2D.minX
            spritePos += subtractedWidth
            rasterizerPos += subtractedWidth
            spriteOffset += subtractedWidth
            rasterizerOffset += subtractedWidth
        }
        if (spriteWidth + startX > rasterizer2D.maxX) {
            val widthOffset = spriteWidth + startX - rasterizer2D.maxX
            spriteWidth -= widthOffset
            spriteOffset += widthOffset
            rasterizerOffset += widthOffset
        }
        if (spriteWidth <= 0 || spriteHeight <= 0) return
        drawTransparentBackgroundSprite(
            rasterizer2D.pixels,
            pixels,
            spritePos,
            rasterizerPos,
            spriteWidth,
            spriteHeight,
            rasterizerOffset,
            spriteOffset
        )
    }

    private fun drawTransparentBackgroundSprite(
        rasterizerPixels: IntArray,
        spritePixels: IntArray,
        spritePos: Int,
        rasterizerPos: Int,
        spriteWidth: Int,
        spriteHeight: Int,
        rasterizerOffset: Int,
        spriteOffset: Int
    ) {
        var pixel: Int
        var currentSpritePos = spritePos
        var currentRasterizerPos = rasterizerPos
        val blocksOfFourPixels = -(spriteWidth shr 2)
        val remaining = -(spriteWidth and 3)
        for (yPos in -spriteHeight..-1) {
            var offset = blocksOfFourPixels
            while (offset < 0) {
                pixel = spritePixels[currentSpritePos++]
                if (pixel != 0) {
                    rasterizerPixels[currentRasterizerPos++] = pixel
                } else {
                    ++currentRasterizerPos
                }
                pixel = spritePixels[currentSpritePos++]
                if (pixel != 0) {
                    rasterizerPixels[currentRasterizerPos++] = pixel
                } else {
                    ++currentRasterizerPos
                }
                pixel = spritePixels[currentSpritePos++]
                if (pixel != 0) {
                    rasterizerPixels[currentRasterizerPos++] = pixel
                } else {
                    ++currentRasterizerPos
                }
                pixel = spritePixels[currentSpritePos++]
                if (pixel != 0) {
                    rasterizerPixels[currentRasterizerPos++] = pixel
                } else {
                    ++currentRasterizerPos
                }
                ++offset
            }
            offset = remaining
            while (offset < 0) {
                pixel = spritePixels[currentSpritePos++]
                if (pixel != 0) {
                    rasterizerPixels[currentRasterizerPos++] = pixel
                } else {
                    ++currentRasterizerPos
                }
                ++offset
            }
            currentRasterizerPos += rasterizerOffset
            currentSpritePos += spriteOffset
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SingleFrameSprite

        if (width != other.width) return false
        if (height != other.height) return false
        if (xOffset != other.xOffset) return false
        if (yOffset != other.yOffset) return false
        if (subWidth != other.subWidth) return false
        if (subHeight != other.subHeight) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + xOffset
        result = 31 * result + yOffset
        result = 31 * result + subWidth
        result = 31 * result + subHeight
        return result
    }

    companion object {
        fun SpriteSheet.toSingleSprite(): SingleFrameSprite {
            val frame = frames.single()
            val width = width
            val height = height
            val xOffset = frame.xOffset
            val yOffset = frame.yOffset
            val subWidth = frame.innerWidth
            val subHeight = frame.innerHeight
            return SingleFrameSprite(
                width,
                height,
                xOffset,
                yOffset,
                subWidth,
                subHeight,
                frame.pixels
            )
        }
    }
}
