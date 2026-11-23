package dev.openrune.definition.codec

import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import dev.openrune.definition.util.writeString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Rs2ObjCodecTest {

    @Test
    fun `obj round trips the common scalar opcodes`() {
        val codec = Rs2ObjCodec(900)
        val raw = body {
            writeByte(1); writeNullableLargeSmartCorrect(90_000)
            writeByte(2); writeString("Bronze sword")
            writeByte(3); writeString("A sword.")
            writeByte(4); writeShort(2000)
            writeByte(7); writeShort(-3)
            writeByte(11)
            writeByte(12); writeInt(26)
            writeByte(13); writeByte(3)
            writeByte(15)
            writeByte(16)
            writeByte(30); writeString("Wield")
            writeByte(35); writeString("Drop")
            writeByte(94); writeShort(42)
            writeByte(113); writeByte(-5)
            writeByte(131); writeString("Second")
            writeByte(249); writeByte(1); writeByte(1); writeMedium(5); writeString("param")
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(90_000, decoded.models[1])
        assertEquals("Bronze sword", decoded.name)
        assertEquals(-3, decoded.xOffset2d)
        assertEquals(11, decoded.stackable)
        assertEquals(26L, decoded.cost)
        assertEquals(false, decoded.costLong)
        assertEquals(false, decoded.tradeable)
        assertEquals(true, decoded.members)
        assertEquals(mapOf(30 to "Wield"), decoded.ops)
        assertEquals(mapOf(35 to "Drop"), decoded.interfaceOps)
        assertEquals("Second", decoded.secondaryDescription)
        assertEquals("param", decoded.params?.get(5))
    }

    @Test
    fun `obj keeps a long cost distinct from an int cost`() {
        val codec = Rs2ObjCodec(900)

        val asInt = codec.assertRoundTrips(body { writeByte(12); writeInt(500) })
        assertEquals(500L, asInt.cost)
        assertEquals(false, asInt.costLong)

        val asLong = codec.assertRoundTrips(body { writeByte(181); writeLong(5_000_000_000L) })
        assertEquals(5_000_000_000L, asLong.cost)
        assertEquals(true, asLong.costLong)
    }

    @Test
    fun `obj keeps the short and medium template links apart`() {
        val codec = Rs2ObjCodec(900)
        val raw = body {
            writeByte(97); writeShort(1000)
            writeByte(201); writeMedium(70_000)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(1000, decoded.links[97])
        assertEquals(70_000, decoded.links[201])
    }

    @Test
    fun `obj keeps the short and medium stack count tables apart`() {
        val codec = Rs2ObjCodec(900)
        val raw = body {
            writeByte(100); writeShort(995); writeShort(1)
            writeByte(190); writeMedium(70_000); writeShort(2)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(listOf(995, 1), decoded.counts[100])
        assertEquals(listOf(70_000, 2), decoded.counts[190])
    }

    @Test
    fun `obj wear models carry an offset byte below revision 501`() {
        val legacy = Rs2ObjCodec(500)
        val legacyDecoded = legacy.assertRoundTrips(
            body { writeByte(23); writeShort(300); writeByte(7) },
        )
        assertEquals(300, legacyDecoded.manWear)
        assertEquals(7, legacyDecoded.manWearOffsetLegacy)

        val modern = Rs2ObjCodec(900)
        val modernDecoded = modern.assertRoundTrips(
            body { writeByte(23); writeNullableLargeSmartCorrect(300) },
        )
        assertEquals(300, modernDecoded.manWear)
        assertEquals(null, modernDecoded.manWearOffsetLegacy)
    }

    @Test
    fun `obj opcode 9 is a model list from revision 949`() {
        val codec = Rs2ObjCodec(950)
        val raw = body {
            writeByte(9); writeByte(2)
            writeNullableLargeSmartCorrect(10); writeNullableLargeSmartCorrect(11)
        }

        assertEquals(listOf(10, 11), codec.assertRoundTrips(raw).modelList)
    }

    @Test
    fun `obj opcode 9 is an unused string between revisions 300 and 949`() {
        val codec = Rs2ObjCodec(900)
        val raw = body { writeByte(9); writeString("unused") }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals("unused", decoded.unknown9Text)
        assertEquals(null, decoded.modelList)
    }

    @Test
    fun `obj round trips the extra model slots and the three stackable forms`() {
        val codec = Rs2ObjCodec(900)
        val raw = body {
            writeByte(46); writeNullableLargeSmartCorrect(2)
            writeByte(56); writeNullableLargeSmartCorrect(12)
            writeByte(165)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(2, decoded.models[46])
        assertEquals(12, decoded.models[56])
        assertEquals(165, decoded.stackable)

        assertEquals(178, codec.assertRoundTrips(body { writeByte(178) }).stackable)
    }

    @Test
    fun `obj keeps legacy and modern cursors apart`() {
        val codec = Rs2ObjCodec(900)
        val raw = body {
            writeByte(127); writeByte(1); writeShort(500)
            writeByte(142); writeShort(600)
            writeByte(150); writeShort(700)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(listOf(1, 500), decoded.legacyCursors[127])
        assertEquals(600, decoded.cursors[142])
        assertEquals(700, decoded.cursors[150])
    }
}
