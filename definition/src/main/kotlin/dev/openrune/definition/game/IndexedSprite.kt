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
    var originalWidth: Int = 0,
    var originalHeight: Int = 0,
    var alpha: ByteArray? = null,
    /**
     * Set instead of [raster]/[palette] for sprite formats that only ever
     * hand us a fully composed image rather than an indexed palette+raster
     * breakdown to rebuild - e.g. RS3's raw-RGB sprite layout, or any
     * already-decoded [java.awt.image.BufferedImage] (openrs2's own indexed
     * sprite decoder doesn't expose its internal palette either). When set,
     * [toBufferedImage] uses it directly instead of the palette lookup.
     */
    var argb: IntArray? = null
) {

    fun getHorizontalOffset(alignment: Int): Int {
        val value = when (alignment) {
            0 -> 1
            1 -> 2
            else -> 0
        }
        return when (value) {
            1 -> 0
            2 -> -subWidth / 2
            else -> -subWidth
        }
    }

    fun getVerticalOffset(alignment: Int): Int {
        val value = when (alignment) {
            0 -> 1
            1 -> 2
            else -> 1
        }
        return when (value) {
            1 -> 0
            2 -> -subHeight / 2
            else -> -subHeight
        }
    }

    constructor(width : Int, height : Int) : this() {
        this.width = width
        this.height = height
        this.offsetX = 0
        this.offsetY = 0
        palette = IntArray(width * height)
        raster = ByteArray(width * height)
    }

    /** Wraps an already-composed image (see [argb]); no cropping/offset data available for these. */
    constructor(width: Int, height: Int, argb: IntArray) : this() {
        this.width = width
        this.height = height
        this.offsetX = 0
        this.offsetY = 0
        this.argb = argb
        palette = IntArray(0)
        raster = ByteArray(0)
    }

    lateinit var raster: ByteArray
    lateinit var palette: IntArray

    val fullWidth: Int get() = offsetX + width + subWidth
    val fullHeight: Int get() = offsetY + height + subHeight

    /** Expands the cropped raster back out to the full sheet, for anything sampling by absolute coordinate. */
    fun normalized(): IndexedSprite {
        if (argb != null || (width == fullWidth && height == fullHeight)) return this

        val padded = ByteArray(fullWidth * fullHeight)
        var source = 0
        for (y in 0 until height) {
            val row = (y + offsetY) * fullWidth + offsetX
            for (x in 0 until width) {
                padded[row + x] = raster[source++]
            }
        }

        return copy(offsetX = 0, offsetY = 0, width = fullWidth, height = fullHeight, subWidth = 0, subHeight = 0)
            .also {
                it.raster = padded
                it.palette = palette
            }
    }

    fun toBufferedImage(): BufferedImage {
        if (width <= 0 || height <= 0) {
            return BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)
        }

        val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        val direct = argb
        if (direct != null) {
            bi.setRGB(0, 0, width, height, direct, 0, width)
            return bi
        }

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
        if (argb != null) {
            if (other.argb == null) return false
            if (!argb.contentEquals(other.argb)) return false
        } else if (other.argb != null) return false
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
        result = 31 * result + (argb?.contentHashCode() ?: 0)
        result = 31 * result + raster.contentHashCode()
        result = 31 * result + palette.contentHashCode()
        return result
    }

}