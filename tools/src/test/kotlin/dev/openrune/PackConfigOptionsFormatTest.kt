package dev.openrune

import cc.ekblad.toml.decode
import cc.ekblad.toml.model.TomlValue
import cc.ekblad.toml.serialization.from
import cc.ekblad.toml.util.InternalAPI
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.definition.codec.ItemCodec
import dev.openrune.definition.type.ItemType
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class PackConfigOptionsFormatTest {

    @OptIn(InternalAPI::class)
    private fun decodeSingleItem(toml: String): ItemType {
        // Register pack types/mappers via init block
        PackConfig(File("."))
        val packType = PackConfig.packTypes["item"] ?: error("item pack type not registered")
        val document = TomlValue.from(toml)
        val itemTable = document.properties["item"] ?: error("Missing [[item]] table")
        val decoded: List<ItemType> = packType.tomlMapper.decode(packType.kType, itemTable)
        return decoded.first()
    }

    private fun roundTrip(codec: ItemCodec, def: ItemType): ItemType {
        val writer = Unpooled.buffer(2048)
        with(codec) { writer.encode(def) }
        val bytes = ByteArray(writer.readableBytes())
        writer.getBytes(0, bytes)
        return codec.loadData(def.id, bytes)
    }

    @Test
    fun `revision 236 old option format uses option1`() {
        val toml = """
            [[item]]
            id = 1337
            name = "Test item"
            option1 = "use"
        """.trimIndent()

        val decodedFromToml = decodeSingleItem(toml)
        val pre237 = roundTrip(ItemCodec(236), decodedFromToml)
        println("revision=236 options=${pre237.options}")
        assertEquals("use", pre237.options.ops.getOrNull(0)?.text)
    }

    @Test
    fun `revision 237 round trip preserves conditional option structures`() {
        val toml = """
            [[item]]
            id = 4242
            name = "Extended options item"
            option1 = "Use"
            multiop1 = { text = "Activate", varp = 100, varbit = 200, min = 1, max = 9 }
            subop2 = [
              { index = 3, text = "Use (sub)" }
            ]
            multisubop4 = [
              { index = 3, text = "Activate (sub)", varp = 300, varbit = 400, min = 2, max = 8 }
            ]
        """.trimIndent()

        val decodedFromToml = decodeSingleItem(toml)
        val post237 = roundTrip(ItemCodec(237), decodedFromToml)
        println("revision=237 options=${post237.options}")

        // old option still works in 237
        assertEquals("Use", post237.options.ops.getOrNull(0)?.text)

        // conditional ops
        val conditional = post237.options.conditionalOps.getOrNull(0)?.firstOrNull()
        assertEquals("Activate", conditional?.text)
        assertEquals(100, conditional?.varpID)
        assertEquals(200, conditional?.varbitID)
        assertEquals(1, conditional?.minValue)
        assertEquals(9, conditional?.maxValue)

        // sub ops
        val sub = post237.options.subOps.getOrNull(1)?.firstOrNull()
        assertEquals("Use (sub)", sub?.text)
        assertEquals(2, sub?.subID)

        // conditional sub ops
        val conditionalSub = post237.options.conditionalSubOps
            .getOrNull(3)
            ?.get(2)
            ?.firstOrNull()
        assertEquals("Activate (sub)", conditionalSub?.text)
        assertEquals(2, conditionalSub?.subID)
        assertEquals(300, conditionalSub?.varpID)
        assertEquals(400, conditionalSub?.varbitID)
        assertEquals(2, conditionalSub?.minValue)
        assertEquals(8, conditionalSub?.maxValue)
    }

    @Test
    fun `revision 237 supports multisubop slot inferred format`() {
        val toml = """
            [[item]]
            id = 5252
            name = "Compact multisubop item"
            multisubop1 = [
              { index = 3, text = "Activate (sub)", varp = 300, varbit = 400, min = 2, max = 8 },
              { index = 1, text = "Use (sub)", varp = 111, varbit = 222, min = 0, max = 5 }
            ]
        """.trimIndent()

        val decodedFromToml = decodeSingleItem(toml)
        val post237 = roundTrip(ItemCodec(237), decodedFromToml)
        println("revision=237 multisub options=${post237.options}")

        // slot inferred from multisubop1 => slot index 0
        val first = post237.options.conditionalSubOps
            .getOrNull(0)
            ?.get(2)
            ?.firstOrNull()
        assertEquals("Activate (sub)", first?.text)
        assertEquals(2, first?.subID)
        assertEquals(300, first?.varpID)
        assertEquals(400, first?.varbitID)
        assertEquals(2, first?.minValue)
        assertEquals(8, first?.maxValue)
    }
}

