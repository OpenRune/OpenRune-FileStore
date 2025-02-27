package dev.openrune.filesystem.util

import java.nio.ByteBuffer

internal fun ByteBuffer.readUnsignedByte() = readByte() and 0xff

internal fun ByteBuffer.readInt() = (readUnsignedByte() shl 24) or (readUnsignedByte() shl 16) or (readUnsignedByte() shl 8) or readUnsignedByte()

internal fun ByteBuffer.readByte() = get().toInt()