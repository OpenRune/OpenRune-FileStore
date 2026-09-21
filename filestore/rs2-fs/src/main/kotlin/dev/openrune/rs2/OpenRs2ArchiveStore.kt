package dev.openrune.rs2

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.openrs2.cache.Js5Compression
import org.openrs2.cache.Js5Index
import org.openrs2.cache.Js5MasterIndex
import org.openrs2.cache.MasterIndexFormat
import org.openrs2.cache.Store
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.ConcurrentHashMap

/**
 * A read-only [Store] that fetches individual JS5 archive/group files from
 * [archive.openrs2.org](https://archive.openrs2.org) on demand, instead of
 * downloading the whole cache. The plain `/caches/{scope}/{id}/...` endpoint
 * only serves current content, not the pinned snapshot [id] - only safe to
 * use here for the one-off master index fetch - so everything else goes
 * through the content-addressed `versions/.../checksums/...` endpoint,
 * using version/checksum from the parent index (master index for per-archive
 * indexes, each archive's own index for its groups).
 */
class OpenRs2ArchiveStore(
    private val scope: String,
    private val id: Int,
    private val client: HttpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build()
) : Store {

    private val archiveIndexes = ConcurrentHashMap<Int, Js5Index>()

    private val master: ParsedMasterIndex by lazy { fetchMasterIndex() }

    override fun exists(archive: Int): Boolean = exists(Store.ARCHIVESET, archive)

    override fun exists(archive: Int, group: Int): Boolean {
        val request = HttpRequest.newBuilder(simpleUri(archive, group)).method("HEAD", HttpRequest.BodyPublishers.noBody()).build()
        return send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200
    }

    override fun list(): List<Int> = unsupported()

    override fun list(archive: Int): List<Int> {
        if (archive != Store.ARCHIVESET) unsupported()
        val entries = master.index.entries
        return entries.indices.filter { entries[it].checksum != 0 }
    }

    override fun create(archive: Int) = unsupported()

    override fun read(archive: Int, group: Int): ByteBuf {
        if (archive == Store.ARCHIVESET && group == Store.ARCHIVESET) {
            return fetch(HttpRequest.newBuilder(simpleUri(archive, group)).GET().build(), archive, group)
        }

        if (archive == Store.ARCHIVESET) {
            val entry = master.index.entries.getOrNull(group)
                ?: throw FileNotFoundException("Archive $group not found for $scope/$id")

            // ORIGINAL master indexes have no version field, so fall back to the
            // plain endpoint here - these builds are long frozen, not moving targets.
            return if (master.format == MasterIndexFormat.ORIGINAL) {
                fetch(HttpRequest.newBuilder(simpleUri(archive, group)).GET().build(), archive, group)
            } else {
                fetchVersioned(archive, group, entry.version, entry.checksum)
            }
        }

        val entry = archiveIndex(archive)[group]
            ?: throw FileNotFoundException("Archive $archive group $group not found for $scope/$id")
        return fetchVersioned(archive, group, entry.version, entry.checksum)
    }

    override fun write(archive: Int, group: Int, buf: ByteBuf) = unsupported()

    override fun remove(archive: Int) = unsupported()

    override fun remove(archive: Int, group: Int) = unsupported()

    override fun flush() {}

    override fun close() {}

    private class ParsedMasterIndex(val format: MasterIndexFormat, val index: Js5MasterIndex)

    private fun fetchMasterIndex(): ParsedMasterIndex {
        val format = OpenRs2CacheArchive.masterIndexFormat(scope, id)

        val compressed = fetch(HttpRequest.newBuilder(simpleUri(Store.ARCHIVESET, Store.ARCHIVESET)).GET().build(), Store.ARCHIVESET, Store.ARCHIVESET)
        return try {
            val uncompressed = Js5Compression.uncompress(compressed)
            try {
                ParsedMasterIndex(format, Js5MasterIndex.readUnverified(uncompressed, format))
            } finally {
                uncompressed.release()
            }
        } finally {
            compressed.release()
        }
    }

    private fun archiveIndex(archive: Int): Js5Index = archiveIndexes.computeIfAbsent(archive) {
        val compressed = read(Store.ARCHIVESET, archive)
        try {
            val uncompressed = Js5Compression.uncompress(compressed)
            try {
                Js5Index.read(uncompressed)
            } finally {
                uncompressed.release()
            }
        } finally {
            compressed.release()
        }
    }

    // Content-addressed responses have no version trailer, but CacheArchive.verifyCompressed
    // expects one - append it ourselves rather than let it misread real data as a bogus version.
    private fun fetchVersioned(archive: Int, group: Int, version: Int, checksum: Int): ByteBuf {
        val body = fetch(HttpRequest.newBuilder(versionedUri(archive, group, version, checksum)).GET().build(), archive, group)
        return try {
            Unpooled.buffer(body.readableBytes() + 2)
                .writeBytes(body)
                .writeShort(version and 0xFFFF)
        } finally {
            body.release()
        }
    }

    private fun fetch(request: HttpRequest, archive: Int, group: Int): ByteBuf {
        val response = send(request, HttpResponse.BodyHandlers.ofByteArray())

        return when (response.statusCode()) {
            200 -> Unpooled.wrappedBuffer(response.body())
            404 -> throw FileNotFoundException("Archive $archive group $group not found for $scope/$id")
            else -> throw IOException("Unexpected status ${response.statusCode()} fetching archive $archive group $group")
        }
    }

    private fun <T> send(request: HttpRequest, bodyHandler: HttpResponse.BodyHandler<T>): HttpResponse<T> {
        return try {
            client.send(request, bodyHandler)
        } catch (e: IOException) {
            throw IOException("Failed to request ${request.uri()}", e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Interrupted requesting ${request.uri()}", e)
        }
    }

    private fun simpleUri(archive: Int, group: Int): URI =
        URI.create("https://archive.openrs2.org/caches/$scope/$id/archives/$archive/groups/$group.dat")

    private fun versionedUri(archive: Int, group: Int, version: Int, checksum: Int): URI =
        URI.create("https://archive.openrs2.org/caches/$scope/archives/$archive/groups/$group/versions/$version/checksums/$checksum.dat")

    private fun unsupported(): Nothing =
        throw UnsupportedOperationException("OpenRs2ArchiveStore is a selective, read-only store")

}
