package dev.openrune

import dev.openrune.definition.codec.Rs2AchievementCodec
import dev.openrune.definition.type.Rs2AchievementDesc
import dev.openrune.definition.type.Rs2AchievementRef
import dev.openrune.definition.type.Rs2AchievementStatReq
import dev.openrune.definition.type.Rs2AchievementTally
import dev.openrune.definition.type.Rs2AchievementTestBit
import dev.openrune.definition.type.Rs2AchievementType
import dev.openrune.definition.type.Rs2AchievementVarReq
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class Rs2AchievementCodecTest {

    private val codec = Rs2AchievementCodec()

    private fun buffer(build: ByteBuf.() -> Unit): ByteBuf = Unpooled.buffer().apply(build)

    private fun ByteBuf.writeJstr2(value: String) {
        writeByte(0)
        value.forEach { writeByte(it.code) }
        writeByte(0)
    }

    private fun ByteBuf.writeSmart1or2(value: Int) {
        if (value < 128) writeByte(value) else writeShort(value or 0x8000)
    }

    private fun decode(data: ByteBuf): Rs2AchievementType {
        val definition = codec.loadData(3, data)
        assertEquals(0, data.readableBytes(), "decoder did not consume the whole record")
        return definition
    }

    private fun encode(definition: Rs2AchievementType): ByteArray {
        val out = Unpooled.buffer()
        with(codec) { out.encode(definition) }
        return ByteArray(out.readableBytes()).also { out.readBytes(it) }
    }

    private fun ByteBuf.toArray() = ByteArray(readableBytes()).also { getBytes(readerIndex(), it) }

    @Test
    fun `decodes scalar opcodes and flags`() {
        val achievement = decode(buffer {
            writeByte(1); writeJstr2("Runecrafter")
            writeByte(2); writeByte(2); writeByte(0); writeJstr2("Short"); writeByte(1); writeJstr2("Long")
            writeByte(3); writeShort(12)
            writeByte(4); writeShort(500)
            writeByte(5); writeByte(25)
            writeByte(6); writeShort(9000)
            writeByte(7); writeJstr2("A shiny reward")
            writeByte(16); writeShort(13)
            writeByte(17)
            writeByte(18); writeByte(2)
            writeByte(19)
            writeByte(26); writeShort(77)
            writeByte(27)
            writeByte(29); writeByte(3)
            writeByte(31); writeByte(4)
            writeByte(32); writeByte(1); writeByte(2); writeByte(3)
            writeByte(0)
        })

        assertEquals(3, achievement.id)
        assertEquals("Runecrafter", achievement.name)
        assertEquals(listOf(Rs2AchievementDesc(0, "Short"), Rs2AchievementDesc(1, "Long")), achievement.descriptions)
        assertEquals(12, achievement.category)
        assertEquals(500, achievement.sprite)
        assertEquals(25, achievement.runescore)
        assertEquals(9000, achievement.graceDay)
        assertEquals("A shiny reward", achievement.reward)
        assertEquals(13, achievement.subCategory)
        assertEquals(true, achievement.locked)
        assertEquals(2, achievement.hide)
        assertEquals(false, achievement.members)
        assertEquals(77, achievement.dbRow)
        assertEquals(true, achievement.checklist)
        assertEquals(3, achievement.numPrereqGroupsToComplete)
        assertEquals(4, achievement.numReqGroupsToComplete)
        assertEquals(Rs2AchievementTally(1, 2, 3), achievement.tally)
    }

    @Test
    fun `decodes narrow requirement lists`() {
        val achievement = decode(buffer {
            writeByte(8); writeSmart1or2(1); writeByte(0); writeByte(50); writeJstr2("50 Attack"); writeSmart1or2(2); writeShort(0); writeShort(1)
            writeByte(9); writeSmart1or2(1); writeByte(1); writeShort(7); writeJstr2("varp"); writeSmart1or2(1); writeShort(29)
            writeByte(10); writeSmart1or2(1); writeByte(2); writeInt(0x80012345.toInt()); writeJstr2("varbit"); writeSmart1or2(1); writeShort(1000)
            writeByte(11); writeSmart1or2(2); writeByte(0); writeShort(5); writeByte(1); writeShort(6)
            writeByte(12); writeSmart1or2(1); writeByte(3); writeByte(99); writeJstr2("99 Slayer"); writeSmart1or2(0)
            writeByte(13); writeSmart1or2(1); writeByte(0); writeShort(0x7FFF); writeJstr2("v"); writeSmart1or2(1); writeShort(1)
            writeByte(14); writeSmart1or2(1); writeByte(0); writeShort(1); writeJstr2("b"); writeSmart1or2(1); writeShort(2)
            writeByte(15); writeSmart1or2(1); writeByte(0); writeShort(8)
            writeByte(20); writeSmart1or2(1); writeByte(0); writeShort(9)
            writeByte(21); writeSmart1or2(1); writeByte(1); writeShort(10)
            writeByte(22); writeSmart1or2(1); writeByte(0); writeShort(29); writeByte(3); writeJstr2("t1"); writeByte(1)
            writeByte(23); writeSmart1or2(1); writeByte(1); writeShort(30); writeByte(4); writeJstr2("t2"); writeByte(0)
            writeByte(24); writeSmart1or2(1); writeByte(2); writeShort(31); writeByte(5); writeJstr2("t3"); writeByte(1)
            writeByte(25); writeSmart1or2(1); writeByte(3); writeShort(32); writeByte(6); writeJstr2("t4"); writeByte(0)
            writeByte(28); writeSmart1or2(200); repeat(200) { writeSmart1or2(it) }
            writeByte(30); writeSmart1or2(2); writeSmart1or2(1); writeSmart1or2(300)
            writeByte(0)
        })

        assertEquals(listOf(Rs2AchievementStatReq(0, 50, "50 Attack", listOf(0, 1))), achievement.statPrereqs)
        assertEquals(listOf(Rs2AchievementVarReq(1, 7, "varp", listOf(29))), achievement.varpPrereqs)
        assertEquals(listOf(Rs2AchievementVarReq(2, 0x12345, "varbit", listOf(1000))), achievement.varbitPrereqs)
        assertEquals(listOf(Rs2AchievementRef(0, 5), Rs2AchievementRef(1, 6)), achievement.achievementPrereqs)
        assertEquals(listOf(Rs2AchievementStatReq(3, 99, "99 Slayer", emptyList())), achievement.statReqs)
        assertEquals(listOf(Rs2AchievementVarReq(0, 0x7FFF, "v", listOf(1))), achievement.varpReqs)
        assertEquals(listOf(Rs2AchievementVarReq(0, 1, "b", listOf(2))), achievement.varbitReqs)
        assertEquals(listOf(Rs2AchievementRef(0, 8)), achievement.achievementReqs)
        assertEquals(listOf(Rs2AchievementRef(0, 9)), achievement.questPrereqs)
        assertEquals(listOf(Rs2AchievementRef(1, 10)), achievement.questReqs)
        assertEquals(listOf(Rs2AchievementTestBit(0, 29, 3, "t1", 1)), achievement.varpTestBitPrereqs)
        assertEquals(listOf(Rs2AchievementTestBit(1, 30, 4, "t2", 0)), achievement.varpTestBitReqs)
        assertEquals(listOf(Rs2AchievementTestBit(2, 31, 5, "t3", 1)), achievement.varbitTestBitPrereqs)
        assertEquals(listOf(Rs2AchievementTestBit(3, 32, 6, "t4", 0)), achievement.varbitTestBitReqs)
        assertEquals((0 until 200).toList(), achievement.prereqItemsToComplete)
        assertEquals(listOf(1, 300), achievement.reqGroupItemsToComplete)
        assertEquals(false, achievement.wideVarbits)
    }

    @Test
    fun `wide opcodes read 3-byte varbit ids and mark the record wide`() {
        val achievement = decode(buffer {
            writeByte(33); writeSmart1or2(1); writeByte(0); writeShort(1); writeJstr2("a"); writeSmart1or2(1); writeMedium(70000)
            writeByte(34); writeSmart1or2(1); writeByte(0); writeMedium(70001); writeByte(1); writeJstr2("b"); writeByte(1)
            writeByte(35); writeSmart1or2(1); writeByte(0); writeShort(2); writeJstr2("c"); writeSmart1or2(1); writeMedium(70002)
            writeByte(36); writeSmart1or2(1); writeByte(0); writeMedium(70003); writeByte(2); writeJstr2("d"); writeByte(0)
            writeByte(0)
        })

        assertEquals(true, achievement.wideVarbits)
        assertEquals(listOf(70000), achievement.varbitPrereqs.single().variables)
        assertEquals(70001, achievement.varbitTestBitPrereqs.single().variable)
        assertEquals(listOf(70002), achievement.varbitReqs.single().variables)
        assertEquals(70003, achievement.varbitTestBitReqs.single().variable)
    }

    @Test
    fun `null sprite sentinel decodes to null and is omitted on encode`() {
        val achievement = decode(buffer { writeByte(4); writeShort(0x7FFF); writeByte(0) })
        assertEquals(null, achievement.sprite)
        assertArrayEquals(byteArrayOf(0), encode(achievement))
    }

    @Test
    fun `unknown opcode throws instead of silently desyncing`() {
        assertThrows(IllegalStateException::class.java) {
            decode(buffer { writeByte(37); writeByte(0) })
        }
    }

    @Test
    fun `encode then decode is byte identical for narrow and wide records`() {
        val narrow = buffer {
            writeByte(1); writeJstr2("Narrow")
            writeByte(2); writeByte(1); writeByte(0); writeJstr2("d")
            writeByte(3); writeShort(1)
            writeByte(4); writeShort(4321)
            writeByte(5); writeByte(10)
            writeByte(8); writeSmart1or2(1); writeByte(0); writeByte(50); writeJstr2("s"); writeSmart1or2(1); writeShort(0)
            writeByte(9); writeSmart1or2(1); writeByte(0); writeInt(0x80100000.toInt()); writeJstr2("p"); writeSmart1or2(1); writeShort(1)
            writeByte(10); writeSmart1or2(1); writeByte(0); writeShort(5); writeJstr2("b"); writeSmart1or2(1); writeShort(2)
            writeByte(14); writeSmart1or2(1); writeByte(0); writeShort(6); writeJstr2("c"); writeSmart1or2(1); writeShort(3)
            writeByte(17)
            writeByte(19)
            writeByte(24); writeSmart1or2(1); writeByte(0); writeShort(7); writeByte(1); writeJstr2("t"); writeByte(1)
            writeByte(25); writeSmart1or2(1); writeByte(0); writeShort(8); writeByte(2); writeJstr2("u"); writeByte(0)
            writeByte(28); writeSmart1or2(130); repeat(130) { writeSmart1or2(it + 100) }
            writeByte(32); writeByte(1); writeByte(2); writeByte(3)
            writeByte(0)
        }
        assertArrayEquals(narrow.toArray(), encode(decode(narrow.copy())))

        val wide = buffer {
            writeByte(1); writeJstr2("Wide")
            writeByte(33); writeSmart1or2(1); writeByte(0); writeShort(1); writeJstr2("a"); writeSmart1or2(1); writeMedium(70000)
            writeByte(35); writeSmart1or2(1); writeByte(0); writeShort(2); writeJstr2("c"); writeSmart1or2(1); writeMedium(70002)
            writeByte(34); writeSmart1or2(1); writeByte(0); writeMedium(70001); writeByte(1); writeJstr2("b"); writeByte(1)
            writeByte(36); writeSmart1or2(1); writeByte(0); writeMedium(70003); writeByte(2); writeJstr2("d"); writeByte(0)
            writeByte(0)
        }
        assertArrayEquals(wide.toArray(), encode(decode(wide.copy())))
    }

    @Test
    fun `encoding a wide record with a 2-byte-only value still uses the wide opcodes`() {
        val encoded = encode(Rs2AchievementType(0, varbitReqs = listOf(Rs2AchievementVarReq(0, 1, "x", listOf(5))), wideVarbits = true))
        assertEquals(35, encoded[0].toInt())
        val decoded = decode(Unpooled.wrappedBuffer(encoded))
        assertEquals(listOf(5), decoded.varbitReqs.single().variables)
        assertEquals(true, decoded.wideVarbits)
    }
}
