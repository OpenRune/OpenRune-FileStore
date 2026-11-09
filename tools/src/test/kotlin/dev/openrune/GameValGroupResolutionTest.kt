package dev.openrune

import dev.openrune.cache.GAMEVALS
import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.filesystem.Cache
import dev.openrune.filesystem.Compression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 242 split the single sound gameval group (archive 11, which only ever held jingles) into sfx,
 * midi, jingle and ambient groups, moving jingles to archive 18. Callers keep asking for
 * [GameValGroupTypes.SOUNDTYPES]; the handler resolves it to the archive the cache in hand uses,
 * the same way [GameValGroupTypes.IFTYPES] resolves to `IFTYPES_V2`.
 */
class GameValGroupResolutionTest {

    private val jingle = GameValGroupTypes.JINGLETYPES.id
    private val sound = GameValGroupTypes.SOUNDTYPES.id

    @Test
    fun `reads fall through to the 242 jingle archive`() {
        val cache = MemoryCache()
        cache.write(GAMEVALS, jingle, 7, "quest_complete".toByteArray())

        val elements = GameValHandler.readGameVal(GameValGroupTypes.SOUNDTYPES, cache, 242)

        assertEquals(listOf("quest_complete"), elements.map { it.name })
        assertEquals(listOf(7), elements.map { it.id })
    }

    @Test
    fun `reads keep using archive 11 when that is where the data is`() {
        val cache = MemoryCache()
        cache.write(GAMEVALS, sound, 3, "levelup".toByteArray())

        // Revision says 242, but this cache was packed by an older tool — follow the data.
        val elements = GameValHandler.readGameVal(GameValGroupTypes.SOUNDTYPES, cache, 242)

        assertEquals(listOf("levelup"), elements.map { it.name })
    }

    @Test
    fun `writes go to the jingle archive at 242`() {
        val cache = MemoryCache()

        GameValHandler.encodeGameVals(GameValGroupTypes.SOUNDTYPES, listOf(GameValElement("levelup", 3)), cache, 242)

        assertEquals(listOf(3), cache.files(GAMEVALS, jingle).toList())
        assertTrue(cache.files(GAMEVALS, sound).isEmpty()) { "jingles should no longer be written to archive 11" }
    }

    @Test
    fun `writes go to archive 11 before 242`() {
        val cache = MemoryCache()

        GameValHandler.encodeGameVals(GameValGroupTypes.SOUNDTYPES, listOf(GameValElement("levelup", 3)), cache, 241)

        assertEquals(listOf(3), cache.files(GAMEVALS, sound).toList())
        assertTrue(cache.files(GAMEVALS, jingle).isEmpty())
    }

    private class MemoryCache : Cache {
        val entries = LinkedHashMap<Triple<Int, Int, Int>, ByteArray>()

        override val versionTable: ByteArray get() = byteArrayOf()

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
