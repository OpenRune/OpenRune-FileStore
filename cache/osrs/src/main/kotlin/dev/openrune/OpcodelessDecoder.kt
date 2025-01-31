package dev.openrune

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.DefinitionDecoder

abstract class OpcodelessDecoder<T : Definition>(
    index: Int,
    codec: DefinitionCodec<T>
) : DefinitionDecoder<T>(index, codec) {

    override fun getArchive(id: Int) = 0
}