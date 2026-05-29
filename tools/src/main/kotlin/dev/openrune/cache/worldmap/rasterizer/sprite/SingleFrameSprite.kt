package dev.openrune.cache.worldmap.rasterizer.sprite

import dev.openrune.cache.worldmap.rasterizer.Rasterizer2D
import dev.openrune.definition.game.IndexedSprite

fun IndexedSprite.rasterizeScanLine(
    rasterizer: Rasterizer2D,
    x: Int,
    y: Int,
    destWidth: Int,
    destHeight: Int
) {
    var var1 = x
    var var2 = y
    var var3 = destWidth
    var var4 = destHeight
    val frameWidth = width
    val frameHeight = height
    var var7 = 0
    var var8 = 0
    val canvasWidth = if (originalWidth > 0) originalWidth else width
    val canvasHeight = if (originalHeight > 0) originalHeight else height
    val var11 = (canvasWidth shl 16) / var3
    val var12 = (canvasHeight shl 16) / var4
    var var13: Int
    if (offsetX > 0) {
        var13 = ((offsetX shl 16) + var11 - 1) / var11
        var1 += var13
        var7 += var13 * var11 - (offsetX shl 16)
    }
    if (offsetY > 0) {
        var13 = ((offsetY shl 16) + var12 - 1) / var12
        var2 += var13
        var8 += var13 * var12 - (offsetY shl 16)
    }
    if (frameWidth < canvasWidth) {
        var3 = ((frameWidth shl 16) - var7 + var11 - 1) / var11
    }
    if (frameHeight < canvasHeight) {
        var4 = ((frameHeight shl 16) - var8 + var12 - 1) / var12
    }
    var13 = var1 + var2 * rasterizer.width
    var var14 = rasterizer.width - var3
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
        raster,
        palette,
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

private fun IndexedSprite.drawScanLine(
    rasterizerPixels: IntArray,
    spritePixels: ByteArray,
    palette: IntArray,
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
            val index = spritePixels[(var3 shr 16) + var14].toInt() and 0xFF
            if (index != 0) {
                rasterizerPixels[var5++] = palette[index] or 0xFF000000.toInt()
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

fun IndexedSprite.drawTransparentBackgroundSprite(rasterizer2D: Rasterizer2D, x: Int, y: Int) {
    var startX = x + offsetX
    var startY = y + offsetY
    var rasterizerPos: Int = startX + startY * rasterizer2D.width
    var spritePos = 0
    var spriteHeight: Int = height
    var spriteWidth: Int = width
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
        raster,
        palette,
        spritePos,
        rasterizerPos,
        spriteWidth,
        spriteHeight,
        rasterizerOffset,
        spriteOffset
    )
}

private fun IndexedSprite.drawTransparentBackgroundSprite(
    rasterizerPixels: IntArray,
    spritePixels: ByteArray,
    palette: IntArray,
    spritePos: Int,
    rasterizerPos: Int,
    spriteWidth: Int,
    spriteHeight: Int,
    rasterizerOffset: Int,
    spriteOffset: Int
) {
    var currentSpritePos = spritePos
    var currentRasterizerPos = rasterizerPos
    val blocksOfFourPixels = -(spriteWidth shr 2)
    val remaining = -(spriteWidth and 3)
    for (yPos in -spriteHeight..-1) {
        var offset = blocksOfFourPixels
        while (offset < 0) {
            repeat(4) {
                val index = spritePixels[currentSpritePos++].toInt() and 0xFF
                if (index != 0) {
                    rasterizerPixels[currentRasterizerPos++] = palette[index] or 0xFF000000.toInt()
                } else {
                    ++currentRasterizerPos
                }
            }
            ++offset
        }
        offset = remaining
        while (offset < 0) {
            val index = spritePixels[currentSpritePos++].toInt() and 0xFF
            if (index != 0) {
                rasterizerPixels[currentRasterizerPos++] = palette[index] or 0xFF000000.toInt()
            } else {
                ++currentRasterizerPos
            }
            ++offset
        }
        currentRasterizerPos += rasterizerOffset
        currentSpritePos += spriteOffset
    }
}
