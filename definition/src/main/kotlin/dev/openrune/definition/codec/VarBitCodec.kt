package dev.openrune.definition.codec

import dev.openrune.buffer.Reader
import dev.openrune.buffer.Writer
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.VarBitType

class VarBitCodec : DefinitionCodec<VarBitType> {
    override fun VarBitType.read(opcode: Int, buffer: Reader) {
        if (opcode == 1) {
            varp = buffer.readShort()
            startBit = buffer.readUnsignedByte()
            endBit = buffer.readUnsignedByte()
        }
    }

    override fun Writer.encode(definition: VarBitType) {
        writeByte(1)
        writeShort(definition.varp)
        writeByte(definition.startBit)
        writeByte(definition.endBit)

        writeByte(0)
    }

    override fun createDefinition() = VarBitType()
}