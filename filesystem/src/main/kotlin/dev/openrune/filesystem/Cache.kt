package dev.openrune.filesystem

import java.nio.file.Path

internal typealias MapFactory = () -> MutableMap<Int, Int>

interface Cache {

    val versionTable: ByteArray

    fun indexCount(): Int

    fun exists(id : Int): Boolean

    fun createIndex(
        compressionType: Compression = Compression.GZIP,
        version: Int = 6,
        revision: Int = 0,
        named: Boolean = false,
        whirlpool: Boolean = false,
        lengths: Boolean = false,
        checksums: Boolean = false,
        writeReferenceTable: Boolean = true,
        id: Int = if (indices().isEmpty()) 0 else indexCount() + 1
    )

    fun indices(): IntArray

    fun sector(index: Int, archive: Int): ByteArray?

    fun archives(index: Int): IntArray

    fun archiveCount(index: Int): Int

    fun lastArchiveId(indexId: Int): Int

    fun archiveId(index: Int, hash: Int): Int

    fun archiveId(index: Int, name: String): Int = archiveId(index, name.hashCode())

    fun files(index: Int, archive: Int): IntArray

    fun fileCount(indexId: Int, archiveId: Int): Int

    fun lastFileId(indexId: Int, archive: Int): Int

    fun data(index: Int, archive: Int, file: Int = 0, xtea: IntArray? = null): ByteArray?

    fun data(index: Int, name: String, xtea: IntArray? = null) = data(index, archiveId(index, name), xtea = xtea)

    fun crc(index: Int): Int

    fun crc(index: Int, archive: Int): Int

    fun write(index: Int, archive: Int, file: Int, data: ByteArray, xteas: IntArray? = null)

    fun write(index: Int, archive: Int, data: ByteArray, xteas: IntArray? = null)

    fun write(index: Int, archive: String, data: ByteArray, xteas: IntArray? = null)

    fun update(): Boolean

    fun close()

    companion object {

        fun load(location : Path): Cache {
            return FileCache.load(location.toFile().absolutePath)
        }

    }
}