package dev.openrune.cache.tools.incremental

import dev.openrune.filesystem.Cache
import dev.openrune.filesystem.Compression
import java.util.zip.CRC32

internal class RecordingCache(private val delegate: Cache) : Cache by delegate {
    var record: UnitRecord? = null

    /**
     * When false, reads are served but not recorded as dependencies. Use for reads that only inspect the
     * cache — an existence check, say — where the caller's output does not depend on what was read.
     */
    var trackReads: Boolean = true

    override fun write(index: Int, archive: Int, file: Int, data: ByteArray, xteas: IntArray?) {
        recordOutput(index, archive, file, data)
        delegate.write(index, archive, file, data, xteas)
    }

    override fun write(index: Int, archive: Int, data: ByteArray, xteas: IntArray?) {
        recordOutput(index, archive, 0, data)
        delegate.write(index, archive, data, xteas)
    }

    override fun write(index: Int, archive: String, data: ByteArray, xteas: IntArray?) {
        delegate.write(index, archive, data, xteas)
        recordOutput(index, delegate.archiveId(index, archive), 0, data)
    }

    override fun data(index: Int, archive: Int, file: Int, xtea: IntArray?): ByteArray? {
        if (trackReads) record?.cacheReads?.add(CacheTarget(index, archive, file))
        return delegate.data(index, archive, file, xtea)
    }

    override fun data(index: Int, name: String, xtea: IntArray?): ByteArray? {
        val archive = delegate.archiveId(index, name)
        if (trackReads) record?.cacheReads?.add(CacheTarget(index, archive, 0))
        return delegate.data(index, name, xtea)
    }

    override fun fileData(index: Int, archive: Int, xtea: IntArray?): Array<ByteArray?>? {
        if (trackReads) record?.cacheReads?.add(CacheTarget(index, archive, ANY_FILE))
        return delegate.fileData(index, archive, xtea)
    }

    fun declareOutput(index: Int, archive: Int, file: Int) {
        record?.outputs?.putIfAbsent(CacheTarget(index, archive, file), 0)
    }

    private fun recordOutput(index: Int, archive: Int, file: Int, data: ByteArray) {
        val active = record ?: return
        if (archive < 0) return
        active.outputs[CacheTarget(index, archive, file)] = crc(data)
    }

    private fun crc(data: ByteArray): Int = CRC32().apply { update(data) }.value.toInt()

    override fun createIndex(
        compressionType: Compression,
        version: Int,
        revision: Int,
        named: Boolean,
        whirlpool: Boolean,
        lengths: Boolean,
        checksums: Boolean,
        writeReferenceTable: Boolean,
        id: Int,
    ) = delegate.createIndex(
        compressionType, version, revision, named, whirlpool, lengths, checksums, writeReferenceTable, id,
    )

    companion object {
        /**
         * Runs [block] without recording the reads it performs. A no-op for a plain cache, so callers do
         * not need to know whether they are being recorded.
         */
        fun <T> unrecorded(cache: Cache, block: () -> T): T {
            if (cache !is RecordingCache) return block()
            val previous = cache.trackReads
            cache.trackReads = false
            return try {
                block()
            } finally {
                cache.trackReads = previous
            }
        }

        const val ANY_FILE = -1
    }
}
