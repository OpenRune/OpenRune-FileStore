package dev.openrune.definition.codec

import dev.openrune.buffer.Reader
import dev.openrune.buffer.Writer
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.VarpType

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