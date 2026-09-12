package dev.openrune.cache.tools

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.CLIENTSCRIPT
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.cs2.PackCs2
import dev.openrune.cache.tools.cs2.UnpackDefaultCs2
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
    private val tempLocation: File = File(cacheLocation, "temp"),
    val tasks: MutableList<CacheTask> = mutableListOf(),
    var revision: Int = -1,
    var serverPass: Boolean = false,
    private val incremental: Boolean = true,
    private val incrementalDatabase: File? = null,
    private val verification: CacheVerification = CacheVerification.FINGERPRINT,
    private val progress: CacheProgress = DefaultCacheProgress()
) {

    private val logger = InlineLogger()

    fun initialize() {
        tempLocation.deleteRecursively()
        tempLocation.mkdirs()

        try {
            cacheLocation.listFiles { f -> f.extension in listOf("dat", "idx") }
                ?.forEach { file ->
                    file.copyTo(File(tempLocation, file.name), overwrite = true)
                }

            val library = CacheLibrary(cacheLocation.absolutePath)

            if (revision == -1) {
                val data = library.data(CLIENTSCRIPT, "version.dat")
                    ?: error("version.dat missing – set revision manually.")
                revision = readCacheRevision(
                    ByteBuffer.wrap(data),
                    "version.dat is invalid – set revision manually."
                )
            }

            logger.info {
                "Building ${if (serverPass) "Server " else ""}Cache (revision=$revision, tasks=${describeTasks()})"
            }

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

            try {
                val time = measureTimeMillis {
                    tasks.forEach { task ->
                        task.revision = revision
                        task.serverPass = serverPass
                        task.incremental = session.build
                        task.progress = progress
                        task.init(delegate)
                    }
                }

                progress.buildFinished()

                logger.info { "Tasks Finished In: ${formatTime(time)}" }
                logger.info { "Cleaning Up..." }

                library.update()
                val versionTable = library.generateUkeys()
                library.rebuild(File(tempLocation, "rebuilt"))
                library.close()

                File(tempLocation, "rebuilt").listFiles { file -> file.extension in listOf("dat", "idx") }?.forEach { file ->
                    file.copyTo(File(tempLocation, file.name), overwrite = true)
                }

                // Recorded state is committed only now, once the cache it describes is written and closed.
                // Anything that throws before this point leaves the transaction uncommitted, so the next
                // build repacks rather than trusting records for a cache that was never finished.
                session.finish(versionTable)

                logger.info { "Build finished in ${formatTime(time)}" }
            } finally {
                session.close()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            tempLocation.deleteRecursively()
        }
    }

    /**
     * Task names for logging, collapsed so a build with one PackConfig per content plugin reads
     * `PackConfig x6` instead of listing it six times. Affects the log line only; every task still runs.
     */
    private fun describeTasks(): String =
        tasks.groupingBy { it.javaClass.simpleName }
            .eachCount()
            .entries
            .joinToString { (name, count) -> if (count > 1) "$name x$count" else name }

    fun formatTime(time : Long) : String {
        val hours = time / 3600000
        val minutes = (time % 3600000) / 60000
        val seconds = (time % 60000) / 1000

        return buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (seconds > 0) append("${seconds}s")
        }
    }

}