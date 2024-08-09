package dev.openrune.cache.filestore.definition

import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.serialization.UShortCustomSerializer

interface Recolourable {

    var originalColours: List<UShort>
    var modifiedColours: List<UShort>
    var originalTextureColours: List<UShort>
    var modifiedTextureColours: List<UShort>

    fun readColours(buffer: Reader) {
        val size = buffer.readUnsignedByte()
        val tempOriginalColours = MutableList(size) { 0.toUShort() }
        val tempModifiedColours = MutableList(size) { 0.toUShort() }

        for (i in 0 until size) {
            tempOriginalColours[i] = buffer.readUnsignedShort().toUShort()
            tempModifiedColours[i] = buffer.readUnsignedShort().toUShort()
        }

        originalColours = tempOriginalColours
        modifiedColours = tempModifiedColours
    }

    fun readTextures(buffer: Reader) {
        val size = buffer.readUnsignedByte()
        val tempOriginalTextures = MutableList(size) { 0.toUShort() }
        val tempModifiedTextures = MutableList(size) { 0.toUShort() }

        for (i in 0 until size) {
            tempOriginalTextures[i] = buffer.readShort().toUShort()
            tempModifiedTextures[i] = buffer.readShort().toUShort()
        }

        originalTextureColours = tempOriginalTextures
        modifiedTextureColours = tempModifiedTextures
    }

    fun writeColoursTextures(writer: Writer) {
        writeList(writer, 40, originalColours, modifiedColours)
        writeList(writer, 41, originalTextureColours, modifiedTextureColours)
    }

    private fun writeList(writer: Writer, opcode: Int, original: List<UShort>, modified: List<UShort>) {
        if (original.isEmpty() || modified.isEmpty()) {
            return
        }
        writer.writeByte(opcode)
        writer.writeByte(original.size)
        for (i in original.indices) {
            writer.writeShort(original[i].toInt())
            writer.writeShort(modified[i].toInt())
        }
    }

}
