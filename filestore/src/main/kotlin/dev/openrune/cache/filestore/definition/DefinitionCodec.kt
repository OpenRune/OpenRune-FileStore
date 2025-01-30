package dev.openrune.cache.filestore.definition

import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.buffer.Writer

interface DefinitionCodec<T : Definition> {
    fun T.read(opcode: Int, buffer: Reader)
    fun Writer.encode(definition: T)
}