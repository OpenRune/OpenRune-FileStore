package dev.openrune

import dev.openrune.cache.tools.SpriteNaming
import dev.openrune.cache.tools.item.dumpItemSprites
import dev.openrune.filesystem.Cache
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

class ItemSpriteDumpTest {
    @Test
    fun `dumps sprites for plain, noted and stacked items`(@TempDir output: File) {
        val cache = Cache.load(File("../data/cache").toPath())

        val result = dumpItemSprites(cache, output, revision = REVISION) {
            ids = listOf(ABYSSAL_WHIP, NOTED_ABYSSAL_WHIP, COINS)
            size = 64
            autoScaleZoom = true
            naming = SpriteNaming.ID_AND_NAME
            showProgress = false
        }

        assertEquals(3, result.written)
        assertTrue(result.failed.isEmpty()) { "failed to render ${result.failed}" }

        val whip = File(output, "${ABYSSAL_WHIP}_Abyssal_whip.png")
        val notedWhip = File(output, "${NOTED_ABYSSAL_WHIP}_Abyssal_whip.png")
        assertTrue(whip.exists() && notedWhip.exists())
        assertTrue(!whip.readBytes().contentEquals(notedWhip.readBytes()))
    }

    @Test
    fun `fits each item to the requested canvas without distorting it`(@TempDir output: File) {
        val cache = Cache.load(File("../data/cache").toPath())

        listOf(32, 128, 512).forEach { size ->
            val dir = File(output, size.toString())
            dumpItemSprites(cache, dir, revision = REVISION) {
                ids = listOf(FIRE_CAPE, ABYSSAL_WHIP)
                this.size = size
                showProgress = false
            }

            dir.listFiles()!!.forEach { file ->
                val image = ImageIO.read(file)
                assertEquals(size, image.width, file.name)
                assertEquals(size, image.height, file.name)

                val bounds = opaqueBounds(image)
                assertTrue(bounds.width in 1..size && bounds.height in 1..size) {
                    "${file.name} at $size covered ${bounds.width}x${bounds.height}"
                }
                assertTrue(abs(bounds.x - (size - bounds.width - bounds.x)) <= 1) { "${file.name} off centre in x" }
                assertTrue(abs(bounds.y - (size - bounds.height - bounds.y)) <= 1) { "${file.name} off centre in y" }
            }
        }
    }

    private fun opaqueBounds(image: BufferedImage): Rectangle {
        var minX = image.width
        var minY = image.height
        var maxX = -1
        var maxY = -1
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (image.getRGB(x, y) ushr 24 == 0) continue
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
        return Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1)
    }

    private companion object {
        const val REVISION = 240
        const val ABYSSAL_WHIP = 4151
        const val NOTED_ABYSSAL_WHIP = 4152
        const val COINS = 995
        const val FIRE_CAPE = 6570
    }
}
