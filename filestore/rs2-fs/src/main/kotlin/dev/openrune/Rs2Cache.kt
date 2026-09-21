package dev.openrune

import dev.openrune.definition.type.SpriteType
import dev.openrune.rs2.OpenRs2ArchiveStore
import dev.openrune.rs2.OpenRs2CacheArchive
import dev.openrune.rs2.Rs2Index
import dev.openrune.rs2.Rs2Sprite
import org.openrs2.cache.Cache
import java.awt.image.BufferedImage
import java.nio.file.Path

/**
 * Wraps an openrs2 [Cache], which transparently supports both the legacy
 * flat-file (.idx/.dat2) and JS5 container RS2 cache formats.
 */
class Rs2Cache private constructor(val cache: Cache) : AutoCloseable {

    companion object {
        fun load(root: Path): Rs2Cache = Rs2Cache(Cache.open(root))

        /**
         * Loads a cache backed by [OpenRs2ArchiveStore], fetching only the
         * specific archives/groups that are actually read instead of
         * downloading the whole (potentially huge, for RS3) cache up front.
         *
         * @param id the opaque archive.openrs2.org cache id (not a build/
         * revision number - see [loadRemoteFromRev] if that's what you have).
         */
        fun loadRemote(id: Int,scope: String = "runescape"): Rs2Cache =
            Rs2Cache(Cache.open(OpenRs2ArchiveStore(scope, id)))

        /**
         * Resolves [build] (a client build/revision number, e.g. `822`) to a
         * cache id via [OpenRs2CacheArchive] and loads it with [loadRemote].
         * This is the entry point for "I just want revision N" callers who
         * don't want to know or track opaque archive.openrs2.org cache ids
         * themselves.
         */
        fun loadRemoteFromRev(build: Int, game: String = "runescape", scope: String = "runescape", environment: String = "live"): Rs2Cache {
            val entry = OpenRs2CacheArchive.findByBuild(build, game, scope, environment)
                ?: throw java.io.FileNotFoundException("No $scope/$game/$environment cache found for build $build")
            return loadRemote(entry.id,scope)
        }
    }

    /**
     * Decodes the sprite stored at [archive]/[group] into its constituent frames.
     */
    fun readSprite(archive: Int = Rs2Index.SPRITES, group: Int): List<BufferedImage> {
        val buf = cache.read(archive, group, 0)
        try {
            return Rs2Sprite.decode(buf)
        } finally {
            buf.release()
        }
    }

    /**
     * Decodes the sprite stored at [archive]/[group] as a [SpriteType] - the
     * same container [dev.openrune.definition.codec.SpriteCodec] (OSRS)
     * produces, so callers get one API regardless of cache generation.
     */
    fun readSpriteType(archive: Int = Rs2Index.SPRITES, group: Int): SpriteType {
        val buf = cache.read(archive, group, 0)
        try {
            return Rs2Sprite.decodeAsType(group, buf)
        } finally {
            buf.release()
        }
    }

    /**
     * Lists every sprite group id present in [archive], sorted ascending.
     */
    fun spriteGroups(archive: Int = Rs2Index.SPRITES): List<Int> =
        cache.list(archive).asSequence().map { it.id }.sorted().toList()

    override fun close() = cache.close()

}
