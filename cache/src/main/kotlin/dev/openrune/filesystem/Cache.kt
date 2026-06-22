package dev.openrune.filesystem

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import org.openrs2.cache.Js5CompressionType
import java.nio.file.Path
import kotlin.io.path.absolutePathString

@Deprecated("Legacy compatibility class for openrune. Use CacheLibrary instead.")
class Cache(val library: CacheLibrary) {

    constructor(directory: String) : this(timed(directory))

    val versionTable: ByteArray = library.generateUkeys().array()

    fun indexCount() = library.indices().size

    fun exists(id : Int): Boolean = library.exists(id)

    fun indices() = library.indices().map { it.id }.toIntArray()

    fun sector(index: Int, archive: Int): ByteArray? {
        try {
            val read = library.store.read(index, archive)
            val decompressed = ByteArray(read.readableBytes())
            read.readBytes(decompressed)
            return decompressed
        } catch (e: Exception) {
            return null
        }
    }

    fun archives(index: Int) = library.index(index).archiveIds()

    fun archiveCount(index: Int) = library.index(index).archiveIds().size

    fun lastArchiveId(indexId: Int) = library.index(indexId).last()?.id ?: -1

    fun archiveId(index: Int, name: String) = library.index(index).archiveId(name)

    fun archiveId(index: Int, hash: Int): Int {
        for (archive in library.index(index).archives()) {
            if (archive.hashName == hash) {
                return archive.id
            }
        }
        return -1
    }

    fun files(index: Int, archive: Int) = library.index(index).archive(archive)?.fileIds() ?: IntArray(0)

    fun fileCount(indexId: Int, archiveId: Int) = library.index(indexId).archive(archiveId)?.fileIds()?.size ?: 0

    fun lastFileId(indexId: Int, archive: Int) = library.index(indexId).archive(archive)?.last()?.id ?: -1
    
    fun fileData(
        index: Int,
        archive: Int,
        xtea: IntArray?
    ): Array<ByteArray?>? {
        return library.index(index).archive(archive, xtea)?.files()?.map { file -> file.data }?.toTypedArray()
    }

    fun data(index: Int, archive: Int, file: Int = 0, xtea: IntArray? = null) = library.data(index, archive, file, xtea)

    fun data(index: Int, name: String, xtea: IntArray? = null) = library.data(index, name, xtea)

    fun crc(index: Int): Int = library.index(index).crc

    fun crc(index: Int, archive: Int): Int = library.index(index).archive(archive)?.crc?: -1

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
    ) {
        library.createIndex(
            Js5CompressionType.valueOf(compressionType.name),
            version,
            revision,
            named,
            whirlpool,
            lengths,
            checksums,
            writeReferenceTable,
            id
        )
    }

    fun write(index: Int, archive: Int, file: Int, data: ByteArray, xteas: IntArray? = null) {
        library.put(index, archive, file, data, xteas)
    }

    fun write(index: Int, archive: Int, data: ByteArray, xteas: IntArray? = null) {
        library.put(index, archive, data, xteas)
    }

    fun write(index: Int, archive: String, data: ByteArray, xteas: IntArray? = null) {
        library.put(index, archive, data, xteas)
    }

    fun update(): Boolean {
        library.update()
        return true
    }

    fun close() {
        library.close()
    }

    companion object {
        private val logger = InlineLogger()

        private fun timed(directory: String): CacheLibrary {
            val start = System.currentTimeMillis()
            val library = CacheLibrary(directory)
            logger.info { "Cache read from $directory in ${System.currentTimeMillis() - start}ms" }
            return library
        }

        fun load(path: Path): Cache {
            return load(path.absolutePathString())
        }

        fun load(location : String): Cache {
            return Cache(CacheLibrary(location))
        }
    }
}