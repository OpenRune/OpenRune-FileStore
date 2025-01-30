package dev.openrune

import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.Definition

fun Definition.getColourPalette(key: String): ColourPalette {
    return (extra[key] as? ColourPalette) ?: ColourPalette.default()
}


interface ColourPalette {

    companion object {
        fun default(length: Int = 1): ColourPalette {
            return object : ColourPalette {
                override var recolourPalette: ByteArray? = ByteArray(length) { -1 }
            }
        }
    }

    var recolourPalette: ByteArray?

    fun readColourPalette(buffer: Reader) {
        val length = buffer.readUnsignedByte()
        recolourPalette = ByteArray(length) { buffer.readByte().toByte() }
    }

    fun writeRecolourPalette(writer: Writer) {
        val palette = recolourPalette
        if (palette != null) {
            writer.writeByte(42)
            writer.writeByte(palette.size)
            for (colour in palette) {
                writer.writeByte(colour.toInt())
            }
        }
    }
}