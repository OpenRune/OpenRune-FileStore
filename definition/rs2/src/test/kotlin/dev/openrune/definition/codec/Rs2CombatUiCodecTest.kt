package dev.openrune.definition.codec

import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import dev.openrune.definition.util.writePrefixedString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Rs2CombatUiCodecTest {

    @Test
    fun `hitmark round trips every scalar opcode`() {
        val codec = HitmarkCodec()
        val raw = body {
            writeByte(1); writeNullableLargeSmartCorrect(80)
            writeByte(2); writeMedium(0xFF0000)
            writeByte(3); writeNullableLargeSmartCorrect(1200)
            writeByte(4); writeNullableLargeSmartCorrect(1201)
            writeByte(5); writeNullableLargeSmartCorrect(1202)
            writeByte(6); writeNullableLargeSmartCorrect(1203)
            writeByte(7); writeShort(-12)
            writeByte(8); writePrefixedString("%1")
            writeByte(9); writeShort(70)
            writeByte(10); writeShort(-34)
            writeByte(11)
            writeByte(12); writeByte(2)
            writeByte(13); writeShort(-8)
            writeByte(14); writeShort(25)
            writeByte(16); writeShort(-3); writeShort(5)
            writeByte(19); writeShort(100)
            writeByte(20); writeShort(-100)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(80, decoded.damageFont)
        assertEquals(0xFF0000, decoded.damageColour)
        assertEquals(1203, decoded.rightGraphic)
        assertEquals(-12, decoded.scrollToOffsetX)
        assertEquals("%1", decoded.damageFormat)
        assertEquals(true, decoded.noFadeOut)
        assertEquals(25, decoded.fadeOut)
        assertEquals(-3, decoded.graphicOffsetX)
        assertEquals(5, decoded.graphicOffsetY)
        assertEquals(100, decoded.damageScaleTo)
        assertEquals(-100, decoded.damageScaleFrom)
    }

    @Test
    fun `hitmark uses the shared transform block at opcodes 17 and 18`() {
        val codec = HitmarkCodec()
        val raw = body {
            writeByte(18)
            writeShort(900)
            writeShort(65535)
            writeShort(7)
            writeByte(1)
            writeShort(11)
            writeShort(65535)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(900, decoded.multiVarBit)
        assertEquals(-1, decoded.multiVarp)
        assertEquals(7, decoded.multiDefault)
        assertEquals(listOf(11, -1, 7), decoded.transforms)
        assertEquals(false, decoded.transformsWideVarBit)
    }

    @Test
    fun `hitmark opcodes 21 and 22 widen only the varbit`() {
        val codec = HitmarkCodec()
        val raw = body {
            writeByte(22)
            writeMedium(70_000)
            writeShort(65535)
            writeShort(7)
            writeByte(0)
            writeShort(11)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(70_000, decoded.multiVarBit)
        assertEquals(7, decoded.multiDefault)
        assertEquals(true, decoded.transformsWideVarBit)
    }

    @Test
    fun `headbar round trips every opcode`() {
        val codec = HeadbarCodec()
        val raw = body {
            writeByte(1); writeShort(5)
            writeByte(2); writeByte(1)
            writeByte(3); writeByte(2)
            writeByte(4)
            writeByte(5); writeShort(300)
            writeByte(6); writeShort(6)
            writeByte(7); writeNullableLargeSmartCorrect(1000)
            writeByte(8); writeNullableLargeSmartCorrect(1001)
            writeByte(9); writeNullableLargeSmartCorrect(1002)
            writeByte(10); writeNullableLargeSmartCorrect(1003)
            writeByte(11); writeShort(150)
            writeByte(12); writeNullableLargeSmartCorrect(1004)
            writeByte(13); writeNullableLargeSmartCorrect(1005)
            writeByte(14); writeNullableLargeSmartCorrect(1006)
            writeByte(15); writeNullableLargeSmartCorrect(1007)
            writeByte(16)
            writeByte(17); writeByte(4)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(1000, decoded.full)
        assertEquals(1001, decoded.empty)
        assertEquals(true, decoded.noFadeOut)
        assertEquals(150, decoded.fadeOut)
        assertEquals(true, decoded.tileFill)
        assertEquals(4, decoded.stackOffset)
    }

    @Test
    fun `headbar sprites survive the wide smart form`() {
        val codec = HeadbarCodec()
        val raw = body {
            writeByte(7); writeNullableLargeSmartCorrect(120_000)
        }

        assertEquals(120_000, codec.assertRoundTrips(raw).full)
    }

    @Test
    fun `light round trips every opcode including floats`() {
        val codec = LightCodec()
        val raw = body {
            writeByte(1); writeByte(3)
            writeByte(2); writeShort(1200)
            writeByte(3); writeShort(90)
            writeByte(4); writeShort(-40)
            writeByte(5); writeInt(-123456)
            writeByte(6); writeInt(7890)
            writeByte(7); writeInt(0)
            writeByte(8); writeFloat(0.25f)
            writeByte(9)
            writeByte(10); writeInt(600)
            writeByte(11); writeInt(120)
            writeByte(12); writeFloat(-1.5f)
            writeByte(13); writeFloat(3.75f)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(3, decoded.function)
        assertEquals(-40, decoded.offset)
        assertEquals(-123456, decoded.unknown5)
        assertEquals(0.25f, decoded.swayAmount)
        assertEquals(true, decoded.swayAmountRandom)
        assertEquals(-1.5f, decoded.swayEasing)
        assertEquals(3.75f, decoded.swayFade)
    }
}
