package com.displee.cache

import com.displee.cache.index.Index
import com.displee.cache.index.Index.Companion.FLAG_CHECKSUMS
import com.displee.cache.index.Index.Companion.FLAG_LENGTHS
import com.displee.cache.index.Index.Companion.FLAG_NAME
import com.displee.cache.index.Index.Companion.FLAG_WHIRLPOOL
import com.displee.cache.index.archive.Archive
import io.netty.buffer.*
import org.openrs2.buffer.use
import org.openrs2.cache.DiskStore
import org.openrs2.cache.Js5CompressionType
import org.openrs2.cache.Store
import org.openrs2.crypto.Whirlpool.Companion.DIGESTBYTES
import org.openrs2.crypto.whirlpool
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.nio.file.Path
import java.util.*

open class CacheLibrary(val path: String, val clearDataAfterUpdate: Boolean = false, private val listener: ProgressListener? = null) {

    lateinit var store: Store
    lateinit var alloc: ByteBufAllocator
    private var indices: Array<Index?> = arrayOfNulls(0)

    var closed = false

    private val indexCount: Int
        get() = indices.size

    init {
        load()
    }

    /**
     * Re-create indices and re-read reference tables.
     */
    fun reload() {
        indices = arrayOfNulls(0)
        load()
    }

    @Throws(IOException::class)
    private fun load() {
        val alloc = UnpooledByteBufAllocator.DEFAULT
        store = Store.open(Path.of(path), alloc)
        this.alloc = alloc
        val indicesLength = store.list().filter { it != 255 }.max() + 1
        indices = arrayOfNulls(indicesLength)
        listener?.notify(0.0, "Reading indices...")
        for(id in store.list()) {
            val progress = id / (indicesLength - 1.0)
            if(id == 255)
                continue
            setIndex(id, Index(this, id))
            listener?.notify(progress, "Loaded index $id.")
        }
    }

    private fun setIndex(index: Int, value: Index?) {
        if (index >= indices.size) {
            indices = indices.copyOf(index + 1)
        }
        indices[index] = value
    }

    @JvmOverloads
    fun createIndex(compressionType: Js5CompressionType = Js5CompressionType.GZIP, version: Int = 6, revision: Int = 0,
                    named: Boolean = false, whirlpool: Boolean = false, lengths: Boolean = false, checksums: Boolean = false,
                    writeReferenceTable: Boolean = true, id: Int = if (indices.isEmpty()) 0 else indexCount + 1): Index {
        if(store.exists(id)) {
            println("Index $id already exists.")
            return index(id)
        }
        store.create(id)
        val index = Index(this, id).also { setIndex(id, it) }
        if (!writeReferenceTable) {
            return index
        }
        index.version = version
        index.revision = revision
        index.compressionType = compressionType
        if (named) {
            index.flagMask(FLAG_NAME)
        }
        if (whirlpool) {
            index.flagMask(FLAG_WHIRLPOOL)
        }
        if (lengths) {
            index.flagMask(FLAG_LENGTHS)
        }
        if (checksums) {
            index.flagMask(FLAG_CHECKSUMS)
        }
        index.flag()
        check(index.update())
        return index
    }

    fun createIndex(index: Index, writeReferenceTable: Boolean = true): Index {
        return createIndex(index.compressionType, index.version, index.revision,
                index.isNamed(), index.hasWhirlpool(), index.hasLengths(), index.hasChecksums(), writeReferenceTable, index.id)
    }

    fun exists(id: Int): Boolean {
        return indices.getOrNull(id) != null
    }

    fun index(id: Int): Index {
        val index = indices.getOrNull(id)
        return checkNotNull(index) { "Index $id doesn't exist. Please use the {@link exists(int) exists} function to verify whether an index exists." }
    }

    @JvmOverloads
    fun put(index: Int, archive: Int, file: Int, data: ByteArray, xtea: IntArray? = null): com.displee.cache.index.archive.file.File {
        return index(index).add(archive, xtea = xtea).add(file, data)
    }

    @JvmOverloads
    fun put(index: Int, archive: Int, data: ByteArray, xtea: IntArray? = null): Archive {
        val currentArchive = index(index).add(archive, -1, xtea)
        currentArchive.add(0, data)
        return currentArchive
    }

    @JvmOverloads
    fun put(index: Int, archive: Int, file: String, data: ByteArray, xtea: IntArray? = null): com.displee.cache.index.archive.file.File {
        return index(index).add(archive, xtea = xtea).add(file, data)
    }

    @JvmOverloads
    fun put(index: Int, archive: String, data: ByteArray, xtea: IntArray? = null): Archive {
        return put(index, archive, 0, data, xtea)
    }

    @JvmOverloads
    fun put(index: Int, archive: String, file: Int, data: ByteArray, xtea: IntArray? = null): Archive {
        val currentArchive = index(index).add(archive, xtea = xtea)
        currentArchive.add(file, data)
        return currentArchive
    }

    @JvmOverloads
    fun put(index: Int, archive: String, file: String, data: ByteArray, xtea: IntArray? = null): com.displee.cache.index.archive.file.File {
        return index(index).add(archive, xtea = xtea).add(file, data)
    }

    @JvmOverloads
    fun data(index: Int, archive: Int, file: Int = 0, xtea: IntArray? = null): ByteArray? {
        return index(index).archive(archive, xtea)?.file(file)?.data
    }

    @JvmOverloads
    fun data(index: Int, archive: Int, file: String, xtea: IntArray? = null): ByteArray? {
        return index(index).archive(archive, xtea)?.file(file)?.data
    }

