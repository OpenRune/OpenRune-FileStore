package dev.openrune

import dev.openrune.cache.CLIENTSCRIPT
import dev.openrune.cache.tools.incremental.CacheVerification
import dev.openrune.cache.tools.incremental.IncrementalSession
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.filesystem.Cache
import dev.openrune.filesystem.Compression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CoarseIncrementalTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var cache: MemoryCache
    private lateinit var sources: File
    private lateinit var database: File

    @BeforeEach
    fun setUp() {
        cache = MemoryCache()
        sources = File(tempDir, "cs2/sources").apply { mkdirs() }
        database = File(tempDir, "state/packstate.db")
    }

    private fun script(name: String, body: String) = File(sources, "$name.cs2").writeText(body)

    @Test
    fun `an untouched project does not compile`() {
        script("a", "one")
        script("b", "two")

        assertEquals(1, build())
        assertEquals(0, build())
        assertEquals(0, build())
    }

    @Test
    fun `editing any file recompiles the whole project`() {
        script("a", "one")
        script("b", "two")
        build()
        assertEquals(0, build())

        script("b", "two changed")

        assertEquals(1, build())
    }

    @Test
    fun `adding a file recompiles the whole project`() {
        script("a", "one")
        build()
        assertEquals(0, build())

        script("c", "three")

        assertEquals(1, build())
        assertNotNull(cache.entries[Triple(CLIENTSCRIPT, "c".hashCode(), 0)])
    }

    @Test
    fun `removing a file recompiles and drops its cache entry`() {
        script("a", "one")
        script("b", "two")
        build()
        assertEquals(0, build())
        assertNotNull(cache.entries[Triple(CLIENTSCRIPT, "b".hashCode(), 0)])

        File(sources, "b.cs2").delete()

        assertEquals(1, build())
        assertNull(
            cache.entries[Triple(CLIENTSCRIPT, "b".hashCode(), 0)],
            "a script removed from the project must not linger in the cache",
        )
        assertNotNull(cache.entries[Triple(CLIENTSCRIPT, "a".hashCode(), 0)])
    }

    @Test
    fun `a wiped output forces a recompile`() {
        script("a", "one")
        build()
        assertEquals(0, build())

        cache.entries.clear()

        assertEquals(1, build())
    }

    @Test
    fun `default state location is a directory inside the cache`() {
        val cacheDir = File(tempDir, "data/cache")
        val database = IncrementalSession.defaultDatabase(cacheDir)

        assertEquals(cacheDir.absoluteFile, database.parentFile.parentFile)
        assertEquals(IncrementalSession.DIRECTORY_NAME, database.parentFile.name)
        assertEquals(
            database.parentFile.absoluteFile,
            IncrementalSession.stateDirectory(cacheDir, null),
        )
    }

    @Test
    fun `an overridden database reports its own directory as the one to preserve`() {
        val cacheDir = File(tempDir, "data/cache")
        val override = File(tempDir, "somewhere/else/state.db")

        assertEquals(
            override.parentFile.absoluteFile,
            IncrementalSession.stateDirectory(cacheDir, override),
        )
    }

    @Test
    fun `clearing state makes the next build pack everything again`() {
        val cacheDir = File(tempDir, "data/cache").apply { mkdirs() }
        val database = IncrementalSession.defaultDatabase(cacheDir)
        script("a", "one")

        assertEquals(1, build(cacheLocation = cacheDir, db = database))
        assertEquals(0, build(cacheLocation = cacheDir, db = database))

        IncrementalSession.clearState(cacheDir, null)

        assertFalse(database.exists(), "a fresh install must not leave the old state behind")
        assertEquals(1, build(cacheLocation = cacheDir, db = database), "cleared state must force a repack")
    }

    @Test
    fun `clearing state removes an overridden database and its sqlite siblings`() {
        val cacheDir = File(tempDir, "data/cache").apply { mkdirs() }
        val override = File(tempDir, "elsewhere/state.db")
        script("a", "one")

        assertEquals(1, build(cacheLocation = cacheDir, db = override))
        assertTrue(override.exists())
        File(override.parentFile, override.name + "-wal").writeText("stale")

        IncrementalSession.clearState(cacheDir, override)

        assertFalse(override.exists(), "an overridden database must be cleared too")
        assertFalse(File(override.parentFile, override.name + "-wal").exists())
        assertEquals(1, build(cacheLocation = cacheDir, db = override))
    }

    @Test
    fun `output crc verification survives a reseed but repacks overwritten entries`() {
        script("a", "one")
        script("b", "two")

        assertEquals(1, build(verification = CacheVerification.OUTPUT_CRC))
        assertEquals(0, build(verification = CacheVerification.OUTPUT_CRC))

        // Mirrors a server cache reseeded from a live one: every entry is rewritten, most with identical
        // bytes, but anything the live cache does not carry comes back wrong.
        cache.entries[Triple(CLIENTSCRIPT, "b".hashCode(), 0)] = "live copy".toByteArray()

        assertEquals(1, build(verification = CacheVerification.OUTPUT_CRC), "overwritten entry must repack")
        assertEquals(
            "two",
            cache.entries[Triple(CLIENTSCRIPT, "b".hashCode(), 0)]?.decodeToString(),
            "the reseeded entry should have been packed back to its own content",
        )
        assertEquals(0, build(verification = CacheVerification.OUTPUT_CRC))
    }

    @Test
    fun `output crc verification ignores a changed cache fingerprint`() {
        script("a", "one")
        build(verification = CacheVerification.OUTPUT_CRC)

        // Under FINGERPRINT this alone forces a full repack; under OUTPUT_CRC the entries decide.
        cache.fingerprint = byteArrayOf(9, 9, 9)

        assertEquals(0, build(verification = CacheVerification.OUTPUT_CRC))
    }

    /** Returns 1 if the project was compiled this build, 0 if it was skipped. */
    private fun build(
        cacheLocation: File = File(tempDir, "cache"),
        db: File = database,
        verification: CacheVerification = CacheVerification.FINGERPRINT,
    ): Int {
        var compiled = 0
        val task = FakeCs2Task()
        val session = IncrementalSession.open(
            enabled = true,
            cacheLocation = cacheLocation,
            databaseOverride = db,
            revision = 240,
            versionTable = cache.versionTable,
            verification = verification,
        )
        session.use {
            task.incremental = session.build
            session.build.runOnce(
                task = task,
                scope = sources.absolutePath,
                label = "cs2",
                cache = cache,
                fingerprint = dev.openrune.cache.tools.incremental.Hashing.hashTree(sources),
            ) { packCache ->
                compiled = 1
                sources.listFiles().orEmpty().sortedBy { it.name }.forEach { file ->
                    packCache.write(CLIENTSCRIPT, file.nameWithoutExtension.hashCode(), file.readBytes())
                }
            }
            session.finish(cache.versionTable)
        }
        return compiled
    }

    private class FakeCs2Task : CacheTask() {
        override fun init(cache: Cache) = Unit
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

        override fun files(index: Int, archive: Int): IntArray =
            entries.keys.filter { it.first == index && it.second == archive }.map { it.third }.toIntArray()

        override fun data(index: Int, archive: Int, file: Int, xtea: IntArray?) = entries[Triple(index, archive, file)]

        override fun fileData(index: Int, archive: Int, xtea: IntArray?): Array<ByteArray?>? =
            entries.filterKeys { it.first == index && it.second == archive }.values.toTypedArray()

        override fun indexCount() = 1
        override fun exists(id: Int) = true
        override fun indices() = intArrayOf(CLIENTSCRIPT)
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
