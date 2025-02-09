package dev.openrune.definition.codec

import io.netty.buffer.ByteBuf
import dev.openrune.buffer.Writer
import dev.openrune.buffer.readShortRD
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.VarBitType

class VarBitCodec : DefinitionCodec<VarBitType> {
    override fun VarBitType.read(opcode: Int, buffer: ByteBuf) {
        if (opcode == 1) {
            varp = buffer.readShortRD()
            startBit = buffer.readUnsignedByte().toInt()
            endBit = buffer.readUnsignedByte().toInt()
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