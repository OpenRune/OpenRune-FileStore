package dev.openrune.definition

import io.netty.buffer.ByteBuf

interface Recolourable {
    var originalColours: MutableList<Int>?
    var modifiedColours: MutableList<Int>?
    var originalTextureColours: MutableList<Int>?
    var modifiedTextureColours: MutableList<Int>?

    fun readColours(buffer: ByteBuf) {
        // Filled straight from the buffer into pre-sized lists. The previous form built two lists of
        // -1 and then overwrote every slot, boxing each entry twice.
        val length = buffer.readUnsignedByte().toInt()
        val original = ArrayList<Int>(length)
        val modified = ArrayList<Int>(length)
        for (count in 0 until length) {
            original.add(buffer.readShort().toInt())
            modified.add(buffer.readShort().toInt())
        }
        originalColours = original
        modifiedColours = modified
    }

    fun readTextures(buffer: ByteBuf) {
        val length = buffer.readUnsignedByte().toInt()
        val original = ArrayList<Int>(length)
        val modified = ArrayList<Int>(length)
        for (count in 0 until length) {
            original.add(buffer.readShort().toInt())
            modified.add(buffer.readShort().toInt())
        }
        originalTextureColours = original
        modifiedTextureColours = modified
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