package dev.openrune.definition.codec

import dev.openrune.definition.VarDomain
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Rs2VarCodecTest {

    @Test
    fun `varbit round trips every opcode`() {
        val codec = Rs2VarBitCodec()
        val raw = body {
            writeByte(1); writeByte(VarDomain.OBJECT.id); writeNullableLargeSmartCorrect(4321)
            writeByte(2); writeByte(3); writeByte(9)
            writeByte(16)
            writeByte(17)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(VarDomain.OBJECT, decoded.domain)
        assertEquals(4321, decoded.baseVar)
        assertEquals(3, decoded.startBit)
        assertEquals(9, decoded.endBit)
        assertEquals(true, decoded.wikiSync)
        assertEquals(false, decoded.pluginApi)
    }

    @Test
    fun `varbit covers every domain`() {
        val codec = Rs2VarBitCodec()
        for (domain in VarDomain.entries) {
            val raw = body {
                writeByte(1); writeByte(domain.id); writeNullableLargeSmartCorrect(1)
            }
            assertEquals(domain, codec.assertRoundTrips(raw).domain)
        }
    }

    @Test
    fun `varbit base var survives the wide smart form`() {
        val codec = Rs2VarBitCodec()
        val raw = body {
            writeByte(1); writeByte(VarDomain.PLAYER.id); writeNullableLargeSmartCorrect(90_000)
        }

        assertEquals(90_000, codec.assertRoundTrips(raw).baseVar)
    }

    @Test
    fun `var client round trips the shared type`() {
        val codec = Rs2VarClientCodec()
        val raw = body {
            writeByte(1); writeByte(CacheVarLiteral.STRING.ch.code)
            writeByte(2)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(CacheVarLiteral.STRING, decoded.rs2?.type)
        assertEquals(true, decoded.persist)
    }

    @Test
    fun `var client without a type stays compatible with the osrs shape`() {
        val codec = Rs2VarClientCodec()
        val raw = body { writeByte(2) }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(null, decoded.rs2?.type)
        assertEquals(true, decoded.persist)
    }

}
