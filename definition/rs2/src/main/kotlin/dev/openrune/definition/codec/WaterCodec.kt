package dev.openrune.definition.codec

import dev.openrune.definition.type.WaterField
import dev.openrune.definition.type.WaterType
import io.netty.buffer.ByteBuf

class WaterCodec : Rs2OrderedCodec<WaterType> {

    override fun WaterType.read(opcode: Int, buffer: ByteBuf) {
        val shape = WaterField.of(opcode) ?: error("Unknown RS3 water opcode: $opcode")
        values[opcode] = when (shape) {
            WaterField.BYTE -> listOf(buffer.readUnsignedByte().toInt())
            WaterField.SHORT -> listOf(buffer.readUnsignedShort())
            WaterField.MEDIUM -> listOf(buffer.readUnsignedMedium())
            WaterField.INT -> listOf(buffer.readInt())
            WaterField.FLOAT -> listOf(buffer.readFloat())
            WaterField.SHORT_X2 -> listOf(buffer.readUnsignedShort(), buffer.readUnsignedShort())
            WaterField.SHORT_X3 -> listOf(
                buffer.readUnsignedShort(),
                buffer.readUnsignedShort(),
                buffer.readUnsignedShort(),
            )

            WaterField.FLOAT_X2 -> listOf(buffer.readFloat(), buffer.readFloat())
            WaterField.FLOAT_X3 -> listOf(buffer.readFloat(), buffer.readFloat(), buffer.readFloat())
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: WaterType) {
        val payload = definition.values.getValue(opcode)
        when (WaterField.of(opcode) ?: error("Unknown RS3 water opcode: $opcode")) {
            WaterField.BYTE -> writeByte(payload[0].toInt())
            WaterField.SHORT -> writeShort(payload[0].toInt())
            WaterField.MEDIUM -> writeMedium(payload[0].toInt())
            WaterField.INT -> writeInt(payload[0].toInt())
            WaterField.FLOAT -> writeFloat(payload[0].toFloat())
            WaterField.SHORT_X2, WaterField.SHORT_X3 -> payload.forEach { writeShort(it.toInt()) }
            WaterField.FLOAT_X2, WaterField.FLOAT_X3 -> payload.forEach { writeFloat(it.toFloat()) }
        }
    }

    override fun createDefinition() = WaterType()
}
