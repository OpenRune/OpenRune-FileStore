package dev.openrune.definition.game

import java.awt.image.BufferedImage

data class IndexedSprite(
    var offsetX: Int = 0,
    var offsetY: Int = 0,
    var width: Int = 0,
    var height: Int = 0,
    var averageColor : Int = -1,
    var subHeight: Int = 0,
    var subWidth: Int = 0,
    var alpha: ByteArray? = null
) {

    constructor(width : Int, height : Int) : this() {
        this.width = width
        this.height = height
        this.offsetX = 0
        this.offsetY = 0
        palette = IntArray(width * height)
        raster = ByteArray(width * height)
    }

    lateinit var raster: ByteArray
    lateinit var palette: IntArray

    fun toBufferedImage(): BufferedImage {
        if (width <= 0 || height <= 0) {
            return BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)
        }

        val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val i = x + y * width
                if (alpha == null) {
                    val colour = palette[raster[i].toInt() and 255]
                    if (colour != 0) {
                        bi.setRGB(x, y, -16777216 or colour)
                    }
                } else {
                    bi.setRGB(x, y, palette[raster[i].toInt() and 255] or (alpha!![i].toInt() shl 24))
                }
            }
        }
        return bi
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IndexedSprite

        if (offsetX != other.offsetX) return false
        if (offsetY != other.offsetY) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (subHeight != other.subHeight) return false
        if (subWidth != other.subWidth) return false
        if (alpha != null) {
            if (other.alpha == null) return false
            if (!alpha.contentEquals(other.alpha)) return false
        } else if (other.alpha != null) return false
        if (!raster.contentEquals(other.raster)) return false
        if (!palette.contentEquals(other.palette)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offsetX
        result = 31 * result + offsetY
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + subHeight
        result = 31 * result + subWidth
        result = 31 * result + (alpha?.contentHashCode() ?: 0)
        result = 31 * result + raster.contentHashCode()
        result = 31 * result + palette.contentHashCode()
        return result
    }

}