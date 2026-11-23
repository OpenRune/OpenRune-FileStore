package dev.openrune.definition

import dev.openrune.definition.util.IntBackedList
import dev.openrune.definition.util.readNullableLargeSmartCorrect
import dev.openrune.definition.util.readSmart
import dev.openrune.definition.util.readUnsignedShortOrNull
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import dev.openrune.definition.util.writeSmart
import io.netty.buffer.ByteBuf

private fun ByteBuf.readTransformId(mediumIds: Boolean, smartIds: Boolean = false): Int = when {
    smartIds -> readNullableLargeSmartCorrect() ?: -1
    mediumIds -> readMedium()
    else -> readUnsignedShortOrNull() ?: -1
}

private fun ByteBuf.writeTransformId(value: Int, mediumIds: Boolean, smartIds: Boolean = false) {
    when {
        smartIds -> writeNullableLargeSmartCorrect(value)
        mediumIds -> writeMedium(value)
        else -> writeShort(value)
    }
}

private fun ByteBuf.readTransformVarBit(mediumVarBit: Boolean): Int =
    if (mediumVarBit) readMedium() else readUnsignedShortOrNull() ?: -1

private fun ByteBuf.writeTransformVarBit(value: Int, mediumVarBit: Boolean) {
    if (mediumVarBit) writeMedium(value) else writeShort(value)
}

interface Transforms {
    val multiVarBit: Int
    val multiVarp: Int
    val multiDefault: Int
    val transforms: List<Int>?

    fun writeTransforms(
        writer: ByteBuf,
        smaller: Int,
        larger: Int,
        mediumIds: Boolean = false,
        mediumVarBit: Boolean = false,
    ) {
        writeTransforms(
            writer, smaller, larger, multiVarBit, multiVarp, multiDefault, transforms, mediumIds, mediumVarBit,
        )
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
        mediumVarBit: Boolean = false,
        smartIds: Boolean = false,
    ) {
        multiVarBit = buffer.readTransformVarBit(mediumVarBit)
        multiVarp = buffer.readUnsignedShortOrNull() ?: -1
        multiDefault = if (isLast) buffer.readTransformId(mediumIds, smartIds) else -1
        val count = if (extendedTransforms) buffer.readSmart() else buffer.readUnsignedByte().toInt()
        val ids = IntArray(count + 2)
        for (i in 0..count) {
            ids[i] = buffer.readTransformId(mediumIds, smartIds)
        }
        ids[count + 1] = multiDefault
        transforms = IntBackedList(ids)
    }

    fun writeTransforms(
        writer: ByteBuf,
        smaller: Int,
        larger: Int,
        mediumIds: Boolean = false,
        mediumVarBit: Boolean = false,
    ) {
        writeTransforms(
            writer, smaller, larger, multiVarBit, multiVarp, multiDefault, transforms, mediumIds, mediumVarBit,
        )
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
    mediumVarBit: Boolean = false,
) {
    val ids = transforms
    if (ids == null || (multiVarBit == -1 && multiVarp == -1)) {
        return
    }
    val last = multiDefault.takeUnless { it == -1 } ?: ids.last()
    val hasDefault = last != -1
    writer.writeByte(if (hasDefault) larger else smaller)
    writer.writeTransformVarBit(multiVarBit, mediumVarBit)
    writer.writeShort(multiVarp)
    if (hasDefault) {
        writer.writeTransformId(last, mediumIds)
    }
    writer.writeByte(ids.size - 2)
    for (i in 0 until ids.size - 1) {
        writer.writeTransformId(ids[i], mediumIds)
    }
}

fun writeTransformBody(
    writer: ByteBuf,
    multiVarBit: Int,
    multiVarp: Int,
    multiDefault: Int,
    transforms: List<Int>?,
    hasDefault: Boolean,
    extendedTransforms: Boolean = false,
    mediumIds: Boolean = false,
    mediumVarBit: Boolean = false,
    smartIds: Boolean = false,
) {
    val ids = transforms.orEmpty()
    writer.writeTransformVarBit(multiVarBit, mediumVarBit)
    writer.writeShort(multiVarp)
    if (hasDefault) {
        writer.writeTransformId(multiDefault, mediumIds, smartIds)
    }
    val count = ids.size - 2
    if (extendedTransforms) writer.writeSmart(count) else writer.writeByte(count)
    for (i in 0 until ids.size - 1) {
        writer.writeTransformId(ids[i], mediumIds, smartIds)
    }
}
