package dev.openrune.cache.filestore.definition

import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.filestore.buffer.BufferReader
import dev.openrune.cache.filestore.buffer.Reader
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.BufferUnderflowException

abstract class DefinitionDecoder<T : Definition>(val index: Int, val codec: DefinitionCodec<T>) {

    open fun isRS2() : Boolean = false

    fun size(cache: Cache): Int {
        return cache.lastArchiveId(index) * 256 + (cache.fileCount(index, cache.lastArchiveId(index)))
    }

    /**
     * Load from cache
     */
    open fun load(cache: Cache, definitions: MutableMap<Int, T>) {
        val start = System.currentTimeMillis()

        val ids: IntArray = if (isRS2()) {
            IntArray(size(cache)) { it }
        } else {
            cache.files(index, getArchive(0))
        }

        for (id in ids) {
            try {
                val archive = getArchive(id)
                val file = getFile(id)
                val data = cache.data(index, archive, file)
                if (data != null) {
                    val definition = loadData(id, data)
                    changeValues(id, definition)
                    definitions[id] = definition
                }
            } catch (e: BufferUnderflowException) {
                println("Error reading definition $id")
            }
        }

        logger.info { "${definitions.size} ${this::class.simpleName} definitions loaded in ${System.currentTimeMillis() - start}ms" }
    }

    open fun loadData(id: Int, data: ByteArray): T {
        val reader = BufferReader(data)
        val definition = createDefinition()
        codec.readLoop(definition, reader)
        return definition
    }

    protected abstract fun createDefinition(): T

    open fun getFile(id: Int) = id

    open fun getArchive(id: Int) = id

    /**
     * Allows modifications to be made to the definition after loading.
     */
    open fun changeValues(id: Int, definition: T) {}

    companion object {
        internal val logger = KotlinLogging.logger {}
    }
}