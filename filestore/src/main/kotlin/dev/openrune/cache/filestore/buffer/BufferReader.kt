package dev.openrune.cache.filestore.buffer

import java.nio.ByteBuffer

class BufferReader(
    val buffer: ByteBuffer
) : dev.openrune.cache.filestore.buffer.Reader {

    constructor(array: ByteArray) : this(buffer = ByteBuffer.wrap(array))

    override val length: Int = buffer.remaining()
    override val remaining: Int
        get() = buffer.remaining()
    private var bitIndex = 0

    override fun readByteOLD(): Int {
        return buffer.get().toInt()
    }

    override fun readByte(): Byte {
         return buffer.get()
    }

    override fun readByteAdd(): Int {
        return (readByteOLD() - 128).toByte().toInt()
    }

    override fun readByteInverse(): Int {
        return -readByteOLD()
    }

    override fun readByteSubtract(): Int {
        return (readByteInverse() + 128).toByte().toInt()
    }

    override fun readUnsignedByteOLD(): Int {
        return readByteOLD() and 0xff
    }

    override fun readUnsignedByte(): UByte {
        return readUnsignedByteOLD().toUByte()
    }

    override fun readShortOLD(): Int {
        return (readByteOLD() shl 8) or readUnsignedByteOLD()
    }

    override fun readShort(): Short {
        return readShortOLD().toShort()
    }

    override fun readShortAdd(): Int {
        return (readByteOLD() shl 8) or readUnsignedByteAdd()
    }

    override fun readUnsignedShortAdd(): Int {
        return (readByteOLD() shl 8) or ((readByteOLD() - 128) and 0xff)
    }

    override fun readShortLittle(): Int {
        return readUnsignedByteOLD() or (readByteOLD() shl 8)
    }

    override fun readShortAddLittle(): Int {
        return readUnsignedByteAdd() or (readByteOLD() shl 8)
    }

    override fun readShortSmart() : Int {
        val peek = readUnsignedByteOLD()
        return if (peek < 128) peek - 64 else (peek shl 8 or readUnsignedByteOLD()) - 49152
    }

    override fun readUnsignedByteAdd(): Int {
        return (readByteOLD() - 128).toByte().toInt()
    }

    override fun readUnsignedShortOLD(): Int {
        return (readUnsignedByteOLD() shl 8) or readUnsignedByteOLD()
    }

    override fun readUnsignedShort(): UShort {
        return readUnsignedShortOLD().toUShort()
    }

    override fun readUnsignedShortLittle(): Int {
        return readUnsignedByteOLD() or (readUnsignedByteOLD() shl 8)
    }

    override fun readMedium(): Int {
        return (readByteOLD() shl 16) or (readByteOLD() shl 8) or readUnsignedByteOLD()
    }

    override fun readUnsignedMedium(): Int {
        return (readUnsignedByteOLD() shl 16) or (readUnsignedByteOLD() shl 8) or readUnsignedByteOLD()
    }

    override fun readInt(): Int {
        return (readUnsignedByteOLD() shl 24) or (readUnsignedByteOLD() shl 16) or (readUnsignedByteOLD() shl 8) or readUnsignedByteOLD()
    }

    override fun readIntInverseMiddle(): Int {
        return (readByteOLD() shl 16) or (readByteOLD() shl 24) or readUnsignedByteOLD() or (readByteOLD() shl 8)
    }

    override fun readIntLittle(): Int {
        return readUnsignedByteOLD() or (readByteOLD() shl 8) or (readByteOLD() shl 16) or (readByteOLD() shl 24)
    }

    override fun readUnsignedIntMiddle(): Int {
        return (readUnsignedByteOLD() shl 8) or readUnsignedByteOLD() or (readUnsignedByteOLD() shl 24) or (readUnsignedByteOLD() shl 16)
    }

    override fun readSmart(): Int {
        val peek = readUnsignedByteOLD()
        return if (peek < 128) {
            peek and 0xFF
        } else {
            (peek shl 8 or readUnsignedByteOLD()) - 32768
        }
    }

    override fun readBigSmart(): Int {
        val peek = readByteOLD()
        return if (peek < 0) {
            ((peek shl 24) or (readUnsignedByteOLD() shl 16) or (readUnsignedByteOLD() shl 8) or readUnsignedByteOLD()) and 0x7fffffff
        } else {
            val value = (peek shl 8) or readUnsignedByteOLD()
            if (value == 32767) -1 else value
        }
    }

    override fun readLargeSmart(): Int {
        var baseValue = 0
        var lastValue = readSmart()
        while (lastValue == 32767) {
            lastValue = readSmart()
            baseValue += 32767
        }
        return baseValue + lastValue
    }

    override fun readLong(): Long {
        val first = readInt().toLong() and 0xffffffffL
        val second = readInt().toLong() and 0xffffffffL
        return second + (first shl 32)
    }

    override fun readString(): String {
        val sb = StringBuilder()
        var b: Int
        while (buffer.hasRemaining()) {
            b = readUnsignedByteOLD()
            if (b == 0) {
                break
            }
            sb.append(b.toChar())
        }
        return sb.toString()
    }

    override fun readBytes(value: ByteArray) {
        buffer.get(value)
    }

    override fun readBytes(array: ByteArray, offset: Int, length: Int) {
        buffer.get(array, offset, length)
    }

    override fun skip(amount: Int) {
        buffer.position(buffer.position() + amount)
    }

    override fun position(): Int {
        return buffer.position()
    }

    override fun position(index: Int) {
        buffer.position(index)
    }

    override fun array(): ByteArray {
        return buffer.array()
    }

    override fun readableBytes(): Int {
        return buffer.remaining()
    }

    override fun startBitAccess(): dev.openrune.cache.filestore.buffer.Reader {
        bitIndex = buffer.position() * 8
        return this
    }

    override fun stopBitAccess(): dev.openrune.cache.filestore.buffer.Reader {
        buffer.position((bitIndex + 7) / 8)
        return this
    }

    @Suppress("NAME_SHADOWING")
    override fun readBits(bitCount: Int): Int {
        if (bitCount < 0 || bitCount > 32) {
            throw IllegalArgumentException("Number of bits must be between 1 and 32 inclusive")
        }

        var bitCount = bitCount
        var bytePos = bitIndex shr 3
        var bitOffset = 8 - (bitIndex and 7)
        var value = 0
        bitIndex += bitCount

        while (bitCount > bitOffset) {
            value += buffer.get(bytePos++).toInt() and dev.openrune.cache.filestore.buffer.BufferReader.Companion.BIT_MASKS[bitOffset] shl bitCount - bitOffset
            bitCount -= bitOffset
            bitOffset = 8
        }
        value += if (bitCount == bitOffset) {
            buffer.get(bytePos).toInt() and dev.openrune.cache.filestore.buffer.BufferReader.Companion.BIT_MASKS[bitOffset]
        } else {
            buffer.get(bytePos).toInt() shr bitOffset - bitCount and dev.openrune.cache.filestore.buffer.BufferReader.Companion.BIT_MASKS[bitCount]
        }
        return value
    }

    companion object {
        /**
         * Bit masks for [readBits]
         */
        private val BIT_MASKS = IntArray(32)

        init {
            for (i in dev.openrune.cache.filestore.buffer.BufferReader.Companion.BIT_MASKS.indices)
                dev.openrune.cache.filestore.buffer.BufferReader.Companion.BIT_MASKS[i] = (1 shl i) - 1
        }
    }
}