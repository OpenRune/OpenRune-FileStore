package dev.openrune.buffer

import io.netty.buffer.ByteBuf


fun ByteBuf.readUnsignedBoolean() = readUnsignedByte().toInt() == 1

fun ByteBuf.readShortRD(): Int {
    return (readByte().toInt() shl 8) or readUnsignedByte().toInt()
}

fun ByteBuf.readMediumRD(): Int {
    return (readByte().toInt() shl 16) or (readByte().toInt() shl 8) or readUnsignedByte().toInt()
}

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

fun ByteBuf.readLargeSmart(): Int {
    var baseValue = 0
    var lastValue = readSmart()
    while (lastValue == 32767) {
        lastValue = readSmart()
        baseValue += 32767
    }
    return baseValue + lastValue
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

