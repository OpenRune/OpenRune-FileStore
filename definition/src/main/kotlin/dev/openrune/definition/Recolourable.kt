package dev.openrune.definition

import dev.openrune.definition.util.BoxedInts
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

    /**
     * The same palette values recolour thousands of definitions, so the boxes are pooled. The
     * unchecked view only widens the element type; the list still holds `Integer`s.
     */
    private fun readPairs(buffer: ByteBuf): Pair<MutableList<Int>, MutableList<Int>> {
        val length = buffer.readUnsignedByte().toInt()
        val original = ArrayList<Int>(length)
        val modified = ArrayList<Int>(length)

        @Suppress("UNCHECKED_CAST")
        val originalSink = original as ArrayList<Any>

        @Suppress("UNCHECKED_CAST")
        val modifiedSink = modified as ArrayList<Any>

        for (count in 0 until length) {
            originalSink.add(BoxedInts.of(buffer.readShort().toInt()))
            modifiedSink.add(BoxedInts.of(buffer.readShort().toInt()))
        }
        return original to modified
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