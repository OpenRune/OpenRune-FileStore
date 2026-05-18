package dev.openrune.cache.tools.worldmap.providers

import io.netty.buffer.ByteBuf

/**
 * @author Kris | 18/08/2022
 */
abstract class CacheProvider {
    private val usedIds = mutableMapOf<Int, MutableSet<Int>>()
    abstract fun read(archive: Int, group: Int, file: Int): ByteBuf
    abstract fun read(archive: Int, group: String, file: String): ByteBuf
    abstract fun exists(archive: Int, group: Int, file: Int): Boolean
    abstract fun exists(archive: Int, group: String, file: String): Boolean
    abstract fun write(archive: Int, group: Int, file: Int, buf: ByteBuf)
    abstract fun write(archive: Int, group: String, file: String, buf: ByteBuf)
    abstract fun write(archive: Int, group: String, file: String, fileId: Int, buf: ByteBuf)
    abstract fun list(archive: Int): List<Int>
    abstract fun list(archive: Int, group: Int): List<Int>
    abstract fun list(archive: Int, group: String): List<Int>

    fun allocateEmpty(archive: Int): Int {
        val used = usedIds.getOrPut(archive) { list(archive).toMutableSet() }
        val next = FILE_RANGE.first { it !in used }
        used.add(next)
        return next
    }

    private companion object {
        private val FILE_RANGE = 0..0xFFFF
    }
}
