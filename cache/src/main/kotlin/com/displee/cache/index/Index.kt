package com.displee.cache.index

import com.displee.cache.CacheLibrary
import com.displee.cache.ProgressListener
import com.displee.cache.index.archive.Archive
import com.displee.cache.index.archive.file.File
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.openrs2.buffer.crc32
import org.openrs2.buffer.readUnsignedIntSmart
import org.openrs2.buffer.use
import org.openrs2.buffer.writeUnsignedIntSmart
import org.openrs2.cache.Js5Compression
import org.openrs2.cache.Js5CompressionType
import org.openrs2.crypto.SymmetricKey
import org.openrs2.crypto.Whirlpool.Companion.DIGESTBYTES
import org.openrs2.crypto.whirlpool
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class Index(private val origin: CacheLibrary, val id: Int) : Comparable<Index> {

    var crc = 0
    var whirlpool: ByteArray? = null
    var compressionType: Js5CompressionType = Js5CompressionType.UNCOMPRESSED
    private var cached = false

    var revision = 0
    private var mask = 0x0
    private var needUpdate = false
    private var archives: SortedMap<Int, Archive> = TreeMap()
    private var archiveNames = mutableListOf<Int>()

    var version = 0

    val info get() = "Index[id=" + id + ", archives=" + archives.size + ", compression=" + compressionType + "]"

    init {
        init()
    }

    private fun init() {
        if (id < 0 || id >= 255) {
            return
        }
        try {
            val archiveSector = origin.store.read(255, id)
            crc = archiveSector.crc32()
            whirlpool = archiveSector.whirlpool()
            val typeId = archiveSector.getByte(archiveSector.readerIndex()).toInt() and 0xFF
            compressionType = Js5CompressionType.entries.getOrNull(typeId)
                ?: throw IOException("Invalid compression type: $typeId")
            val decompressed: ByteBuf = Js5Compression.uncompress(archiveSector)
            read(decompressed)
        } catch (e: Exception) {
            //TODO: before we returned if there was no data. Now we throw an exception.
            //TODO: We should handle this better.
            return
        }
    }

    fun cache() {
        check(origin.store.exists(this.id)) { "Index is closed." }
        if (cached) {
            return
        }
        archives.values.forEach {
            try {
                archive(it.id, it.xtea, false)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
        cached = true
    }

    fun unCache() {
        for (archive in archives()) {
            archive.restore()
        }
        cached = false
    }

    @JvmOverloads
    fun update(listener: ProgressListener? = null): Boolean {
        if(this.id == 255)
            throw IllegalAccessException("Not allowed.")
        check(origin.store.exists(this.id)) { "Index is closed." }
        val flaggedArchives = flaggedArchives()
        var i = 0.0
        flaggedArchives.forEach {
            i++
            if (it.autoUpdateRevision) {
                it.revision++
            }
            it.unFlag()
            listener?.notify((i / flaggedArchives.size) * 0.80, "Repacking archive ${it.id}...")
            val key =
                if (it.xtea != null && (it.xtea!![0] != 0 || it.xtea!![1] != 0 || it.xtea!![2] != 0 || 0 != it.xtea!![3])) {
                    SymmetricKey.fromIntArray(it.xtea!!)
                } else {
                    SymmetricKey.ZERO
                }
            val input = packArchive(it.files)
            val byteBuf = Js5Compression.compress(input, it.compressionType, key)
            it.crc = byteBuf.crc32()
            it.whirlpool = byteBuf.whirlpool()
            if (it.revision != -1) {
                byteBuf.writeShort(it.revision)
            }
            try {
                origin.store.write(this.id, it.id, byteBuf)
            } catch (e: Exception) {
                System.err.println("Unable to write data to archive sector. Your cache may be corrupt.")
                e.printStackTrace()
            }
            if (origin.clearDataAfterUpdate) {
                it.restore()
            }
        }
        listener?.notify(0.85, "Updating checksum table for index $id...")
        if (flaggedArchives.isNotEmpty() && !flagged()) {
            flag()
        }
        if (flagged()) {
            unFlag()
            revision++
            origin.alloc.buffer().use { uncompressed ->
                write(uncompressed)

                Js5Compression.compress(uncompressed, compressionType).use { compressed ->
                    crc = compressed.crc32()
                    whirlpool = compressed.whirlpool()
                    origin.store.write(255, this.id, compressed)
                }
            }
        }
        listener?.notify(1.0, "Successfully updated index $id.")
        origin.store.flush()
        return true
    }

    fun fixCRCs(update: Boolean) {
        check(origin.store.exists(this.id)) { "Index is closed." }
        val archiveIds = archiveIds()
        var flag = false
        for (i in archiveIds) {
            try {
                val sector = origin.store.read(id, i)
                val correctCRC = sector.crc32(sector.readerIndex(), sector.readableBytes() - 2)
                val archive = archive(i) ?: continue
                val currentCRC = archive.crc
                if (currentCRC == correctCRC) {
                    continue
                }
                println("Incorrect CRC in index $id -> archive $i, current_crc=$currentCRC, correct_crc=$correctCRC")
                archive.flag()
                flag = true
            } catch (e: Exception) {
                continue
            }
        }
        try {
            val sectorData = origin.store.read(255, id)
            val indexCRC = sectorData.crc32()
            if (crc != indexCRC) {
                flag = true
            }
            if (flag && update) {
                update()
            } else if (!flag) {
                println("No invalid CRCs found.")
                return
            }
            unCache()
        } catch (e: Exception) {
            return
        }
    }

    /**
     * Clear the archives.
     */
    fun clear() {
        archives.clear()
        crc = 0
        whirlpool = ByteArray(DIGESTBYTES)
    }

    fun flaggedArchives(): Array<Archive> {
        return archives.values.filter { it.flagged() }.toTypedArray()
    }

    override fun toString(): String {
        return "Index $id"
    }

    override fun compareTo(other: Index): Int {
        return id.compareTo(other.id)
    }

    fun read(buffer: ByteBuf) {
        version = buffer.readUnsignedByte().toInt()
        if (version < 5 || version > 7) {
            throw RuntimeException("Unknown version: $version")
        }
        revision = if (version >= 6) buffer.readInt() else 0
        mask = buffer.readUnsignedByte().toInt()
        val named = mask and FLAG_NAME != 0
        val whirlpool = mask and FLAG_WHIRLPOOL != 0
        val lengths = mask and FLAG_LENGTHS != 0
        val checksums = mask and FLAG_CHECKSUMS != 0

        val readFun: () -> (Int) = if (version >= 7) {
            {
                buffer.readUnsignedIntSmart()
            }
        } else {
            {
                buffer.readUnsignedShort()
            }
        }

        val archiveIds = IntArray(readFun())
        for (i in archiveIds.indices) {
            val archiveId = readFun() + if (i == 0) 0 else archiveIds[i - 1]
            archiveIds[i] = archiveId.also { archives[i] = Archive(it) }
        }
        val archives = archives()
        archiveNames = ArrayList(archives.size)
        if (named) {
            archives.forEach {
                it.hashName = buffer.readInt()
                if (it.hashName != 0) {
                    archiveNames.add(it.hashName)
                }
            }
        }
        archives.forEach { it.crc = buffer.readInt() }
        if (checksums) {
            archives.forEach { it.checksum = buffer.readInt() }
        }
        if (whirlpool) {
            archives.forEach {
                var archiveWhirlpool = it.whirlpool
                if (archiveWhirlpool == null) {
                    archiveWhirlpool = ByteArray(DIGESTBYTES)
                    it.whirlpool = archiveWhirlpool
                }
                buffer.readBytes(archiveWhirlpool)
            }
        }
        if (lengths) {
            archives.forEach {
                it.length = buffer.readInt()
                it.uncompressedLength = buffer.readInt()
            }
        }
        archives.forEach { it.revision = buffer.readInt() }
        val archiveFileSizes = IntArray(archives.size)
        for (i in archives.indices) {
            archiveFileSizes[i] = readFun()
        }
        for (i in archives.indices) {
            val archive = archives[i]
            var fileId = 0
            for (fileIndex in 0 until archiveFileSizes[i]) {
                fileId += readFun()
                archive.files[fileId] = File(fileId)
            }
        }
        if (named) {
            for (i in archives.indices) {
                val archive = archives[i]
                val fileIds = archive.fileIds()
                for (fileIndex in 0 until archiveFileSizes[i]) {
                    archive.file(fileIds[fileIndex])?.hashName = buffer.readInt()
                }
            }
        }

        this.archives.clear()
        archives.forEach { this.archives.putIfAbsent(it.id, it) }
    }

    fun write(buffer: ByteBuf) {
        buffer.writeByte(version)
        if (version >= 6) {
            buffer.writeInt(revision)
        }
        buffer.writeByte(mask)

        val writeFun: (Int) -> Unit = if (version >= 7) {
            {
                buffer.writeUnsignedIntSmart(it)
            }
        } else {
            {
                buffer.writeShort(it)
            }
        }

        writeFun(archives.size)
        val archiveIds = archiveIds()
        val archives = archives()
        for (i in archives.indices) {
            writeFun(archiveIds[i] - if (i == 0) 0 else archiveIds[i - 1])
        }
        if (isNamed()) {
            archives.forEach { buffer.writeInt(it.hashName) }
        }
        archives.forEach { buffer.writeInt(it.crc) }
        if (hasChecksums()) {
            archives.forEach { buffer.writeInt(it.checksum) }
        }
        if (hasWhirlpool()) {
            val empty = ByteArray(DIGESTBYTES)
            archives.forEach { buffer.writeBytes(it.whirlpool ?: empty) }
        }
        if (hasLengths()) {
            archives.forEach { buffer.writeInt(it.length).writeInt(it.uncompressedLength) }
        }
        archives.forEach { buffer.writeInt(it.revision) }
        archives.forEach { writeFun(it.files.size) }
        archives.forEach {
            val fileIds = it.fileIds()
            for (fileIndex in fileIds.indices) {
                writeFun(fileIds[fileIndex] - if (fileIndex == 0) 0 else fileIds[fileIndex - 1])
            }
        }
        if (isNamed()) {
            archives.forEach { archive ->
                archive.files().forEach { file ->
                    buffer.writeInt(file.hashName)
                }
            }
        }
    }

    @JvmOverloads
    fun add(data: ByteArray, xtea: IntArray? = null): Archive {
        val archive = add(xtea = xtea)
        archive.add(data)
        return archive
    }

    fun add(vararg archives: Archive?, overwrite: Boolean = true): Array<Archive> {
        val newArchives = ArrayList<Archive>(archives.size)
        archives.forEach {
            it ?: return@forEach
            newArchives.add(add(it, overwrite = overwrite))
        }
        return newArchives.toTypedArray()
    }

    @JvmOverloads
    fun add(archive: Archive, newId: Int = archive.id, xtea: IntArray? = null, overwrite: Boolean = true): Archive {
        val new = add(newId, archive.hashName, xtea, overwrite)
        if (overwrite) {
            new.clear()
            new.add(*archive.copyFiles())
            new.flag()
        }
        return new
    }

    @JvmOverloads
    fun add(name: String? = null, xtea: IntArray? = null): Archive {
        var id = if (name == null) nextId() else archiveId(name)
        if (id == -1) {
            id = nextId()
        }
        return add(id, toHash(name ?: ""), xtea)
    }

    @JvmOverloads
    fun add(id: Int, hashName: Int = -1, xtea: IntArray? = null, overwrite: Boolean = true): Archive {
        var existing = archive(id, direct = true)
        if (existing != null && !existing.read && !existing.new && !existing.flagged()) {
            existing = archive(id, xtea)
        }
        if (existing == null) {
            existing = Archive(id, if (hashName == -1) 0 else hashName, xtea)
            existing.compressionType = Js5CompressionType.GZIP
            if (hashName != -1) {
                archiveNames.add(existing.hashName)
            }
            archives[id] = existing
            existing.new = true
            existing.flag()
            flag()
        } else if (overwrite) {
            var flag = false
            val existingXtea = existing.xtea
            if (xtea == null && existingXtea != null || xtea != null && existingXtea == null ||
                xtea != null && existingXtea != null && !xtea.contentEquals(existingXtea)) {
                existing.xtea = xtea
                flag = true
            }
            if (hashName != -1 && existing.hashName != hashName) {
                archiveNames.remove(existing.hashName)
                existing.hashName = hashName
                archiveNames.add(existing.hashName)
                flag = true
            }
            if (flag) {
                existing.flag()
                flag()
            }
        }
        return existing
    }

    fun archive(name: String, direct: Boolean = false): Archive? {
        return archive(archiveId(name), direct)
    }

    @JvmOverloads
    fun archive(name: String, xtea: IntArray? = null, direct: Boolean = false): Archive? {
        return archive(archiveId(name), xtea, direct)
    }

    fun archive(id: Int, direct: Boolean = false): Archive? {
        return archive(id, null, direct)
    }

    @JvmOverloads
    fun archive(id: Int, xtea: IntArray? = null, direct: Boolean = false): Archive? {
        check(!origin.closed) { "Cache is closed." }
        val archive = archives[id] ?: return null
        if (direct || archive.read || archive.new) {
            return archive
        }
        try {
            val sector = origin.store.read(this.id, id)
            val typeNew = sector.getByte(sector.readerIndex()).toInt() and 0xFF
            archive.compressionType = Js5CompressionType.entries.getOrNull(typeNew)
                ?: throw IOException("Invalid compression type: $typeNew")
            val key = if (xtea != null && (xtea[0] != 0 || xtea[1] != 0 || xtea[2] != 0 || 0 != xtea[3])) {
                SymmetricKey.fromIntArray(xtea)
            } else {
                SymmetricKey.ZERO
            }
            val data: ByteBuf = Js5Compression.uncompress(sector, key)
            if (data.readableBytes() > 0) {
                archive.read = true
                unpackArchive(archive.files, data)
                archive.xtea = xtea
            }
            val mapsIndex = 5
            if (this.id == mapsIndex && !archive.containsData()) {
                archive.read = false
            }
            sector.readerIndex(0)
            val sectorSize = sector.writerIndex()
            val sectorBuffer = Unpooled.wrappedBuffer(sector)
            sectorBuffer.readerIndex(1)
            val remaining: Int = sectorSize - (sectorBuffer.readInt() + 1)
            if (remaining >= 2) {
                sectorBuffer.readerIndex(sectorSize - 2)
                archive.revision = sectorBuffer.readUnsignedShort()
            }
            return archive
        } catch (e: Exception) {
            archive.read = true
            archive.new = true
            archive.clear()
            return archive
        }
    }

    fun contains(id: Int): Boolean {
        return archives.containsKey(id)
    }

    fun contains(name: String): Boolean {
        return archiveNames.contains(toHash(name))
    }

    fun remove(id: Int): Archive? {
        val archive = archives.remove(id) ?: return null
        archiveNames.remove(archive.hashName)
        flag()
        return archive
    }

    fun remove(name: String): Archive? {
        return remove(archiveId(name))
    }

    fun first(): Archive? {
        if (archives.isEmpty()) {
            return null
        }
        return archive(archives.firstKey())
    }

    fun last(): Archive? {
        if (archives.isEmpty()) {
            return null
        }
        return archive(archives.lastKey())
    }

    fun archiveId(name: String): Int {
        val hashName = toHash(name)
        archives.values.forEach {
            if (it.hashName == hashName) {
                return it.id
            }
        }
        return -1
    }

    fun nextId(): Int {
        val last = last()
        return if (last == null) 0 else last.id + 1
    }

    fun copyArchives(): Array<Archive> {
        val archives = archives()
        val copy = ArrayList<Archive>(archives.size)
        for (i in archives.indices) {
            copy.add(i, Archive(archives[i]))
        }
        return copy.toTypedArray()
    }

    fun flag() {
        needUpdate = true
    }

    fun flagged(): Boolean {
        return needUpdate
    }

    fun unFlag() {
        needUpdate = false
    }

    fun mask(referenceTable: Index) {
        this.mask = referenceTable.mask
    }

    fun flagMask(flag: Int) {
        mask = mask or flag
    }

    fun unFlagMask(flag: Int) {
        mask = mask and flag.inv()
    }

    fun isNamed(): Boolean {
        return mask and FLAG_NAME != 0
    }

    fun hasWhirlpool(): Boolean {
        return mask and FLAG_WHIRLPOOL != 0
    }

    fun hasLengths(): Boolean {
        return mask and FLAG_LENGTHS != 0
    }

    fun hasChecksums(): Boolean {
        return mask and FLAG_CHECKSUMS != 0
    }

    fun archiveIds(): IntArray {
        return archives.keys.toIntArray()
    }

    fun archives(): Array<Archive> {
        return archives.values.toTypedArray()
    }

    private fun toHash(name: String): Int {
        return name.hashCode()
    }

    companion object {
        const val FLAG_NAME = 0x1
        const val FLAG_WHIRLPOOL = 0x2
        const val FLAG_LENGTHS = 0x4
        const val FLAG_CHECKSUMS = 0x8

        private fun unpackArchive(filesMap: SortedMap<Int, File>, input: ByteBuf) {
            if (filesMap.size == 1) {
                val decompressed = ByteArray(input.readableBytes())
                input.readBytes(decompressed)
                filesMap[filesMap.firstKey()]?.data = decompressed
                return
            }

            val fileIds = filesMap.keys.toIntArray()

            require(input.isReadable)

            val stripes = input.getUnsignedByte(input.writerIndex() - 1)

            var dataIndex = input.readerIndex()
            val trailerIndex = input.writerIndex() - (stripes * fileIds.size * 4) - 1
            require(trailerIndex >= dataIndex)

            input.readerIndex(trailerIndex)


            val lens = IntArray(fileIds.size)
            for (i in 0 until stripes) {
                var prevLen = 0
                for (fileIndex in fileIds.indices) {
                    prevLen += input.readInt()
                    lens[fileIndex] += prevLen
                }
            }

            input.readerIndex(trailerIndex)

            val filesData = arrayOfNulls<ByteArray>(fileIds.size)
            for (i in fileIds.indices) {
                filesData[i] = ByteArray(lens[i])
                lens[i] = 0
            }

            input.readerIndex(trailerIndex)
            var offset = 0
            for (i in 0 until stripes) {
                var read = 0
                for (j in fileIds.indices) {
                    read += input.readInt()
                    input.getBytes(offset, filesData[j], lens[j], read)
                    offset += read
                    lens[j] += read
                }
            }
            for (i in fileIds.indices) {
                filesMap[fileIds[i]]?.data = filesData[i]
            }
        }

        private fun packArchive(filesMap: SortedMap<Int, File>): ByteBuf {
            val files = filesMap.values.toTypedArray<File>()
            var size = 0
            files.forEach { size += it.data?.size ?: 0 }
            val buffer = Unpooled.buffer(size + files.size * 4)
            val emptyByteArray = byteArrayOf()
            if (files.size == 1) {
                return Unpooled.wrappedBuffer(filesMap[filesMap.firstKey()]?.data ?: emptyByteArray)
            } else {
                files.forEach { buffer.writeBytes(it.data ?: emptyByteArray) }
                val chunks = 1 //TODO Implement chunk writing
                for (i in files.indices) {
                    val file = files[i]
                    val fileDataSize = file.data?.size ?: 0
                    val previousFileDataSize = if (i == 0) 0 else files[i - 1].data?.size ?: 0
                    buffer.writeInt(fileDataSize - previousFileDataSize)
                }
                buffer.writeByte(chunks)
            }
            return buffer
        }
    }

}
