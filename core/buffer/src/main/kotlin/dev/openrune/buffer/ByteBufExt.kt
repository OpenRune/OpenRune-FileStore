package dev.openrune.buffer

import io.netty.buffer.ByteBuf


fun ByteBuf.readByteRD(): Int {
    return readByte().toInt()
}

fun ByteBuf.readUnsignedBooleanRD() = readUnsignedByteRD() == 1

fun ByteBuf.readUnsignedByteRD(): Int {
    return readByteRD() and 0xff
}

fun ByteBuf.readShortRD(): Int {
    return (readByteRD() shl 8) or readUnsignedByteRD()
}

fun ByteBuf.readShortSmartRD(): Int {
    val peek = readUnsignedByteRD()
    return if (peek < 128) peek - 64 else (peek shl 8 or readUnsignedByteRD()) - 49152
}

fun ByteBuf.readUnsignedShortRD(): Int {
    return (readUnsignedByteRD() shl 8) or readUnsignedByteRD()
}

fun ByteBuf.readMediumRD(): Int {
    return (readByteRD() shl 16) or (readByteRD() shl 8) or readUnsignedByteRD()
}

fun ByteBuf.readUnsignedMediumRD(): Int {
    return (readUnsignedByteRD() shl 16) or (readUnsignedByteRD() shl 8) or readUnsignedByteRD()
}

fun ByteBuf.readIntRD(): Int {
    return (readUnsignedByteRD() shl 24) or (readUnsignedByteRD() shl 16) or (readUnsignedByteRD() shl 8) or readUnsignedByteRD()
}

fun ByteBuf.readSmartRD(): Int {
    val peek = readUnsignedByteRD()
    return if (peek < 128) {
        peek and 0xFF
    } else {
        (peek shl 8 or readUnsignedByteRD()) - 32768
    }
}

fun ByteBuf.readBigSmartRD(): Int {
    val peek = readByteRD()
    return if (peek < 0) {
        ((peek shl 24) or (readUnsignedByteRD() shl 16) or (readUnsignedByteRD() shl 8) or readUnsignedByteRD()) and 0x7fffffff
    } else {
        val value = (peek shl 8) or readUnsignedByteRD()
        if (value == 32767) -1 else value
    }
}

fun ByteBuf.readLargeSmartRD(): Int {
    var baseValue = 0
    var lastValue = readSmartRD()
    while (lastValue == 32767) {
        lastValue = readSmartRD()
        baseValue += 32767
    }
    return baseValue + lastValue
}

fun ByteBuf.readStringRD(): String {
    val sb = StringBuilder()
    var b: Int
    while (isReadable) {
        b = readUnsignedByteRD()
        if (b == 0) {
            break
        }
        sb.append(b.toChar())
    }
    return sb.toString()
}

