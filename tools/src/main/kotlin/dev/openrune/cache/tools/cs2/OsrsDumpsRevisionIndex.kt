package dev.openrune.cache.tools.cs2

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import dev.openrune.cache.tools.CacheInfo
import dev.openrune.cache.tools.GameType
import dev.openrune.cache.tools.OpenRS2
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Builds an index of every commit in `Joshua-F/osrs-dumps` and links each one to an
 * OSRS revision / sub revision as understood by [OpenRS2.findRevision].
 *
 * Commit messages in that repository follow `YYYY-MM-DD[a|b]-revNNN`, and OpenRS2
 * records a timestamp plus `builds[].major` per cache, so a commit is matched to the
 * OpenRS2 cache with the same major revision whose timestamp is closest to the commit
 * date. The sub revision is the 1-based position of that cache among all live OSRS
 * caches sharing the same major revision, ordered by timestamp, which is exactly what
 * [OpenRS2.downloadCacheByRevision] expects for its `subRev` argument.
 */
object OsrsDumpsRevisionIndex {

    const val REPO = "Joshua-F/osrs-dumps"
    const val BRANCH = "master"

    private const val COMMITS_API = "https://api.github.com/repos/$REPO/commits"
    private const val PER_PAGE = 100

    /** Commits whose date is further than this from every cache with the same major are left unmatched. */
    private const val MAX_DAY_DISTANCE = 7L

    private val messagePattern = Regex("""^(\d{4}-\d{2}-\d{2})([a-z]?)-rev(\d+)$""")

    data class DumpRevision(
        /** Major revision parsed from the commit message. */
        val rev: Int,
        /** 1-based index among OpenRS2 caches with the same major, or -1 when unmatched. */
        val subRev: Int,
        /** Date parsed from the commit message. */
        val date: LocalDate,
        /** Same-day disambiguator from the commit message (`a`, `b`, ...) or empty. */
        val suffix: String,
        val commitSha: String,
        val commitMessage: String,
        /** OpenRS2 cache id the commit was matched to, or null. */
        val openRs2CacheId: Int?,
        /** First parent commit, or null for the root commit. */
        val parentSha: String? = null,
    ) {
        val commitUrl: String get() = "https://github.com/$REPO/commit/$commitSha"
        val treeUrl: String get() = "https://github.com/$REPO/tree/$commitSha"
        val openRs2Url: String? get() = openRs2CacheId?.let { "https://archive.openrs2.org/caches/runescape/$it" }
        val label: String get() = if (subRev > 0) "$rev.$subRev" else "$rev.?"
    }

    private data class Commit(val sha: String, val message: String, val parentSha: String?)

    /**
     * Fetches every commit on [BRANCH] and resolves them against OpenRS2. The result is
     * ordered newest first, mirroring the GitHub commit listing.
     */
    fun generate(game: GameType = GameType.OLDSCHOOL): List<DumpRevision> {
        OpenRS2.loadCaches()
        val cachesByMajor = OpenRS2.allCaches.asSequence()
            // Gson bypasses Kotlin null checks, so fields may be null despite their declared types.
            .filter { it.game != null && it.game.contains(game.formatName()) && it.environment == "live" }
            .filter { !it.builds.isNullOrEmpty() && it.builds[0].major > 0 }
            .filter { it.timestamp != null && it.timestamp.isNotBlank() && it.size > 0 }
            .groupBy { it.builds[0].major }
            .mapValues { (_, caches) -> caches.sortedBy { Instant.parse(it.timestamp) } }

        return fetchCommits().mapNotNull { commit ->
            val match = messagePattern.find(commit.message.lineSequence().first().trim()) ?: return@mapNotNull null
            val (dateText, suffix, revText) = match.destructured
            val date = LocalDate.parse(dateText)
            val rev = revText.toInt()

            val (subRev, cacheId) = resolveSubRev(cachesByMajor[rev].orEmpty(), date, suffix)
            DumpRevision(rev, subRev, date, suffix, commit.sha, commit.message.lineSequence().first(), cacheId, commit.parentSha)
        }
    }

    /**
     * Picks the OpenRS2 cache for a commit. Prefers caches on the same calendar day; when several
     * exist on that day the commit suffix (`a` = first, `b` = second, ...) selects between them.
     * Falls back to the nearest cache within [MAX_DAY_DISTANCE] days.
     */
    private fun resolveSubRev(candidates: List<CacheInfo>, date: LocalDate, suffix: String): Pair<Int, Int?> {
        if (candidates.isEmpty()) return -1 to null

        val sameDay = candidates.withIndex().filter { (_, cache) -> cacheDate(cache) == date }
        val chosen = when {
            sameDay.isEmpty() -> candidates.withIndex()
                .minByOrNull { (_, cache) -> abs(ChronoUnit.DAYS.between(cacheDate(cache), date)) }
                ?.takeIf { (_, cache) -> abs(ChronoUnit.DAYS.between(cacheDate(cache), date)) <= MAX_DAY_DISTANCE }
            suffix.isEmpty() -> sameDay.first()
            else -> sameDay.getOrNull(suffix[0] - 'a') ?: sameDay.last()
        } ?: return -1 to null

        return (chosen.index + 1) to chosen.value.id
    }

    private fun cacheDate(cache: CacheInfo): LocalDate =
        Instant.parse(cache.timestamp).atZone(ZoneOffset.UTC).toLocalDate()

    private fun fetchCommits(): List<Commit> {
        val commits = mutableListOf<Commit>()
        var page = 1
        while (true) {
            val body = get("$COMMITS_API?sha=$BRANCH&per_page=$PER_PAGE&page=$page")
            val array: JsonArray = JsonParser.parseString(body).asJsonArray
            if (array.isEmpty) break
            for (element in array) {
                val obj = element.asJsonObject
                commits += Commit(
                    sha = obj["sha"].asString,
                    message = obj.getAsJsonObject("commit")["message"].asString,
                    parentSha = obj.getAsJsonArray("parents").firstOrNull()?.asJsonObject?.get("sha")?.asString,
                )
            }
            if (array.size() < PER_PAGE) break
            page++
        }
        return commits
    }

    /**
     * GitHub token used for API calls: `GITHUB_TOKEN` if set, otherwise whatever the `gh` CLI is
     * logged in with. Unauthenticated calls are capped at 60/hour, which a full package build exceeds.
     */
    val githubToken: String? by lazy {
        System.getenv("GITHUB_TOKEN")?.takeIf { it.isNotBlank() }
            ?: runCatching {
                val process = ProcessBuilder("gh", "auth", "token").redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().readText().trim()
                if (process.waitFor() == 0 && output.isNotBlank()) output else null
            }.getOrNull()
    }

    fun get(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "OpenRune-FileStore")
            githubToken?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        try {
            if (connection.responseCode != 200) {
                val error = connection.errorStream?.bufferedReader()?.readText().orEmpty()
                error("GitHub request failed (${connection.responseCode}) for $url: $error")
            }
            return connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }
}
