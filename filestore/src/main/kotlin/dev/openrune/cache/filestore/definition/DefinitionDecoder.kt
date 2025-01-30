package dev.openrune.cache.filestore.definition

import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.filestore.buffer.BufferReader
import dev.openrune.cache.filestore.buffer.Reader
import io.github.oshai.kotlinlogging.KotlinLogging
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import java.nio.BufferUnderflowException

abstract class DefinitionDecoder<T : Definition>(val index: Int) {

    open fun readId(reader: Reader) = reader.readInt()

    open fun isFlat() : Boolean = false
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
                    val definition = if (isRS2()) {
                        loadData(id, data) // RS2-specific data loading
                    } else {
                        loadData(id, data, isFlat()) // Non-RS2-specific data loading
                    }
                    changeValues(id, definition)
                    definitions[id] = definition
                }
            } catch (e: BufferUnderflowException) {
                println("Error reading definition $id")
                throw e
            }
        }

        logger.info { "${definitions.size} ${this::class.simpleName} definitions loaded in ${System.currentTimeMillis() - start}ms" }
    }

    open fun loadData(id: Int, data: ByteArray, skipOpcode : Boolean = true): T {
        val reader = BufferReader(data)
        val definition = createDefinition()
        readLoop(definition, reader)
        return definition
    }

    protected abstract fun createDefinition(): T

    open fun getFile(id: Int) = id

    open fun getArchive(id: Int) = id

    open fun readLoop(definition: T, buffer: Reader) {
        while (true) {
            val opcode = buffer.readUnsignedByte()
            if (opcode == 0) {
                break
            }
            definition.read(opcode, buffer)
        }
    }

    protected abstract fun T.read(opcode: Int, buffer: Reader)

    /**
     * Allows modifications to be made to the definition after loading.
     */
    open fun changeValues(id: Int, definition: T) {}

    companion object {
        internal val logger = KotlinLogging.logger {}
    }
}