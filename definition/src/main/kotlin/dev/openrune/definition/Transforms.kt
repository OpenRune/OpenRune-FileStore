package dev.openrune.definition

import dev.openrune.definition.util.IntBackedList
import dev.openrune.definition.util.readSmart
import dev.openrune.definition.util.readUnsignedShortOrNull
import io.netty.buffer.ByteBuf

interface Transforms {
    var multiVarBit: Int
    var multiVarp: Int
    var multiDefault: Int
    var transforms: MutableList<Int>?

    fun readTransforms(buffer: ByteBuf, isLast: Boolean, extendedTransforms: Boolean = false) {
        multiVarBit = buffer.readUnsignedShortOrNull() ?: -1
        multiVarp = buffer.readUnsignedShortOrNull() ?: -1
        multiDefault = if (isLast) buffer.readUnsignedShortOrNull() ?: -1 else -1
        val count = if (extendedTransforms) buffer.readSmart() else buffer.readUnsignedByte().toInt()
        val ids = IntArray(count + 2)
        for (i in 0..count) {
            ids[i] = buffer.readUnsignedShortOrNull() ?: -1
        }
        ids[count + 1] = multiDefault
        transforms = IntBackedList(ids)
    }

    fun writeTransforms(writer: ByteBuf, smaller: Int, larger: Int) {
        writeTransforms(writer, smaller, larger, multiVarBit, multiVarp, multiDefault, transforms)
    }
}

/** Standalone form for immutable types that carry the transform fields without the interface. */
fun writeTransforms(
    writer: ByteBuf,
    smaller: Int,
    larger: Int,
    multiVarBit: Int,
    multiVarp: Int,
    multiDefault: Int,
    transforms: List<Int>?,
) {
    val ids = transforms
    if (ids == null || (multiVarBit == -1 && multiVarp == -1)) {
        return
    }
    val last = multiDefault.takeUnless { it == -1 } ?: ids.last()
    val hasDefault = last != -1
    writer.writeByte(if (hasDefault) larger else smaller)
    writer.writeShort(multiVarBit)
    writer.writeShort(multiVarp)
    if (hasDefault) {
        writer.writeShort(last)
    }
    writer.writeByte(ids.size - 2)
    for (i in 0 until ids.size - 1) {
        writer.writeShort(ids[i])
    }
}
