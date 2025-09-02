import dev.openrune.cache.CLIENTSCRIPT
import dev.openrune.filesystem.Cache
import java.nio.ByteBuffer

fun readCacheRevision(buffer: ByteBuffer, errorMessage: String): Int {
    val size = buffer.array().size
    if (size < 6) {
        error(errorMessage)
    }
    val scriptVer = buffer.short
    return buffer.int
}

fun readCacheRevision(cache: Cache, errorMessage: String): Int {
    val data = cache.data(CLIENTSCRIPT, "version.dat") ?: error(errorMessage)
    return readCacheRevision(ByteBuffer.wrap(data), errorMessage)
}

fun readCacheRevision(cache: Cache): Int {
    return readCacheRevision(
        cache,
        "version.dat: file is missing. Unable to determine cache revision — please set the revision manually."
    )
}