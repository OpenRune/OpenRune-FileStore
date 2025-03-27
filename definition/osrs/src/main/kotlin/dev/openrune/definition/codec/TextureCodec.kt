package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.TextureType
import io.netty.buffer.ByteBuf

class TextureCodec : DefinitionCodec<TextureType> {
    override fun TextureType.read(opcode: Int, buffer: ByteBuf) {
        averageRgb = buffer.readUnsignedShort()
        isTransparent = buffer.readUnsignedByte().toInt() == 1
        val count: Int = buffer.readUnsignedByte().toInt()

        if (count in 1..4) {
            fileIds = IntArray(count).toMutableList()
            for (index in 0 until count) {
                fileIds[index] = buffer.readUnsignedShort()
            }

            if (count > 1) {

                combineModes = IntArray(count - 1).toMutableList()
                for (index in 0 until count - 1) {
                    combineModes[index] = buffer.readUnsignedShort()
                }

                field2440 = IntArray(count - 1).toMutableList()
                for (index in 0 until count - 1) {
                    field2440[index] = buffer.readUnsignedShort()
                }

            }

            colourAdjustments = IntArray(count).toMutableList()
            for (index in 0 until count) {
                colourAdjustments[index] = buffer.readInt()
            }

            animationDirection = buffer.readUnsignedByte().toInt()
            animationSpeed = buffer.readUnsignedByte().toInt()
        }
    }

    override fun ByteBuf.encode(definition: TextureType) {
        writeShort(definition.averageRgb)
        writeByte(if(definition.isTransparent) 1 else 0)
        val fileCount = definition.fileIds.size
        writeByte(fileCount)
        for(index in 0..<fileCount) {
            writeShort(definition.fileIds[index])
        }
        if (fileCount > 1) {
            definition.combineModes.forEach { combineMode ->
                writeByte(combineMode)
            }

            definition.field2440.forEach { field2440 ->
                writeByte(field2440)
            }

        }

        definition.colourAdjustments.forEach { colourAdjustment ->
            writeInt(colourAdjustment)
        }

        writeByte(definition.animationDirection)
        writeByte(definition.animationSpeed)
    }

    override fun createDefinition() = TextureType()

    override fun readLoop(definition: TextureType, buffer: ByteBuf) {
        definition.read(-1, buffer)
    }
}