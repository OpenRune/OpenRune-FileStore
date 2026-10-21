package dev.openrune

import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.codec.EnumCodec
import dev.openrune.definition.codec.IdentityKitCodec
import dev.openrune.definition.codec.NPCCodec
import dev.openrune.definition.util.CacheVarLiteral
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The 237 config opcodes that widen a field: enum long values (7/8), identity kit chathead models
 * read as ints (70-79) and the npc sub-op opcodes (251/253).
 *
 * [CodecByteRoundTripTest] covers the same ground against a real cache, but only when one is
 * checked out, and no shipped cache exercises these yet. These build the bytes by hand instead.
 */
class ExtendedConfigOpcodeTest {

    private val revision = 240

    private fun buffer(build: ByteBuf.() -> Unit): ByteBuf =
        Unpooled.buffer().apply(build).apply { writeByte(0) }

    private fun <T : Definition> encode(codec: DefinitionCodec<T>, definition: T): ByteArray {
        val out = Unpooled.buffer()
        try {
            with(codec) { out.encode(definition) }
            return ByteArray(out.readableBytes()).also { out.readBytes(it) }
        } finally {
            out.release()
        }
    }

    private fun <T : Definition> reencode(codec: DefinitionCodec<T>, raw: ByteBuf): ByteArray {
        val once = encode(codec, codec.loadData(1, raw))
        return encode(codec, codec.loadData(1, Unpooled.wrappedBuffer(once)))
            .also { assertArrayEquals(once, it, "re-encode differs from first encode") }
    }

    @Test
    fun `enum keeps long values and a long default`() {
        val codec = EnumCodec()
        val raw = buffer {
            writeByte(1); writeByte(CacheVarLiteral.INT.ch.code)
            writeByte(2); writeByte(CacheVarLiteral.HASH64.ch.code)
            writeByte(8); writeLong(-1L)
            writeByte(7); writeShort(2)
            writeInt(4); writeLong(Long.MAX_VALUE)
            writeInt(7); writeLong(1L shl 40)
        }

        val decoded = codec.loadData(1, raw.copy())

        assertEquals(-1L, decoded.defaultLong)
        assertEquals(0, decoded.defaultInt)
        assertEquals(Long.MAX_VALUE, decoded.values[4])
        assertEquals(1L shl 40, decoded.values[7])
        assertEquals(Long.MAX_VALUE, decoded.getLong(4))
        assertEquals(-1L, decoded.getLong(999))

        reencode(codec, raw)
    }

    @Test
    fun `enum still writes int values as opcode 6`() {
        val codec = EnumCodec()
        val raw = buffer {
            writeByte(1); writeByte(CacheVarLiteral.INT.ch.code)
            writeByte(2); writeByte(CacheVarLiteral.OBJ.ch.code)
            writeByte(4); writeInt(-1)
            writeByte(6); writeShort(1)
            writeInt(3); writeInt(995)
        }

        assertArrayEquals(
            ByteArray(raw.readableBytes()).also { raw.copy().readBytes(it) },
            reencode(codec, raw),
        )
    }

    @Test
    fun `identity kit reads chathead models as ints`() {
        val codec = IdentityKitCodec(revision)
        val raw = buffer {
            writeByte(1); writeByte(0)
            writeByte(5); writeByte(2); writeInt(70_000); writeInt(70_001)
            writeByte(70); writeInt(80_000)
            writeByte(79); writeInt(80_009)
        }

        val decoded = codec.loadData(1, raw.copy())

        assertEquals(listOf(70_000, 70_001), decoded.models)
        assertEquals(80_000, decoded.chatheadModels[0])
        assertEquals(80_009, decoded.chatheadModels[9])
        assertEquals(List(8) { -1 }, decoded.chatheadModels.subList(1, 9))

        reencode(codec, raw)
    }

    @Test
    fun `npc reads sub ops and conditional sub ops`() {
        val codec = NPCCodec(revision)
        val raw = buffer {
            writeByte(2); writeCString("Banker")
            writeByte(30); writeCString("Talk-to")
            writeByte(251); writeByte(0); writeByte(2); writeCString("Collect")
            writeByte(251); writeByte(0); writeByte(3); writeCString("Deposit")
            writeByte(253)
            writeByte(0); writeShort(1); writeShort(0xFFFF); writeShort(4); writeInt(1); writeInt(2)
            writeCString("Bank")
        }

        val decoded = codec.loadData(1, raw.copy())

        // One sub-op per opcode occurrence, not a zero-terminated list behind one op index.
        assertEquals(listOf("Collect", "Deposit"), decoded.actions.subOpsOrEmpty[0].map { it.text })
        assertEquals(listOf(1, 2), decoded.actions.subOpsOrEmpty[0].map { it.subID })

        val conditional = decoded.actions.conditionalSubOpsOrEmpty[0].values.single().single()
        assertEquals("Bank", conditional.text)
        assertEquals(4, conditional.varbitID)
        assertEquals(1, conditional.minValue)
        assertEquals(2, conditional.maxValue)

        reencode(codec, raw)
    }

    private fun ByteBuf.writeCString(value: String) {
        value.forEach { writeByte(it.code) }
        writeByte(0)
    }
}
