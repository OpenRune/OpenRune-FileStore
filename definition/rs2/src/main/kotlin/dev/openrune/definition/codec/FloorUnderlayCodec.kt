package dev.openrune.definition.codec

import dev.openrune.definition.type.FloorUnderlayType
import dev.openrune.definition.util.readUnsignedShortOrNull
import io.netty.buffer.ByteBuf

class FloorUnderlayCodec : Rs2OrderedCodec<FloorUnderlayType> {

    override fun FloorUnderlayType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> colour = buffer.readUnsignedMedium()
            2 -> material = buffer.readUnsignedShortOrNull() ?: -1
            3 -> textureScale = buffer.readUnsignedShort()
            4 -> hardShadow = false
            5 -> occlude = false
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: FloorUnderlayType) {
        when (opcode) {
            1 -> writeMedium(definition.colour)
            2 -> writeShort(definition.material!!)
            3 -> writeShort(definition.textureScale!!)
            4, 5 -> Unit
        }
    }

    override fun createDefinition() = FloorUnderlayType()
}
