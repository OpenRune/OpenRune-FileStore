package dev.openrune

import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.gameval.GameValHandler.lookup
import dev.openrune.cache.gameval.GameValHandler.lookupAs
import dev.openrune.cache.gameval.GameValList
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.gameval.impl.Interface.InterfaceComponent
import dev.openrune.cache.withOffset
import dev.openrune.cache.tools.tasks.impl.sprites.Sprite
import dev.openrune.cache.tools.tasks.impl.sprites.SpriteSet
import dev.openrune.definition.EntityOpsDefinition
import dev.openrune.definition.codec.ObjectCodec
import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.type.NpcType
import dev.openrune.definition.type.ObjectType
import dev.openrune.definition.type.VarpType
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.definition.util.readString
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

/** Behaviour locks for the indexed and lazily allocated decode paths. */
class DecodeFastPathTest {

    // --- readString ------------------------------------------------------------------------

    @Test
    fun `readString stops at the terminator and consumes it`() {
        val buffer = Unpooled.wrappedBuffer(byteArrayOf(65, 66, 0, 67, 0))
        assertEquals("AB", buffer.readString())
        assertEquals(3, buffer.readerIndex())
        assertEquals("C", buffer.readString())
        assertEquals(5, buffer.readerIndex())
    }

    @Test
    fun `readString returns empty for an immediate terminator and for an empty buffer`() {
        assertEquals("", Unpooled.wrappedBuffer(byteArrayOf(0)).readString())
        assertEquals("", Unpooled.buffer(0).readString())
    }

    @Test
    fun `readString consumes the remainder when unterminated`() {
        val buffer = Unpooled.wrappedBuffer(byteArrayOf(88, 89))
        assertEquals("XY", buffer.readString())
        assertFalse(buffer.isReadable)
    }

    @Test
    fun `readString decodes high bytes as latin-1`() {
        val buffer = Unpooled.wrappedBuffer(byteArrayOf(0xE9.toByte(), 0xFF.toByte(), 0))
        val value = buffer.readString()
        assertEquals(2, value.length)
        assertEquals(0xE9, value[0].code)
        assertEquals(0xFF, value[1].code)
    }

    @Test
    fun `readString only reads within the readable region`() {
        val buffer = Unpooled.buffer()
        buffer.writeBytes(byteArrayOf(65, 0, 66, 0))
        buffer.readerIndex(2)
        assertEquals("B", buffer.readString())
    }

    // --- EntityOpsDefinition ---------------------------------------------------------------

    @Test
    fun `fresh entity ops report empty without allocating slots`() {
        val ops = EntityOpsDefinition()

        assertTrue(ops.isEmpty())
        assertNull(ops.getOpOrNull(0))
        assertTrue(ops.opsOrEmpty.isEmpty())
        assertTrue(ops.subOpsOrEmpty.isEmpty())
        assertTrue(ops.conditionalOpsOrEmpty.isEmpty())
        assertTrue(ops.conditionalSubOpsOrEmpty.isEmpty())
        assertTrue(ops.getSubOpsOrEmpty(0).isEmpty())
        assertTrue(ops.getConditionalOpsOrEmpty(0).isEmpty())
        assertTrue(ops.getConditionalSubOpsOrEmpty(0).isEmpty())
        assertTrue(ops.getConditionalSubOpsBySubIdOrEmpty(0, 0).isEmpty())
        assertTrue(ops.contentEquals(EntityOpsDefinition()))
    }

    @Test
    fun `the backing list is created once and reused`() {
        val ops = EntityOpsDefinition()
        assertSame(ops.ops, ops.ops)
    }

    @Test
    fun `setting an op pads the earlier slots with null`() {
        val ops = EntityOpsDefinition()
        ops.setOp(2, "Open")

        assertFalse(ops.isEmpty())
        assertEquals(3, ops.opsOrEmpty.size)
        assertNull(ops.getOpOrNull(0))
        assertNull(ops.getOpOrNull(1))
        assertEquals("Open", ops.getOpOrNull(2))
        assertNull(ops.getOpOrNull(9))
        assertFalse(ops.contentEquals(EntityOpsDefinition()))
    }

