package dev.openrune

import dev.openrune.cache.CONFIGS
import dev.openrune.cache.DBROW
import dev.openrune.cache.ENUM
import dev.openrune.cache.ITEM
import dev.openrune.cache.NPC
import dev.openrune.cache.OBJECT
import dev.openrune.cache.SEQUENCE
import dev.openrune.cache.STRUCT
import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.codec.DBRowCodec
import dev.openrune.definition.codec.EnumCodec
import dev.openrune.definition.codec.ItemCodec
import dev.openrune.definition.codec.NPCCodec
import dev.openrune.definition.codec.ObjectCodec
import dev.openrune.definition.codec.SequenceCodec
import dev.openrune.definition.codec.StructCodec
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Byte round trips for the decode path over every definition in a real cache.
 *
 * Decoded values now flow through the shared string and boxed-int pools, so this pins the
 * property that pooling only shares instances and never changes values: a definition encoded
 * from a decode must re-decode and re-encode to exactly the same bytes. The first encode
 * canonicalises opcode order, so the comparison is encode(decode(encode(x))) == encode(x).
 */
class CodecByteRoundTripTest {

    private val revision = 240

    private fun <T : Definition> roundTrip(codec: DefinitionCodec<T>, index: Int, archive: Int) {
        val cache = Cache.load(File("../data/cache").toPath())
        var checked = 0
        try {
            for (id in cache.files(index, archive)) {
                val raw = cache.data(index, archive, id) ?: continue

                val once = encode(codec, codec.loadData(id, raw))
                val reDecoded = try {
                    codec.loadData(id, once)
                } catch (e: Exception) {
                    throw AssertionError("re-decode of definition $id in archive $archive failed", e)
                }
                val twice = encode(codec, reDecoded)

                assertArrayEquals(once, twice, "definition $id in archive $archive")
                checked++
            }
        } finally {
            cache.close()
        }
        assertTrue(checked > 100) { "only checked $checked definitions" }
    }

    private fun <T : Definition> encode(codec: DefinitionCodec<T>, definition: T): ByteArray {
        val buffer = Unpooled.buffer(512)
        try {
            with(codec) { buffer.encode(definition) }
            return ByteArray(buffer.readableBytes()).also { buffer.readBytes(it) }
        } finally {
            buffer.release()
        }
    }

    @Test
    fun `objects survive a byte round trip`() = roundTrip(ObjectCodec(revision), CONFIGS, OBJECT)

    @Test
    fun `npcs survive a byte round trip`() = roundTrip(NPCCodec(revision), CONFIGS, NPC)

    @Test
    fun `items survive a byte round trip`() = roundTrip(ItemCodec(revision), CONFIGS, ITEM)

    @Test
    fun `anims survive a byte round trip`() = roundTrip(SequenceCodec(revision), CONFIGS, SEQUENCE)

    @Test
    fun `enums survive a byte round trip`() = roundTrip(EnumCodec(), CONFIGS, ENUM)

    @Test
    fun `structs survive a byte round trip`() = roundTrip(StructCodec(), CONFIGS, STRUCT)

    @Test
    fun `dbrows survive a byte round trip`() = roundTrip(DBRowCodec(), CONFIGS, DBROW)
}
