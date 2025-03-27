package dev.openrune.definition.util

import io.netty.buffer.ByteBuf

fun ByteBuf.readUnsignedBoolean() = readUnsignedByte().toInt() == 1

// SMARTS
fun ByteBuf.readShortSmart(): Int {
    val peek = readUnsignedByte().toInt()
    return if (peek < 128) peek - 64 else (peek shl 8 or readUnsignedByte().toInt()) - 49152
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

//Writing

fun ByteBuf.writeByte(value: Boolean) {
    writeByte(if (value) 1 else 0)
}

fun ByteBuf.writeSmart(value: Int) {
    if (value >= 128) {
        writeShort(value + 32768)
    } else {
        writeByte(value)
    }
}

fun ByteBuf.writeString(value: String?) {
    if (value != null) {
        for (char in value) {
            writeByte(char.code)
        }
    }
    writeByte(0)
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