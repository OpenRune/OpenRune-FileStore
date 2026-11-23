package dev.openrune

import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import dev.openrune.rs2.Rs2BasDecoder
import dev.openrune.rs2.Rs2BillboardDecoder
import dev.openrune.rs2.Rs2ConfigDecoder
import dev.openrune.rs2.Rs2ConfigLocation
import dev.openrune.rs2.Rs2DbRowDecoder
import dev.openrune.rs2.Rs2DbTableDecoder
import dev.openrune.rs2.Rs2EnumDecoder
import dev.openrune.rs2.Rs2FloorOverlayDecoder
import dev.openrune.rs2.Rs2FloorUnderlayDecoder
import dev.openrune.rs2.Rs2HeadbarDecoder
import dev.openrune.rs2.Rs2HitmarkDecoder
import dev.openrune.rs2.Rs2IdentityKitDecoder
import dev.openrune.rs2.Rs2LightDecoder
import dev.openrune.rs2.Rs2NpcDecoder
import dev.openrune.rs2.Rs2ObjDecoder
import dev.openrune.rs2.Rs2ParamDecoder
import dev.openrune.rs2.Rs2ParticleEffectorDecoder
import dev.openrune.rs2.Rs2ParticleEmitterDecoder
import dev.openrune.rs2.Rs2SequenceDecoder
import dev.openrune.rs2.Rs2SpotAnimDecoder
import dev.openrune.rs2.Rs2StructDecoder
import dev.openrune.rs2.Rs2VarBitDecoder
import dev.openrune.rs2.Rs2VarClientDecoder
import dev.openrune.rs2.Rs2WaterDecoder
import dev.openrune.rs2.Rs2WorldAreaDecoder
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

