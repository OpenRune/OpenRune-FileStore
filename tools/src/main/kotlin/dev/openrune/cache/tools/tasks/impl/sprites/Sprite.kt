package dev.openrune.cache.tools.tasks.impl.sprites

import java.awt.image.BufferedImage

data class Sprite(val offsetX: Int, val offsetY: Int, val image: BufferedImage) {
    val width: Int get() = image.width
    val height: Int get() = image.height
    fun getRGB(x: Int, y: Int): Int = image.getRGB(x, y)
    fun setRGB(x: Int, y: Int, rgb: Int) {
        image.setRGB(x, y, rgb)
    }

    fun averageColorForPixels() : Int {
        var redTotal = 0
        var greenTotal = 0
        var blueTotal = 0
        var totalPixels = 0

        val scaledImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
        val g = scaledImage.createGraphics()
        g.drawImage(image, 0, 0, 1, 1, null)
        g.dispose()

        for (y in 0 until scaledImage.height) {
            for (x in 0 until scaledImage.width) {
                val pixel = scaledImage.getRGB(x, y)
                if (pixel == 0xff00ff) continue

                redTotal += (pixel shr 16) and 0xff
                greenTotal += (pixel shr 8) and 0xff
                blueTotal += pixel and 0xff
                totalPixels++
            }
        }

        if (totalPixels == 0) return 0
        val averageRed = redTotal / totalPixels
        val averageGreen = greenTotal / totalPixels
        val averageBlue = blueTotal / totalPixels

        var averageRGB = (averageRed shl 16) + (averageGreen shl 8) + averageBlue
        if (averageRGB == 0) averageRGB = 1

        return averageRGB
    }

}