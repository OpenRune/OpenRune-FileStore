package dev.openrune.rs2

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.openrs2.cache.MasterIndexFormat
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

/** Looks up cache ids/metadata on [archive.openrs2.org](https://archive.openrs2.org), so callers can work in build numbers instead of opaque cache ids. */
object OpenRs2CacheArchive {

    private const val CACHES_URL = "https://archive.openrs2.org/caches.json"

    private val client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build()
    private val gson = Gson()

    data class Build(val major: Int, val minor: Int?)

    data class CacheEntry(
        val id: Int,
        val scope: String,
        val game: String,
        val environment: String,
        val language: String,
        val builds: List<Build>,
        val timestamp: String?,
        val indexes: Int,
        @SerializedName("disk_store_valid") val diskStoreValid: Boolean,
        @SerializedName("valid_groups") val validGroups: Long,
        val groups: Long,
        @SerializedName("valid_keys") val validKeys: Long,
        val keys: Long
    )

    private val cacheDir: Path = Paths.get(System.getProperty("user.home"), "openrs2")
    private val cachesJsonFile: Path = cacheDir.resolve("caches.json")
    private const val CACHES_JSON_TTL_MILLIS = 60 * 60 * 1000L // 1 hour - archive.openrs2.org keeps adding new caches

    @Volatile
    private var cached: List<CacheEntry>? = null

    /** [caches.json](https://archive.openrs2.org/caches.json) is ~1.3 MB, so this is cached in-memory for the
     * process lifetime and on disk (with a TTL, since it's a growing/live list) across process runs. */
    fun list(): List<CacheEntry> {
        cached?.let { return it }

        val body = readCachedFile(cachesJsonFile, CACHES_JSON_TTL_MILLIS) ?: run {
            val request = HttpRequest.newBuilder(URI.create(CACHES_URL)).GET().build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                throw IOException("Unexpected status ${response.statusCode()} fetching $CACHES_URL")
            }

            writeCachedFile(cachesJsonFile, response.body())
            response.body()
        }

        val entries = gson.fromJson(body, Array<CacheEntry>::class.java).toList()
        cached = entries
        return entries
    }

    private fun readCachedFile(path: Path, ttlMillis: Long): String? {
        if (!Files.isRegularFile(path)) return null
        if (System.currentTimeMillis() - Files.getLastModifiedTime(path).toMillis() > ttlMillis) return null
        return Files.readString(path)
    }

    private fun writeCachedFile(path: Path, content: String) {
        Files.createDirectories(path.parent)
        Files.writeString(path, content)
    }

    fun findByBuild(build: Int, game: String = "runescape", scope: String = "runescape", environment: String = "live", language: String = "en"): CacheEntry? =
        list()
            .filter { it.scope == scope && it.game == game && it.environment == environment && it.language == language }
            .filter { entry -> entry.builds.any { it.major == build } }
            .maxByOrNull { it.id }

    fun findById(scope: String, id: Int): CacheEntry? =
        list().firstOrNull { it.scope == scope && it.id == id }

    fun buildFor(scope: String, id: Int): Int? =
        findById(scope, id)?.builds?.firstOrNull()?.major

    private val masterIndexFormats = ConcurrentHashMap<Pair<String, Int>, MasterIndexFormat>()
    private val masterIndexFormatRegex = Regex("""Format</th>\s*<td>(\w+)</td>""")

    // The master index's binary layout isn't self-describing, and guessing it from build
    // number or byte length is unreliable (the trailing RSA signature block's size varies
    // by era). archive.openrs2.org's cache detail page states the real format though, so
    // we scrape that instead - cached per (scope, id), on disk with no TTL since it can't
    // change once archived, plus in-memory for the process lifetime.
    fun masterIndexFormat(scope: String, id: Int): MasterIndexFormat =
        masterIndexFormats.computeIfAbsent(scope to id) {
            val file = cacheDir.resolve(scope).resolve("$id-format.txt")
            val cachedName = readCachedFile(file, Long.MAX_VALUE)
            if (cachedName != null) return@computeIfAbsent MasterIndexFormat.valueOf(cachedName)

            val request = HttpRequest.newBuilder(URI.create("https://archive.openrs2.org/caches/$scope/$id")).GET().build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                throw IOException("Unexpected status ${response.statusCode()} fetching cache page for $scope/$id")
            }

            val name = masterIndexFormatRegex.find(response.body())?.groupValues?.get(1)
                ?: error("Could not find master index format on cache page for $scope/$id")
            writeCachedFile(file, name)
            MasterIndexFormat.valueOf(name)
        }

    /** One [CacheEntry] per build, restricted to caches archive.openrs2.org considers completely archived. */
    fun validCaches(game: String = "runescape", scope: String = "runescape", environment: String = "live", language: String = "en"): List<CacheEntry> =
        list()
            .filter { it.scope == scope && it.game == game && it.environment == environment && it.language == language }
            .filter { it.diskStoreValid && it.groups > 0 && it.validGroups == it.groups }
            .filter { it.builds.isNotEmpty() }
            .groupBy { it.builds.first().major }
            .values
            .map { candidates -> candidates.maxBy { it.groups } }
            .sortedBy { it.builds.first().major }

    /** One [CacheEntry] per build - unlike [validCaches], includes incomplete/unverified captures too. */
    fun allBuilds(game: String = "runescape", scope: String = "runescape", environment: String = "live", language: String = "en"): List<CacheEntry> =
        list()
            .filter { it.scope == scope && it.game == game && it.environment == environment && it.language == language }
            .filter { it.builds.isNotEmpty() }
            .groupBy { it.builds.first().major }
            .values
            .map { candidates ->
                candidates.sortedWith(
                    compareByDescending<CacheEntry> { it.diskStoreValid && it.groups > 0 && it.validGroups == it.groups }
                        .thenByDescending { it.groups }
                ).first()
            }
            .sortedBy { it.builds.first().major }

}
