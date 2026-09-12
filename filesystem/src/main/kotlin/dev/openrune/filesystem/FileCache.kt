package dev.openrune.filesystem

import dev.openrune.filesystem.util.compress.DecompressionContext
import dev.openrune.filesystem.util.secure.VersionTableBuilder
import java.io.File
import java.io.RandomAccessFile

/**
 * [Cache] which reads data directly from file
 * Average read speeds, fast loading and low but variable memory usage.
 */
class FileCache(
    private val main: RandomAccessFile,
    private val index255: RandomAccessFile,
    private val indexes: Array<RandomAccessFile?>,
    indexCount: Int,
    val xteas: Map<Int, IntArray>?,
    mapFactory: MapFactory
) : ReadOnlyCache(indexCount, mapFactory) {

    private val dataCache = object : LinkedHashMap<Int, Array<ByteArray?>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Array<ByteArray?>>): Boolean {
            return size > 12
        }
    }
    private val sectorCache = object : LinkedHashMap<Int, ByteArray?>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, ByteArray?>): Boolean {
            return size > 12
        }
    }
    private val length = main.length()
    private val context = DecompressionContext()

    private var lastArchiveHash = -1
    private var lastArchiveFiles: Array<ByteArray?>? = null

    /** Index files read whole up front, so an archive read costs no seek on the index file. */
    private val index255Table = readIndexTable(index255)
    private val indexTables: Array<ByteArray?> = Array(indexes.size) { id ->
        indexes[id]?.let { readIndexTable(it) }
    }

    private fun indexTable(index: Int): ByteArray? =
        if (index == 255) index255Table else indexTables.getOrNull(index)

    override fun sector(index: Int, archive: Int): ByteArray? {
        val table = indexTable(index) ?: return null
        return sectorCache.getOrPut(index + (archive shl 6)) {
            readSector(main, length, table, index, archive)
        }
    }

    override fun data(index: Int, archive: Int, file: Int, xtea: IntArray?): ByteArray? {
        val matchingIndex = fileIndex(index, archive, file)
        if (matchingIndex == -1) {
            return null
        }
        val files = fileData(index, archive, xtea) ?: return null
        return files.getOrNull(matchingIndex)
    }

    override fun fileData(index: Int, archive: Int, xtea: IntArray?): Array<ByteArray?>? {
        val hash = index + (archive shl 6)
        // Definition loads hammer one archive at a time, so skip the map lookup and its boxed key.
        if (hash == lastArchiveHash) {
            return lastArchiveFiles
        }
        val files = dataCache.getOrPut(hash) {
            val table = indexTables.getOrNull(index) ?: return null
            fileData(context, main, length, table, index, archive, xtea) ?: return null
        }
        lastArchiveHash = hash
        lastArchiveFiles = files
        return files
    }

    override fun crc(index: Int): Int {
        TODO("Not yet implemented")
    }

    override fun crc(index: Int, archive: Int): Int {
        TODO("Not yet implemented")
    }

    override fun close() {
        main.close()
        index255.close()
        for (file in indexes) {
            file?.close()
        }
    }

    companion object : CacheLoader {
        const val CACHE_FILE_NAME = "main_file_cache"

        operator fun invoke(path: String, xteas: Map<Int, IntArray>? = null): Cache {
            return load(path, xteas)
        }

        /**
         * Create [RandomAccessFile]'s for each index file, load only the archive data into memory
         */
        override fun load(
            path: String,
            mainFile: File,
            main: RandomAccessFile,
            index255File: File,
            index255: RandomAccessFile,
            indexCount: Int,
            versionTable: VersionTableBuilder?,
            xteas: Map<Int, IntArray>?,
            threadUsage: Double,
            mapFactory: MapFactory
        ): Cache {
            val length = mainFile.length()
            val context = DecompressionContext()
            val indices = Array(indexCount) { indexId ->
                val file = File(path, "$CACHE_FILE_NAME.idx$indexId")
                if (file.exists()) RandomAccessFile(file, "r") else null
            }
            val cache = FileCache(main, index255, indices, indexCount, xteas, mapFactory)
            for (indexId in 0 until indexCount) {
                cache.archiveData(context, main, length, cache.index255Table, indexId, versionTable)
            }
            cache.versionTable = versionTable?.build() ?: ByteArray(0)
            return cache
        }
    }
}