    @JvmOverloads
    fun data(index: Int, archive: String, file: Int, xtea: IntArray? = null): ByteArray? {
        return index(index).archive(archive, xtea)?.file(file)?.data
    }

    @JvmOverloads
    fun data(index: Int, archive: String, file: String, xtea: IntArray? = null): ByteArray? {
        return index(index).archive(archive, xtea)?.file(file)?.data
    }

    @JvmOverloads
    fun data(index: Int, archive: String, xtea: IntArray? = null): ByteArray? {
        return data(index, archive, 0, xtea)
    }

    fun remove(index: Int, archive: Int, file: Int): com.displee.cache.index.archive.file.File? {
        return index(index).archive(archive)?.remove(file)
    }

    fun remove(index: Int, archive: Int, file: String): com.displee.cache.index.archive.file.File? {
        return index(index).archive(archive)?.remove(file)
    }

    fun remove(index: Int, archive: String, file: String): com.displee.cache.index.archive.file.File? {
        return index(index).archive(archive)?.remove(file)
    }

    fun remove(index: Int, archive: String, file: Int): com.displee.cache.index.archive.file.File? {
        return index(index).archive(archive)?.remove(file)
    }

    fun remove(index: Int, archive: Int): Archive? {
        return index(index).remove(archive)
    }

    fun remove(index: Int, archive: String): Archive? {
        return index(index).remove(archive)
    }

    fun update() {
        for (index in indices) {
            if (index == null) {
                continue
            }
            if (index.flaggedArchives().isEmpty() && !index.flagged()) {
                continue
            }
            index.update()
        }
    }

    @Throws(RuntimeException::class)
    fun deleteLastIndex() {
        val id = indexCount
        store.remove(id)
        val file = File(path, "$CACHE_FILE_NAME.idx$id")
        if (!file.exists() || !file.delete()) {
            throw RuntimeException("Failed to remove the random access file of the argued index[id=$id, file exists=${file.exists()}]")
        }
        indices[id] = null
        indices = indices.copyOf(id)
    }

    @JvmOverloads
    fun generateUkeys(writeWhirlpool: Boolean = true, exponent: BigInteger? = null, modulus: BigInteger? = null): ByteBuf {
        val buffer = Unpooled.buffer(6 + indexCount * 72)
        if (writeWhirlpool) {
            buffer.writeByte(indexCount)
        }
        val emptyWhirlpool = ByteArray(DIGESTBYTES)
        for (index in indices()) {
            buffer.writeInt(index.crc).writeInt(index.revision)
            if (writeWhirlpool) {
                buffer.writeBytes(index.whirlpool ?: emptyWhirlpool)
            }
        }
        if (writeWhirlpool) {
            val digest = buffer.whirlpool(5, buffer.writerIndex() - 5)
            val SIGNATURE_LENGTH = DIGESTBYTES + 1

            if (exponent != null && modulus != null) {
                buffer.alloc().buffer(SIGNATURE_LENGTH, SIGNATURE_LENGTH).use { plaintext ->
                    plaintext.writeByte(0)
                    plaintext.writeBytes(digest)

                    plaintext.rsa(exponent, modulus).use { ciphertext ->
                        buffer.writeBytes(ciphertext)
                    }
                }
            } else {
                buffer.writeByte(0)
                buffer.writeBytes(digest)
            }
        }
        return buffer
    }

    private fun ByteBuf.toBigInteger(): BigInteger {
        val bytes = ByteBufUtil.getBytes(this, readerIndex(), readableBytes(), false)
        return BigInteger(bytes)
    }

    private fun ByteBuf.rsa(exponent: BigInteger, modulus: BigInteger): ByteBuf {
        return Unpooled.wrappedBuffer(toBigInteger().modPow(exponent, modulus).toByteArray())
    }

    fun rebuild(directory: File) {
        File(directory.path).mkdirs()
        File(directory.path, "$CACHE_FILE_NAME.idx255").createNewFile()
        File(directory.path, "$CACHE_FILE_NAME.dat2").createNewFile()
        val indicesSize = indices.size
        val newLibrary = CacheLibrary(directory.path)
        val newStore = DiskStore.create(Path.of(directory.path))
        for (index in indices) {
            if (index == null) {
                continue
            }
            val id = index.id
            print("\rBuilding index $id/$indicesSize...")
            try {
                val archiveSector = store.read(255, id)
                val newIndex = newStore.create(id)
                for (i in index.archiveIds()) { //only write referenced archives
                    val data = store.read(id, i)//TODO crash patch?
                    newStore.write(id, i, data)
                }
                newStore.write(255, id, archiveSector)
            } catch (e: Exception) {
                continue
            }
        }
        newLibrary.close()
        println("\rFinished building $indicesSize indices.")
    }

    fun fixCrcs(update: Boolean) {
        for(index in indices) {
            if (index == null || index.archiveIds().isEmpty()) {
                continue
            }
            index.fixCRCs(update)
        }
    }

    fun close() {
        if (closed) {
            return
        }
        store.close()
        closed = true
    }

    fun first(): Index? {
        if (indices.isEmpty()) {
            return null
        }
        return indices.getOrNull(0)
    }

    fun last(): Index? {
        if (indices.isEmpty()) {
            return null
        }
        return indices.getOrNull(indices.size - 1)
    }

    fun indices(): Array<Index> {
        return indices.filterNotNull().toTypedArray()
    }

    companion object {
        const val CACHE_FILE_NAME = "main_file_cache"

        @JvmStatic
        @JvmOverloads
        fun create(path: String, clearDataAfterUpdate: Boolean = false, listener: ProgressListener? = null): CacheLibrary {
            return CacheLibrary(path, clearDataAfterUpdate, listener)
        }
    }

}
