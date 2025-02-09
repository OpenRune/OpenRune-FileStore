package dev.openrune.buffer

import java.nio.ByteBuffer

class Reader(
    val buffer: ByteBuffer
) {

    constructor(array: ByteArray) : this(buffer = ByteBuffer.wrap(array))

    val length: Int = buffer.remaining()

    fun readByte(): Int {
        return buffer.get().toInt()
    }

    fun readUnsignedByte(): Int {
        return readByte() and 0xff
    }

    fun readShort(): Int {
        return (readByte() shl 8) or readUnsignedByte()
    }

    fun readShortSmart() : Int {
        val peek = readUnsignedByte()
        return if (peek < 128) peek - 64 else (peek shl 8 or readUnsignedByte()) - 49152
    }

    fun readUnsignedShort(): Int {
        return (readUnsignedByte() shl 8) or readUnsignedByte()
    }

    fun readMedium(): Int {
        return (readByte() shl 16) or (readByte() shl 8) or readUnsignedByte()
    }

    fun readUnsignedMedium(): Int {
        return (readUnsignedByte() shl 16) or (readUnsignedByte() shl 8) or readUnsignedByte()
    }

    fun readInt(): Int {
        return (readUnsignedByte() shl 24) or (readUnsignedByte() shl 16) or (readUnsignedByte() shl 8) or readUnsignedByte()
    }

    fun readSmart(): Int {
        val peek = readUnsignedByte()
        return if (peek < 128) {
            peek and 0xFF
        } else {
            (peek shl 8 or readUnsignedByte()) - 32768
        }
    }

    fun readBigSmart(): Int {
        val peek = readByte()
        return if (peek < 0) {
            ((peek shl 24) or (readUnsignedByte() shl 16) or (readUnsignedByte() shl 8) or readUnsignedByte()) and 0x7fffffff
        } else {
            val value = (peek shl 8) or readUnsignedByte()
            if (value == 32767) -1 else value
        }
    }

    fun readLargeSmart(): Int {
        var baseValue = 0
        var lastValue = readSmart()
        while (lastValue == 32767) {
            lastValue = readSmart()
            baseValue += 32767
        }
        return baseValue + lastValue
    }

    fun readString(): String {
        val sb = StringBuilder()
        var b: Int
        while (buffer.hasRemaining()) {
            b = readUnsignedByte()
            if (b == 0) {
                break
            }
            sb.append(b.toChar())
        }
        return sb.toString()
    }

    fun position(): Int {
        return buffer.position()
    }

    fun position(index: Int) {
        buffer.position(index)
    }

    fun array(): ByteArray {
        return buffer.array()
    }

    fun readUnsignedBoolean() = readUnsignedByte() == 1

    fun getByte(pos: Int): Byte {
        return buffer.get(pos)
    }

    fun writerIndex(): Int {
        return length
    }

    fun duplicate(): Reader {
        val copy = ByteBuffer.allocate(buffer.remaining())
        copy.put(buffer.duplicate().clear())
        copy.flip()
        return Reader(copy)
    }
}