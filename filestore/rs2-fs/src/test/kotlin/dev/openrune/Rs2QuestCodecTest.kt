package dev.openrune

import dev.openrune.definition.codec.Rs2QuestCodec
import dev.openrune.definition.type.Rs2QuestDifficulty
import dev.openrune.definition.type.Rs2QuestKind
import dev.openrune.definition.type.Rs2QuestStatReq
import dev.openrune.definition.type.Rs2QuestType
import dev.openrune.definition.type.Rs2QuestVarRange
import dev.openrune.definition.type.Rs2QuestVarReq
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class Rs2QuestCodecTest {

    private val codec = Rs2QuestCodec()

    private fun buffer(build: ByteBuf.() -> Unit): ByteBuf = Unpooled.buffer().apply(build)

    private fun ByteBuf.writeJstr(value: String) {
        value.forEach { writeByte(it.code) }
        writeByte(0)
    }

    private fun ByteBuf.writeJstr2(value: String) {
        writeByte(0)
        writeJstr(value)
    }

    private fun decode(data: ByteBuf): Rs2QuestType {
        val definition = codec.loadData(42, data)
        assertEquals(0, data.readableBytes(), "decoder did not consume the whole record")
        return definition
    }

    private fun encode(definition: Rs2QuestType): ByteArray {
        val out = Unpooled.buffer()
        with(codec) { out.encode(definition) }
        return ByteArray(out.readableBytes()).also { out.readBytes(it) }
    }

    private fun ByteBuf.toArray() = ByteArray(readableBytes()).also { getBytes(readerIndex(), it) }

    @Test
    fun `decodes every opcode`() {
        val data = buffer {
            writeByte(1); writeJstr2("Cook's Assistant")
            writeByte(2); writeJstr2("cooks assistant")
            writeByte(3); writeByte(1); writeShort(29); writeInt(1); writeInt(2)
            writeByte(4); writeByte(1); writeShort(1000); writeInt(0); writeInt(5)
            writeByte(5); writeShort(7)
            writeByte(6); writeByte(2)
            writeByte(7); writeByte(250)
            writeByte(8)
            writeByte(9); writeByte(1)
            writeByte(10); writeByte(2); writeInt(0x0C8A0C5F); writeInt(0x0C8B0C60)
            writeByte(12); writeInt(0x0C8C0C61)
            writeByte(13); writeByte(2); writeShort(3); writeShort(4)
            writeByte(14); writeByte(1); writeByte(7); writeByte(40)
            writeByte(15); writeShort(32)
            writeByte(17); writeShort(1234)
            writeByte(18); writeByte(1); writeInt(29); writeInt(1); writeInt(3); writeJstr("Need varp")
            writeByte(19); writeByte(1); writeInt(1000); writeInt(2); writeInt(4); writeJstr("Need varbit")
            writeByte(249); writeByte(2)
            writeByte(0); writeMedium(5); writeInt(99)
            writeByte(1); writeMedium(6); writeJstr("str")
            writeByte(0)
        }

        val quest = decode(data)

        assertEquals(42, quest.id)
        assertEquals("Cook's Assistant", quest.name)
        assertEquals("cooks assistant", quest.sortName)
        assertEquals(listOf(Rs2QuestVarRange(29, 1, 2)), quest.masterQuestVarps)
        assertEquals(listOf(Rs2QuestVarRange(1000, 0, 5)), quest.masterQuestVarbits)
        assertEquals(false, quest.wideVarbits)
        assertEquals(7, quest.parent)
        assertEquals(Rs2QuestKind.TUTORIAL, quest.kind)
        assertEquals(Rs2QuestDifficulty.MULTI, quest.difficulty)
        assertEquals(true, quest.members)
        assertEquals(1, quest.questPoints)
        assertEquals(listOf(0x0C8A0C5F, 0x0C8B0C60), quest.startCoords)
        assertEquals(0x0C8C0C61, quest.viaCoord)
        assertEquals(listOf(3, 4), quest.questReqs)
        assertEquals(listOf(Rs2QuestStatReq(7, 40)), quest.statReqs)
        assertEquals(32, quest.questPointsReq)
        assertEquals(1234, quest.icon)
        assertEquals(listOf(Rs2QuestVarReq(29, 1, 3, "Need varp")), quest.varpReqs)
        assertEquals(listOf(Rs2QuestVarReq(1000, 2, 4, "Need varbit")), quest.varbitReqs)
        assertEquals(mapOf(5 to 99, 6 to "str"), quest.params)
    }

    @Test
    fun `opcode 22 reads 3-byte varbit ids and marks the record wide`() {
        val quest = decode(buffer {
            writeByte(22); writeByte(1); writeMedium(70000); writeInt(-1); writeInt(1)
            writeByte(0)
        })

        assertEquals(listOf(Rs2QuestVarRange(70000, -1, 1)), quest.masterQuestVarbits)
        assertEquals(true, quest.wideVarbits)
    }

    @Test
    fun `null icon sentinel decodes to null and is omitted on encode`() {
        val quest = decode(buffer { writeByte(17); writeShort(0x7FFF); writeByte(0) })
        assertEquals(null, quest.icon)
        assertArrayEquals(byteArrayOf(0), encode(quest))
    }

    @Test
    fun `invalid difficulty throws`() {
        assertThrows(IllegalStateException::class.java) {
            decode(buffer { writeByte(7); writeByte(9); writeByte(0) })
        }
    }

    @Test
    fun `unknown opcode throws instead of silently desyncing`() {
        assertThrows(IllegalStateException::class.java) {
            decode(buffer { writeByte(11); writeByte(0) })
        }
    }

    @Test
    fun `encode then decode is byte identical for narrow and wide records`() {
        val narrow = buffer {
            writeByte(1); writeJstr2("Dragon Slayer")
            writeByte(3); writeByte(1); writeShort(176); writeInt(0); writeInt(10)
            writeByte(4); writeByte(2); writeShort(1); writeInt(0); writeInt(1); writeShort(2); writeInt(1); writeInt(2)
            writeByte(6); writeByte(1)
            writeByte(7); writeByte(4)
            writeByte(9); writeByte(2)
            writeByte(10); writeByte(1); writeInt(123456)
            writeByte(13); writeByte(1); writeShort(9)
            writeByte(14); writeByte(2); writeByte(0); writeByte(1); writeByte(1); writeByte(2)
            writeByte(15); writeShort(1)
            writeByte(17); writeShort(1234)
            writeByte(0)
        }
        assertArrayEquals(narrow.toArray(), encode(decode(narrow.copy())))

        val wide = buffer {
            writeByte(2); writeJstr2("sort")
            writeByte(22); writeByte(1); writeMedium(0x12345); writeInt(3); writeInt(4)
            writeByte(8)
            writeByte(12); writeInt(77)
            writeByte(18); writeByte(1); writeInt(1); writeInt(2); writeInt(3); writeJstr("m")
            writeByte(19); writeByte(1); writeInt(4); writeInt(5); writeInt(6); writeJstr("n")
            writeByte(0)
        }
        assertArrayEquals(wide.toArray(), encode(decode(wide.copy())))
    }

    @Test
    fun `large icon ids use the 4-byte smart form`() {
        val encoded = encode(Rs2QuestType(0, icon = 40000))
        assertArrayEquals(byteArrayOf(17, 0x80.toByte(), 0, 0x9C.toByte(), 0x40, 0), encoded)
        assertEquals(40000, decode(Unpooled.wrappedBuffer(encoded)).icon)
    }
}
