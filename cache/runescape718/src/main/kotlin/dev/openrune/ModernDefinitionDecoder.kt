package dev.openrune

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.DefinitionDecoder

abstract class ModernDefinitionDecoder<T : Definition>(
    index: Int,
    private val shift: Int,
    private val factory: () -> T,
    codec: DefinitionCodec<T>
) : DefinitionDecoder<T>(index, codec) {

    private val mask = (1 shl shift) - 1

    override fun getFile(id: Int) = id and mask
    override fun getArchive(id: Int) = id ushr shift
    override fun isRS2() = true
    override fun createDefinition(): T = factory()
}
