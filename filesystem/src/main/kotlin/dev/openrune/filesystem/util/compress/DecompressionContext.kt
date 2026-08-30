package dev.openrune.filesystem.util.compress

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.filesystem.util.secure.Xtea
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.Inflater

/**
 * Context per thread for decompressing data in parallel
 */
internal class DecompressionContext {
    private val gzipInflater = Inflater(true)
    private val bzip2Compressor: BZIP2Compressor by lazy { BZIP2Compressor() }

    fun decompress(data: ByteArray, keys: IntArray? = null): ByteArray? {
        if (keys != null && (keys[0] != 0 || keys[1] != 0 || keys[2] != 0 || 0 != keys[3])) {
            Xtea.decipher(data, keys, 5)
        }
        if (data.size < HEADER_SIZE) {
            return null
        }
        // Header is read straight off the array; wrapping it in a ByteBuffer allocated one throwaway
        // object per archive, and this runs once for every archive in the cache.
        val type = data[0].toInt() and 0xff
        val compressedSize = readInt(data, 1)
        val headerSize = if (type == NONE) HEADER_SIZE else HEADER_SIZE + 4
        val decompressedSize = if (type == NONE) 0 else readInt(data, HEADER_SIZE)
        when (type) {
            NONE -> {
                if (compressedSize < 0 || headerSize + compressedSize > data.size) {
                    return null
                }
                return data.copyOfRange(headerSize, headerSize + compressedSize)
            }
            BZIP2 -> {
                if (!warned.get()) {
                    logger.warn { "BZIP2 Compression found - replace to improve read performance." }
                    warned.set(true)
                }
                val decompressed = ByteArray(decompressedSize)
                bzip2Compressor.decompress(decompressed, decompressedSize, data, 9)
                return decompressed
            }
            GZIP -> {
                if (data.size < headerSize + GZIP_HEADER_SIZE + GZIP_TRAILER_SIZE) {
                    return null
                }
                if (data[headerSize].toInt() != 31 || data[headerSize + 1].toInt() != -117) {
                    return null
                }
                return try {
                    val decompressed = ByteArray(decompressedSize)
                    gzipInflater.setInput(
                        data,
                        headerSize + GZIP_HEADER_SIZE,
                        data.size - (headerSize + GZIP_HEADER_SIZE + GZIP_TRAILER_SIZE)
                    )
                    var written = 0
                    while (written < decompressedSize) {
                        val count = gzipInflater.inflate(decompressed, written, decompressedSize - written)
                        if (count == 0) {
                            break
                        }
                        written += count
                    }
                    decompressed
                } catch (exception: Exception) {
                    logger.warn(exception) { "Error decompressing gzip data." }
                    null
                } finally {
                    gzipInflater.reset()
                }
            }
        }
        return null
    }

    private fun readInt(data: ByteArray, index: Int) =
        ((data[index].toInt() and 0xff) shl 24) or
            ((data[index + 1].toInt() and 0xff) shl 16) or
            ((data[index + 2].toInt() and 0xff) shl 8) or
            (data[index + 3].toInt() and 0xff)

    companion object {
        private const val NONE = 0
        private const val BZIP2 = 1
        private const val GZIP = 2
        private const val HEADER_SIZE = 5
        private const val GZIP_HEADER_SIZE = 10
        private const val GZIP_TRAILER_SIZE = 8
        private val warned = AtomicBoolean()
        private val logger = InlineLogger()
    }
}