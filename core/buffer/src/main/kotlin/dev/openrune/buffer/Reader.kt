package dev.openrune.buffer

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

/**
 * Created by Advo on 9/14/2024
 */
class Reader(private val buf: ByteBuf) {
    constructor(buf: ByteArray) : this(Unpooled.wrappedBuffer(buf))

    val length: Int
        get() = buf.readableBytes()

    fun readByte(): Int {
        return buf.readByte().toInt()
    }

    fun readUnsignedBoolean() = readUnsignedByte() == 1

    fun readUnsignedByte(): Int {
        return readByte() and 0xff
    }

    fun readShort(): Int {
        return (readByte() shl 8) or readUnsignedByte()
    }

    fun readShortSmart(): Int {
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
        while (buf.isReadable) {
            b = readUnsignedByte()
            if (b == 0) {
                break
            }
            sb.append(b.toChar())
        }
        return sb.toString()
    }

    fun position(index: Int) {
        buf.readerIndex(index)
    }

    fun array(): ByteArray {
        val copy = ByteArray(buf.readableBytes())
        buf.readBytes(copy)
        return copy
    }

}
