package dev.openrune.filesystem

interface WritableCache: Cache {

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

    fun write(index: Int, archive: Int, file: Int, data: ByteArray, xteas: IntArray? = null)

    fun write(index: Int, archive: Int, data: ByteArray, xteas: IntArray? = null)

    fun write(index: Int, archive: String, data: ByteArray, xteas: IntArray? = null)

    fun update(): Boolean
}