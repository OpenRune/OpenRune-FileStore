package dev.openrune.cache.filestore.definition

import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.buffer.Writer
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap

class DefinitionExt {

    fun Definition.readParameters(buffer: Reader) {
        val length = buffer.readUnsignedByte()
        if (length == 0) {
            return
        }
        val params = Int2ObjectArrayMap<Any>()
        for (i in 0 until length) {
            val string = buffer.readUnsignedBoolean()
            val id = buffer.readUnsignedMedium()
            params[id] = if (string) buffer.readString() else buffer.readInt()
        }
        this.values["params"] = params
    }

    fun Definition.writeParameters(writer: Writer) {
        val params2 = values["params"] as? Int2ObjectArrayMap<*>
        params2?.let { params ->
            writer.writeByte(249)
            writer.writeByte(params.size)
            params.forEach { (id, value) ->
                writer.writeByte(value is String)
                writer.writeMedium(id)
                if (value is String) {
                    writer.writeString(value)
                } else if (value is Int) {
                    writer.writeInt(value)
                }
            }
        }
    }

    fun Definition.readColours(buffer: Reader) {
        val length = buffer.readUnsignedByte()
        val originalColours = MutableList(length) { -1 }
        val modifiedColours = MutableList(length) { -1 }
        for (count in 0 until length) {
            originalColours[count] = buffer.readShort().toShort().toInt()
            modifiedColours[count] = buffer.readShort().toShort().toInt()
        }
        this.values["originalColours"] = originalColours
        this.values["modifiedColours"] = modifiedColours
    }

    fun Definition.readTextures(buffer: Reader) {
        val length = buffer.readUnsignedByte()
        val originalTextureColours = MutableList(length) { -1 }
        val modifiedTextureColours = MutableList(length) { -1 }
        for (count in 0 until length) {
            originalTextureColours[count] = buffer.readShort().toShort().toInt()
            modifiedTextureColours[count] = buffer.readShort().toShort().toInt()
        }
        this.values["originalTextureColours"] = originalTextureColours
        this.values["modifiedTextureColours"] = modifiedTextureColours
    }

    fun Definition.writeColoursTextures(writer: Writer) {
        val originalColours = values["originalColours"] as? MutableList<Int>?
        val modifiedColours = values["modifiedColours"] as? MutableList<Int>?
        val originalTextureColours = values["originalTextureColours"] as? MutableList<Int>?
        val modifiedTextureColours = values["modifiedTextureColours"] as? MutableList<Int>?
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

    fun Definition.readTransforms(buffer: Reader, isLast: Boolean) {
        var varbit = buffer.readShort()
        if (varbit == 65535) {
            varbit = -1
        }
        var varp = buffer.readShort()
        if (varp == 65535) {
            varp = -1
        }
        var last = -1
        if (isLast) {
            last = buffer.readUnsignedShort()
            if (last == 65535) {
                last = -1
            }
        }
        val length = buffer.readUnsignedByte()
        val transforms = MutableList(length + 2) { -1 }
        for (count in 0..length) {
            transforms[count] = buffer.readUnsignedShort()
            if (transforms[count] == 65535) {
                transforms[count] = -1
            }
        }
        transforms[length + 1] = last
        this.values["varbit"] = varbit
        this.values["varp"] = varp
        this.values["transforms"] = transforms
    }

    fun Definition.writeTransforms(writer: Writer, smaller: Int, larger: Int) {
        val varbit = values.getOrDefault("varbit", -1) as Int
        val varp = values.getOrDefault("varp", -1) as Int
        val transforms = values["originalTextureColours"] as? MutableList<Int>?

        val configIds = transforms
        if (configIds != null && (varbit != -1 || varp != -1)) {
            val last = configIds.last()
            val extended = last != -1
            writer.writeByte(if (extended) larger else smaller)
            writer.writeShort(varbit)
            writer.writeShort(varp)

            if (extended) {
                writer.writeShort(last)
            }
            writer.writeByte(configIds.size - 2)
            for (i in 0 until configIds.size - 1) {
                writer.writeShort(configIds[i])
            }
        }
    }
}