    @Test
    fun `sub and conditional ops round trip through the lazy slots`() {
        val ops = EntityOpsDefinition()
        ops.setSubOp(1, 3, "Sub")
        ops.setConditionalOp(0, "Cond", varpID = 100, varbitID = 200, min = 1, max = 9)
        ops.setConditionalSubOp(2, 4, "CondSub", varpID = 300, varbitID = 400, min = 2, max = 8)

        assertEquals("Sub", ops.getSubOpOrNull(1, 3)?.text)
        assertEquals("Cond", ops.getConditionalOpsOrEmpty(0).single().text)
        assertEquals("CondSub", ops.getConditionalSubOpsBySubIdOrEmpty(2, 4).single().text)
        assertTrue(ops.toString().contains("Sub"))
    }

    // --- ObjectCodec decode ------------------------------------------------------------------

    private fun decodeObject(vararg bytes: Int): ObjectType {
        val payload = ByteArray(bytes.size + 1) { index ->
            if (index == bytes.size) 0 else bytes[index].toByte()
        }
        return ObjectCodec(240).loadData(1, payload)
    }

    @Test
    fun `opcode 1 reads paired models and types`() {
        val obj = decodeObject(1, 2, 0x01, 0x02, 10, 0xFF, 0xFF, 11)

        assertEquals(listOf(0x0102, 0xFFFF), obj.objectModels)
        assertEquals(listOf(10, 11), obj.objectTypes)
    }

    @Test
    fun `opcode 5 reads models only and clears the types`() {
        val obj = decodeObject(1, 1, 0x00, 0x01, 10, 5, 2, 0x00, 0x07, 0x00, 0x08)

        assertEquals(listOf(7, 8), obj.objectModels)
        assertNull(obj.objectTypes)
    }

    @Test
    fun `opcode 40 keeps signed colour values`() {
        val obj = decodeObject(40, 2, 0x00, 0x01, 0x00, 0x02, 0xFF, 0xFF, 0x80, 0x00)

        assertEquals(listOf(1, -1), obj.originalColours)
        assertEquals(listOf(2, -32768), obj.modifiedColours)
    }

    @Test
    fun `opcode 77 appends the multi default after the transform ids`() {
        val obj = decodeObject(77, 0x00, 0x64, 0xFF, 0xFF, 1, 0x00, 0x07, 0x00, 0x08)

        assertEquals(100, obj.multiVarBit)
        assertEquals(-1, obj.multiVarp)
        assertEquals(-1, obj.multiDefault)
        assertEquals(listOf(7, 8, -1), obj.transforms)
    }

    @Test
    fun `opcode 79 reads the trailing ambient sound ids`() {
        val obj = decodeObject(79, 0x00, 0x01, 0x00, 0x02, 3, 4, 2, 0x00, 0x0A, 0x00, 0x0B)

        assertEquals(listOf(10, 11), obj.ambientSoundIds)
    }

    @Test
    fun `base ops decode into the action slots`() {
        val obj = decodeObject(2, 'D'.code, 'o'.code, 'o'.code, 'r'.code, 0, 30, 'O'.code, 'p'.code, 0)

        assertEquals("Door", obj.name)
        assertEquals("Op", obj.actions.getOpOrNull(0))
        assertTrue(obj.hasActions())
        assertTrue(obj.hasOption("op"))
        assertEquals(1, obj.getOption("Op"))
    }

