package dev.openrune

import com.displee.cache.CacheLibrary
import com.displee.compress.CompressionType
import dev.openrune.cache.CacheDelegate
import dev.openrune.filesystem.Cache
import dev.openrune.filesystem.Compression
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.random.Random

/**
 * Packs payloads into a cache with the writer, reads them back with the reader, and asserts the
 * bytes survive unchanged. The writer and the reader are separate implementations, so this pins
 * the reader's archive lookup, index table handling and sector walk against an independent oracle.
 *
 * The scratch directory is managed here rather than with `@TempDir`: Windows keeps a deleted file
 * visible until every handle on it is dropped, which makes the JUnit cleanup fail the test for
 * reasons that have nothing to do with the bytes.
 */
class CacheByteRoundTripTest {

    private val random = Random(20240912)

    private val root: File = File(System.getProperty("java.io.tmpdir"), "openrune-roundtrip-${System.nanoTime()}")

    @AfterEach
    fun cleanUp() {
        root.listFiles()?.forEach { it.delete() }
        root.delete()
    }

    private fun payload(size: Int) = random.nextBytes(size)

    /**
     * [indexes] is created before the delegate is wrapped around the library: `CacheDelegate`
     * builds its version table in its constructor, and that needs at least one index to exist.
     */
    private fun buildCache(indexes: Map<Int, Compression>, block: (CacheDelegate) -> Unit) {
        root.mkdirs()
        File(root, "main_file_cache.dat2").createNewFile()
        File(root, "main_file_cache.idx255").createNewFile()

        val library = CacheLibrary.create(root.absolutePath)
        for ((id, compression) in indexes) {
            library.createIndex(CompressionType.valueOf(compression.name), id = id)
        }

        val delegate = CacheDelegate(library)
        block(delegate)
        delegate.update()
        delegate.close()
    }

    private fun buildCache(block: (CacheDelegate) -> Unit) = buildCache(mapOf(0 to Compression.GZIP), block)

    private fun <T> read(block: (Cache) -> T): T {
        val cache = Cache.load(root.toPath())
        return try {
            block(cache)
        } finally {
            cache.close()
        }
    }

    @Test
    fun `single file archives round trip byte for byte`() {
        val expected = HashMap<Int, ByteArray>()

        buildCache { cache ->
            for (archive in 0 until 64) {
                val data = payload(1 + random.nextInt(4000))
                expected[archive] = data
                cache.write(0, archive, 0, data)
            }
        }

        read { cache ->
            for ((archive, data) in expected) {
                assertArrayEquals(data, cache.data(0, archive, 0), "archive $archive")
            }
        }
    }

    @Test
    fun `multi file archives round trip and keep their file ids`() {
        // Sparse, non-contiguous file ids: the reader has to map a file id to its slot rather than
        // assume the id is the slot.
        val fileIds = intArrayOf(0, 1, 5, 6, 40, 41, 900)
        val expected = HashMap<Int, ByteArray>()

        buildCache { cache ->
            for (file in fileIds) {
                val data = payload(1 + random.nextInt(2000))
                expected[file] = data
                cache.write(0, 7, file, data)
            }
        }

        read { cache ->
            assertArrayEquals(fileIds, cache.files(0, 7))
            for (file in fileIds) {
                assertArrayEquals(expected[file], cache.data(0, 7, file), "file $file")
            }
            assertNull(cache.data(0, 7, 2), "file 2 was never written")
            assertNull(cache.data(0, 7, 901), "file 901 was never written")
        }
    }

    @Test
    fun `payloads spanning many sectors round trip`() {
        // A sector holds 512 bytes of payload, so these cross several sector boundaries and make
        // the reader follow the sector chain.
        val sizes = intArrayOf(511, 512, 513, 520, 1023, 1024, 5_000, 200_000)
        val expected = HashMap<Int, ByteArray>()

        buildCache { cache ->
            sizes.forEachIndexed { archive, size ->
                val data = payload(size)
                expected[archive] = data
                cache.write(0, archive, 0, data)
            }
        }

        read { cache ->
            for ((archive, data) in expected) {
                val actual = cache.data(0, archive, 0)
                assertEquals(data.size, actual?.size, "archive $archive length")
                assertArrayEquals(data, actual, "archive $archive")
            }
        }
    }

    @Test
    fun `every compression type round trips`() {
        val expected = HashMap<Int, ByteArray>()
        val types = listOf(Compression.NONE, Compression.GZIP, Compression.BZIP2)

        buildCache(types.withIndex().associate { (id, compression) -> id to compression }) { cache ->
            types.indices.forEach { id ->
                val data = payload(3000)
                expected[id] = data
                cache.write(id, 0, 0, data)
            }
        }

        read { cache ->
            for ((index, data) in expected) {
                assertArrayEquals(data, cache.data(index, 0, 0), "index $index")
            }
        }
    }

    @Test
    fun `archives stay readable across several indexes and repeated reads`() {
        val expected = HashMap<Pair<Int, Int>, ByteArray>()

        buildCache((0 until 6).associateWith { Compression.GZIP }) { cache ->
            for (index in 0 until 6) {
                for (archive in 0 until 10) {
                    val data = payload(200 + index * 50 + archive)
                    expected[index to archive] = data
                    cache.write(index, archive, 0, data)
                }
            }
        }

        read { cache ->
            repeat(2) { pass ->
                for ((key, data) in expected) {
                    val (index, archive) = key
                    assertArrayEquals(data, cache.data(index, archive, 0), "index $index archive $archive pass $pass")
                }
            }
        }
    }

    @Test
    fun `interleaved reads are not confused by the reader's caches`() {
        val expected = HashMap<Int, ByteArray>()

        buildCache { cache ->
            for (archive in 0 until 40) {
                val data = payload(600 + archive)
                expected[archive] = data
                cache.write(0, archive, 0, data)
            }
        }

        read { cache ->
            // Deliberately thrash the reader's one-archive memo and its bounded LRU.
            val order = expected.keys.toMutableList()
            repeat(3) { pass ->
                order.shuffle(Random(pass))
                for (archive in order) {
                    assertArrayEquals(expected[archive], cache.data(0, archive, 0), "archive $archive pass $pass")
                }
            }
        }
    }
}

