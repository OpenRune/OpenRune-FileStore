package dev.openrune.codec

import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.data.VarBitType

class VarBitCodec : DefinitionCodec<VarBitType> {
    override fun VarBitType.read(opcode: Int, buffer: Reader) {
        if (opcode == 1) {
            varp = buffer.readShort()
            startBit = buffer.readUnsignedByte()
            endBit = buffer.readUnsignedByte()
        }
    }

    override fun Writer.encode(definition: VarBitType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = VarBitType()
}