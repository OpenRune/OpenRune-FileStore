package dev.openrune.definition.codec

import dev.openrune.definition.type.LightType
import io.netty.buffer.ByteBuf

class LightCodec : Rs2OrderedCodec<LightType> {

    override fun LightType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> function = buffer.readUnsignedByte().toInt()
            2 -> frequency = buffer.readUnsignedShort()
            3 -> amplitude = buffer.readUnsignedShort()
            4 -> offset = buffer.readShort().toInt()
            5 -> unknown5 = buffer.readInt()
            6 -> unknown6 = buffer.readInt()
            7 -> unknown7 = buffer.readInt()
            8 -> swayAmount = buffer.readFloat()
            9 -> swayAmountRandom = true
            10 -> swayDuration = buffer.readInt()
            11 -> swayDurationRandom = buffer.readInt()
            12 -> swayEasing = buffer.readFloat()
            13 -> swayFade = buffer.readFloat()
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: LightType) {
        when (opcode) {
            1 -> writeByte(definition.function!!)
            2 -> writeShort(definition.frequency!!)
            3 -> writeShort(definition.amplitude!!)
            4 -> writeShort(definition.offset!!)
            5 -> writeInt(definition.unknown5!!)
            6 -> writeInt(definition.unknown6!!)
            7 -> writeInt(definition.unknown7!!)
            8 -> writeFloat(definition.swayAmount!!)
            9 -> Unit
            10 -> writeInt(definition.swayDuration!!)
            11 -> writeInt(definition.swayDurationRandom!!)
            12 -> writeFloat(definition.swayEasing!!)
            13 -> writeFloat(definition.swayFade!!)
        }
    }

    override fun createDefinition() = LightType()
}
