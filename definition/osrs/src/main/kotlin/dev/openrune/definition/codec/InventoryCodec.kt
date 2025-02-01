package dev.openrune.definition.codec

import dev.openrune.buffer.Reader
import dev.openrune.buffer.Writer
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.InventoryType
import dev.openrune.definition.type.VarpType

class InventoryCodec : DefinitionCodec<InventoryType> {
    override fun InventoryType.read(opcode: Int, buffer: Reader) {
        if (opcode == 2) {
            size = buffer.readUnsignedShort()
        }
    }

    override fun Writer.encode(definition: InventoryType) {
        if (definition.size != 0) {
            writeByte(2)
            writeShort(definition.size)
        }
    }

    override fun createDefinition() = InventoryType()
}