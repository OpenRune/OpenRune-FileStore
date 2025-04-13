package dev.openrune.filesystem.util.compress

import dev.openrune.filesystem.util.readByte
import dev.openrune.filesystem.util.readInt
import dev.openrune.filesystem.util.readUnsignedByte
import dev.openrune.filesystem.util.secure.Xtea
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.ByteBuffer
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
        val buffer = ByteBuffer.wrap(data)
        val type = buffer.readUnsignedByte()
        val compressedSize = buffer.readInt()
        var decompressedSize = 0
        if (type != 0) {
            decompressedSize = buffer.readInt()
        }
        when (type) {
            NONE -> {
                val decompressed = ByteArray(compressedSize)
                buffer.get(decompressed, 0, compressedSize)
                return decompressed
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
                val offset = buffer.position()
                if (buffer.readByte() != 31 || buffer.readByte() != -117) {
                    return null
                }
                return try {
                    val decompressed = ByteArray(decompressedSize)
                    gzipInflater.setInput(data, offset + 10, data.size - (offset + 18))
                    gzipInflater.finished()
                    gzipInflater.inflate(decompressed)
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

    companion object {
        private const val NONE = 0
        private const val BZIP2 = 1
        private const val GZIP = 2
        private val warned = AtomicBoolean()
        private val logger = KotlinLogging.logger {}
    }
}