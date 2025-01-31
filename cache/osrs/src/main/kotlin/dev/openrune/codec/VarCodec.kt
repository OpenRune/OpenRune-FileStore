package dev.openrune.codec

import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.data.VarpType

class VarCodec : DefinitionCodec<VarpType> {
    override fun VarpType.read(opcode: Int, buffer: Reader) {
        if (opcode == 5) {
            configType = buffer.readUnsignedShort()
        }
    }

    override fun Writer.encode(definition: VarpType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = VarpType()
}