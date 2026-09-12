package dev.openrune

import dev.openrune.cache.tools.SpriteNaming
import dev.openrune.cache.tools.npc.NpcSpriteFactory
import dev.openrune.cache.tools.npc.dumpNpcSprites
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

class NpcSpriteDumpTest {
    @Test
    fun `dumps sprites for single and multi model npcs`(@TempDir output: File) {
        val cache = Cache.load(File("../data/cache").toPath())

        val result = dumpNpcSprites(cache, output, revision = REVISION) {
            ids = listOf(TOOL_LEPRECHAUN, ABYSSAL_DEMON, KREE_ARRA)
            size = 128
            naming = SpriteNaming.ID_AND_NAME
            showProgress = false
        }

        assertEquals(3, result.written)
        assertTrue(result.failed.isEmpty()) { "failed to render ${result.failed}" }

        output.listFiles()!!.forEach { file ->
            val image = ImageIO.read(file)
            assertEquals(128, image.width, file.name)
            assertEquals(128, image.height, file.name)

            val bounds = opaqueBounds(image)
            assertTrue(bounds.width in 1..128 && bounds.height in 1..128) { "${file.name} covered nothing" }
            assertTrue(abs(bounds.x - (128 - bounds.width - bounds.x)) <= 1) { "${file.name} off centre in x" }
            assertTrue(abs(bounds.y - (128 - bounds.height - bounds.y)) <= 1) { "${file.name} off centre in y" }
        }
    }

    @Test
    fun `scales proportionally and keeps aspect across sizes`() {
        val cache = Cache.load(File("../data/cache").toPath())
        val factory = NpcSpriteFactory.fromCache(cache, REVISION)

        val aspects = listOf(64, 256).map { size ->
            val bounds = opaqueBounds(factory.npc(KREE_ARRA).size(size).create()!!)
            bounds.width.toDouble() / bounds.height
        }

        assertTrue(abs(aspects[0] - aspects[1]) < 0.1) { "aspect drifted across sizes: $aspects" }
    }

    @Test
    fun `renders chatheads from the chathead models`() {
        val cache = Cache.load(File("../data/cache").toPath())
        val factory = NpcSpriteFactory.fromCache(cache, REVISION)

        val body = factory.npc(TOOL_LEPRECHAUN).size(128).create()!!
        val head = factory.npc(TOOL_LEPRECHAUN).size(128).chathead(true).create()!!

        assertTrue(opaqueBounds(head).width > 1)
        assertTrue(!pixels(body).contentEquals(pixels(head))) { "chathead matched the body render" }
    }

    private fun pixels(image: BufferedImage): IntArray =
        image.getRGB(0, 0, image.width, image.height, null, 0, image.width)

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
        const val TOOL_LEPRECHAUN = 0
        const val ABYSSAL_DEMON = 415
        const val KREE_ARRA = 3162
    }
}
