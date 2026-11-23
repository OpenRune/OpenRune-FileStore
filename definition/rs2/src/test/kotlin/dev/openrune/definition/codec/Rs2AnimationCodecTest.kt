package dev.openrune.definition.codec

import dev.openrune.definition.type.WaterField
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import dev.openrune.definition.util.writeSmart
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test

class Rs2AnimationCodecTest {

    @Test
    fun `sequence round trips the parallel frame table`() {
        val codec = Rs2SequenceCodec(900)
        val raw = body {
            writeByte(1)
            writeShort(2)
            writeShort(30); writeShort(40)
            writeShort(7); writeShort(9)
            writeShort(1); writeShort(2)
            writeByte(2); writeShort(3)
            writeByte(5); writeByte(6)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(listOf(30, 40), decoded.delays)
        assertEquals(listOf((1 shl 16) or 7, (2 shl 16) or 9), decoded.frames)
        assertEquals(3, decoded.loops)
        assertEquals(6, decoded.priority)
    }

    @Test
    fun `sequence round trips the pre 457 interleaved frame table`() {
        val codec = Rs2SequenceCodec(400)
        val raw = body {
            writeByte(1)
            writeByte(1)
            writeShort(7); writeShort(8); writeShort(30)
            writeByte(4)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(true, decoded.legacyFrames)
        assertEquals(listOf(7), decoded.frames)
        assertEquals(listOf(8), decoded.legacyIFrames)
        assertEquals(listOf(30), decoded.delays)
        assertEquals(true, decoded.stretches)
    }

    @Test
    fun `sequence opcode 4 carries a value below revision 204`() {
        val codec = Rs2SequenceCodec(200)
        val raw = body {
            writeByte(4); writeShort(99)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(99, decoded.unknown4)
        assertEquals(false, decoded.stretches)
    }

    @Test
    fun `sequence keeps the wide twin of an opcode`() {
        val codec = Rs2SequenceCodec(900)

        val narrow = body { writeByte(19); writeByte(5); writeByte(6) }
        val narrowDecoded = codec.assertRoundTrips(narrow)
        assertEquals(false, narrowDecoded.soundVolumeWide)
        assertEquals(5, narrowDecoded.soundVolume)

        val wide = body { writeByte(119); writeShort(400); writeByte(6) }
        val wideDecoded = codec.assertRoundTrips(wide)
        assertEquals(true, wideDecoded.soundVolumeWide)
        assertEquals(400, wideDecoded.soundVolume)
    }

    @Test
    fun `sequence round trips the counted sound table`() {
        val codec = Rs2SequenceCodec(900)
        val raw = body {
            writeByte(13)
            writeShort(3)
            writeByte(0)
            writeByte(1); writeMedium(0x012345)
            writeByte(3); writeMedium(0x067890); writeShort(11); writeShort(12)
            writeByte(18)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(true, decoded.soundVorbis)
        assertEquals(3, decoded.sounds?.size)
        assertEquals(null, decoded.sounds?.get(0)?.packed)
        assertEquals(0x012345, decoded.sounds?.get(1)?.packed)
        assertEquals(listOf(11, 12), decoded.sounds?.get(2)?.extra)
    }

    @Test
    fun `sequence walk merge widens at revision 751`() {
        val narrow = Rs2SequenceCodec(700)
        assertEquals(
            listOf(3, 4),
            narrow.assertRoundTrips(body { writeByte(3); writeByte(2); writeByte(3); writeByte(4) }).walkMerge,
        )

        val wide = Rs2SequenceCodec(800)
        assertEquals(
            listOf(300, 4),
            wide.assertRoundTrips(
                body { writeByte(3); writeSmart(2); writeSmart(300); writeSmart(4) },
            ).walkMerge,
        )
    }

    @Test
    fun `bas round trips the animation fields and the random ready table`() {
        val codec = BasCodec(900)
        val raw = body {
            writeByte(1); writeNullableLargeSmartCorrect(100); writeNullableLargeSmartCorrect(101)
            writeByte(2); writeNullableLargeSmartCorrect(200)
            writeByte(9); writeNullableLargeSmartCorrect(209)
            writeByte(26); writeByte(1); writeByte(2)
            writeByte(29); writeByte(7)
            writeByte(30); writeShort(600)
            writeByte(37); writeByte(4)
            writeByte(45); writeShort(90)
            writeByte(52); writeByte(1)
            writeNullableLargeSmartCorrect(300); writeByte(10)
            writeByte(53)
            writeByte(54); writeByte(3); writeByte(4)
            writeByte(56); writeByte(1); writeShort(-2); writeShort(3); writeShort(-4)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(100, decoded.readyAnim)
        assertEquals(101, decoded.readyAnimSecondary)
        assertEquals(209, decoded.runAnimRight)
        assertEquals(listOf(1, 2), decoded.hillRotate)
        assertEquals(7, decoded.turnSpeed)
        assertEquals(4, decoded.walkSpeed)
        assertEquals(90, decoded.overlayHeight)
        assertEquals(300, decoded.randomReadyAnims?.single()?.anim)
        assertEquals(10, decoded.randomReadyAnims?.single()?.weight)
        assertEquals(false, decoded.unknown53)
        assertEquals(listOf(1, -2, 3, -4), decoded.unknown56)
    }

    @Test
    fun `bas random ready gains the previous ready tail at revision 916`() {
        val codec = BasCodec(920)
        val raw = body {
            writeByte(52); writeByte(1)
            writeNullableLargeSmartCorrect(300); writeByte(10)
            writeByte(2); writeByte(5); writeByte(6)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(listOf(5, 6), decoded.randomReadyAnims?.single()?.previousReady)
    }

    @Test
    fun `bas uses short anims below revision 681`() {
        val codec = BasCodec(500)
        val raw = body {
            writeByte(2); writeShort(200)
        }

        assertEquals(200, codec.assertRoundTrips(raw).crawlAnim)
    }

    @Test
    fun `water round trips one opcode of every shape`() {
        val codec = WaterCodec()
        val raw = body {
            writeByte(6); writeMedium(0x334455)
            writeByte(7); writeShort(10); writeShort(20)
            writeByte(12); writeInt(-1)
            writeByte(18); writeByte(3)
            writeByte(25); writeShort(900)
            writeByte(26); writeShort(1); writeShort(2); writeShort(3)
            writeByte(83); writeFloat(0.5f)
            writeByte(88); writeFloat(1.5f); writeFloat(2.5f)
            writeByte(97); writeFloat(1f); writeFloat(2f); writeFloat(3f)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(0x334455, decoded.values[6]?.first()?.toInt())
        assertEquals(-1, decoded.baseRgba)
        assertEquals(900, decoded.specularShininess)
        assertEquals(0.5f, decoded.fresnelBias)
        assertEquals(listOf(1f, 2f, 3f), decoded.values[97])
    }

    @Test
    fun `water round trips the whole normal map grid`() {
        val codec = WaterCodec()
        val raw = body {
            for (target in 0 until 6) {
                for (op in 0 until 8) {
                    writeByte(WaterField.normalMapOpcode(target, op))
                    when (op) {
                        0 -> writeByte(1)
                        4, 5 -> { writeFloat(target.toFloat()); writeFloat(op.toFloat()) }
                        else -> writeFloat(target * 10f + op)
                    }
                }
            }
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(48, decoded.values.size)
        assertEquals(true, decoded.normalMapEnabled(0))
        assertEquals(true, decoded.normalMapEnabled(5))
        assertEquals(listOf(3f, 4f), decoded.values[WaterField.normalMapOpcode(3, 4)])
    }

    @Test
    fun `an undeclared water opcode is rejected rather than desyncing the record`() {
        val codec = WaterCodec()
        val raw = body { writeByte(109); writeShort(1) }

        assertThrows<IllegalStateException> { codec.loadData(1, raw) }
    }
}
