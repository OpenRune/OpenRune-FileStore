package dev.openrune

import dev.openrune.definition.type.SpriteType
import dev.openrune.rs2.OpenRs2CacheArchive
import dev.openrune.rs2.SpriteDecoder
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import javax.imageio.ImageIO

/**
 * Exercises [OpenRs2ArchiveStore][dev.openrune.rs2.OpenRs2ArchiveStore] end
 * to end against the real archive.openrs2.org service, fetching only the
 * sprite archives/groups it needs rather than a whole cache.
 *
 * [CACHE_ID] is a JS5-era "runescape" (main game, not oldschool) cache with
 * `disk_store_valid: true` in its archive.openrs2.org manifest - i.e. a
 * properly, completely archived snapshot. Its build number is looked up from
 * the manifest at runtime rather than pinned here (see
 * [OpenRs2CacheArchive.buildFor]).
 *
 * Caches with `disk_store_valid: false` (e.g. still-updating snapshots of
 * the live game) are not safe to use here: their master/archive indexes and
 * data groups can genuinely disagree with each other, which
 * [org.openrs2.cache.Cache] correctly rejects as corrupt. Pre-JS5 (roughly
 * build < 400) caches use a flat idx/dat layout with no per-archive JS5
 * index, which this store doesn't support either.
 */
class Rs2CacheSpriteTest {

    @Test
    fun `dumps a sprite from a remote cache to disk`() {
        val outputDir = OUTPUT_DIR.resolve("single").apply { mkdirs() }

        Rs2Cache.loadRemote(scope = SCOPE, id = CACHE_ID).use { cache ->
            val frames = cache.readSprite(group = SPRITE_GROUP)
            assertTrue(frames.isNotEmpty()) { "expected at least one frame" }

            val frame = frames[0]
            assertTrue(frame.width > 0 && frame.height > 0) { "sprite has no dimensions" }

            val outFile = File(outputDir, "$SPRITE_GROUP.png")
            ImageIO.write(frame, "png", outFile)
            assertTrue(outFile.exists() && outFile.length() > 0) { "sprite was not written to disk" }
        }
    }

    @Test
    fun `resolves a cache id from a build number`() {
        val build = OpenRs2CacheArchive.buildFor(SCOPE, CACHE_ID)
        assertNotNull(build) { "no build found in the manifest for $SCOPE/$CACHE_ID" }

        Rs2Cache.loadRemoteFromRev(build = build!!).use { cache ->
            val frames = cache.readSprite(group = SPRITE_GROUP)
            assertTrue(frames.isNotEmpty()) { "expected at least one frame" }
        }
    }

    @Test
    fun `dumps 10 sprites spread across the archive`() {
        val outputDir = OUTPUT_DIR.resolve("spread").apply { mkdirs() }

        Rs2Cache.loadRemote(scope = SCOPE, id = CACHE_ID).use { cache ->
            val groups = cache.spriteGroups()
            assertTrue(groups.size >= 10) { "expected at least 10 sprite groups, found ${groups.size}" }

            val selected = pickSpread(groups, 10)
            assertTrue(selected.first() == groups.first()) { "first selected group should be the very first group" }
            assertTrue(selected.last() == groups.last()) { "last selected group should be the very last group" }

            for (group in selected) {
                val frame = cache.readSprite(group = group).firstOrNull()
                assertTrue(frame != null && frame.width > 0 && frame.height > 0) { "sprite $group decoded with no dimensions" }

                val outFile = File(outputDir, "$group.png")
                ImageIO.write(frame, "png", outFile)
                assertTrue(outFile.exists() && outFile.length() > 0) { "sprite $group was not written to disk" }
            }
        }
    }

    @Test
    fun `loads all sprites into SpriteType via SpriteDecoder`() {
        Rs2Cache.loadRemote(scope = SCOPE, id = CACHE_ID).use { cache ->
            val sprites = mutableMapOf<Int, SpriteType>()
            SpriteDecoder().load(cache, sprites)

            assertTrue(sprites.isNotEmpty()) { "expected at least one sprite to decode" }
            assertTrue(sprites.containsKey(SPRITE_GROUP)) { "expected sprite group $SPRITE_GROUP to be present" }
        }
    }

    /**
     * Picks [count] entries from [sorted]: the first, the last, and [count] -
     * 2 more evenly spaced out (with a little jitter) across what remains.
     */
    private fun pickSpread(sorted: List<Int>, count: Int): List<Int> {
        if (sorted.size <= count) return sorted

        val middleCount = count - 2
        val step = (sorted.size - 1).toDouble() / (middleCount + 1)
        val random = java.util.Random(sorted.size.toLong())

        val middle = (1..middleCount).map { i ->
            val base = (i * step).toInt().coerceIn(1, sorted.size - 2)
            val jitterRange = (step / 2).toInt().coerceAtLeast(1)
            val jittered = (base + random.nextInt(2 * jitterRange + 1) - jitterRange).coerceIn(1, sorted.size - 2)
            sorted[jittered]
        }

        return (listOf(sorted.first()) + middle + listOf(sorted.last())).distinct().sorted()
    }

    private companion object {
        const val SCOPE = "runescape"
        const val CACHE_ID = 600
        const val SPRITE_GROUP = 1

        val OUTPUT_DIR = File("build/sprite-dump")
    }
}
