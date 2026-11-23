package dev.openrune.definition.codec

import dev.openrune.definition.util.writeString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Rs2FloorCodecTest {

    @Test
    fun `floor overlay round trips every opcode`() {
        val codec = FloorOverlayCodec(500)
        val raw = body {
            writeByte(1); writeMedium(0x66AA33)
            writeByte(3); writeShort(4000)
            writeByte(5)
            writeByte(6); writeString("water_edge")
            writeByte(7); writeMedium(0x112233)
            writeByte(8)
            writeByte(9); writeShort(512)
            writeByte(10)
            writeByte(11); writeByte(7)
            writeByte(12)
            writeByte(13); writeMedium(0x0A0B0C)
            writeByte(14); writeByte(9)
            writeByte(15); writeShort(1234)
            writeByte(16); writeByte(3)
            writeByte(20); writeShort(4321)
            writeByte(21); writeByte(11)
            writeByte(22); writeShort(777)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(0x66AA33, decoded.colour)
        assertEquals(4000, decoded.material)
        assertEquals(true, decoded.materialWide)
        assertEquals(false, decoded.occlude)
        assertEquals("water_edge", decoded.debugName)
        assertEquals(0x112233, decoded.mapColour)
        assertEquals(512, decoded.textureScale)
        assertEquals(false, decoded.hardShadow)
        assertEquals(7, decoded.priority)
        assertEquals(true, decoded.smoothEdges)
        assertEquals(0x0A0B0C, decoded.waterFogColour)
        assertEquals(777, decoded.waterFogUnknownC)
    }

    @Test
    fun `floor overlay treats opcode 3 as a flag below revision 400`() {
        val codec = FloorOverlayCodec(399)
        val raw = body {
            writeByte(1); writeMedium(0x010203)
            writeByte(2); writeByte(42)
            writeByte(3)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(42, decoded.material)
        assertEquals(false, decoded.materialWide)
        assertEquals(true, decoded.overlay)
    }

    @Test
    fun `floor overlay keeps the byte material form at 400 and later`() {
        val codec = FloorOverlayCodec(500)
        val raw = body {
            writeByte(2); writeByte(42)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(42, decoded.material)
        assertEquals(false, decoded.materialWide)
    }

    @Test
    fun `floor underlay round trips every opcode`() {
        val codec = FloorUnderlayCodec()
        val raw = body {
            writeByte(1); writeMedium(0x3F7F1F)
            writeByte(2); writeShort(88)
            writeByte(3); writeShort(256)
            writeByte(4)
            writeByte(5)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(0x3F7F1F, decoded.colour)
        assertEquals(88, decoded.material)
        assertEquals(256, decoded.textureScale)
        assertEquals(false, decoded.hardShadow)
        assertEquals(false, decoded.occlude)
    }

    @Test
    fun `billboard round trips every opcode`() {
        val codec = BillboardCodec()
        val raw = body {
            writeByte(1); writeShort(1500)
            writeByte(2); writeShort(63); writeShort(127)
            writeByte(3); writeByte(-5)
            writeByte(4); writeByte(2)
            writeByte(5); writeByte(3)
            writeByte(6)
            writeByte(7)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(1500, decoded.material)
        assertEquals(63, decoded.width)
        assertEquals(127, decoded.height)
        assertEquals(-5, decoded.unknown3)
        assertEquals(2, decoded.sourceBlendMode)
        assertEquals(3, decoded.destinationBlendMode)
        assertEquals(true, decoded.unknown6)
        assertEquals(true, decoded.unknown7)
    }

    @Test
    fun `a null material round trips as the null marker`() {
        val codec = FloorUnderlayCodec()
        val raw = body {
            writeByte(2); writeShort(65535)
        }

        assertEquals(-1, codec.assertRoundTrips(raw).material)
    }

}
