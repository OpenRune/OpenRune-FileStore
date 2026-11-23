package dev.openrune.definition.codec

import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import dev.openrune.definition.util.writeSmart
import dev.openrune.definition.util.writeString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Rs2NpcCodecTest {

    @Test
    fun `npc round trips the common scalar opcodes`() {
        val codec = Rs2NpcCodec(900)
        val raw = body {
            writeByte(1); writeByte(2)
            writeNullableLargeSmartCorrect(100_000); writeNullableLargeSmartCorrect(2)
            writeByte(2); writeString("Goblin")
            writeByte(3); writeString("An ugly green creature.")
            writeByte(12); writeByte(1)
            writeByte(13); writeShort(808)
            writeByte(18); writeShort(42)
            writeByte(30); writeString("Attack")
            writeByte(34); writeString("Examine")
            writeByte(60); writeByte(1); writeNullableLargeSmartCorrect(55)
            writeByte(93)
            writeByte(95); writeShort(5)
            writeByte(100); writeByte(-20)
            writeByte(101); writeByte(30)
            writeByte(127); writeShort(7)
            writeByte(150); writeString("Members")
            writeByte(158)
            writeByte(249); writeByte(1); writeByte(0); writeMedium(10); writeInt(42)
            writeByte(253); writeByte(3)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(listOf(100_000, 2), decoded.models)
        assertEquals("Goblin", decoded.name)
        assertEquals("An ugly green creature.", decoded.description)
        assertEquals(mapOf(30 to "Attack", 34 to "Examine"), decoded.ops)
        assertEquals(listOf(55), decoded.heads)
        assertEquals(false, decoded.minimap)
        assertEquals(-20, decoded.ambient)
        assertEquals(7, decoded.bas)
        assertEquals(mapOf(150 to "Members"), decoded.membersOps)
        assertEquals(true, decoded.reprioritiseAttackOp)
        assertEquals(42, decoded.params?.get(10))
        assertEquals(3, decoded.priorityOffset)
    }

    @Test
    fun `npc round trips the filtered head icon table`() {
        val codec = Rs2NpcCodec(900)
        val raw = body {
            writeByte(102)
            writeByte(0b0000_0101)
            writeNullableLargeSmartCorrect(1000); writeSmart(4 + 1)
            writeNullableLargeSmartCorrect(1002); writeSmart(6 + 1)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(setOf(0, 2), decoded.headIcons.keys)
        assertEquals(1000, decoded.headIcons[0]?.graphic)
        assertEquals(4, decoded.headIcons[0]?.index)
        assertEquals(6, decoded.headIcons[2]?.index)
    }

    @Test
    fun `npc head icon is a plain short below revision 809`() {
        val codec = Rs2NpcCodec(800)
        val raw = body { writeByte(102); writeShort(7) }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(7, decoded.headIconLegacy)
        assertEquals(true, decoded.headIcons.isEmpty())
    }

    @Test
    fun `npc transform count widens at revision 910`() {
        val narrow = Rs2NpcCodec(900)
        val narrowRaw = body {
            writeByte(106); writeShort(900); writeShort(65535); writeByte(1); writeShort(11); writeShort(12)
        }
        assertEquals(900, narrow.assertRoundTrips(narrowRaw).multiVarBit)

        val wide = Rs2NpcCodec(910)
        val wideRaw = body {
            writeByte(106); writeShort(900); writeShort(65535); writeSmart(1); writeShort(11); writeShort(12)
        }
        assertEquals(900, wide.assertRoundTrips(wideRaw).multiVarBit)
    }

    @Test
    fun `npc transforms at 187 and 188 widen the varbit to a medium`() {
        val codec = Rs2NpcCodec(910)
        val raw = body {
            writeByte(188)
            writeMedium(70_000)
            writeShort(65535)
            writeShort(7)
            writeSmart(0)
            writeShort(11)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(70_000, decoded.multiVarBit)
        assertEquals(7, decoded.multiDefault)
        assertEquals(true, decoded.transformsWideVarBit)
    }

    @Test
    fun `npc round trips the appearance override block`() {
        val codec = Rs2NpcCodec(900)
        val raw = body {
            writeByte(186)
            writeShort(1)
            writeShort(500)
            writeShort(65535)
            writeByte(0b0001_1111)
            writeByte(1); writeByte(3); writeByte(1)
            writeShort(10); writeShort(20); writeShort(300); writeByte(2); writeByte(7); writeByte(8)
            writeByte(1); writeByte(4); writeByte(1)
            writeShort(11); writeShort(21); writeShort(301)
            writeByte(1); writeByte(5); writeByte(1)
            writeShort(12); writeShort(22); writeShort(1); writeShort(2)
            writeByte(1); writeByte(6); writeByte(1)
            writeShort(13); writeShort(23); writeShort(3); writeShort(4)
            writeByte(1)
            writeShort(14); writeShort(24); writeByte(1); writeByte(2); writeByte(3); writeByte(4)
            writeShort(99)
        }

        val decoded = codec.assertRoundTrips(raw)

        val override = decoded.appearanceOverrides.single()
        assertEquals(500, override.varBit)
        assertEquals(false, override.mediumVarBit)
        assertEquals(0b11111, override.flags)
        assertEquals(3, override.models.single().key)
        assertEquals(listOf(7, 8), override.models.single().entries.single().extra)
        assertEquals(300, override.models.single().entries.single().model)
        assertEquals(301, override.headModels.single().entries.single().model)
        assertEquals(2, override.retextures.single().entries.single().destination)
        assertEquals(4, override.recolours.single().entries.single().destination)
        assertEquals(4, override.tints.single().opacity)
        assertEquals(99, override.default)
    }

    @Test
    fun `npc override at opcode 189 widens the varbit`() {
        val codec = Rs2NpcCodec(900)
        val raw = body {
            writeByte(189)
            writeShort(1)
            writeMedium(70_000)
            writeShort(65535)
            writeByte(0)
            writeShort(5)
        }

        val override = codec.assertRoundTrips(raw).appearanceOverrides.single()

        assertEquals(70_000, override.varBit)
        assertEquals(true, override.mediumVarBit)
        assertEquals(5, override.default)
    }

    @Test
    fun `npc uses short models below revision 681`() {
        val codec = Rs2NpcCodec(500)
        val raw = body {
            writeByte(1); writeByte(1); writeShort(250)
        }

        assertEquals(listOf(250), codec.assertRoundTrips(raw).models)
    }
}
