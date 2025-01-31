package dev.openrune.codec

import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.data.UnderlayType

class UnderlayCodec : DefinitionCodec<UnderlayType> {
    override fun UnderlayType.read(opcode: Int, buffer: Reader) {
        if (opcode == 1) {
            color = buffer.readMedium()
        }
    }

    override fun Writer.encode(definition: UnderlayType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = UnderlayType()
}