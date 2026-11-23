package dev.openrune.definition.codec

import dev.openrune.definition.type.ParticleEffectorType
import io.netty.buffer.ByteBuf

class ParticleEffectorCodec : Rs2OrderedCodec<ParticleEffectorType> {

    override fun ParticleEffectorType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> angle = buffer.readUnsignedShort()
            2 -> unknown2 = buffer.readUnsignedByte().toInt()
            3 -> {
                forceDirectionX = buffer.readInt()
                forceDirectionY = buffer.readInt()
                forceDirectionZ = buffer.readInt()
            }

            4 -> {
                falloffType = buffer.readUnsignedByte().toInt()
                falloffValue = buffer.readInt()
            }

            6 -> forceType = buffer.readUnsignedByte().toInt()
            8 -> unknown8 = true
            9 -> unknown9 = true
            10 -> reverseDirection = true
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: ParticleEffectorType) {
        when (opcode) {
            1 -> writeShort(definition.angle!!)
            2 -> writeByte(definition.unknown2!!)
            3 -> {
                writeInt(definition.forceDirectionX!!)
                writeInt(definition.forceDirectionY!!)
                writeInt(definition.forceDirectionZ!!)
            }

            4 -> {
                writeByte(definition.falloffType!!)
                writeInt(definition.falloffValue!!)
            }

            6 -> writeByte(definition.forceType!!)
            8, 9, 10 -> Unit
        }
    }

    override fun createDefinition() = ParticleEffectorType()
}
