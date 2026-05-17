package dev.openrune.cache.filestore.definition

import dev.openrune.cache.DBTABLEINDEX
import dev.openrune.definition.codec.DBTableIndexCodec
import dev.openrune.definition.type.DBTableIndexType
import dev.openrune.filesystem.Cache
import java.nio.BufferUnderflowException

class DBTableIndexDecoder : DefinitionDecoder<DBTableIndexType>(DBTABLEINDEX, DBTableIndexCodec()) {

    override fun load(cache: Cache, definitions: MutableMap<Int, DBTableIndexType>) {
        val start = System.currentTimeMillis()
        cache.archives(DBTABLEINDEX).forEach { archive ->
            cache.files(DBTABLEINDEX, archive).forEach { fileId ->
                val data = cache.data(DBTABLEINDEX, archive, fileId) ?: return@forEach
                val id = archive shl 16 or fileId
                try {
                    definitions[id] = codec.loadData(id, data)
                } catch (e: BufferUnderflowException) {
                    println("Error reading definition $index: $id")
                }
            }
        }
        DefinitionDecoder.logger.info { "${definitions.size} ${this::class.simpleName} definitions loaded in ${System.currentTimeMillis() - start}ms" }
    }
}
