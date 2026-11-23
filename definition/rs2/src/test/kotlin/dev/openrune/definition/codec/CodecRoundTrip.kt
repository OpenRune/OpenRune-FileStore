package dev.openrune.definition.codec

import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Assertions.assertArrayEquals

internal fun body(build: ByteBuf.() -> Unit): ByteArray {
    val buffer = Unpooled.buffer().apply(build).apply { writeByte(0) }
    return ByteArray(buffer.readableBytes()).also { buffer.readBytes(it) }
}

internal fun <T : Definition> DefinitionCodec<T>.encodeToBytes(definition: T): ByteArray {
    val out = Unpooled.buffer()
    try {
        with(this) { out.encode(definition) }
        return ByteArray(out.readableBytes()).also { out.readBytes(it) }
    } finally {
        out.release()
    }
}

internal fun <T : Definition> DefinitionCodec<T>.assertRoundTrips(raw: ByteArray, id: Int = 1): T {
    val decoded = loadData(id, raw)
    assertArrayEquals(raw, encodeToBytes(decoded)) {
        "re-encode differs from the source bytes" +
            "\n  expected: ${raw.toHex()}" +
            "\n  actual:   ${encodeToBytes(decoded).toHex()}"
    }
    return decoded
}

private fun ByteArray.toHex() = joinToString(" ") { "%02x".format(it) }
