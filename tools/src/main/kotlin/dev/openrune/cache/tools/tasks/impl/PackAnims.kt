package dev.openrune.cache.tools.tasks.impl

import dev.openrune.cache.ANIMATIONS
import dev.openrune.cache.tools.incremental.PackUnit
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.getFiles
import dev.openrune.filesystem.Cache
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * Packs `.rsanim` animation packs into the animations index.
 *
 * A pack is every frame of one animation in a single file, written by the OpenRune editor. The
 * frame bytes inside it are untouched — each one is exactly what the animations index stores —
 * so packing is a write per frame with nothing to decode first.
 *
 * The archive to write into is carried in the pack rather than taken from the file name: the
 * editor picks it against the cache it was exported from and checks it's free, and a file that
 * gets renamed on the way here shouldn't silently land somewhere else.
 *
 * The sequence that plays the frames is a separate config — pack it with [PackConfig] from the
 * same bundle's `config.dat`, or from the `frameIds` in its `anim.toml`.
 */
class PackAnims(
    private val animDirectory: File,
) : CacheTask() {

    override fun init(cache: Cache) {
        val packs = getFiles(animDirectory, "rsanim")
        if (packs.isEmpty()) return

        val root = animDirectory.absoluteFile
        val units = packs.map { file ->
            PackUnit(key = file.absoluteFile.relativeTo(root).path.replace('\\', '/'), source = file)
        }

        incremental.run(
            task = this,
            scope = animDirectory.absolutePath,
            label = "Packing Animations",
            cache = cache,
            units = units,
        ) { packCache, unit ->
            packAnim(packCache, unit.sources.single())
        }
    }

    private fun packAnim(cache: Cache, file: File) {
        val buffer = Unpooled.wrappedBuffer(Files.readAllBytes(file.toPath()))
        try {
            val header = readHeader(buffer, file)
            repeat(header.frameCount) {
                val fileId = buffer.readUnsignedShort()
                val length = buffer.readUnsignedShort()
                if (buffer.readableBytes() < length) {
                    error("${file.name} ends mid-frame: frame $fileId wants $length bytes, ${buffer.readableBytes()} left")
                }
                val data = ByteArray(length)
                buffer.readBytes(data)
                cache.write(ANIMATIONS, header.archive, fileId, data)
            }
        } finally {
            buffer.release()
        }
    }

    private data class Header(val archive: Int, val frameCount: Int)

    private fun readHeader(buffer: ByteBuf, file: File): Header {
        if (buffer.readableBytes() < HEADER_BYTES) {
            error("${file.name} is too short to be an animation pack")
        }

        val magic = buffer.readCharSequence(MAGIC.length, StandardCharsets.US_ASCII).toString()
        if (magic != MAGIC) {
            error("${file.name} is not an animation pack: expected $MAGIC, found $magic")
        }

        val version = buffer.readUnsignedByte().toInt()
        if (version != VERSION) {
            error("${file.name} is animation pack version $version; this packer reads version $VERSION")
        }

        return Header(archive = buffer.readUnsignedShort(), frameCount = buffer.readUnsignedShort())
    }

    private companion object {
        const val MAGIC = "RSAN"
        const val VERSION = 1

        /** Magic, version, archive, frame count. */
        const val HEADER_BYTES = 4 + 1 + 2 + 2
    }
}
