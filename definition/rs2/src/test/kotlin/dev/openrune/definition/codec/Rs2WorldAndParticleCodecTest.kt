package dev.openrune.definition.codec

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Rs2WorldAndParticleCodecTest {

    @Test
    fun `world area round trips every opcode`() {
        val codec = WorldAreaCodec()
        val raw = body {
            writeByte(2); writeMedium(0x00FF80)
            writeByte(3); writeInt(0x3000_3000); writeInt(0x3100_3100)
            writeByte(4); writeInt(0x3200_3200); writeInt(0x0100_0002)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(0x00FF80, decoded.colour)
        assertEquals(0x30003000, decoded.impostorSquareFrom)
        assertEquals(0x31003100, decoded.impostorSquareTo)
        assertEquals(0x32003200, decoded.impostorZoneCoord)
        assertEquals(0x01000002, decoded.impostorZoneTemplate)
    }

    @Test
    fun `particle effector round trips every opcode`() {
        val codec = ParticleEffectorCodec()
        val raw = body {
            writeByte(1); writeShort(1024)
            writeByte(2); writeByte(3)
            writeByte(3); writeInt(100); writeInt(-200); writeInt(300)
            writeByte(4); writeByte(2); writeInt(4096)
            writeByte(6); writeByte(1)
            writeByte(8)
            writeByte(9)
            writeByte(10)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(1024, decoded.angle)
        assertEquals(-200, decoded.forceDirectionY)
        assertEquals(2, decoded.falloffType)
        assertEquals(4096, decoded.falloffValue)
        assertEquals(true, decoded.reverseDirection)
    }

    @Test
    fun `particle emitter round trips the scalar and list opcodes`() {
        val codec = ParticleEmitterCodec()
        val raw = body {
            writeByte(1); writeShort(1); writeShort(2); writeShort(3); writeShort(4)
            writeByte(3); writeInt(10); writeInt(20)
            writeByte(4); writeByte(1); writeByte(-3)
            writeByte(5); writeShort(64)
            writeByte(6); writeInt(-1); writeInt(0)
            writeByte(7); writeShort(30); writeShort(60)
            writeByte(8); writeShort(5); writeShort(9)
            writeByte(9); writeByte(2); writeShort(11); writeShort(12)
            writeByte(10); writeByte(1); writeShort(13)
            writeByte(12); writeByte(-1)
            writeByte(13); writeByte(2)
            writeByte(15); writeShort(777)
            writeByte(16); writeByte(1); writeShort(2); writeShort(3); writeByte(4)
            writeByte(24)
            writeByte(25); writeByte(1); writeShort(14)
            writeByte(26)
            writeByte(32)
            writeByte(34)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(listOf(1, 2, 3, 4), decoded.coneAngle)
        assertEquals(-3, decoded.speedControlValue)
        assertEquals(64, decoded.size)
        assertEquals(listOf(11, 12), decoded.worldEffectors)
        assertEquals(listOf(13), decoded.localEffectors)
        assertEquals(-1, decoded.killBelowLevel)
        assertEquals(777, decoded.material)
        assertEquals(listOf(1, 2, 3, 4), decoded.unknown16)
        assertEquals(false, decoded.linkColourChannels)
        assertEquals(listOf(14), decoded.globalEffectors)
        assertEquals(false, decoded.enabled)
        assertEquals(false, decoded.lighting)
        assertEquals(false, decoded.unknown34)
    }

    @Test
    fun `particle emitter keeps the short variant of opcode 29`() {
        val codec = ParticleEmitterCodec()
        val raw = body {
            writeByte(29); writeByte(0); writeShort(-50)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(0, decoded.angularVelocityVariant)
        assertEquals(listOf(-50), decoded.angularVelocity)
    }

    @Test
    fun `particle emitter keeps the long variant of opcode 29`() {
        val codec = ParticleEmitterCodec()
        val raw = body {
            writeByte(29); writeByte(1); writeShort(-50); writeShort(60)
        }

        val decoded = codec.assertRoundTrips(raw)

        assertEquals(1, decoded.angularVelocityVariant)
        assertEquals(listOf(-50, 60), decoded.angularVelocity)
    }

    @Test
    fun `particle emitter keeps both variants of opcode 35`() {
        val codec = ParticleEmitterCodec()

        val short = body { writeByte(35); writeByte(0); writeShort(-7) }
        assertEquals(listOf(-7), codec.assertRoundTrips(short).rotationSpawn)

        val long = body { writeByte(35); writeByte(2); writeShort(-7); writeShort(8); writeByte(9) }
        val decoded = codec.assertRoundTrips(long)
        assertEquals(listOf(-7, 8, 9), decoded.rotationSpawn)
        assertEquals(2, decoded.rotationSpawnVariant)
    }
}
