package dev.openrune

import dev.openrune.cache.CONFIGS
import dev.openrune.cache.ITEM
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.incremental.IncrementalSession
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.definition.codec.ItemCodec
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.constants.MappingProvider
import dev.openrune.filesystem.Cache
import dev.openrune.filesystem.Compression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PackConfigIncrementalTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var cache: MemoryCache
    private lateinit var configs: File
    private lateinit var database: File

    private var coinsId = 995
    private var restrictedParamId = 42

    @BeforeEach
    fun setUp() {
        cache = MemoryCache()
        configs = File(tempDir, "configs").apply { mkdirs() }
        database = File(tempDir, "state/packstate.db")
        CacheTool.gameValMappings.clear()
        loadConstants()
    }

    private fun loadConstants() {
        ConstantProvider.load(
            TestMappings(
                mutableMapOf(
                    "obj" to mutableMapOf("obj.coins" to coinsId),
                    "param" to mutableMapOf("param.shop_sale_restricted" to restrictedParamId),
                )
            )
        )
    }

    private fun writeCoins() {
        File(configs, "coins.toml").writeText(
            """
            [[item]]
            id = "obj.coins"
            name = "Coins"

            [item.params]
            "param.shop_sale_restricted" = 1
            """.trimIndent()
        )
    }

    @Test
    fun `config using a gameval id is packed then skipped while nothing changes`() {
        writeCoins()

        assertEquals(1, build(), "first build must pack the config")
        assertNotNull(cache.entries[Triple(CONFIGS, ITEM, coinsId)], "config was not written at its gameval id")
        assertEquals(0, build(), "unchanged config must be skipped")
    }

    @Test
    fun `renumbering the gameval used as the id repacks the config`() {
        writeCoins()
        build()
        assertEquals(0, build())

        coinsId = 6000
        loadConstants()

        assertEquals(1, build(), "config must repack when its id gameval is renumbered")
        assertNotNull(cache.entries[Triple(CONFIGS, ITEM, 6000)], "config was not rewritten at the new id")
    }

    @Test
    fun `renumbering a gameval used only as a param key repacks the config`() {
        writeCoins()
        build()
        assertEquals(0, build())

        restrictedParamId = 77
        loadConstants()

        assertEquals(1, build(), "config must repack when a param-key gameval is renumbered")
    }

    @Test
    fun `a config that uses no changed gameval is left alone`() {
        writeCoins()
        File(configs, "other.toml").writeText(
            """
            [[item]]
            id = 30001
            name = "Unrelated"
            """.trimIndent()
        )
        build()
        assertEquals(0, build())

        coinsId = 6001
        loadConstants()

        assertEquals(1, build(), "only the config referencing the renumbered gameval may repack")
    }

    @Test
    fun `renumbering the gameval used as an inherit target repacks the child`() {
        File(configs, "a-base.toml").writeText(
            """
            [[item]]
            id = "obj.coins"
            name = "Coins"
            """.trimIndent()
        )
        File(configs, "b-child.toml").writeText(
            """
            [[item]]
            id = 30010
            inherit = "obj.coins"
            """.trimIndent()
        )
        build()
        assertEquals(0, build())

        coinsId = 6002
        loadConstants()

        assertEquals(2, build())
    }

    @Test
    fun `editing an inherited parent repacks the child that inherits it`() {
        writeInheritPair(parentName = "Coins")
        build()
        assertEquals(0, build())

        writeInheritPair(parentName = "Coins Renamed")

        assertEquals(2, build())
        assertEquals("Coins Renamed", packedItemName(30010), "child merged against a stale parent")
    }

    @Test
    fun `a child is packed after its parent regardless of file order`() {
        writeInheritPair(parentName = "Coins")

        assertEquals(2, build())
        assertEquals("Coins", packedItemName(30010), "child did not inherit its parent's name")
    }

    private fun writeInheritPair(parentName: String) {
        File(configs, "a-child.toml").writeText(
            """
            [[item]]
            id = 30010
            inherit = "obj.coins"
            """.trimIndent()
        )
        File(configs, "z-parent.toml").writeText(
            """
            [[item]]
            id = "obj.coins"
            name = "$parentName"
            """.trimIndent()
        )
    }

    private fun packedItemName(id: Int): String? {
        val data = cache.entries[Triple(CONFIGS, ITEM, id)] ?: return null
        return ItemCodec(240).loadData(id, data).name
    }

    @Test
    fun `changing a local tokenizedReplacement repacks the config`() {
        val file = File(configs, "tokens.toml")
        file.writeText(
            """
            [[tokenizedReplacement]]
            ITEMNAME="Coins"

            [[item]]
            id = 30020
            name = "%ITEMNAME%"
            """.trimIndent()
        )
        build()
        assertEquals(0, build())

        file.writeText(
            """
            [[tokenizedReplacement]]
            ITEMNAME="Gold"

            [[item]]
            id = 30020
            name = "%ITEMNAME%"
            """.trimIndent()
        )

        assertEquals(1, build())
    }

    @Test
    fun `changing a global tokenizedReplacement repacks configs that use it`() {
        File(configs, "global-token.toml").writeText(
            """
            [[item]]
            id = 30030
            name = "%global.itemname%"
            """.trimIndent()
        )

        assertEquals(1, build(tokens = mapOf("global.itemname" to "Coins")))
        assertEquals(0, build(tokens = mapOf("global.itemname" to "Coins")))

        assertEquals(1, build(tokens = mapOf("global.itemname" to "Gold")))
    }

    private fun build(tokens: Map<String, String> = emptyMap()): Int {
        val task = PackConfig(configs, tokenizedReplacements = tokens)
        task.revision = 240

        val before = cache.writes
        val session = IncrementalSession.open(
            enabled = true,
            cacheLocation = File(tempDir, "cache"),
            databaseOverride = database,
            revision = 240,
            versionTable = cache.versionTable,
        )
        session.use {
            task.incremental = session.build
            task.init(cache)
            session.finish(cache.versionTable)
        }
        return cache.writes - before
    }

    private class TestMappings(
        override val mappings: MutableMap<String, MutableMap<String, Int>>,
    ) : MappingProvider {
        override fun load(vararg mappings: File) = Unit
        override fun getSupportedExtensions() = listOf("rscm")
    }

    private class MemoryCache : Cache {
        val entries = LinkedHashMap<Triple<Int, Int, Int>, ByteArray>()
        var writes = 0

        override val versionTable: ByteArray get() = byteArrayOf(1, 2, 3)

        override fun write(index: Int, archive: Int, file: Int, data: ByteArray, xteas: IntArray?) {
            entries[Triple(index, archive, file)] = data
            writes++
        }

        override fun write(index: Int, archive: Int, data: ByteArray, xteas: IntArray?) =
            write(index, archive, 0, data, xteas)

        override fun write(index: Int, archive: String, data: ByteArray, xteas: IntArray?) =
            write(index, archive.hashCode(), 0, data, xteas)

        override fun remove(index: Int, archive: Int, file: Int) {
            entries.remove(Triple(index, archive, file))
        }

        override fun files(index: Int, archive: Int): IntArray =
            entries.keys.filter { it.first == index && it.second == archive }.map { it.third }.toIntArray()

        override fun data(index: Int, archive: Int, file: Int, xtea: IntArray?) = entries[Triple(index, archive, file)]

        override fun fileData(index: Int, archive: Int, xtea: IntArray?): Array<ByteArray?>? =
            entries.filterKeys { it.first == index && it.second == archive }.values.toTypedArray()

        override fun indexCount() = 1
        override fun exists(id: Int) = true
        override fun indices() = intArrayOf(CONFIGS)
        override fun sector(index: Int, archive: Int): ByteArray? = null
        override fun archives(index: Int) = entries.keys.filter { it.first == index }.map { it.second }.distinct().toIntArray()
        override fun archiveCount(index: Int) = archives(index).size
        override fun lastArchiveId(indexId: Int) = archives(indexId).maxOrNull() ?: -1
        override fun archiveId(index: Int, hash: Int) = hash
        override fun fileCount(indexId: Int, archiveId: Int) = files(indexId, archiveId).size
        override fun lastFileId(indexId: Int, archive: Int) = files(indexId, archive).maxOrNull() ?: -1
        override fun crc(index: Int) = 0
        override fun crc(index: Int, archive: Int) = 0
        override fun update() = true
        override fun close() = Unit
        override fun createIndex(
            compressionType: Compression,
            version: Int,
            revision: Int,
            named: Boolean,
            whirlpool: Boolean,
            lengths: Boolean,
            checksums: Boolean,
            writeReferenceTable: Boolean,
            id: Int,
        ) = Unit
    }
}
