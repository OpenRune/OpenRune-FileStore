package dev.openrune.definition.codec

import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test

class Rs2AppearanceCodecTest {

    @Test
    fun `identity kit round trips at the wide model revision`() {
        val codec = Rs2IdentityKitCodec(900)
        val raw = body {
            writeByte(1); writeByte(7)
            writeByte(2); writeByte(2)
            writeNullableLargeSmartCorrect(100_000)
            writeNullableLargeSmartCorrect(250)
            writeByte(3)
            writeByte(40); writeByte(2)
            writeShort(10); writeShort(20)
            writeShort(30); writeShort(40)
            writeByte(44); writeShort(0b0000_0000_0000_0101)
            writeByte(60); writeNullableLargeSmartCorrect(500)
            writeByte(69); writeNullableLargeSmartCorrect(509)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(7, decoded.bodyPart)
        assertEquals(listOf(100_000, 250), decoded.models)
        assertEquals(true, decoded.disable)
        assertEquals(listOf(10, 30), decoded.originalColours)
        assertEquals(listOf(20, 40), decoded.modifiedColours)
        assertEquals(0b101, decoded.recolourMask)
        assertEquals(500, decoded.heads[60])
        assertEquals(509, decoded.heads[69])
    }

    @Test
    fun `the recolour mask selects slots instead of filling them in order`() {
        val codec = Rs2IdentityKitCodec(900)
        val raw = body {
            writeByte(40); writeByte(2)
            writeShort(10); writeShort(20)
            writeShort(30); writeShort(40)
            writeByte(44); writeShort(0b0000_0000_0000_0101)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(listOf(0, 2), decoded.recolourSlots())
    }

    @Test
    fun `without a mask the pairs fill slots in order`() {
        val codec = Rs2IdentityKitCodec(900)
        val raw = body {
            writeByte(40); writeByte(2)
            writeShort(10); writeShort(20)
            writeShort(30); writeShort(40)
        }

        assertEquals(listOf(0, 1), codec.assertRoundTrips(raw).recolourSlots())
    }

    @Test
    fun `identity kit uses short models below revision 681`() {
        val codec = Rs2IdentityKitCodec(500)
        val raw = body {
            writeByte(2); writeByte(1); writeShort(250)
            writeByte(60); writeShort(500)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(listOf(250), decoded.models)
        assertEquals(500, decoded.heads[60])
    }

    @Test
    fun `pre 465 revisions are rejected rather than mis-decoded`() {
        val codec = Rs2IdentityKitCodec(400)
        val raw = body { writeByte(1); writeByte(7) }

        assertThrows<IllegalStateException> { codec.loadData(1, raw) }
    }

    @Test
    fun `identity kit keeps the individual slot opcodes exactly`() {
        val codec = Rs2IdentityKitCodec(900)
        val raw = body {
            writeByte(42); writeShort(111)
            writeByte(43); writeShort(222)
            writeByte(50); writeShort(333)
            writeByte(59); writeShort(444)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(111, decoded.legacyColourSlots[42])
        assertEquals(444, decoded.legacyColourSlots[59])
    }

    @Test
    fun `spotanim round trips every opcode`() {
        val codec = Rs2SpotAnimCodec(900)
        val raw = body {
            writeByte(1); writeNullableLargeSmartCorrect(70_000)
            writeByte(2); writeNullableLargeSmartCorrect(1234)
            writeByte(3)
            writeByte(4); writeShort(160)
            writeByte(5); writeShort(170)
            writeByte(6); writeShort(512)
            writeByte(7); writeByte(5)
            writeByte(8); writeByte(9)
            writeByte(9)
            writeByte(10)
            writeByte(15); writeShort(42)
            writeByte(16); writeInt(-7)
            writeByte(41); writeByte(1)
            writeShort(3); writeShort(4)
            writeByte(45); writeShort(0b11)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(70_000, decoded.model)
        assertEquals(1234, decoded.anim)
        assertEquals(true, decoded.hasAlpha)
        assertEquals(160, decoded.resizeHorizontal)
        assertEquals(512, decoded.rotation)
        assertEquals(true, decoded.hillChangeRotate)
        assertEquals(true, decoded.allowLoop)
        assertEquals(42, decoded.hillChangeRotateShort)
        assertEquals(-7, decoded.hillChangeRotateInt)
        assertEquals(listOf(3), decoded.originalTextureColours)
        assertEquals(0b11, decoded.textureMask)
    }

    @Test
    fun `spotanim keeps a short model below revision 681 but a smart anim`() {
        val codec = Rs2SpotAnimCodec(500)
        val raw = body {
            writeByte(1); writeShort(250)
            writeByte(2); writeNullableLargeSmartCorrect(60_000)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(250, decoded.model)
        assertEquals(60_000, decoded.anim)
    }
}
