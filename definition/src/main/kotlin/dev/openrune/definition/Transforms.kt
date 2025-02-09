package dev.openrune.definition

import dev.openrune.buffer.*
import io.netty.buffer.ByteBuf

interface Transforms {
    var varbit: Int
    var varp: Int
    var transforms: MutableList<Int>?

    fun readTransforms(buffer: ByteBuf, isLast: Boolean) {
        varbit = buffer.readShortRD()
        if (varbit == 65535) {
            varbit = -1
        }
        varp = buffer.readShortRD()
        if (varp == 65535) {
            varp = -1
        }
        var last = -1
        if (isLast) {
            last = buffer.readUnsignedShortRD()
            if (last == 65535) {
                last = -1
            }
        }
        val length = buffer.readUnsignedByteRD()
        transforms = MutableList(length + 2) { -1 }
        for (count in 0..length) {
            transforms!![count] = buffer.readUnsignedShortRD()
            if (transforms!![count] == 65535) {
                transforms!![count] = -1
            }
        }
        transforms!![length + 1] = last
    }

    fun writeTransforms(writer: Writer, smaller: Int, larger: Int) {
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