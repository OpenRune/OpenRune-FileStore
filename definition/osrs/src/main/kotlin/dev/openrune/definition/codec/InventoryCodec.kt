package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.InventoryType
import io.netty.buffer.ByteBuf

class InventoryCodec : DefinitionCodec<InventoryType> {
    override fun InventoryType.read(opcode: Int, buffer: ByteBuf) {
        if (opcode == 2) {
            size = buffer.readUnsignedShort()
        }
    }

    override fun ByteBuf.encode(definition: InventoryType) {
        if (definition.size != 0) {
            writeByte(2)
            writeShort(definition.size)
        }
        writeByte(0)
    }

    override fun createDefinition() = InventoryType()
}