package dev.openrune

import dev.openrune.cache.tools.SpriteNaming
import dev.openrune.cache.tools.obj.ObjectSpriteFactory
import dev.openrune.cache.tools.obj.dumpObjectSprites
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

class ObjectSpriteDumpTest {
    @Test
    fun `dumps sprites for objects with and without a shape mapping`(@TempDir output: File) {
        val cache = Cache.load(File("../data/cache").toPath())

        val result = dumpObjectSprites(cache, output, revision = REVISION) {
            ids = listOf(TREE, ANVIL, LARGE_DOOR)
            size = 128
            yan = 512
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
    fun `never returns a sprite with nothing drawn on it`() {
        val cache = Cache.load(File("../data/cache").toPath())
        val factory = ObjectSpriteFactory.fromCache(cache, REVISION)

        var rendered = 0
        var empty = 0
        for (id in 0 until 2000) {
            val sprite = factory.obj(id).size(64).create()
            if (sprite == null) {
                empty++
            } else {
                rendered++
                assertTrue(opaqueBounds(sprite).width >= 1) { "object $id came back blank instead of null" }
            }
        }

        assertTrue(rendered > 0) { "nothing rendered at all" }
        assertTrue(empty > 0) { "expected some objects to have no drawable model" }
    }

    @Test
    fun `names from gamevals by default and from display names on request`(@TempDir output: File) {
        val cache = Cache.load(File("../data/cache").toPath())
        val ids = listOf(TREE, ANVIL)

        val gameval = File(output, "gameval")
        dumpObjectSprites(cache, gameval, revision = REVISION) {
            this.ids = ids
            size = 64
            showProgress = false
        }

        val display = File(output, "display")
        dumpObjectSprites(cache, display, revision = REVISION) {
            this.ids = ids
            size = 64
            naming = SpriteNaming.ID_AND_NAME
            showProgress = false
        }

        assertEquals(setOf("tree.png", "viking_anvil.png"), gameval.listFiles()!!.map { it.name }.toSet())
        assertEquals(setOf("1276_Tree.png", "4306_Anvil.png"), display.listFiles()!!.map { it.name }.toSet())
    }

    @Test
    fun `orientation turns the object a quarter at a time and wraps past three`() {
        val cache = Cache.load(File("../data/cache").toPath())
        val factory = ObjectSpriteFactory.fromCache(cache, REVISION)

        fun render(orientation: Int) = factory.obj(LARGE_DOOR).size(64).orientation(orientation).create()!!
            .let { it.getRGB(0, 0, it.width, it.height, null, 0, it.width) }

        assertTrue(!render(0).contentEquals(render(1))) { "orientation 1 matched orientation 0" }
        assertTrue(render(0).contentEquals(render(4))) { "orientation 4 did not wrap back to 0" }
    }

    @Test
    fun `scales proportionally and keeps aspect across sizes`() {
        val cache = Cache.load(File("../data/cache").toPath())
        val factory = ObjectSpriteFactory.fromCache(cache, REVISION)

        val aspects = listOf(64, 256).map { size ->
            val bounds = opaqueBounds(factory.obj(TREE).size(size).create()!!)
            bounds.width.toDouble() / bounds.height
        }

        assertTrue(abs(aspects[0] - aspects[1]) < 0.1) { "aspect drifted across sizes: $aspects" }
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
        const val TREE = 1276
        const val ANVIL = 4306
        const val LARGE_DOOR = 1516
    }
}
