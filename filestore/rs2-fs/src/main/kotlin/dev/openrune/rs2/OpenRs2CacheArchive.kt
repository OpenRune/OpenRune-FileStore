package dev.openrune.rs2

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.openrs2.cache.MasterIndexFormat
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.ConcurrentHashMap

/**
 * Looks up cache ids on [archive.openrs2.org](https://archive.openrs2.org) by
 * revision, so callers only need to know a build/revision number rather than
 * an opaque archive id - mirrors how
 * [zwyz/rs3-cache](https://github.com/zwyz/rs3-cache)'s `Main`/`data/caches.txt`
 * resolve a build number to a cache id before unpacking, except this queries
 * OpenRS2's `caches.json` endpoint directly instead of a pinned local table.
 */
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
        val groups: Long
    )

    @Volatile
    private var cached: List<CacheEntry>? = null

    /**
     * Fetches (and caches for the lifetime of the process) the full list of
     * caches known to archive.openrs2.org.
     */
    fun list(): List<CacheEntry> {
        cached?.let { return it }

        val request = HttpRequest.newBuilder(URI.create(CACHES_URL)).GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw java.io.IOException("Unexpected status ${response.statusCode()} fetching $CACHES_URL")
        }

        val entries = gson.fromJson(response.body(), Array<CacheEntry>::class.java).toList()
        cached = entries
        return entries
    }

    /**
     * Finds the most recent cache matching [scope]/[game]/[environment] whose
     * builds include [build] as a major revision.
     */
    fun findByBuild(build: Int, game: String = "runescape", scope: String = "runescape", environment: String = "live"): CacheEntry? =
        list()
            .filter { it.scope == scope && it.game == game && it.environment == environment }
            .filter { entry -> entry.builds.any { it.major == build } }
            .maxByOrNull { it.id }

    /**
     * Finds a cache by its exact [scope]/[id], used by [OpenRs2ArchiveStore]
     * to work out how many archives a cache has (so it can answer
     * `list(Store.ARCHIVESET)` without downloading the whole cache).
     */
    fun findById(scope: String, id: Int): CacheEntry? =
        list().firstOrNull { it.scope == scope && it.id == id }

    /**
     * Looks up the major build/revision number a cache belongs to, straight
     * from its archive.openrs2.org manifest entry - no need to track it
     * separately alongside the cache id.
     */
    fun buildFor(scope: String, id: Int): Int? =
        findById(scope, id)?.builds?.firstOrNull()?.major

    private val masterIndexFormats = ConcurrentHashMap<Pair<String, Int>, MasterIndexFormat>()
    private val masterIndexFormatRegex = Regex("""Format</th>\s*<td>(\w+)</td>""")

    /**
     * The master index's binary layout isn't self-describing, and guessing
     * it from either the client build number or the decompressed byte
     * length turns out to be unreliable: the layout has more variation
     * across cache generations than either approach accounts for (e.g. the
     * trailing RSA signature block's size isn't fixed - it varies by era -
     * so it can't be subtracted out to solve for the entry width).
     *
     * archive.openrs2.org's cache detail page renders the actual format
     * it detected (as one of [MasterIndexFormat]'s names) in a "Format" row,
     * which is the one genuinely reliable source for this - so we scrape
     * that instead of guessing. Cached per (scope, id) for the process
     * lifetime, since it can't change for an already-archived cache.
     */
    fun masterIndexFormat(scope: String, id: Int): MasterIndexFormat =
        masterIndexFormats.computeIfAbsent(scope to id) {
            val request = HttpRequest.newBuilder(URI.create("https://archive.openrs2.org/caches/$scope/$id")).GET().build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                throw IOException("Unexpected status ${response.statusCode()} fetching cache page for $scope/$id")
            }

            val name = masterIndexFormatRegex.find(response.body())?.groupValues?.get(1)
                ?: error("Could not find master index format on cache page for $scope/$id")
            MasterIndexFormat.valueOf(name)
        }

    /**
     * One [CacheEntry] per major build/revision, matching [scope]/[game]/
     * [environment], restricted to caches archive.openrs2.org considers
     * completely and correctly archived (`disk_store_valid: true` and every
     * group present, not just most of them). Still-updating snapshots of the
     * live game are excluded - their master/archive indexes and data groups
     * can genuinely disagree with each other, which is a real inconsistency
     * in the archived data rather than anything a client can work around.
     *
     * Where multiple caches exist for the same build, the one with the most
     * archived groups is preferred, since a more complete capture is more
     * likely to include the sprite groups being scanned.
     */
    fun validCaches(game: String = "runescape", scope: String = "runescape", environment: String = "live"): List<CacheEntry> =
        list()
            .filter { it.scope == scope && it.game == game && it.environment == environment }
            .filter { it.diskStoreValid && it.groups > 0 && it.validGroups == it.groups }
            .filter { it.builds.isNotEmpty() }
            .groupBy { it.builds.first().major }
            .values
            .map { candidates -> candidates.maxBy { it.groups } }
            .sortedBy { it.builds.first().major }

    /**
     * One [CacheEntry] per major build/revision, matching [scope]/[game]/
     * [environment] - unlike [validCaches], every build is included, even
     * ones archive.openrs2.org doesn't consider completely/correctly
     * archived (`disk_store_valid: false`, e.g. still-updating snapshots of
     * the live game, such as the most recent build). For those, whichever
     * candidate has the most archived groups is used on a best-effort basis;
     * it may still fail to decode, which is exactly the point of scanning
     * every build rather than only the known-good ones.
     */
    fun allBuilds(game: String = "runescape", scope: String = "runescape", environment: String = "live"): List<CacheEntry> =
        list()
            .filter { it.scope == scope && it.game == game && it.environment == environment }
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
