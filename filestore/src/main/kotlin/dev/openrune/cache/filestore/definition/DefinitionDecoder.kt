package dev.openrune.cache.filestore.definition

import dev.openrune.cache.CONFIGS
import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.filestore.buffer.BufferReader
import dev.openrune.cache.filestore.buffer.Reader
import io.github.oshai.kotlinlogging.KotlinLogging
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import java.nio.BufferUnderflowException
import kotlin.reflect.full.createInstance

abstract class DefinitionDecoder<T : Definition>(val index: Int) {

    var isRS2 = false

    open fun load(definitions: Int2ObjectOpenHashMap<T>, reader: Reader) {
        val id = readId(reader)
        read(definitions, id, reader)
    }

    open fun readId(reader: Reader) = reader.readInt()


    fun size(cache: Cache): Int {
        return cache.lastArchiveId(index) * 256 + (cache.fileCount(index, cache.lastArchiveId(index)))
    }

    open fun load(cache: Cache, definitions: MutableMap<Int, T>) {
        load(cache,definitions,false)
    }


    /**
     * Load from cache
     */
    open fun load(cache: Cache, definitions: MutableMap<Int, T>, isRS2 : Boolean = false) {
        val start = System.currentTimeMillis()
        if (isRS2) {
            for (id in 0..size(cache)) {
                try {
                    val archive = getArchive(id)
                    val file = getFile(id)
                    val data = cache.data(index, archive, file)
                    if (data != null) {
                        val definition = loadSingle(id, data)
                        definitions[id] = definition
                    }
                } catch (e: BufferUnderflowException) {

                }
            }
        } else {
            cache.files(index, getArchive(0)).forEach { id ->
                try {
                    val archive = getArchive(id)
                    val file = getFile(id)
                    val data = cache.data(index, archive, file)
                    if (data != null) {
                        val definition = loadSingle(id, data)
                        definitions[id] = definition
                    }
                } catch (e: BufferUnderflowException) {
                    println("Error reading definition $id")
                    throw e
                }
            }
        }

        logger.info { "${definitions.size} ${this::class.simpleName} definitions loaded in ${System.currentTimeMillis() - start}ms" }
    }

    open fun load(definitions: Int2ObjectOpenHashMap<T>, cache: Cache, id: Int) {
        val archive = getArchive(id)
        val file = getFile(id)
        val data = cache.data(index, archive, file) ?: return
        read(definitions, id, BufferReader(data))
    }

    protected fun readFlat(definitions: Int2ObjectOpenHashMap<T>, id: Int, reader: Reader) {
        val definition = definitions[id]
        readFlatFile(definition, reader)
        changeValues(definitions, definition)
    }

    open fun readFlatFile(definition: T, buffer: Reader) {
        definition.read(0,buffer)
    }


    open fun loadSingle(id: Int, data: ByteArray): T {
        val reader = BufferReader(data)
        val definition = createDefinition()
        read(definition, reader)
        return definition
    }


    protected abstract fun createDefinition(): T

    protected fun read(definition: T, reader: Reader) {
        while (true) {
            val opcode = reader.readUnsignedByte()
            if (opcode == 0) {
                break
            }
            definition.read(opcode, reader)
        }
    }


    open fun loadSingleFlat(id: Int, data: ByteArray): T? {
        val definitions = Int2ObjectOpenHashMap<T>(1)
        val reader = BufferReader(data)
        readFlat(definitions, 0, reader)
        return definitions[0]
    }


    open fun getFile(id: Int) = id

    open fun getArchive(id: Int) = id

    protected fun read(definitions: Int2ObjectOpenHashMap<T>, id: Int, reader: Reader) {
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

    open fun changeValues(definitions: Int2ObjectOpenHashMap<T>, definition: T) {
    }

    companion object {
        internal val logger = KotlinLogging.logger {}
    }
}
