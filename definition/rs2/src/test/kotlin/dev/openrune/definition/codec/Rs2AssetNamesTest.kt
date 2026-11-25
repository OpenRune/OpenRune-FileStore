package dev.openrune.definition.codec

import dev.openrune.definition.Rs2AssetNameGroup
import dev.openrune.definition.util.writeString
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test

class Rs2AssetNamesTest {

    private fun pack(packType: Int, entries: List<Pair<Int, String?>>): ByteArray {
        val heap = Unpooled.buffer()
        val offsets = entries.map { (_, name) ->
            if (name == null) {
                -1
            } else {
                val at = heap.writerIndex()
                heap.writeString(name)
                at
            }
        }
        val out = Unpooled.buffer()
        out.writeInt(packType)
        out.writeInt(entries.size)
        entries.forEachIndexed { index, entry ->
            if (packType == 2) out.writeInt(entry.first)
            out.writeInt(offsets[index])
        }
        out.writeBytes(heap)
        return ByteArray(out.readableBytes()).also { out.readBytes(it) }
    }

    @Test
    fun `pack type 1 uses the entry index as the id`() {
        val raw = pack(1, listOf(0 to "first", 1 to "second", 2 to "third"))

        val names = Rs2AssetNames.decode(Unpooled.wrappedBuffer(raw))

        assertEquals(1, names.packType)
        assertEquals(listOf("first", "second", "third"), names.elements.map { it.name })
        assertEquals(listOf(0, 1, 2), names.elements.map { it.id })
        assertArrayEquals(raw, Rs2AssetNames.encode(names))
    }

    @Test
    fun `pack type 2 carries explicit ids`() {
        val raw = pack(2, listOf(40 to "forty", 7 to "seven"))

        val names = Rs2AssetNames.decode(Unpooled.wrappedBuffer(raw))

        assertEquals(2, names.packType)
        assertEquals(listOf(40, 7), names.elements.map { it.id })
        assertEquals(listOf("forty", "seven"), names.elements.map { it.name })
        assertArrayEquals(raw, Rs2AssetNames.encode(names))
    }

    @Test
    fun `an entry with no name is skipped but its hole survives a re-encode`() {
        val raw = pack(2, listOf(1 to "one", 2 to null, 3 to "three"))

        val names = Rs2AssetNames.decode(Unpooled.wrappedBuffer(raw))

        assertEquals(listOf("one", "three"), names.elements.map { it.name })
        assertEquals(listOf(2), names.unnamed)
        assertArrayEquals(raw, Rs2AssetNames.encode(names))
    }

    @Test
    fun `names are lower cased on read`() {
        val raw = pack(1, listOf(0 to "Bronze_Sword"))

        assertEquals("bronze_sword", Rs2AssetNames.decode(Unpooled.wrappedBuffer(raw)).elements.single().name)
    }

    @Test
    fun `an unknown pack type is rejected`() {
        val raw = pack(3, listOf(0 to "x"))

        assertThrows<IllegalArgumentException> { Rs2AssetNames.decode(Unpooled.wrappedBuffer(raw)) }
    }

    @Test
    fun `a var group splits into vars and varbits on the underscore prefix`() {
        val raw = pack(2, listOf(1 to "quest_points", 2 to "_some_bit"))
        val names = Rs2AssetNames.decode(Unpooled.wrappedBuffer(raw))

        val (vars, bits) = Rs2AssetNames.splitVarNames(Rs2AssetNameGroup.VAR_PLAYER, names)

        assertEquals(listOf("quest_points"), vars.map { it.name })
        assertEquals(listOf("some_bit"), bits.map { it.name })
        assertEquals(listOf(0), bits.map { it.id })
    }

    @Test
    fun `varbit ids are numbered from the var they follow`() {
        val raw = pack(
            2,
            listOf(
                10 to "first_var",
                11 to "_bit_a",
                12 to "_bit_b",
                20 to "second_var",
                21 to "_bit_c",
            ),
        )
        val names = Rs2AssetNames.decode(Unpooled.wrappedBuffer(raw))

        val (vars, bits) = Rs2AssetNames.splitVarNames(Rs2AssetNameGroup.VAR_PLAYER, names)

        assertEquals(listOf(10, 20), vars.map { it.id })
        assertEquals(listOf("bit_a", "bit_b", "bit_c"), bits.map { it.name })
        assertEquals(listOf(0, 1, 0), bits.map { it.id })
    }

    @Test
    fun `a non var group is not split`() {
        val raw = pack(1, listOf(0 to "_odd_name"))
        val names = Rs2AssetNames.decode(Unpooled.wrappedBuffer(raw))

        val (vars, bits) = Rs2AssetNames.splitVarNames(Rs2AssetNameGroup.OBJ, names)

        assertEquals(1, vars.size)
        assertEquals(true, bits.isEmpty())
    }

    @Test
    fun `group ids map to the osrs table names where they match`() {
        assertEquals("obj", Rs2AssetNameGroup.fromId(36)?.groupName)
        assertEquals("npc", Rs2AssetNameGroup.fromId(35)?.groupName)
        assertEquals("loc", Rs2AssetNameGroup.fromId(28)?.groupName)
        assertEquals("component", Rs2AssetNameGroup.fromId(0)?.groupName)
        assertEquals(null, Rs2AssetNameGroup.fromId(999))
    }
}
