package dev.openrune

import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.DefinitionDecoder

abstract class OpcodelessDecoder<T : Definition>(
    index: Int,
    private val factory: () -> T,
    private val codec: DefinitionCodec<T>
) : DefinitionDecoder<T>(index) {

    override fun getArchive(id: Int) = 0
    override fun createDefinition(): T = factory()

    override fun readLoop(definition: T, buffer: Reader) {
        definition.read(-1, buffer)
    }

    override fun T.read(opcode: Int, buffer: Reader) {
        codec.run {
            this@read.read(opcode, buffer)
        }
    }
}