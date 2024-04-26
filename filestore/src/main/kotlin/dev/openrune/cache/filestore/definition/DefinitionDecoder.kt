package dev.openrune.cache.filestore.definition

import dev.openrune.cache.filestore.buffer.Reader
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.BufferUnderflowException

abstract class DefinitionDecoder<T : Definition>(val index: Int) {

    val configArchive = 2

    abstract fun create(size: Int): Array<T>

    open fun load(definitions: Array<T>, reader: Reader) {
        val id = readId(reader)
        read(definitions, id, reader)
    }

    open fun readId(reader: Reader) = reader.readInt()

    /**
     * Load from cache
     */
    open fun load(cache: dev.openrune.cache.filestore.Cache): Array<T> {
        val start = System.currentTimeMillis()
        val size = size(cache) + 1
        val definitions = create(size)
        for (id in 0 until size) {
            try {
                load(definitions, cache, id)
            } catch (e: BufferUnderflowException) {
                logger.error(e) { "Error reading definition $id" }
                throw e
            }
        }
        logger.info { "$size ${this::class.simpleName} definitions loaded in ${System.currentTimeMillis() - start}ms" }
        return definitions
    }

    open fun size(cache: dev.openrune.cache.filestore.Cache): Int {
        return cache.fileCount(configArchive,index)
    }

    open fun load(definitions: Array<T>, cache: dev.openrune.cache.filestore.Cache, id: Int) {
        val file = getFile(id)
        val data = cache.data(configArchive, index, file) ?: return
        read(definitions, id, dev.openrune.cache.filestore.buffer.BufferReader(data))
    }

    open fun getFile(id: Int) = id

    protected fun read(definitions: Array<T>, id: Int, reader: Reader) {
        val definition = definitions[id]
        readLoop(definition, reader)
        changeValues(definitions, definition)
    }

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

    open fun changeValues(definitions: Array<T>, definition: T) {
    }

    companion object {
        internal val logger = KotlinLogging.logger {}

    }
}