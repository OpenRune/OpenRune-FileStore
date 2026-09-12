package dev.openrune.definition

import dev.openrune.definition.util.IntBackedList
import io.netty.buffer.ByteBuf

interface Recolourable {
    var originalColours: MutableList<Int>?
    var modifiedColours: MutableList<Int>?
    var originalTextureColours: MutableList<Int>?
    var modifiedTextureColours: MutableList<Int>?

    fun readColours(buffer: ByteBuf) {
        val (original, modified) = readPairs(buffer)
        originalColours = original
        modifiedColours = modified
    }

    fun readTextures(buffer: ByteBuf) {
        val (original, modified) = readPairs(buffer)
        originalTextureColours = original
        modifiedTextureColours = modified
    }

    /** Palette pairs are stored array-backed, so the lists hold no boxed elements. */
    private fun readPairs(buffer: ByteBuf): Pair<MutableList<Int>, MutableList<Int>> {
        val length = buffer.readUnsignedByte().toInt()
        val original = IntArray(length)
        val modified = IntArray(length)

        for (count in 0 until length) {
            original[count] = buffer.readShort().toInt()
            modified[count] = buffer.readShort().toInt()
        }
        return IntBackedList(original) to IntBackedList(modified)
    }

    fun writeColoursTextures(writer: ByteBuf) {
        writeArray(writer, 40, originalColours, modifiedColours)
        writeArray(writer, 41, originalTextureColours, modifiedTextureColours)
    }

    private fun writeArray(writer: ByteBuf, opcode: Int, original: List<Int>?, modified: List<Int>?) {
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