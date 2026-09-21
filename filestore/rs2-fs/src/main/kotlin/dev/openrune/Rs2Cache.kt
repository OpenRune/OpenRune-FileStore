package dev.openrune

import dev.openrune.definition.type.SpriteType
import dev.openrune.rs2.OpenRs2ArchiveStore
import dev.openrune.rs2.OpenRs2CacheArchive
import dev.openrune.rs2.Rs2Index
import dev.openrune.rs2.Rs2Manifest
import dev.openrune.rs2.Rs2Sprite
import org.openrs2.cache.Cache
import java.awt.image.BufferedImage
import java.nio.file.Path

/** Wraps an openrs2 [Cache], which transparently supports both the legacy flat-file and JS5 container RS2 cache formats. */
class Rs2Cache private constructor(val cache: Cache, val build: Int? = null) : AutoCloseable {

    companion object {
        fun load(root: Path, build: Int? = null): Rs2Cache = Rs2Cache(Cache.open(root), build)

        /** @param id the opaque archive.openrs2.org cache id - see [loadRemoteFromRev] for a build number instead. */
        fun loadRemote(id: Int, scope: String = "runescape"): Rs2Cache {
            val build = OpenRs2CacheArchive.buildFor(scope, id)
            return Rs2Cache(Cache.open(OpenRs2ArchiveStore(scope, id)), build)
        }

        /** @param build the client build/revision number, e.g. `822`. */
        fun loadRemoteFromRev(build: Int, game: String = "runescape", scope: String = "runescape", environment: String = "live"): Rs2Cache {
            val entry = OpenRs2CacheArchive.findByBuild(build, game, scope, environment)
                ?: throw java.io.FileNotFoundException("No $scope/$game/$environment cache found for build $build")
            return Rs2Cache(Cache.open(OpenRs2ArchiveStore(scope, entry.id)), build)
        }
    }

    fun readSprite(archive: Int = Rs2Index.SPRITES, group: Int): List<BufferedImage> {
        val buf = cache.read(archive, group, 0)
        try {
            return Rs2Sprite.decode(buf)
        } finally {
            buf.release()
        }
    }

    /** Same as [readSprite] but as a [SpriteType], matching OSRS's [dev.openrune.definition.codec.SpriteCodec] output. */
    fun readSpriteType(archive: Int = Rs2Index.SPRITES, group: Int): SpriteType {
        val buf = cache.read(archive, group, 0)
        try {
            return Rs2Sprite.decodeAsType(group, buf)
        } finally {
            buf.release()
        }
    }

    fun spriteGroups(archive: Int = Rs2Index.SPRITES): List<Int> =
        cache.list(archive).asSequence().map { it.id }.sorted().toList()

    fun warnIfArchiveTooEarly(archiveName: String) {
        build?.let { Rs2Manifest.warnIfArchiveTooEarly(archiveName, it) }
    }

    fun warnIfConfigTooEarly(configName: String) {
        build?.let { Rs2Manifest.warnIfConfigTooEarly(configName, it) }
    }

    override fun close() = cache.close()

}
