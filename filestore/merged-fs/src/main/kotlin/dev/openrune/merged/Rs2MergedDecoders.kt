package dev.openrune.merged

import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec

class Rs2PromotedDecoder<T : Definition>(
    index: Int,
    codec: DefinitionCodec<T>,
    private val bits: Int,
) : DefinitionDecoder<T>(index, codec) {

    override fun isRS2(): Boolean = true

    override fun getArchive(id: Int) = id shr bits

    override fun getFile(id: Int) = id and ((1 shl bits) - 1)
}

class Rs2GroupDecoder<T : Definition>(
    index: Int,
    codec: DefinitionCodec<T>,
    private val archive: Int,
) : DefinitionDecoder<T>(index, codec) {

    override fun getArchive(id: Int) = archive

    override fun getFile(id: Int) = id
}
