package dev.openrune.definition.codec

import dev.openrune.definition.BuilderDefinitionCodec
import dev.openrune.definition.type.InventoryType
import dev.openrune.definition.type.builders.InventoryTypeBuilder
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class InventoryCodec : BuilderDefinitionCodec<InventoryType, InventoryTypeBuilder> {
    override fun builder(id: Int) = InventoryTypeBuilder(id)

    override fun build(builder: InventoryTypeBuilder) = builder.build()

    override fun InventoryTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
        when(opcode) {
            2 -> size = buffer.readUnsignedShort()
            249 -> readParameters(buffer)
        }
    }

    override fun ByteBuf.encode(definition: InventoryType) {
        if (definition.size != 0) {
            writeByte(2)
            writeShort(definition.size)
        }

        definition.writeParameters(this)

        writeByte(0)
    }

    override fun createDefinition() = InventoryType()
}
