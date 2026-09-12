package dev.openrune

import dev.openrune.filesystem.Cache
import dev.openrune.filesystem.ReadOnlyCache
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * `ReadOnlyCache.fileIndex` replaced a linear scan of every archive's file id table with an
 * identity or binary search shortcut. This checks the shortcut agrees with the scan for every
 * archive in a real cache, including ids that are absent.
 */
class CacheFileIndexTest {

    @Test
    fun `file index agrees with a linear scan across the whole cache`() {
        val cache = Cache.load(File("../data/cache").toPath()) as ReadOnlyCache

        var archivesChecked = 0
        var lookupsChecked = 0

        for (index in cache.indices()) {
            for (archive in cache.archives(index)) {
                val ids = cache.files(index, archive)
                if (ids.isEmpty()) continue
                archivesChecked++

                for (file in ids) {
                    assertEquals(ids.indexOf(file), cache.fileIndex(index, archive, file)) {
                        "index $index archive $archive file $file"
                    }
                    lookupsChecked++
                }

                for (absent in intArrayOf(-1, ids.last() + 1, ids.last() + 1000, Int.MAX_VALUE)) {
                    assertEquals(-1, cache.fileIndex(index, archive, absent)) {
                        "index $index archive $archive absent id $absent"
                    }
                }
            }
        }

        cache.close()

        assertTrue(archivesChecked > 1000) { "only checked $archivesChecked archives" }
        assertTrue(lookupsChecked > 100_000) { "only checked $lookupsChecked lookups" }
    }

    @Test
    fun `unknown indexes and archives report no match`() {
        val cache = Cache.load(File("../data/cache").toPath()) as ReadOnlyCache

        assertEquals(-1, cache.fileIndex(-1, 0, 0))
        assertEquals(-1, cache.fileIndex(Int.MAX_VALUE, 0, 0))
        assertEquals(-1, cache.fileIndex(0, -1, 0))
        assertEquals(-1, cache.fileIndex(0, Int.MAX_VALUE, 0))

        cache.close()
    }
}
