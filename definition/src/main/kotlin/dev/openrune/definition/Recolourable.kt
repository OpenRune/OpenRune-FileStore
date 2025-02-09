package dev.openrune.definition

import io.netty.buffer.ByteBuf
import dev.openrune.buffer.Writer

interface Recolourable {
    var originalColours: MutableList<Int>?
    var modifiedColours: MutableList<Int>?
    var originalTextureColours: MutableList<Int>?
    var modifiedTextureColours: MutableList<Int>?

    fun readColours(buffer: ByteBuf) {
        val length = buffer.readUnsignedByte().toInt()
        originalColours = MutableList(length) { -1 }
        modifiedColours = MutableList(length) { -1 }
        for (count in 0 until length) {
            originalColours!![count] = buffer.readShort().toInt().toShort().toInt()
            modifiedColours!![count] = buffer.readShort().toInt().toShort().toInt()
        }
    }

    fun readTextures(buffer: ByteBuf) {
        val length = buffer.readUnsignedByte().toInt()
        originalTextureColours = MutableList(length) { -1 }
        modifiedTextureColours = MutableList(length) { -1 }
        for (count in 0 until length) {
            originalTextureColours!![count] = buffer.readShort().toInt().toShort().toInt()
            modifiedTextureColours!![count] = buffer.readShort().toInt().toShort().toInt()
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