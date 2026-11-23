package dev.openrune.definition.codec

import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test

class Rs2TextureCodecTest {

    private fun flat(build: io.netty.buffer.ByteBuf.() -> Unit): ByteArray {
        val buffer = Unpooled.buffer().apply(build)
        return ByteArray(buffer.readableBytes()).also { buffer.readBytes(it) }
    }

    @Test
    fun `texture round trips byte for byte`() {
        val codec = Rs2TextureCodec()
        val raw = flat {
            writeShort(0x1234)
            writeByte(1)
            writeByte(1)
            writeShort(900)
            writeInt(-16777216)
            writeByte(2)
            writeByte(5)
        }

        val decoded = codec.loadData(1, raw)

        assertEquals(0x1234, decoded.averageColour)
        assertEquals(true, decoded.opaque)
        assertEquals(900, decoded.sprite)
        assertEquals(-16777216, decoded.colourAdjustment)
        assertEquals(2, decoded.animationDirection)
        assertEquals(5, decoded.animationSpeed)

        assertArrayEquals(raw, codec.encodeToBytes(decoded))
    }

    @Test
    fun `a null sprite round trips as the null marker`() {
        val codec = Rs2TextureCodec()
        val raw = flat {
            writeShort(0)
            writeByte(0)
            writeByte(1)
            writeShort(65535)
            writeInt(0)
            writeByte(0)
            writeByte(0)
        }

        assertEquals(-1, codec.loadData(1, raw).sprite)
        assertArrayEquals(raw, codec.encodeToBytes(codec.loadData(1, raw)))
    }

    @Test
    fun `an unsupported file count is rejected rather than misread`() {
        val codec = Rs2TextureCodec()
        val raw = flat {
            writeShort(0)
            writeByte(1)
            writeByte(3)
            writeShort(1)
            writeInt(0)
            writeByte(0)
            writeByte(0)
        }

        assertThrows<IllegalStateException> { codec.loadData(1, raw) }
    }
}
