package dev.openrune.definition

import dev.openrune.definition.util.IntBackedList
import dev.openrune.definition.util.readSmart
import dev.openrune.definition.util.readUnsignedShortOrNull
import io.netty.buffer.ByteBuf

/**
 * Transform ids are unsigned shorts (65535 = none) up to 241. Loc opcodes 97 and 98, added in 242,
 * carry them as signed mediums (-1 = none) instead; [mediumIds] picks between the two widths.
 */
private fun ByteBuf.readTransformId(mediumIds: Boolean): Int =
    if (mediumIds) readMedium() else readUnsignedShortOrNull() ?: -1

private fun ByteBuf.writeTransformId(value: Int, mediumIds: Boolean) {
    if (mediumIds) writeMedium(value) else writeShort(value)
}

interface Transforms {
    val multiVarBit: Int
    val multiVarp: Int
    val multiDefault: Int
    val transforms: List<Int>?

    fun writeTransforms(writer: ByteBuf, smaller: Int, larger: Int, mediumIds: Boolean = false) {
        writeTransforms(writer, smaller, larger, multiVarBit, multiVarp, multiDefault, transforms, mediumIds)
    }
}

interface MutableTransforms {
    var multiVarBit: Int
    var multiVarp: Int
    var multiDefault: Int
    var transforms: MutableList<Int>?

    fun readTransforms(
        buffer: ByteBuf,
        isLast: Boolean,
        extendedTransforms: Boolean = false,
        mediumIds: Boolean = false,
    ) {
        multiVarBit = buffer.readUnsignedShortOrNull() ?: -1
        multiVarp = buffer.readUnsignedShortOrNull() ?: -1
        multiDefault = if (isLast) buffer.readTransformId(mediumIds) else -1
        val count = if (extendedTransforms) buffer.readSmart() else buffer.readUnsignedByte().toInt()
        val ids = IntArray(count + 2)
        for (i in 0..count) {
            ids[i] = buffer.readTransformId(mediumIds)
        }
        ids[count + 1] = multiDefault
        transforms = IntBackedList(ids)
    }

    fun writeTransforms(writer: ByteBuf, smaller: Int, larger: Int, mediumIds: Boolean = false) {
        writeTransforms(writer, smaller, larger, multiVarBit, multiVarp, multiDefault, transforms, mediumIds)
    }
}

fun writeTransforms(
    writer: ByteBuf,
    smaller: Int,
    larger: Int,
    multiVarBit: Int,
    multiVarp: Int,
    multiDefault: Int,
    transforms: List<Int>?,
    mediumIds: Boolean = false,
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
        writer.writeTransformId(last, mediumIds)
    }
    writer.writeByte(ids.size - 2)
    for (i in 0 until ids.size - 1) {
        writer.writeTransformId(ids[i], mediumIds)
    }
}
