package dev.openrune

import dev.openrune.cache.CONFIGS
import dev.openrune.cache.GAMEVALS
import dev.openrune.cache.ITEM
import dev.openrune.cache.PARAMS
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.incremental.IncrementalSession
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.codec.ItemCodec
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.constants.MappingProvider
import dev.openrune.filesystem.Cache
import dev.openrune.filesystem.Compression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PackConfigBlockGranularityTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var cache: MemoryCache
    private lateinit var configs: File
    private lateinit var database: File

    private val ids = mutableMapOf(
        "obj.chinchompa_captured" to 10033,
        "obj.chinchompa_big_captured" to 11959,
        "param.charged_variant" to 2400,
        "param.attack_range" to 2401,
    )

    @BeforeEach
    fun setUp() {
        cache = MemoryCache()
        configs = File(tempDir, "configs").apply { mkdirs() }
        database = File(tempDir, "state/packstate.db")
        CacheTool.gameValMappings.clear()
        loadConstants()

        seedItem(10033, "Chinchompa")
        seedItem(11959, "Red chinchompa")
    }

    private fun loadConstants() {
        val tables = mutableMapOf<String, MutableMap<String, Int>>()
        ids.forEach { (key, value) ->
            tables.getOrPut(key.substringBefore('.')) { mutableMapOf() }[key] = value
        }
        ConstantProvider.load(TestMappings(tables))
    }

    private fun seedItem(id: Int, name: String) {
        val codec = ItemCodec(240)
        val definition = codec.createDefinition().apply {
            this.id = id
            this.name = name
        }
        val buffer = io.netty.buffer.Unpooled.buffer(256)
        with(codec) { buffer.encode(definition) }
        val bytes = ByteArray(buffer.readableBytes())
        buffer.getBytes(0, bytes)
        cache.entries[Triple(CONFIGS, ITEM, id)] = bytes
        cache.writes = 0
    }

    private fun writeChinchompas(smallRange: Int = 9, bigRange: Int = 9, paramMembers: Boolean = true) {
        File(configs, "chinchompas.toml").writeText(
            """
            [[item]]
            id = "obj.chinchompa_captured"
            inherit = "obj.chinchompa_captured"
            weaponCategory="Chinchompas"
            [item.params]
            "param.attack_range"=$smallRange

            [[item]]
            id = "obj.chinchompa_big_captured"
            inherit = "obj.chinchompa_big_captured"
            weaponCategory="Chinchompas"
            [item.params]
            "param.attack_range"=$bigRange

            [[params]]
            id = "param.charged_variant"
            type = "NAMEDOBJ"
            isMembers = $paramMembers
            """.trimIndent()
        )
    }

    @Test
    fun `editing one block in a file repacks only that block`() {
        writeChinchompas()
        assertEquals(3, build(), "first build packs all three definitions")
        assertEquals(0, build())

        writeChinchompas(bigRange = 12)

        assertEquals(1, build(), "only the edited block may repack")
    }

    @Test
    fun `editing a param block does not repack the items beside it`() {
        writeChinchompas()
        build()
        assertEquals(0, build())

        writeChinchompas(paramMembers = false)

        assertEquals(1, build())
        assertNotNull(cache.entries[Triple(CONFIGS, PARAMS, ids.getValue("param.charged_variant"))])
    }

    @Test
    fun `adding a block to a file packs only the new block`() {
        writeChinchompas()
        build()
        assertEquals(0, build())

        File(configs, "chinchompas.toml").appendText(
            """

            [[item]]
            id = 40001
            name = "Extra"
            """.trimIndent()
        )

        assertEquals(1, build(), "only the added block may pack")
    }

    @Test
    fun `removing a block from a file removes its cache entry and leaves the rest alone`() {
        writeChinchompas()
        build()
        assertEquals(0, build())

        File(configs, "chinchompas.toml").writeText(
            File(configs, "chinchompas.toml").readText().substringBefore("[[params]]").trimEnd()
        )

        assertEquals(0, build(), "removing a block must not repack the surviving blocks")
        assertNull(
            cache.entries[Triple(CONFIGS, PARAMS, ids.getValue("param.charged_variant"))],
            "the removed block's cache entry should have been pruned",
        )
        assertNotNull(cache.entries[Triple(CONFIGS, ITEM, ids.getValue("obj.chinchompa_captured"))])
    }

    @Test
    fun `moving a definition to another file repacks nothing`() {
        writeChinchompas()
        build()
        assertEquals(0, build())

        val original = File(configs, "chinchompas.toml").readText()
        val paramsBlock = "[[params]]" + original.substringAfter("[[params]]")
        File(configs, "chinchompas.toml").writeText(original.substringBefore("[[params]]").trimEnd())
        File(configs, "params.toml").writeText(paramsBlock)

        assertEquals(0, build(), "a definition that only moved file is unchanged and must not repack")
        assertNotNull(
            cache.entries[Triple(CONFIGS, PARAMS, ids.getValue("param.charged_variant"))],
            "the moved definition must survive in the cache",
        )
    }

    @Test
    fun `moving a definition and editing it repacks only that definition`() {
        writeChinchompas()
        build()
        assertEquals(0, build())

        val original = File(configs, "chinchompas.toml").readText()
        File(configs, "chinchompas.toml").writeText(original.substringBefore("[[params]]").trimEnd())
        File(configs, "params.toml").writeText(
            """
            [[params]]
            id = "param.charged_variant"
            type = "NAMEDOBJ"
            isMembers = false
            """.trimIndent()
        )

        assertEquals(1, build())
    }

    @Test
    fun `renumbering an id removes the gameval entry at the old id`() {
        writeChinchompas()
        build()

        val oldId = ids.getValue("obj.chinchompa_big_captured")
        val gameValGroup = GameValGroupTypes.OBJTYPES.id
        assertNotNull(
            cache.entries[Triple(GAMEVALS, gameValGroup, oldId)],
            "the first build should have registered a gameval at the original id",
        )

        ids["obj.chinchompa_big_captured"] = 12000
        loadConstants()
        seedItem(12000, "Red chinchompa")
        build()

        assertNotNull(cache.entries[Triple(GAMEVALS, gameValGroup, 12000)], "gameval missing at the new id")
        assertNull(
            cache.entries[Triple(GAMEVALS, gameValGroup, oldId)],
            "the gameval at the abandoned id must not survive, or the name reports at two ids and the " +
                "CS2 compile fails on a duplicate symbol",
        )
    }

    @Test
    fun `renumbering a gameval used only inside a block body repacks that block`() {
        writeChinchompas()
        build()
        assertEquals(0, build())

        // Not the id and not an inherit target: a param key buried in the block's body. The file is
        // byte-identical, so only the resolved value can reveal the change.
        ids["param.attack_range"] = 2500
        loadConstants()

        assertEquals(2, build(), "both items name this param, and only those two")
    }

    @Test
    fun `renumbering a gameval repacks only the block that used it`() {
        writeChinchompas()
        build()
        assertEquals(0, build())

        ids["obj.chinchompa_big_captured"] = 12000
        loadConstants()
        seedItem(12000, "Red chinchompa")

        assertEquals(1, build(), "only the block whose id gameval moved may repack")
        assertNotNull(cache.entries[Triple(CONFIGS, ITEM, 12000)])
    }

    private fun build(): Int {
        val task = PackConfig(configs)
        task.revision = 240

        // BuildCache clears this between builds; the registrations would otherwise accumulate and a
        // renumbered definition would keep reporting at both ids.
        CacheTool.gameValMappings.clear()

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
            flushGameVals()
            session.finish(cache.versionTable)
        }
        return cache.writes - before
    }

    /**
     * Stands in for PackGameVals, which writes the registered gamevals at the end of a real build. Writes
     * straight into the backing map so it does not disturb the pack counter the assertions read.
     */
    private fun flushGameVals() {
        CacheTool.gameValMappings.forEach { (group, elements) ->
            elements.forEach { element ->
                cache.entries[Triple(GAMEVALS, group.id, element.id)] = element.name.toByteArray()
            }
        }
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
