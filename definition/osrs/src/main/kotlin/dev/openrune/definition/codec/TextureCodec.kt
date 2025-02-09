package dev.openrune.definition.codec

import dev.openrune.buffer.*
import io.netty.buffer.ByteBuf
import dev.openrune.buffer.Writer
import dev.openrune.buffer.readIntRD
import dev.openrune.buffer.readUnsignedShortRD
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.TextureType

class TextureCodec : DefinitionCodec<TextureType> {
    override fun TextureType.read(opcode: Int, buffer: ByteBuf) {
        averageRgb = buffer.readUnsignedShortRD()
        isTransparent = buffer.readUnsignedByteRD() == 1
        val count: Int = buffer.readUnsignedByteRD()

        if (count in 1..4) {
            fileIds = IntArray(count).toMutableList()
            for (index in 0 until count) {
                fileIds[index] = buffer.readUnsignedShortRD()
            }

            if (count > 1) {

                combineModes = IntArray(count -1).toMutableList()
                for (index in 0 until count - 1) {
                    combineModes[index] = buffer.readUnsignedShortRD()
                }

                field2440 = IntArray(count -1).toMutableList()
                for (index in 0 until count - 1) {
                    field2440[index] = buffer.readUnsignedShortRD()
                }

            }

            colourAdjustments = IntArray(count).toMutableList()
            for (index in 0 until count) {
                colourAdjustments[index] = buffer.readIntRD()
            }

            animationDirection = buffer.readUnsignedByteRD()
            animationSpeed = buffer.readUnsignedByteRD()
        }
    }

    override fun Writer.encode(definition: TextureType) {
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