@EnabledIfSystemProperty(named = "livecache", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Rs2LiveCacheCodecTest {

    private lateinit var cache: Rs2Cache

    @BeforeAll
    fun open() {
        cache = Rs2Cache.loadRemote(id = DUMP_CACHE_ID)
        println("Opened RS3 cache $DUMP_CACHE_ID (build ${cache.build})")
    }

    @AfterAll
    fun close() {
        if (::cache.isInitialized) cache.close()
    }

    private fun <T : Definition> roundTrip(
        decoder: Rs2ConfigDecoder<T>,
        location: Rs2ConfigLocation,
        id: Int,
    ): T {
        val raw = cache.configBytes(location, id)
        val decoded = decoder.read(cache, id)

        @Suppress("UNCHECKED_CAST")
        val codec = decoder.javaClass
            .getDeclaredMethod("codecFor", Int::class.javaPrimitiveType)
            .apply { isAccessible = true }
            .invoke(decoder, cache.build ?: Int.MAX_VALUE) as DefinitionCodec<T>

        val out = Unpooled.buffer()
        try {
            with(codec) { out.encode(decoded) }
            val reencoded = ByteArray(out.readableBytes()).also { out.readBytes(it) }
            assertArrayEquals(raw, reencoded) {
                "${decoder.javaClass.simpleName} id $id re-encoded differently" +
                    "\n  cache:     ${raw.toHex()}" +
                    "\n  re-encode: ${reencoded.toHex()}"
            }
        } finally {
            out.release()
        }
        return decoded
    }

    @Test
    fun `floor underlay 0 matches the dump`() {
        val decoded = roundTrip(Rs2FloorUnderlayDecoder(), Rs2ConfigLocation.FLOOR_UNDERLAY, 0)

        assertEquals(0x282820, decoded.colour)
        assertEquals(256, decoded.textureScale)
        assertTrue(decoded.material != null && decoded.material != -1) { "expected a material id" }
    }

    @Test
    fun `billboard 0 matches the dump`() {
        val decoded = roundTrip(Rs2BillboardDecoder(), Rs2ConfigLocation.BILLBOARD, 0)

        assertEquals(50, decoded.width)
        assertEquals(50, decoded.height)
        assertEquals(0, decoded.unknown3)
        assertEquals(1, decoded.sourceBlendMode)
        assertEquals(0, decoded.destinationBlendMode)
        assertEquals(true, decoded.unknown7)
        assertTrue(decoded.material != null) { "expected a material id" }
    }

    @Test
    fun `light 0 matches the dump`() {
        val decoded = roundTrip(Rs2LightDecoder(), Rs2ConfigLocation.LIGHT, 0)

        assertEquals(724, decoded.amplitude)
        assertEquals(2048, decoded.frequency)
        assertEquals(205, decoded.offset)
        assertEquals(3, decoded.function)
    }

    @Test
    fun `every config type round trips byte for byte over a sample`() {
        val cases = listOf<Triple<String, Rs2ConfigDecoder<*>, Rs2ConfigLocation>>(
            Triple("npc", Rs2NpcDecoder(), Rs2ConfigLocation.NPC),
            Triple("obj", Rs2ObjDecoder(), Rs2ConfigLocation.OBJ),
            Triple("seq", Rs2SequenceDecoder(), Rs2ConfigLocation.SEQ),
            Triple("spot", Rs2SpotAnimDecoder(), Rs2ConfigLocation.SPOT),
            Triple("idk", Rs2IdentityKitDecoder(), Rs2ConfigLocation.IDK),
            Triple("enum", Rs2EnumDecoder(), Rs2ConfigLocation.ENUM),
            Triple("struct", Rs2StructDecoder(), Rs2ConfigLocation.STRUCT),
            Triple("param", Rs2ParamDecoder(), Rs2ConfigLocation.PARAM),
            Triple("varc", Rs2VarClientDecoder(), Rs2ConfigLocation.VAR_CLIENT),
            Triple("varbit", Rs2VarBitDecoder(), Rs2ConfigLocation.VAR_BIT),
            Triple("dbrow", Rs2DbRowDecoder(), Rs2ConfigLocation.DBROW),
            Triple("dbtable", Rs2DbTableDecoder(), Rs2ConfigLocation.DBTABLE),
            Triple("flo", Rs2FloorOverlayDecoder(), Rs2ConfigLocation.FLOOR_OVERLAY),
            Triple("flu", Rs2FloorUnderlayDecoder(), Rs2ConfigLocation.FLOOR_UNDERLAY),
            Triple("headbar", Rs2HeadbarDecoder(), Rs2ConfigLocation.HEADBAR),
            Triple("hitmark", Rs2HitmarkDecoder(), Rs2ConfigLocation.HITMARK),
            Triple("light", Rs2LightDecoder(), Rs2ConfigLocation.LIGHT),
            Triple("bas", Rs2BasDecoder(), Rs2ConfigLocation.BAS),
            Triple("water", Rs2WaterDecoder(), Rs2ConfigLocation.WATER),
            Triple("worldarea", Rs2WorldAreaDecoder(), Rs2ConfigLocation.WORLD_AREA),
            Triple("particleemitter", Rs2ParticleEmitterDecoder(), Rs2ConfigLocation.PARTICLE_EMITTER),
            Triple("particleeffector", Rs2ParticleEffectorDecoder(), Rs2ConfigLocation.PARTICLE_EFFECTOR),
            Triple("billboard", Rs2BillboardDecoder(), Rs2ConfigLocation.BILLBOARD),
        )

        val failures = mutableListOf<String>()
        var checked = 0

        for ((name, decoder, location) in cases) {
            val ids = decoder.ids(cache)
            if (ids.isEmpty()) {
                println("$name: no ids in this cache, skipped")
                continue
            }
            val sample = ids.filterIndexed { index, _ -> index % maxOf(1, ids.size / SAMPLE_PER_TYPE) == 0 }
                .take(SAMPLE_PER_TYPE)

            var ok = 0
            for (id in sample) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    roundTrip(decoder as Rs2ConfigDecoder<Definition>, location, id)
                    ok++
                    checked++
                } catch (e: Throwable) {
                    failures += "$name id $id: ${e.message?.lineSequence()?.first()}"
                }
            }
            println("$name: $ok/${sample.size} of ${ids.size} ids round tripped")
        }

        assertTrue(checked > 0) { "nothing was checked - is the cache reachable?" }
        assertTrue(failures.isEmpty()) {
            "${failures.size} round trip failures:\n" + failures.take(25).joinToString("\n")
        }
    }

    private companion object {
        const val DUMP_CACHE_ID = 2767
        const val SAMPLE_PER_TYPE = 25

        fun ByteArray.toHex() = take(64).joinToString(" ") { "%02x".format(it) } +
            if (size > 64) " ... (${size} bytes)" else ""
    }
}
