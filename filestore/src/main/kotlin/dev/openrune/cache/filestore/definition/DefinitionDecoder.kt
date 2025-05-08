package dev.openrune.cache.filestore.definition

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.SPRITES
import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import dev.openrune.filesystem.Cache
import java.nio.BufferUnderflowException

abstract class DefinitionDecoder<T : Definition>(val index: Int, private val codec: DefinitionCodec<T>, private var transform: DefinitionTransform<T>? = null) {

    open fun isRS2() : Boolean = false

    open fun size(cache: Cache): Int {
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
            if (index == SPRITES) {
                cache.archives(SPRITES)
            } else {
                cache.files(index, getArchive(0))
            }
        }

        for (id in ids) {
            try {
                val archive = getArchive(id)
                val file = getFile(id)
                val data = cache.data(index, archive, file)
                if (data != null) {
                    val definition = codec.loadData(id, data)
                    transform?.changeValues(id, definition)
                    definitions[id] = definition
                }
            } catch (e: BufferUnderflowException) {
                println("Error reading definition ${index}: $id")
            }
        }

        logger.info { "${definitions.size} ${this::class.simpleName} definitions loaded in ${System.currentTimeMillis() - start}ms" }
    }

    open fun getFile(id: Int) = id

    open fun getArchive(id: Int) = id

    companion object {
        internal val logger = InlineLogger()
    }
}