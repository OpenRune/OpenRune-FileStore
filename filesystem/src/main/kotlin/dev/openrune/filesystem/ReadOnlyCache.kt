package dev.openrune.filesystem

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.filesystem.util.compress.DecompressionContext
import dev.openrune.filesystem.util.readByte
import dev.openrune.filesystem.util.readInt
import dev.openrune.filesystem.util.readUnsignedByte
import dev.openrune.filesystem.util.secure.VersionTableBuilder
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.Arrays

/**
 * [Cache] which efficiently stores information about its indexes, archives and files.
 */
abstract class ReadOnlyCache(
    indexCount: Int,
    mapFactory: MapFactory
) : Cache {
    val indices: IntArray = IntArray(indexCount) { it }
    val archives: Array<IntArray?> = arrayOfNulls(indexCount)
    val fileCounts: Array<IntArray?> = arrayOfNulls(indexCount)
    val files: Array<Array<IntArray?>?> = arrayOfNulls(indexCount)

    private val fileIdKinds: Array<ByteArray?> = arrayOfNulls(indexCount)
    private val hashes: MutableMap<Int, Int> = mapFactory()

    override lateinit var versionTable: ByteArray

    @Suppress("UNCHECKED_CAST")
    internal fun fileData(
        context: DecompressionContext,
        main: RandomAccessFile,
        mainLength: Long,
        indexTable: ByteArray,
        indexId: Int,
        archiveId: Int,
        xteas: Map<Int, IntArray>?,
        sectors: Array<Array<ByteArray?>?>? = null
    ): Array<ByteArray?>? {
        val keys = if (xteas != null && indexId == MAPS) xteas[archiveId] else null
        return fileData(context, main, mainLength, indexTable, indexId, archiveId, keys, sectors)
    }

    @Suppress("UNCHECKED_CAST")
    internal fun fileData(
        context: DecompressionContext,
        main: RandomAccessFile,
        mainLength: Long,
        indexTable: ByteArray,
        indexId: Int,
        archiveId: Int,
        xtea: IntArray?,
        sectors: Array<Array<ByteArray?>?>? = null
    ): Array<ByteArray?>? {
        val fileCounts = fileCounts[indexId] ?: return null
        val fileIds = files[indexId] ?: return null
        val fileCount = fileCounts.getOrNull(archiveId) ?: return null
        val sectorData = readSector(main, mainLength, indexTable, indexId, archiveId) ?: return null
        if (sectors != null) {
            sectors[indexId]!![archiveId] = sectorData
        }
        val keys = if (xtea != null && indexId == MAPS) xtea else null
        val decompressed = context.decompress(sectorData, keys) ?: return null
        if (fileCount == 1) {
            val fileId = fileIds[archiveId]?.last() ?: return null
            return Array(fileId + 1) {
                if (it == fileId) decompressed else null
            }
        }

        val reader = ByteBuffer.wrap(decompressed)
        val rawArray = reader.array()
        var fileDataSizesOffset = decompressed.size
        val chunkSize: Int = rawArray[--fileDataSizesOffset].toInt() and 0xFF
        fileDataSizesOffset -= chunkSize * (fileCount * 4)
        val offsets = IntArray(fileCount)
        reader.position(fileDataSizesOffset)
        for (i in 0 until chunkSize) {
            var previousLength = 0
            for (fileIndex in 0 until fileCount) {
                previousLength += reader.readInt()
                offsets[fileIndex] += previousLength
            }
        }
        val archiveFiles = Array(fileCount) { index ->
            val array = ByteArray(offsets[index])
            offsets[index] = 0
            array
        }
        var offset = 0
        reader.position(fileDataSizesOffset)
        for (i in 0 until chunkSize) {
            var length = 0
            for (fileIndex in 0 until fileCount) {
                val read = reader.readInt()
                val fileData = archiveFiles[fileIndex]
                length += read
                System.arraycopy(rawArray, offset, fileData, offsets[fileIndex], length)
                offset += length
                offsets[fileIndex] += length
            }
        }
        return archiveFiles as Array<ByteArray?>
    }

    internal fun archiveData(
        context: DecompressionContext,
        main: RandomAccessFile,
        length: Long,
        index255Table: ByteArray,
        indexId: Int,
        versionTable: VersionTableBuilder?,
        sectors: Array<ByteArray?>? = null
    ): Int {
        val archiveSector = readSector(main, length, index255Table, 255, indexId)
        if (sectors != null) {
            sectors[indexId] = archiveSector
        }
        if (archiveSector == null) {
            logger.trace { "Empty index $indexId." }
            versionTable?.skip(indexId)
            return -1
        }
        versionTable?.sector(indexId, archiveSector)
        val decompressed = context.decompress(archiveSector) ?: return -1
        val reader = ByteBuffer.wrap(decompressed)
        val version = reader.readUnsignedByte()
        if (version < 5 || version > 7) {
            throw RuntimeException("Unknown version: $version")
        }
        if (version >= 6) {
            val revision = reader.readInt()
            versionTable?.revision(indexId, revision)
        }
        val flags = reader.readByte()
        val archiveCount = reader.readSmart(version)
        var previous = 0
        var highest = 0
        val archiveIds = IntArray(archiveCount) {
            val archiveId = reader.readSmart(version) + previous
            previous = archiveId
            if (archiveId > highest) {
                highest = archiveId
            }
            archiveId
        }
        archives[indexId] = archiveIds
        if (flags and NAME_FLAG != 0) {
            for (i in 0 until archiveCount) {
                val archiveId = archiveIds[i]
                hashes[reader.readInt()] = archiveId
            }
        }
        reader.skip(archiveCount * 4) // Crc
        if (flags and HASH_FLAG != 0) {
            reader.skip(archiveCount * 4)
        }
        if (flags and WHIRLPOOL_FLAG != 0) {
            reader.skip(archiveCount * WHIRLPOOL_SIZE)
        }
        if (flags and SIZE_FLAG != 0) {
            reader.skip(archiveCount * 8) // Uncompressed/compressed size
        }
        reader.skip(archiveCount * 4) // Version
        val archiveSizes = IntArray(highest + 1)
        for (i in 0 until archiveCount) {
            val id = archiveIds[i]
            val size = reader.readSmart(version)
            archiveSizes[id] = size
        }
        fileCounts[indexId] = archiveSizes
        val fileIds = arrayOfNulls<IntArray>(highest + 1)
        files[indexId] = fileIds
        val kinds = ByteArray(highest + 1) { KIND_UNSORTED }
        fileIdKinds[indexId] = kinds
        for (i in 0 until archiveCount) {
            var fileId = 0
            val archiveId = archiveIds[i]
            val fileCount = archiveSizes[archiveId]
            val ids = IntArray(fileCount) {
                fileId += reader.readSmart(version)
                fileId
            }
            fileIds[archiveId] = ids
            kinds[archiveId] = classify(ids)
        }
        return highest
    }

    fun fileIndex(index: Int, archive: Int, file: Int): Int {
        val ids = files.getOrNull(index)?.getOrNull(archive) ?: return -1
        return when (fileIdKinds.getOrNull(index)?.getOrNull(archive)?.toInt() ?: KIND_UNSORTED.toInt()) {
            KIND_IDENTITY.toInt() -> if (file in ids.indices) file else -1
            KIND_SORTED.toInt() -> Arrays.binarySearch(ids, file).let { if (it < 0) -1 else it }
            else -> ids.indexOf(file)
        }
    }

    override fun indexCount() = indices.size

    override fun indices() = indices

    override fun exists(id: Int) = indices.getOrNull(id) != null

    override fun archives(index: Int) = archives.getOrNull(index) ?: IntArray(0)

    override fun archiveCount(index: Int) = archives.size

    override fun lastArchiveId(indexId: Int) = archives.getOrNull(indexId)?.lastOrNull() ?: -1

    override fun archiveId(index: Int, hash: Int) = hashes[hash] ?: -1

    override fun files(index: Int, archive: Int) = files.getOrNull(index)?.getOrNull(archive) ?: IntArray(0)

    override fun fileCount(indexId: Int, archiveId: Int) = fileCounts.getOrNull(indexId)?.getOrNull(archiveId) ?: 0

    override fun lastFileId(indexId: Int, archive: Int) = files.getOrNull(indexId)?.getOrNull(archive)?.lastOrNull() ?: -1

    override fun write(index: Int, archive: Int, file: Int, data: ByteArray, xteas: IntArray?) {
        throw UnsupportedOperationException("Read only cache.")
    }

    override fun write(index: Int, archive: Int, data: ByteArray, xteas: IntArray?) {
        throw UnsupportedOperationException("Read only cache.")
    }

    override fun write(index: Int, archive: String, data: ByteArray, xteas: IntArray?) {
        throw UnsupportedOperationException("Read only cache.")
    }

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
        throw UnsupportedOperationException("Read only cache.")
    }

    override fun update(): Boolean {
        return false
    }

    override fun close() {
    }

    companion object {
        private const val MAPS = 5
        private val logger = InlineLogger()
        private const val NAME_FLAG = 0x1
        private const val WHIRLPOOL_FLAG = 0x2
        private const val SIZE_FLAG: Int = 0x4
        private const val HASH_FLAG: Int = 0x8

        private const val KIND_UNSORTED: Byte = 0
        private const val KIND_SORTED: Byte = 1
        private const val KIND_IDENTITY: Byte = 2

        private fun classify(ids: IntArray): Byte {
            var identity = true
            for (i in ids.indices) {
                if (ids[i] != i) {
                    identity = false
                    break
                }
            }
            if (identity) return KIND_IDENTITY

            for (i in 1 until ids.size) {
                if (ids[i] <= ids[i - 1]) return KIND_UNSORTED
            }
            return KIND_SORTED
        }

        const val INDEX_SIZE = 6
        const val WHIRLPOOL_SIZE = 64
        private const val SECTOR_SIZE = 520
        private const val SECTOR_HEADER_SIZE_SMALL = 8
        private const val SECTOR_DATA_SIZE_SMALL = 512
        private const val SECTOR_HEADER_SIZE_BIG = 10
        private const val SECTOR_DATA_SIZE_BIG = 510

        private fun ByteBuffer.readSmart(version: Int) = if (version >= 7) readBigSmart() else readUnsignedShort()

        private fun ByteBuffer.readUnsignedShort() = (readUnsignedByte() shl 8) or readUnsignedByte()

        private fun ByteBuffer.readUnsignedMedium() = (readUnsignedByte() shl 16) or (readUnsignedByte() shl 8) or readUnsignedByte()

        private fun ByteBuffer.readBigSmart(): Int {
            val peek = readByte()
            return if (peek < 0) {
                ((peek shl 24) or (readUnsignedByte() shl 16) or (readUnsignedByte() shl 8) or readUnsignedByte()) and 0x7fffffff
            } else {
                val value = (peek shl 8) or readUnsignedByte()
                if (value == 32767) -1 else value
            }
        }

        private fun ByteBuffer.skip(amount: Int) = position(position() + amount)

        private fun ByteArray.readUnsignedMedium(offset: Int) =
            ((this[offset].toInt() and 0xFF) shl 16) or
                ((this[offset + 1].toInt() and 0xFF) shl 8) or
                (this[offset + 2].toInt() and 0xFF)

        internal fun readIndexTable(raf: RandomAccessFile): ByteArray {
            val table = ByteArray(raf.length().toInt())
            raf.seek(0)
            raf.readFully(table)
            return table
        }

        private fun readAt(channel: FileChannel, target: ByteArray, wanted: Int, position: Long): Boolean {
            val buffer = ByteBuffer.wrap(target, 0, wanted)
            var offset = position
            while (buffer.hasRemaining()) {
                val read = channel.read(buffer, offset)
                if (read <= 0) return false
                offset += read
            }
            return true
        }

        internal fun readSector(mainFile: RandomAccessFile, length: Long, table: ByteArray, indexId: Int, sectorId: Int): ByteArray? {
            val entry = sectorId * INDEX_SIZE
            if (sectorId < 0 || entry + INDEX_SIZE > table.size) {
                return null
            }
            val bigSector = sectorId > 65535
            val sectorData = ByteArray(SECTOR_SIZE)
            val buffer = ByteBuffer.wrap(sectorData)
            val sectorSize = table.readUnsignedMedium(entry)
            var sectorPosition = table.readUnsignedMedium(entry + 3)
            val sectorLimit = length / SECTOR_SIZE
            if (sectorSize < 0 || sectorPosition <= 0 || sectorPosition > sectorLimit) {
                return null
            }
            var read = 0
            var chunk = 0
            val channel = mainFile.channel
            val sectorHeaderSize = if (bigSector) SECTOR_HEADER_SIZE_BIG else SECTOR_HEADER_SIZE_SMALL
            val sectorDataSize = if (bigSector) SECTOR_DATA_SIZE_BIG else SECTOR_DATA_SIZE_SMALL
            val output = ByteArray(sectorSize)
            while (read < sectorSize) {
                if (sectorPosition == 0) {
                    return null
                }
                var requiredToRead = sectorSize - read
                if (requiredToRead > sectorDataSize) {
                    requiredToRead = sectorDataSize
                }
                val wanted = requiredToRead + sectorHeaderSize
                if (!readAt(channel, sectorData, wanted, sectorPosition.toLong() * SECTOR_SIZE)) {
                    return null
                }
                buffer.position(0)
                val id = if (bigSector) buffer.readInt() else buffer.readUnsignedShort()
                val sectorChunk = buffer.readUnsignedShort()
                val sectorNextPosition = buffer.readUnsignedMedium()
                val sectorIndex = buffer.readUnsignedByte()
                if (sectorIndex != indexId || id != sectorId || sectorChunk != chunk) {
                    return null
                } else if (sectorNextPosition < 0 || sectorNextPosition > sectorLimit) {
                    return null
                }
                System.arraycopy(sectorData, sectorHeaderSize, output, read, requiredToRead)
                read += requiredToRead
                sectorPosition = sectorNextPosition
                chunk++
            }
            return output
        }
    }

}