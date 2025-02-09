package dev.openrune.definition

import io.netty.buffer.ByteBuf
import dev.openrune.buffer.Writer
import dev.openrune.buffer.readShortRD
import dev.openrune.buffer.readUnsignedByteRD

interface Recolourable {
    var originalColours: MutableList<Int>?
    var modifiedColours: MutableList<Int>?
    var originalTextureColours: MutableList<Int>?
    var modifiedTextureColours: MutableList<Int>?

    fun readColours(buffer: ByteBuf) {
        val length = buffer.readUnsignedByteRD()
        originalColours = MutableList(length) { -1 }
        modifiedColours = MutableList(length) { -1 }
        for (count in 0 until length) {
            originalColours!![count] = buffer.readShortRD().toShort().toInt()
            modifiedColours!![count] = buffer.readShortRD().toShort().toInt()
        }
    }

    fun readTextures(buffer: ByteBuf) {
        val length = buffer.readUnsignedByteRD()
        originalTextureColours = MutableList(length) { -1 }
        modifiedTextureColours = MutableList(length) { -1 }
        for (count in 0 until length) {
            originalTextureColours!![count] = buffer.readShortRD().toShort().toInt()
            modifiedTextureColours!![count] = buffer.readShortRD().toShort().toInt()
        }
    }

    fun writeColoursTextures(writer: Writer) {
        writeArray(writer, 40, originalColours, modifiedColours)
        writeArray(writer, 41, originalTextureColours, modifiedTextureColours)
    }

    private fun writeArray(writer: Writer, opcode: Int, original: List<Int>?, modified: List<Int>?) {
        if (original != null && modified != null) {
            writer.writeByte(opcode)
            writer.writeByte(original.size)
            for (i in original.indices) {
                writer.writeShort(original[i])
                writer.writeShort(modified[i])
            }
        }
    }

}