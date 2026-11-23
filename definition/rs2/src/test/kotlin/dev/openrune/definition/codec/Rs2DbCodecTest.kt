package dev.openrune.definition.codec

import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.definition.util.writeSmart
import dev.openrune.definition.util.writeString
import dev.openrune.definition.util.writeUnsignedShortSmart
import dev.openrune.definition.util.writeVarInt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Rs2DbCodecTest {

    @Test
    fun `dbrow round trips a two column table`() {
        val codec = Rs2DbRowCodec()
        val raw = body {
            writeByte(3)
            writeByte(1)
            writeByte(0)
            writeByte(1); writeSmart(CacheVarLiteral.INT.id)
            writeUnsignedShortSmart(2)
            writeInt(10); writeInt(20)
            writeByte(1)
            writeByte(1); writeSmart(CacheVarLiteral.STRING.id)
            writeUnsignedShortSmart(1)
            writeString("hello")
            writeByte(255)
            writeByte(4); writeVarInt(300)
            writeByte(5)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(1, decoded.leading)
        assertEquals(2, decoded.columns.size)
        assertEquals(listOf(CacheVarLiteral.INT), decoded.columns[0].types)
        assertEquals(listOf<Any>(10, 20), decoded.columns[0].values)
        assertEquals(listOf<Any>("hello"), decoded.columns[1].values)
        assertEquals(300, decoded.table)
        assertEquals(false, decoded.pluginApi)
    }

    @Test
    fun `dbrow round trips a tuple column`() {
        val codec = Rs2DbRowCodec()
        val raw = body {
            writeByte(3)
            writeByte(0)
            writeByte(0)
            writeByte(2)
            writeSmart(CacheVarLiteral.INT.id); writeSmart(CacheVarLiteral.STRING.id)
            writeUnsignedShortSmart(2)
            writeInt(1); writeString("a")
            writeInt(2); writeString("b")
            writeByte(255)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(2, decoded.columns[0].types.size)
        assertEquals(listOf<Any>(1, "a", 2, "b"), decoded.columns[0].values)
    }

    @Test
    fun `dbrow round trips a varint table id past one byte`() {
        val codec = Rs2DbRowCodec()
        val raw = body { writeByte(4); writeVarInt(70_000) }

        assertEquals(70_000, codec.assertRoundTrips(raw).table)
    }

    @Test
    fun `dbtable round trips the inline opcode 1 form`() {
        val codec = Rs2DbTableCodec()
        val raw = body {
            writeByte(1)
            writeByte(2)
            writeByte(0 or 128)
            writeByte(1); writeSmart(CacheVarLiteral.INT.id)
            writeUnsignedShortSmart(1); writeInt(7)
            writeByte(1)
            writeByte(1); writeSmart(CacheVarLiteral.STRING.id)
            writeByte(255)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(false, decoded.sizePrefixed)
        assertEquals(2, decoded.columnCount)
        assertEquals(true, decoded.columns[0].hasDefault)
        assertEquals(listOf<Any>(7), decoded.columns[0].values)
        assertEquals(false, decoded.columns[1].hasDefault)
        assertEquals(listOf(CacheVarLiteral.STRING), decoded.columns[1].types)
    }

    @Test
    fun `dbtable round trips the length prefixed opcode 2 form`() {
        val codec = Rs2DbTableCodec()
        val bodyBytes = body {
            writeByte(1)
            writeByte(0)
            writeByte(1); writeByte(1); writeSmart(CacheVarLiteral.INT.id)
            writeByte(2); writeUnsignedShortSmart(2); writeInt(5); writeInt(6)
            writeByte(0)
            writeByte(255)
        }
        val inner = bodyBytes.copyOf(bodyBytes.size - 1)

        val raw = body {
            writeByte(2)
            writeInt(inner.size)
            writeBytes(inner)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(true, decoded.sizePrefixed)
        assertEquals(1, decoded.columnCount)
        assertEquals(listOf(CacheVarLiteral.INT), decoded.columns[0].types)
        assertEquals(listOf<Any>(5, 6), decoded.columns[0].values)
        assertEquals(true, decoded.columns[0].hasDefault)
    }
}
