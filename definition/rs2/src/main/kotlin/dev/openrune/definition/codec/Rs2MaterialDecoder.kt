package dev.openrune.definition.codec

import dev.openrune.definition.type.Rs2MaterialType
import io.netty.buffer.ByteBuf

object Rs2MaterialDecoder {

    fun decode(id: Int, buffer: ByteBuf): Rs2MaterialType {
        val material = Rs2MaterialType(id = id)
        material.version = buffer.readUnsignedByte().toInt()
        when (material.version) {
            0 -> decodeRt5(material, buffer)
            1, 2 -> decodeRt7(material, buffer)
            else -> error("Material $id: unsupported version ${material.version}")
        }
        check(!buffer.isReadable) { "Material $id: ${buffer.readableBytes()} trailing bytes" }
        return material
    }

    fun materialSize(value: Int): Int = when (value) {
        0 -> 64
        1 -> 128
        2 -> 256
        3 -> 512
        4 -> 1024
        else -> value
    }

    private fun decodeRt5(material: Rs2MaterialType, buffer: ByteBuf) {
        buffer.readUnsignedByte()
        material.diffuseAlphaSize = materialSize(buffer.readUnsignedByte().toInt())
        val flagsA = buffer.readInt()

        val flagA0 = flagsA and 0x1 != 0
        val flagA1 = flagsA and 0x2 != 0
        val flagA3 = flagsA and 0x8 != 0
        val flagA4 = flagsA and 0x10 != 0

        if (flagA0 || flagA4) material.diffuseAlpha = buffer.readInt()
        if (flagA3 || flagA1) material.normalEmissive = buffer.readInt()

        material.readRepeat(buffer)
        val flagsB = buffer.readInt()

        if (flagsB and 0x20 != 0) {
            material.metalnessRoughnessSize = materialSize(buffer.readUnsignedByte().toInt())
            material.metalnessRoughness = buffer.readInt()
        }
        if (flagsB and 0x40 != 0) {
            material.environmentSize = materialSize(buffer.readUnsignedByte().toInt())
            material.environment = buffer.readInt()
        }

        if (flagsB and 0x40000 != 0) buffer.skipBytes(4)
        if (flagsB and 0x80000 != 0) buffer.skipBytes(20)
        if (flagsB and 0x10 != 0) buffer.skipBytes(8)
        if (flagA1) buffer.skipBytes(4)

        material.normal = buffer.readUnsignedByte().toInt() == 1
        material.facetMode = buffer.readUnsignedByte().toInt()
        material.readAlphaMode(buffer)

        if (flagsB and 0x800 != 0) buffer.skipBytes(12)

        val flagsC = buffer.readUnsignedByte().toInt()
        if (flagsC and 0x1 != 0) material.speedU = buffer.readShort().toInt()
        if (flagsC and 0x2 != 0) material.speedV = buffer.readShort().toInt()

        if (buffer.readUnsignedByte().toInt() == 1) {
            material.effect = buffer.readUnsignedByte().toInt()
            buffer.readUnsignedByte()
            buffer.readInt()
            buffer.readUnsignedByte()
            buffer.readUnsignedByte()
            material.mipmapping = buffer.readUnsignedByte().toInt()
            buffer.readUnsignedByte()
            buffer.readUnsignedByte()
            material.lightness = buffer.readUnsignedByte().toInt()
            material.saturation = buffer.readUnsignedByte().toInt()
            material.averageColour = buffer.readUnsignedShort()
        }
    }

    private fun decodeRt7(material: Rs2MaterialType, buffer: ByteBuf) {
        val flags = buffer.readInt()
        check(flags ushr 22 == 0) { "Material ${material.id}: invalid flags ${Integer.toHexString(flags)}" }

        val flagB6 = flags and 0x40 != 0

        if (flags and 0x20 != 0) {
            material.diffuseAlphaSize = materialSize(buffer.readUnsignedByte().toInt())
            material.diffuseAlpha = buffer.readInt()
        }
        if (flagB6) {
            material.normalEmissiveSize = materialSize(buffer.readUnsignedByte().toInt())
            material.normalEmissive = buffer.readInt()
        }
        if (flags and 0x80 != 0) {
            material.metalnessRoughnessSize = materialSize(buffer.readUnsignedByte().toInt())
            material.metalnessRoughness = buffer.readInt()
        }

        if (flags and 0x40000 != 0) buffer.skipBytes(4)
        if (flags and 0x80000 != 0) buffer.skipBytes(20)
        if (flags and 0x1000 != 0) buffer.skipBytes(4)
        if (flags and 0x2000 != 0) buffer.skipBytes(4)
        if (flags and 0x4000 != 0) buffer.skipBytes(4)
        if (flags and 0x8000 != 0) buffer.skipBytes(4)
        if (flagB6) buffer.skipBytes(4)
        if (flags and 0x800 != 0) buffer.skipBytes(12)
        if (flags and 0x10000 != 0) buffer.skipBytes(4)
        if (flags and 0x20000 != 0) buffer.skipBytes(4)

        if (flags and 0x100 != 0) material.speedU = buffer.readShort().toInt()
        if (flags and 0x200 != 0) material.speedV = buffer.readShort().toInt()

        material.readRepeat(buffer)
        material.facetMode = buffer.readUnsignedByte().toInt()
        material.qualityMode = buffer.readUnsignedByte().toInt()
        material.readAlphaMode(buffer)
        material.averageColour = buffer.readUnsignedShort()
        material.size = buffer.readUnsignedByte().toInt()
    }

    private fun Rs2MaterialType.readRepeat(buffer: ByteBuf) {
        val repeat = buffer.readUnsignedByte().toInt()
        repeatU = repeat and 7
        repeatV = repeat shr 3 and 7
    }

    private fun Rs2MaterialType.readAlphaMode(buffer: ByteBuf) {
        alphaMode = buffer.readUnsignedByte().toInt()
        when (alphaMode) {
            0, 2 -> Unit
            1 -> alphaTest = buffer.readUnsignedByte().toInt()
            else -> error("Material $id: unexpected alpha mode $alphaMode")
        }
    }
}
