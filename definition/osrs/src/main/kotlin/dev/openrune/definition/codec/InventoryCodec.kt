package dev.openrune.definition.codec

import io.netty.buffer.ByteBuf
import dev.openrune.buffer.Writer
import dev.openrune.buffer.readUnsignedShortRD
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.InventoryType

class InventoryCodec : DefinitionCodec<InventoryType> {
    override fun InventoryType.read(opcode: Int, buffer: ByteBuf) {
        if (opcode == 2) {
            size = buffer.readUnsignedShortRD()
        }
    }

    override fun Writer.encode(definition: InventoryType) {
        if (definition.size != 0) {
            writeByte(2)
            writeShort(definition.size)
        }
        writeByte(0)
    }

    override fun createDefinition() = InventoryType()
}