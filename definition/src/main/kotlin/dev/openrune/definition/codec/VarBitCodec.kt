package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.VarBitType
import io.netty.buffer.ByteBuf

class VarBitCodec : DefinitionCodec<VarBitType> {
    override fun VarBitType.read(opcode: Int, buffer: ByteBuf) {
        if (opcode == 1) {
            varp = buffer.readShort().toInt()
            startBit = buffer.readUnsignedByte().toInt()
            endBit = buffer.readUnsignedByte().toInt()
        }
    }

    override fun ByteBuf.encode(definition: VarBitType) {
        writeByte(1)
        writeShort(definition.varp)
        writeByte(definition.startBit)
        writeByte(definition.endBit)

        writeByte(0)
    }

    override fun createDefinition() = VarBitType()
}