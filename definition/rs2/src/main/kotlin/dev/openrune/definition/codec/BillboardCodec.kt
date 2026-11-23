package dev.openrune.definition.codec

import dev.openrune.definition.type.BillboardType
import dev.openrune.definition.util.readUnsignedShortOrNull
import io.netty.buffer.ByteBuf

class BillboardCodec : Rs2OrderedCodec<BillboardType> {

    override fun BillboardType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> material = buffer.readUnsignedShortOrNull() ?: -1
            2 -> {
                width = buffer.readUnsignedShort()
                height = buffer.readUnsignedShort()
            }

            3 -> unknown3 = buffer.readByte().toInt()
            4 -> sourceBlendMode = buffer.readUnsignedByte().toInt()
            5 -> destinationBlendMode = buffer.readUnsignedByte().toInt()
            6 -> unknown6 = true
            7 -> unknown7 = true
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: BillboardType) {
        when (opcode) {
            1 -> writeShort(definition.material!!)
            2 -> {
                writeShort(definition.width!!)
                writeShort(definition.height!!)
            }

            3 -> writeByte(definition.unknown3!!)
            4 -> writeByte(definition.sourceBlendMode!!)
            5 -> writeByte(definition.destinationBlendMode!!)
            6, 7 -> Unit
        }
    }

    override fun createDefinition() = BillboardType()
}
