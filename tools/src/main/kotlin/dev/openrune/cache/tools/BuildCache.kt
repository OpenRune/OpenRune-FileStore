package dev.openrune.cache.tools

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.CLIENTSCRIPT
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.cs2.PackCs2
import dev.openrune.cache.tools.cs2.UnpackDefaultCs2
import dev.openrune.cache.tools.gameval.GameValReferenceIndex
import dev.openrune.cache.tools.iftype.PackIfType
import dev.openrune.cache.tools.incremental.CacheVerification
import dev.openrune.cache.tools.incremental.IncrementalSession
import dev.openrune.cache.tools.progress.CacheProgress
import dev.openrune.cache.tools.progress.DefaultCacheProgress
import dev.openrune.cache.tools.tasks.impl.RemoveXteas
import readCacheRevision
import java.io.File
import java.nio.ByteBuffer
import kotlin.system.measureTimeMillis

class BuildCache(
    private val cacheLocation: File,
    @Suppress("unused") @Deprecated("No longer used; the cache is updated in place.")
    private val tempLocation: File = File(cacheLocation, "temp"),
    val tasks: MutableList<CacheTask> = mutableListOf(),
    var revision: Int = -1,
    var subRevision: Int = -1,
    var serverPass: Boolean = false,
    private val incremental: Boolean = true,
    private val incrementalDatabase: File? = null,
    private val verification: CacheVerification = CacheVerification.FINGERPRINT,
    private val progress: CacheProgress = DefaultCacheProgress()
) {

    private val logger = InlineLogger()

    fun initialize() {
        try {
            val library = CacheLibrary(cacheLocation.absolutePath)

            if (revision == -1) {
                val data = library.data(CLIENTSCRIPT, "version.dat")
                    ?: error("version.dat missing – set revision manually.")
                revision = readCacheRevision(
                    ByteBuffer.wrap(data),
                    "version.dat is invalid – set revision manually."
                )
            }

            val label = if (serverPass) "server cache" else "cache"
            logger.info { "Building $label (revision $revision, ${tasks.size} tasks)" }
            logger.debug { "Tasks in run order: ${describeTasks()}" }

            if (revision >= RemoveXteas.OBSOLETE_FROM_REVISION && tasks.any { it is RemoveXteas }) {
                logger.warn {
                    "RemoveXteas is deprecated for revision ${RemoveXteas.OBSOLETE_FROM_REVISION}+ and does nothing (no xteas.json required)."
                }
            }

            if (tasks.any { it is PackCs2 || it is UnpackDefaultCs2 } && revision < 0) {
                error(
                    "CS2 tasks (PackCs2 / UnpackDefaultCs2) require a resolved cache revision; revision is still -1. " +
                        "Ensure version.dat is readable or pass an explicit revision into BuildCache."
                )
            }

            CacheTool.gameValMappings.clear()
            PackIfType.packedThisBuild.clear()

            val delegate = CacheDelegate(library)

            val session = IncrementalSession.open(
                enabled = incremental,
                cacheLocation = cacheLocation,
                databaseOverride = incrementalDatabase,
                revision = revision,
                versionTable = delegate.versionTable,
                verification = verification
            )

            progress.buildStarted(revision, serverPass)

            // Neptune's library baseline records the ids the cache's scripts were compiled with. When the
            // build state is fresh the cache is being treated as pristine, so a baseline left over from an
            // earlier cache would be stale: drop it and let UnpackDefaultCs2 record a new one before packing.
            if (session.fresh && !serverPass) {
                tasks.filterIsInstance<PackCs2>().forEach { task ->
                    val baseline = File(task.cs2Root, PackCs2.LIBRARY_STATE_FILE)
                    if (baseline.delete()) logger.debug { "Dropped stale CS2 library baseline ${baseline.absolutePath}" }
                }
            }

            session.use { session ->
                val time = measureTimeMillis {
                    // Snapshot which gamevals the cache's configs reference before anything touches them.
                    // A server pass starts from a copy of the live cache whose configs were relinked already.
                    if (!serverPass) GameValReferenceIndex.index(delegate, revision, session.build, progress)

                    tasks.forEach { task ->
                        task.revision = revision
                        task.subRevision = subRevision
                        task.serverPass = serverPass
                        task.incremental = session.build
                        task.progress = progress
                        task.init(delegate)
                    }

                    // Every id is now final; re-encode configs and interface hooks whose referenced gamevals moved.
                    if (!serverPass) {
                        GameValReferenceIndex.relink(
                            delegate,
                            revision,
                            session.build,
                            cs2Dir = tasks.filterIsInstance<PackCs2>().firstOrNull()?.cs2Root,
                            repackedInterfaces = PackIfType.packedThisBuild.toSet(),
                            progress = progress,
                        )
                    }

                    session.build.reportRemovals()

                    // The library is updated in place. It used to also be rebuilt into a temp directory that
                    // was deleted straight after, which cost a full copy of the cache per pass for nothing.
                    writeCache(library)
                    progress.buildFinished()
                    val versionTable = library.generateUkeys()
                    library.close()

                    // Recorded state is committed only now, once the cache it describes is written and closed.
                    // Anything that throws before this point leaves the transaction uncommitted, so the next
                    // build repacks rather than trusting records for a cache that was never finished.
                    session.finish(versionTable)
                }

                logger.info { "Built $label in ${formatTime(time)}" }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     * Flushes every flagged index to disk, one progress step per index. Equivalent to `library.update()`
     * but visible: on a big change this is the longest silent stretch of the build otherwise.
     */
    private fun writeCache(library: CacheLibrary) {
        val pending = library.indices().filter { it.flagged() || it.flaggedArchives().isNotEmpty() }
        if (pending.isEmpty()) return
        val bar = progress.begin("Writing cache", pending.size.toLong())
        pending.forEach { index ->
            bar.message("index ${index.id} (${index.flaggedArchives().size} archives)")
            index.update()
            bar.step()
        }
        bar.close()
    }

    /**
     * Task names in run order for the debug log, collapsed so a build with one PackConfig per content
     * plugin reads `PackConfig x6` instead of listing it six times. Every task still runs.
     */
    private fun describeTasks(): String {
        val parts = mutableListOf<String>()
        var index = 0
        while (index < tasks.size) {
            val name = tasks[index].javaClass.simpleName
            var count = 1
            while (index + count < tasks.size && tasks[index + count].javaClass.simpleName == name) count++
            parts += if (count > 1) "$name x$count" else name
            index += count
        }
        return parts.joinToString()
    }

    fun formatTime(time : Long) : String {
        val hours = time / 3600000
        val minutes = (time % 3600000) / 60000
        val seconds = (time % 60000) / 1000

        return buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (seconds > 0 || isEmpty()) append("${seconds}s")
        }.trim()
    }

}