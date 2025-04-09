package dev.openrune.filesystem.openrs2

import dev.openrune.filesystem.Compression
import io.netty.buffer.Unpooled
import io.netty.buffer.UnpooledByteBufAllocator
import org.openrs2.cache.Cache
import org.openrs2.cache.Store
import org.openrs2.crypto.SymmetricKey
import java.io.Closeable
import java.nio.file.Path

class OpenRs2CacheImpl(
    private val store: Store,
    private val cache: org.openrs2.cache.Cache
) : dev.openrune.filesystem.Cache, Closeable {

    companion object {
        fun load(path: Path): OpenRs2CacheImpl {
            val store = Store.open(path, UnpooledByteBufAllocator.DEFAULT)
            val cache = Cache.open(store, UnpooledByteBufAllocator.DEFAULT)
            val lib = OpenRs2CacheImpl(store, cache)
            return lib
        }
    }

    override val versionTable: ByteArray
        get() = TODO("Not yet implemented")

    override fun archiveCount(index: Int): Int {
        return cache.list(index).asSequence().count()
    }

    override fun archiveId(index: Int, hash: Int): Int {
        return cache.listNamed(index, hash).asSequence().map { it.id }.firstOrNull()?: -1
    }

    override fun archives(index: Int): IntArray {
        return cache.list(index).iterator().asSequence().map { it.id }.toList().toIntArray()
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

    override fun data(index: Int, archive: Int, file: Int, xtea: IntArray?): ByteArray? {
        val buf = cache.read(index, archive, file)
        val bytes = ByteArray(buf.readableBytes())
        buf.readBytes(bytes)
        return bytes
    }

    override fun fileCount(indexId: Int, archiveId: Int): Int {
        return cache.list(indexId, archiveId).asSequence().count()
    }

    override fun files(index: Int, archive: Int): IntArray {
        return cache.list(index, archive).iterator().asSequence().map { it.id }.toList().toIntArray()
    }

    override fun indexCount(): Int {
        return store.list().size
    }

    override fun indices(): IntArray {
        return store.list().toIntArray()
    }

    override fun exists(id: Int) = indices().getOrNull(id) != null

    override fun lastArchiveId(indexId: Int): Int {
        return cache.list(indexId).iterator().asSequence().map { it.id }.maxOrNull()?: -1
    }

    override fun lastFileId(indexId: Int, archive: Int): Int {
        return cache.list(indexId, archive).iterator().asSequence().map { it.id }.maxOrNull()?: -1
    }

    override fun sector(index: Int, archive: Int): ByteArray? {
        val buf = store.read(index, archive)
        val bytes = ByteArray(buf.readableBytes())
        buf.readBytes(bytes)
        return bytes
    }

    override fun update(): Boolean {
        TODO("Not yet implemented")
    }

    override fun write(index: Int, archive: Int, data: ByteArray, xteas: IntArray?) {
        val key = xteas?.let { SymmetricKey(it[0], it[1], it[2], it[3]) } ?: SymmetricKey.ZERO
        val buf = Unpooled.wrappedBuffer(data)
        cache.write(index, archive, 0, buf, key)
    }

    override fun write(index: Int, archive: Int, file: Int, data: ByteArray, xteas: IntArray?) {
        val key = xteas?.let { SymmetricKey(it[0], it[1], it[2], it[3]) } ?: SymmetricKey.ZERO
        val buf = Unpooled.wrappedBuffer(data)
        cache.write(index, archive, file, buf, key)
    }

    override fun write(index: Int, archive: String, data: ByteArray, xteas: IntArray?) {
        val key = xteas?.let { SymmetricKey(it[0], it[1], it[2], it[3]) } ?: SymmetricKey.ZERO
        val buf = Unpooled.wrappedBuffer(data)
        cache.write(index, archive, 0, buf, key)
    }

    override fun close() {
        cache.close()
        store.close()
    }
}