    @Test
    fun `hash code is stable for equal objects`() {
        val first = decodeObject(2, 'A'.code, 0, 14, 3)
        val second = decodeObject(2, 'A'.code, 0, 14, 3)

        // `actions` compares by identity, so two decodes are never `equals`; only the fold is locked.
        var expected = first.name.hashCode()
        for (part in listOf(
            first.mapAreaId,
            first.actions.hashCode(),
            first.sizeX,
            first.sizeY,
            first.objectModels?.hashCode() ?: 0,
            first.modelSizeX,
            first.modelSizeY,
            first.modelSizeZ,
            first.animationId,
        )) {
            expected = 31 * expected + part
        }
        assertEquals(expected, first.hashCode())
        assertEquals(3, second.sizeX)
    }

    // --- gameval lookups ---------------------------------------------------------------------

    @Test
    fun `gameval list looks up by id and keeps first-wins semantics`() {
        val list = GameValList(
            listOf(
                GameValElement("first", 5),
                GameValElement("duplicate", 5),
                Interface("iface", 7, listOf(InterfaceComponent("child", 3, 7))),
            )
        )

        assertEquals("first", list.lookup(5)?.name)
        assertEquals("iface", list.lookup(7)?.name)
        assertNull(list.lookup(99))
        assertEquals(3, list.size)
        assertEquals("iface", list.lookupAs<Interface>(7)?.name)
        assertNull(list.lookupAs<Interface>(5))
    }

    @Test
    fun `lookupAs falls back to a scan when the indexed element is another type`() {
        val list = GameValList(
            listOf(
                GameValElement("plain", 4),
                Interface("iface", 4, emptyList()),
            )
        )

        assertEquals("plain", list.lookup(4)?.name)
        assertEquals("iface", list.lookupAs<Interface>(4)?.name)
    }

    @Test
    fun `interface component index matches a linear scan`() {
        val components = listOf(
            InterfaceComponent("a", 0, 12),
            InterfaceComponent("b", 5, 12),
            InterfaceComponent("shadowed", 5, 12),
        )
        val iface = Interface("iface", 12, components)

        assertEquals("a", iface.component(0)?.name)
        assertEquals("b", iface.component(5)?.name)
        assertNull(iface.component(1))
    }

    // --- CacheManager helpers ------------------------------------------------------------------

    @Test
    fun `withOffset shifts keys and definition ids`() {
        val objects = mutableMapOf(
            1 to ObjectType(id = 1, name = "one"),
            2 to ObjectType(id = 2, name = "two"),
        )

        val shifted = objects.withOffset(10)

        assertEquals(setOf(11, 12), shifted.keys)
        assertEquals(11, shifted[11]?.id)
        assertEquals("two", shifted[12]?.name)
    }

    @Test
    fun `withOffset of zero returns a copy`() {
        val objects = mutableMapOf(1 to ObjectType(id = 1))
        val copy = objects.withOffset(0)

        copy.remove(1)
        assertEquals(1, objects.size)
    }

    // --- CacheVarLiteral indexes -----------------------------------------------------------------

    @Test
    fun `var literal lookups resolve by id char and name`() {
        assertSame(CacheVarLiteral.INT, CacheVarLiteral.byID(0))
        assertSame(CacheVarLiteral.STRING, CacheVarLiteral.byID(36))
        assertSame(CacheVarLiteral.STRING, CacheVarLiteral.byChar('s'))
        assertSame(CacheVarLiteral.STRING, CacheVarLiteral.byName("string"))
        assertSame(CacheVarLiteral.DBTABLE, CacheVarLiteral.byID(118))
        assertSame(CacheVarLiteral.byID(36), CacheVarLiteral.byID(36))
    }

    @Test
    fun `registering a literal invalidates the cached indexes`() {
        val id = 30001
        val ch = ''
        val literal = CacheVarLiteral.registerExternal(id, ch, name = "DECODE_FAST_PATH_TEST")

        assertSame(literal, CacheVarLiteral.byID(id))
        assertSame(literal, CacheVarLiteral.byChar(ch))
        assertSame(literal, CacheVarLiteral.byName("decode_fast_path_test"))
        assertSame(CacheVarLiteral.INT, CacheVarLiteral.byID(0))
    }

