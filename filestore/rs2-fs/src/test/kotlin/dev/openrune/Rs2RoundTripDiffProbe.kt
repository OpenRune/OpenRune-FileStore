package dev.openrune

import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import dev.openrune.rs2.Rs2BasDecoder
import dev.openrune.rs2.Rs2ConfigDecoder
import dev.openrune.rs2.Rs2ConfigLocation
import dev.openrune.rs2.Rs2DbRowDecoder
import dev.openrune.rs2.Rs2NpcDecoder
import dev.openrune.rs2.Rs2SequenceDecoder
import dev.openrune.rs2.Rs2SpotAnimDecoder
import dev.openrune.rs2.Rs2WaterDecoder
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

@EnabledIfSystemProperty(named = "diffprobe", matches = "true")
class Rs2RoundTripDiffProbe {

    @Test
    fun `print the first difference for each case`() {
        Rs2Cache.loadRemote(id = 2767).use { cache ->
            val build = cache.build ?: Int.MAX_VALUE
            for ((name, decoder, location, ids) in CASES) {
                for (id in ids) {
                    val raw = cache.configBytes(location, id)
                    if (raw == null) {
                        println("$name $id: no bytes")
                        continue
                    }
                    val reencoded = try {
                        @Suppress("UNCHECKED_CAST")
                        val typed = decoder as Rs2ConfigDecoder<Definition>
                        val decoded = typed.read(cache, id)

                        @Suppress("UNCHECKED_CAST")
                        val codec = typed.javaClass
                            .getDeclaredMethod("codecFor", Int::class.javaPrimitiveType)
                            .apply { isAccessible = true }
                            .invoke(typed, build) as DefinitionCodec<Definition>

                        val out = Unpooled.buffer()
                        try {
                            with(codec) { out.encode(decoded) }
                            ByteArray(out.readableBytes()).also { out.readBytes(it) }
                        } finally {
                            out.release()
                        }
                    } catch (e: Throwable) {
                        println("$name $id: threw ${e.message}")
                        e.cause?.let { println("    caused by $it") }
                        println("    cache: ${raw.toHex(0)}")
                        continue
                    }

                    val at = (0 until minOf(raw.size, reencoded.size)).firstOrNull { raw[it] != reencoded[it] }
                        ?: if (raw.size == reencoded.size) null else minOf(raw.size, reencoded.size)
                    if (at == null) {
                        println("$name $id: identical (${raw.size} bytes)")
                        continue
                    }
                    println("$name $id: differs at byte $at (cache ${raw.size} bytes, re-encode ${reencoded.size})")
                    println("    cache:     ${raw.toHex(at)}")
                    println("    re-encode: ${reencoded.toHex(at)}")
                }
            }
        }
    }

    private data class Case(
        val name: String,
        val decoder: Rs2ConfigDecoder<*>,
        val location: Rs2ConfigLocation,
        val ids: List<Int>,
    )

    private companion object {
        val CASES = listOf(
            Case("npc", Rs2NpcDecoder(), Rs2ConfigLocation.NPC, listOf(5276)),
            Case("seq", Rs2SequenceDecoder(), Rs2ConfigLocation.SEQ, listOf(12256, 15320, 33749)),
            Case("spot", Rs2SpotAnimDecoder(), Rs2ConfigLocation.SPOT, listOf(4941, 6078, 9110)),
            Case("dbrow", Rs2DbRowDecoder(), Rs2ConfigLocation.DBROW, listOf(16460, 19752)),
            Case("bas", Rs2BasDecoder(), Rs2ConfigLocation.BAS, listOf(394, 788, 1182)),
            Case("water", Rs2WaterDecoder(), Rs2ConfigLocation.WATER, listOf(9, 16)),
        )

        fun ByteArray.toHex(at: Int): String {
            val from = maxOf(0, at - 16)
            val to = minOf(size, at + 24)
            return (from until to).joinToString(" ") { i ->
                "%02x".format(this[i]) + if (i == at) "<" else ""
            } + "   [$from..${to - 1} of $size]"
        }
    }
}
