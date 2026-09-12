package dev.openrune

import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.incremental.IncrementalSession
import dev.openrune.cache.tools.incremental.PackUnit
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.constants.MappingProvider
import dev.openrune.filesystem.Cache
import dev.openrune.filesystem.Compression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class IncrementalPackTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var cache: MemoryCache
    private lateinit var sources: File
    private lateinit var database: File

    @BeforeEach
    fun setUp() {
        cache = MemoryCache()
        sources = File(tempDir, "sources").apply { mkdirs() }
        database = File(tempDir, "state/packstate.db")
        CacheTool.gameValMappings.clear()
        ConstantProvider.load(TestMappings(mutableMapOf("item" to mutableMapOf("item.whip" to 4151))))
    }

    @Test
    fun `unchanged sources are skipped on the second build`() {
        File(sources, "a.txt").writeText("a")
        File(sources, "b.txt").writeText("b")

        assertEquals(listOf("a.txt", "b.txt"), build())
        assertEquals(emptyList<String>(), build())
    }

    @Test
    fun `editing one source repacks only that source`() {
        File(sources, "a.txt").writeText("a")
        File(sources, "b.txt").writeText("b")
        build()

        File(sources, "b.txt").writeText("b changed")

        assertEquals(listOf("b.txt"), build())
    }

    @Test
    fun `changing a gameval id repacks every source that used it`() {
        File(sources, "uses-whip.txt").writeText("item.whip")
        File(sources, "plain.txt").writeText("nothing")
        build()

        ConstantProvider.load(TestMappings(mutableMapOf("item" to mutableMapOf("item.whip" to 9999))))

        assertEquals(listOf("uses-whip.txt"), build())
    }

    @Test
    fun `removing a gameval repacks the sources that used it`() {
        File(sources, "uses-whip.txt").writeText("item.whip")
        build()

        ConstantProvider.load(TestMappings(mutableMapOf("item" to mutableMapOf())))

        assertEquals(listOf("uses-whip.txt"), build())
    }

    @Test
    fun `deleting a source removes its cache entry`() {
        File(sources, "a.txt").writeText("a")
        File(sources, "b.txt").writeText("b")
        build()
        assertEquals(2, cache.entries.size)

        File(sources, "b.txt").delete()
        build()

        assertEquals(1, cache.entries.size)
        assertTrue(cache.entries.keys.none { it.third == "b.txt".hashCode() })
    }

    @Test
    fun `gamevals of skipped sources are replayed`() {
        File(sources, "a.txt").writeText("a")
        build()
        val first = CacheTool.gameValMappings[GameValGroupTypes.OBJTYPES]?.map { it.name }

        CacheTool.gameValMappings.clear()
        assertEquals(emptyList<String>(), build())

        assertEquals(first, CacheTool.gameValMappings[GameValGroupTypes.OBJTYPES]?.map { it.name })
        assertEquals(listOf("a.txt"), first)
    }

    @Test
    fun `an output missing from the cache is repacked`() {
        File(sources, "a.txt").writeText("a")
        build()

        cache.entries.clear()

        assertEquals(listOf("a.txt"), build())
    }

    @Test
    fun `a wiped cache fingerprint forces a full repack`() {
        File(sources, "a.txt").writeText("a")
        build()

        cache.fingerprint = byteArrayOf(9, 9, 9)

        assertEquals(listOf("a.txt"), build())
    }

    @Test
    fun `a source that fails to pack is retried on the next build`() {
        File(sources, "boom.txt").writeText("boom")

        assertEquals(listOf("boom.txt"), build(failOn = "boom.txt"))

        assertEquals(listOf("boom.txt"), build())
        assertEquals(emptyList<String>(), build())
    }

    private fun build(failOn: String? = null): List<String> {
        val packed = mutableListOf<String>()
        val task = TestTask()
        val session = IncrementalSession.open(
            enabled = true,
            cacheLocation = File(tempDir, "cache"),
            databaseOverride = database,
            revision = 240,
            versionTable = cache.fingerprint,
        )

        session.use {
            task.incremental = session.build
            val units = sources.listFiles().orEmpty().sortedBy { it.name }.map { PackUnit(it.name, it) }
            session.build.run(task, sources.absolutePath, "test", cache, units) { packCache, unit ->
                packed += unit.key
                if (unit.key == failOn) error("deliberate failure")
                val text = unit.sources.single().readText()

                text.split(" ").forEach { token -> ConstantProvider.getMappingOrNull(token) }
                CacheTool.addGameValMapping(GameValGroupTypes.OBJTYPES, GameValElement(unit.key, 1))
                packCache.write(0, 0, unit.key.hashCode(), text.toByteArray())
            }
            session.finish(cache.fingerprint)
        }
        return packed
    }

    private class TestTask : CacheTask() {
        override fun init(cache: Cache) = Unit
    }

    private class TestMappings(
        override val mappings: MutableMap<String, MutableMap<String, Int>>,
    ) : MappingProvider {
        override fun load(vararg mappings: File) = Unit
        override fun getSupportedExtensions() = listOf("rscm")
    }

    private class MemoryCache : Cache {
        val entries = LinkedHashMap<Triple<Int, Int, Int>, ByteArray>()
        var fingerprint: ByteArray = byteArrayOf(1, 2, 3)

        override val versionTable: ByteArray get() = fingerprint

        override fun write(index: Int, archive: Int, file: Int, data: ByteArray, xteas: IntArray?) {
            entries[Triple(index, archive, file)] = data
        }

        override fun write(index: Int, archive: Int, data: ByteArray, xteas: IntArray?) =
            write(index, archive, 0, data, xteas)

        override fun write(index: Int, archive: String, data: ByteArray, xteas: IntArray?) =
            write(index, archive.hashCode(), 0, data, xteas)

        override fun remove(index: Int, archive: Int, file: Int) {
            entries.remove(Triple(index, archive, file))
        }

        override fun remove(index: Int, archive: Int) {
            entries.keys.removeIf { it.first == index && it.second == archive }
        }

        override fun files(index: Int, archive: Int): IntArray =
            entries.keys.filter { it.first == index && it.second == archive }.map { it.third }.toIntArray()

        override fun data(index: Int, archive: Int, file: Int, xtea: IntArray?) = entries[Triple(index, archive, file)]

        override fun fileData(index: Int, archive: Int, xtea: IntArray?): Array<ByteArray?>? =
            entries.filterKeys { it.first == index && it.second == archive }.values.toTypedArray()

        override fun indexCount() = 1
        override fun exists(id: Int) = id == 0
        override fun indices() = intArrayOf(0)
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