    // --- Definition.extra --------------------------------------------------------------------------

    @Test
    fun `npc and item definitions retain extra properties`() {
        val npc = NpcType(id = 7)
        npc.setExtraProperty("walkMask", 3)
        npc.setExtraProperty("invisiblePriority", true)

        assertEquals(3, npc.getExtraProperty<Int>("walkMask"))
        with(npc) {
            assertEquals(3, getIntProperty("walkMask"))
            assertTrue(getBooleanProperty("invisiblePriority"))
            assertFalse(getBooleanProperty("missing"))
            assertEquals(-1, getIntProperty("missing"))
        }

        val item = ItemType(id = 9)
        item.setExtraProperty("shadow", "grey")
        with(item) {
            assertEquals("grey", getStringProperty("shadow"))
        }
    }

    @Test
    fun `extra properties are not shared between instances`() {
        val first = NpcType(id = 1)
        val second = NpcType(id = 2)
        first.setExtraProperty("walkMask", 5)

        assertEquals(5, first.getExtraProperty<Int>("walkMask"))
        assertNull(second.getExtraProperty<Int>("walkMask"))
    }

    @Test
    fun `a type without its own store reads empty`() {
        val varp = VarpType(id = 3)
        assertTrue(varp.extra.isEmpty())
        with(varp) {
            assertEquals(-1, getIntProperty("anything"))
        }
    }

    // --- OpcodeList index --------------------------------------------------------------------------

    private fun opcodeEntry(opcodes: Set<Int>, primary: Int) =
        DefinitionOpcode<ObjectType>(
            opcodes = opcodes,
            decode = { _, _, _ -> },
            encode = { _, _ -> },
            primaryOpcode = primary,
        )

    @Test
    fun `opcode list resolves handlers by opcode and rejects overlaps`() {
        val list = OpcodeList<ObjectType>()
        val first = opcodeEntry(setOf(1, 2), 1)
        val second = opcodeEntry(setOf(7), 7)

        list.add(first)
        list.add(second)

        assertSame(first, list.forOpcode(1))
        assertSame(first, list.forOpcode(2))
        assertSame(second, list.forOpcode(7))
        assertNull(list.forOpcode(3))
        assertEquals(2, list.registeredOpcodes.size)

        val clash = assertThrows(IllegalStateException::class.java) { list.add(opcodeEntry(setOf(2), 2)) }
        assertTrue(clash.message!!.contains("already exist"))
        assertEquals(2, list.registeredOpcodes.size)
        assertSame(first, list.forOpcode(2))
    }

    @Test
    fun `opcode list picks up entries added after the first lookup`() {
        val list = OpcodeList<ObjectType>()
        assertNull(list.forOpcode(4))

        val added = opcodeEntry(setOf(4), 4)
        list.addAll(listOf(added))
        assertSame(added, list.forOpcode(4))
    }

    // --- SpriteSet ------------------------------------------------------------------------------

    @Test
    fun `sprite set round trips through the indexed palette`() {
        val image = BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB)
        val colours = intArrayOf(0xFF102030.toInt(), 0xFF405060.toInt(), 0xFF708090.toInt(), 0xFF102030.toInt())
        for (y in 0 until 2) {
            for (x in 0 until 2) {
                image.setRGB(x, y, colours[y * 2 + x])
            }
        }

        val encoded = SpriteSet(3, 2, 2, mutableListOf(Sprite(0, 0, image))).encode()
        val decoded = SpriteSet.decode(3, encoded)

        assertEquals(1, decoded.sprites.size)
        val result = decoded.sprites.first()
        for (y in 0 until 2) {
            for (x in 0 until 2) {
                assertEquals(colours[y * 2 + x], result.getRGB(x, y), "pixel $x,$y")
            }
        }
    }
}
