package dev.openrune.cache.filestore.definition

import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.serialization.UShortList

interface Recolourable {

    var originalColours: UShortList
    var modifiedColours: UShortList
    var originalTextureColours: UShortList
    var modifiedTextureColours: UShortList

    fun readColours(buffer: Reader) {
        val size = buffer.readUnsignedByteOLD()
        val tempOriginalColours = MutableList(size) { 0.toUShort() }
        val tempModifiedColours = MutableList(size) { 0.toUShort() }

        for (i in 0 until size) {
            tempOriginalColours[i] = buffer.readUnsignedShortOLD().toUShort()
            tempModifiedColours[i] = buffer.readUnsignedShortOLD().toUShort()
        }

        originalColours = tempOriginalColours
        modifiedColours = tempModifiedColours
    }

    fun readTextures(buffer: Reader) {
        val size = buffer.readUnsignedByteOLD()
        val tempOriginalTextures = MutableList(size) { 0.toUShort() }
        val tempModifiedTextures = MutableList(size) { 0.toUShort() }

        for (i in 0 until size) {
            tempOriginalTextures[i] = buffer.readShortOLD().toUShort()
            tempModifiedTextures[i] = buffer.readShortOLD().toUShort()
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
