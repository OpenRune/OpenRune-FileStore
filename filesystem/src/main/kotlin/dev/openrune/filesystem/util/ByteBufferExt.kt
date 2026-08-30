package dev.openrune.filesystem.util

import java.nio.ByteBuffer

internal fun ByteBuffer.readUnsignedByte() = get().toInt() and 0xff

/**
 * Big-endian int read. [ByteBuffer] is big-endian by default, so this delegates to the
 * intrinsic rather than assembling the value from four single byte reads.
 */
internal fun ByteBuffer.readInt() = int

internal fun ByteBuffer.readByte() = get().toInt()
