package dev.openrune.definition.util

import io.netty.buffer.ByteBuf
import io.netty.util.ByteProcessor
import java.nio.charset.Charset

fun ByteBuf.readUnsignedBoolean() = readUnsignedByte().toInt() == 1

// SMARTS
fun ByteBuf.readShortSmart(): Int {
    val peek = readUnsignedByte().toInt()
    return if (peek < 128) peek - 64 else (peek shl 8 or readUnsignedByte().toInt()) - 49152
}

fun ByteBuf.writeShortSmart(v: Int): ByteBuf {
    when (v) {
        in -0x40..0x3F -> writeByte(v + 0x40)
        in -0x4000..0x3FFF -> writeShort(0x8000 or (v + 0x4000))
        else -> throw IllegalArgumentException()
    }

    return this
}


fun ByteBuf.readSmart(): Int {
    val peek = readUnsignedByte().toInt()
    return if (peek < 128) {
        peek and 0xFF
    } else {
        (peek shl 8 or readUnsignedByte().toInt()) - 32768
    }
}

fun ByteBuf.readBigSmart(): Int {
    val peek = readByte().toInt()
    return if (peek < 0) {
        ((peek shl 24) or (readUnsignedByte().toInt() shl 16) or (readUnsignedByte().toInt() shl 8) or readUnsignedByte().toInt()) and 0x7fffffff
    } else {
        val value = (peek shl 8) or readUnsignedByte().toInt()
        if (value == 32767) -1 else value
    }
}

fun ByteBuf.readNullableLargeSmart(): Int = if (getByte(readerIndex()) < 0) {
    readInt() and Integer.MAX_VALUE
} else {
    val result = readUnsignedShort()
    if (result == 32767) -1 else result
}

fun ByteBuf.readShortSmartSub(): Int {
    val peek = getUnsignedByte(readerIndex())
    return if (peek < 128) {
        readUnsignedByte() - 1
    } else {
        readUnsignedShort() - 32769
    }
}

// 0 terminated string.
fun ByteBuf.readString(): String {
    val sb = StringBuilder()
    var b: Int
    while (isReadable) {
        b = readUnsignedByte().toInt()
        if (b == 0) {
            break
        }
        sb.append(b.toChar())
    }
    return sb.toString()
}

public fun ByteBuf.readStringCP(charset: Charset = Cp1252Charset): String {
    val start = readerIndex()

    val end = forEachByte(ByteProcessor.FIND_NUL)
    require(end != -1) {
        "Unterminated string"
    }

    val s = toString(start, end - start, charset)
    readerIndex(end + 1)
    return s
}

public fun ByteBuf.readUnsignedShortSmart(): Int {
    val peek = getUnsignedByte(readerIndex()).toInt()
    return if ((peek and 0x80) == 0) {
        readUnsignedByte().toInt()
    } else {
        readUnsignedShort() and 0x7FFF
    }
}

//Writing

fun ByteBuf.writeByte(value: Boolean) {
    writeByte(if (value) 1 else 0)
}

fun ByteBuf.writeSmart(value: Int) {
    when (value) {
        in 0..127 -> writeByte(value + 32768)
        in 0..32767 -> writeShort(value)
        else -> throw IllegalArgumentException("writeSmart out of range: $value")
    }
}

public fun ByteBuf.writeUnsignedShortSmart(v: Int): ByteBuf {
    when (v) {
        in 0..0x7F -> writeByte(v)
        in 0..0x7FFF -> writeShort(0x8000 or v)
        else -> throw IllegalArgumentException()
    }

    return this
}

fun ByteBuf.writeString(value: String?) {
    if (value != null) {
        for (char in value) {
            writeByte(char.code)
        }
    }
    writeByte(0)
}

public fun ByteBuf.writeStringCP(s: CharSequence, charset: Charset = Cp1252Charset): ByteBuf {
    writeCharSequence(s, charset)
    writeByte(0)
    return this
}

fun ByteBuf.writePrefixedString(value: String) {
    writeByte(0)
    for (char in value) {
        writeByte(char.code)
    }
    writeByte(0)
}

fun ByteBuf.toArray(): ByteArray {
    return ByteArray(writerIndex()).also { this.getBytes(0, it) }
}

private const val HALF_UBYTE = 0x80

fun ByteBuf.writeNullableLargeSmartCorrect(value: Int?): ByteBuf = when {
    value == null -> {
        writeShort(0x7FFF)
    }
    value < Short.MAX_VALUE -> {
        writeShort(value)
    }
    else -> {
        writeInt(value)
        val writtenValue = getByte(writerIndex() - 4)
        setByte(writerIndex() - 4, writtenValue + HALF_UBYTE)
    }
}

public fun ByteBuf.readNullableLargeSmartCorrect(): Int? = if (getByte(readerIndex()) < 0) {
    readInt() and Integer.MAX_VALUE
} else {
    val result = readUnsignedShort()
    if (result == 32767) null else result
}
