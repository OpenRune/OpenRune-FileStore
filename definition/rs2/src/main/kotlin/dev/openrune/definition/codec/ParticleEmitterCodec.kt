package dev.openrune.definition.codec

import dev.openrune.definition.type.ParticleEmitterType
import io.netty.buffer.ByteBuf

class ParticleEmitterCodec : Rs2OrderedCodec<ParticleEmitterType> {

    private fun ByteBuf.readShortList(count: Int) = MutableList(count) { readUnsignedShort() }

    private fun ByteBuf.writeCountedShorts(values: List<Int>) {
        writeByte(values.size)
        values.forEach { writeShort(it) }
    }

    override fun ParticleEmitterType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> coneAngle = buffer.readShortList(4)
            2 -> unknown2 = buffer.readUnsignedByte().toInt()
            3 -> {
                speedMin = buffer.readInt()
                speedMax = buffer.readInt()
            }

            4 -> {
                speedControlType = buffer.readUnsignedByte().toInt()
                speedControlValue = buffer.readByte().toInt()
            }

            5 -> size = buffer.readUnsignedShort()
            6 -> {
                rgbaFrom = buffer.readInt()
                rgbaTo = buffer.readInt()
            }

            7 -> {
                lifetimeMin = buffer.readUnsignedShort()
                lifetimeMax = buffer.readUnsignedShort()
            }

            8 -> {
                emissionRateMin = buffer.readUnsignedShort()
                emissionRateMax = buffer.readUnsignedShort()
            }

            9 -> worldEffectors = buffer.readShortList(buffer.readUnsignedByte().toInt())
            10 -> localEffectors = buffer.readShortList(buffer.readUnsignedByte().toInt())
            12 -> killBelowLevel = buffer.readByte().toInt()
            13 -> killAboveLevel = buffer.readByte().toInt()
            14 -> unknown14 = buffer.readUnsignedShort()
            15 -> material = buffer.readUnsignedShort()
            16 -> unknown16 = mutableListOf(
                buffer.readUnsignedByte().toInt(),
                buffer.readUnsignedShort(),
                buffer.readUnsignedShort(),
                buffer.readUnsignedByte().toInt(),
            )

            17 -> unknown17 = buffer.readUnsignedShort()
            18 -> rgbaTarget = buffer.readInt()
            19 -> unknown19 = buffer.readUnsignedByte().toInt()
            20 -> rgbTransitionPercent = buffer.readUnsignedByte().toInt()
            21 -> alphaTransitionPercent = buffer.readUnsignedByte().toInt()
            22 -> speedTarget = buffer.readInt()
            23 -> speedTargetPercent = buffer.readUnsignedByte().toInt()
            24 -> linkColourChannels = false
            25 -> globalEffectors = buffer.readShortList(buffer.readUnsignedByte().toInt())
            26 -> enabled = false
            27 -> sizeTarget = buffer.readUnsignedShort()
            28 -> sizeTargetPercent = buffer.readUnsignedByte().toInt()
            29 -> {
                val variant = buffer.readUnsignedByte().toInt()
                angularVelocityVariant = variant
                angularVelocity = if (variant == 0) {
                    mutableListOf(buffer.readShort().toInt())
                } else {
                    mutableListOf(buffer.readShort().toInt(), buffer.readShort().toInt())
                }
            }

            30 -> unknown30 = true
            31 -> {
                sizeRangeMin = buffer.readUnsignedShort()
                sizeRangeMax = buffer.readUnsignedShort()
            }

            32 -> lighting = false
            33 -> unknown33 = true
            34 -> unknown34 = false
            35 -> {
                val variant = buffer.readUnsignedByte().toInt()
                rotationSpawnVariant = variant
                rotationSpawn = if (variant == 0) {
                    mutableListOf(buffer.readShort().toInt())
                } else {
                    mutableListOf(
                        buffer.readShort().toInt(),
                        buffer.readShort().toInt(),
                        buffer.readUnsignedByte().toInt(),
                    )
                }
            }

            36 -> unknown36 = true
            37 -> unknown37 = true
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: ParticleEmitterType) {
        when (opcode) {
            1 -> definition.coneAngle!!.forEach { writeShort(it) }
            2 -> writeByte(definition.unknown2!!)
            3 -> {
                writeInt(definition.speedMin!!)
                writeInt(definition.speedMax!!)
            }

            4 -> {
                writeByte(definition.speedControlType!!)
                writeByte(definition.speedControlValue!!)
            }

            5 -> writeShort(definition.size!!)
            6 -> {
                writeInt(definition.rgbaFrom!!)
                writeInt(definition.rgbaTo!!)
            }

            7 -> {
                writeShort(definition.lifetimeMin!!)
                writeShort(definition.lifetimeMax!!)
            }

            8 -> {
                writeShort(definition.emissionRateMin!!)
                writeShort(definition.emissionRateMax!!)
            }

            9 -> writeCountedShorts(definition.worldEffectors!!)
            10 -> writeCountedShorts(definition.localEffectors!!)
            12 -> writeByte(definition.killBelowLevel!!)
            13 -> writeByte(definition.killAboveLevel!!)
            14 -> writeShort(definition.unknown14!!)
            15 -> writeShort(definition.material!!)
            16 -> {
                val it = definition.unknown16!!
                writeByte(it[0])
                writeShort(it[1])
                writeShort(it[2])
                writeByte(it[3])
            }

            17 -> writeShort(definition.unknown17!!)
            18 -> writeInt(definition.rgbaTarget!!)
            19 -> writeByte(definition.unknown19!!)
            20 -> writeByte(definition.rgbTransitionPercent!!)
            21 -> writeByte(definition.alphaTransitionPercent!!)
            22 -> writeInt(definition.speedTarget!!)
            23 -> writeByte(definition.speedTargetPercent!!)
            25 -> writeCountedShorts(definition.globalEffectors!!)
            27 -> writeShort(definition.sizeTarget!!)
            28 -> writeByte(definition.sizeTargetPercent!!)
            29 -> {
                writeByte(definition.angularVelocityVariant!!)
                definition.angularVelocity!!.forEach { writeShort(it) }
            }

            31 -> {
                writeShort(definition.sizeRangeMin!!)
                writeShort(definition.sizeRangeMax!!)
            }

            35 -> {
                val variant = definition.rotationSpawnVariant!!
                val rotation = definition.rotationSpawn!!
                writeByte(variant)
                if (variant == 0) {
                    writeShort(rotation[0])
                } else {
                    writeShort(rotation[0])
                    writeShort(rotation[1])
                    writeByte(rotation[2])
                }
            }

            11, 24, 26, 30, 32, 33, 34, 36, 37 -> Unit
        }
    }

    override fun createDefinition() = ParticleEmitterType()
}
