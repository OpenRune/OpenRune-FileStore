package dev.openrune.definition.codec

import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.definition.util.writeSmart
import dev.openrune.definition.util.writeString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class Rs2DataConfigCodecTest {

    @Test
    fun `param round trips the char keyed type form`() {
        val codec = Rs2ParamCodec()
        val raw = body {
            writeByte(1); writeByte(CacheVarLiteral.OBJ.ch.code)
            writeByte(2); writeInt(995)
            writeByte(4)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(CacheVarLiteral.OBJ, decoded.type)
        assertEquals(995, decoded.defaultInt)
        assertEquals(false, decoded.isMembers)
        assertNull(decoded.rs2) { "no RS3 only opcode, so no RS3 block" }
    }

    @Test
    fun `param round trips the id keyed type form at opcode 101`() {
        val codec = Rs2ParamCodec()
        val raw = body {
            writeByte(5); writeString("hello")
            writeByte(101); writeSmart(CacheVarLiteral.OBJ.id)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(CacheVarLiteral.OBJ, decoded.type)
        assertEquals("hello", decoded.defaultString)
        assertEquals(true, decoded.rs2?.typeFromId)
    }

    @Test
    fun `param id keyed type survives a two byte literal id`() {
        val codec = Rs2ParamCodec()
        val raw = body {
            writeByte(101); writeSmart(CacheVarLiteral.VARP.id)
        }

        assertEquals(CacheVarLiteral.VARP, codec.assertRoundTrips(raw).type)
    }

    @Test
    fun `struct round trips params and the plugin flag`() {
        val codec = Rs2StructCodec()
        val raw = body {
            writeByte(249)
            writeByte(2)
            writeByte(0); writeMedium(10); writeInt(42)
            writeByte(1); writeMedium(11); writeString("text")
            writeByte(251)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(42, decoded.params?.get(10))
        assertEquals("text", decoded.params?.get(11))
        assertEquals(false, decoded.rs2?.pluginApi)
    }

    @Test
    fun `enum round trips the sparse int form`() {
        val codec = Rs2EnumCodec()
        val raw = body {
            writeByte(1); writeByte(CacheVarLiteral.INT.ch.code)
            writeByte(2); writeByte(CacheVarLiteral.OBJ.ch.code)
            writeByte(4); writeInt(-1)
            writeByte(6); writeShort(1)
            writeInt(3); writeInt(995)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(995, decoded.values[3])
        assertEquals(-1, decoded.defaultInt)
        assertNull(decoded.rs2) { "no RS3 only opcode, so no RS3 block" }
    }

    @Test
    fun `enum round trips the dense string form at opcode 7`() {
        val codec = Rs2EnumCodec()
        val raw = body {
            writeByte(1); writeByte(CacheVarLiteral.INT.ch.code)
            writeByte(2); writeByte(CacheVarLiteral.STRING.ch.code)
            writeByte(3); writeString("none")
            writeByte(7)
            writeShort(64)
            writeShort(2)
            writeShort(0); writeString("first")
            writeShort(1); writeString("second")
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(true, decoded.rs2?.dense)
        assertEquals(64, decoded.rs2?.capacity)
        assertEquals("first", decoded.values[0])
        assertEquals("second", decoded.values[1])
        assertEquals("none", decoded.defaultString)
    }

    @Test
    fun `enum round trips the dense int form at opcode 8`() {
        val codec = Rs2EnumCodec()
        val raw = body {
            writeByte(1); writeByte(CacheVarLiteral.INT.ch.code)
            writeByte(2); writeByte(CacheVarLiteral.NPC.ch.code)
            writeByte(8)
            writeShort(4)
            writeShort(1)
            writeShort(2); writeInt(3000)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(true, decoded.rs2?.dense)
        assertEquals(4, decoded.rs2?.capacity)
        assertEquals(3000, decoded.values[2])
    }

    @Test
    fun `enum round trips the id keyed types and plugin flag`() {
        val codec = Rs2EnumCodec()
        val raw = body {
            writeByte(101); writeSmart(CacheVarLiteral.INT.id)
            writeByte(102); writeSmart(CacheVarLiteral.VARP.id)
            writeByte(103)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(CacheVarLiteral.INT, decoded.keyType)
        assertEquals(CacheVarLiteral.VARP, decoded.valueType)
        assertEquals(true, decoded.rs2?.typesFromId)
        assertEquals(false, decoded.rs2?.pluginApi)
    }
}
