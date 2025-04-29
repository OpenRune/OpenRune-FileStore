package dev.openrune.cache

import com.displee.cache.CacheLibrary
import com.displee.compress.CompressionType
import dev.openrune.filesystem.Cache
import dev.openrune.filesystem.Compression
import io.github.oshai.kotlinlogging.KotlinLogging

class CacheDelegate(val library: CacheLibrary) : Cache {

    constructor(directory: String) : this(timed(directory))

    override val versionTable: ByteArray = library.generateUkeys()

    override fun indexCount() = library.indices().size

    override fun exists(id : Int): Boolean = library.exists(id)

    override fun indices() = library.indices().map { it.id }.toIntArray()

    override fun sector(index: Int, archive: Int): ByteArray? {
        return if (index == 255) {
            library.index255
        } else {
            library.index(index)
        }?.readArchiveSector(archive)?.data
    }

    override fun archives(index: Int) = library.index(index).archiveIds()

    override fun archiveCount(index: Int) = library.index(index).archiveIds().size

    override fun lastArchiveId(indexId: Int) = library.index(indexId).last()?.id ?: -1

    override fun archiveId(index: Int, name: String) = library.index(index).archiveId(name)

    override fun archiveId(index: Int, hash: Int): Int {
        for (archive in library.index(index).archives()) {
            if (archive.hashName == hash) {
                return archive.id
            }
        }
        return -1
    }

    override fun files(index: Int, archive: Int) = library.index(index).archive(archive)?.fileIds() ?: IntArray(0)

    override fun fileCount(indexId: Int, archiveId: Int) = library.index(indexId).archive(archiveId)?.fileIds()?.size ?: 0

    override fun lastFileId(indexId: Int, archive: Int) = library.index(indexId).archive(archive)?.last()?.id ?: -1

    override fun data(index: Int, archive: Int, file: Int, xtea: IntArray?) = library.data(index, archive, file, xtea)

    override fun data(index: Int, name: String, xtea: IntArray?) = library.data(index, name, xtea)

    override fun crc(index: Int): Int = library.index(index).crc

    override fun crc(index: Int, archive: Int): Int = library.index(index).archive(archive)?.crc?: -1

    override fun createIndex(
        compressionType: Compression,
        version: Int,
        revision: Int,
        named: Boolean,
        whirlpool: Boolean,
        lengths: Boolean,
        checksums: Boolean,
        writeReferenceTable: Boolean,
        id: Int
    ) {
        library.createIndex(
            CompressionType.valueOf(compressionType.name),
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

    override fun write(index: Int, archive: Int, file: Int, data: ByteArray, xteas: IntArray?) {
        library.put(index, archive, file, data, xteas)
    }

    override fun write(index: Int, archive: Int, data: ByteArray, xteas: IntArray?) {
        library.put(index, archive, data, xteas)
    }

    override fun write(index: Int, archive: String, data: ByteArray, xteas: IntArray?) {
        library.put(index, archive, data, xteas)
    }

    override fun update(): Boolean {
        library.update()
        return true
    }

    override fun close() {
        library.close()
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        private fun timed(directory: String): CacheLibrary {
            val start = System.currentTimeMillis()
            val library = CacheLibrary(directory)
            logger.info { "Cache read from $directory in ${System.currentTimeMillis() - start}ms" }
            return library
        }
    }
}