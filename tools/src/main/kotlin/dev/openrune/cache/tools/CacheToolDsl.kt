package dev.openrune.cache.tools

import dev.openrune.cache.tools.autocert.AutoCertSettings
import dev.openrune.cache.tools.autocert.CertCandidate
import dev.openrune.cache.tools.autocert.CertCandidateScanner
import dev.openrune.cache.tools.gameval.GameValAssigner
import dev.openrune.cache.tools.incremental.CacheVerification
import dev.openrune.cache.tools.progress.CacheProgress
import dev.openrune.cache.tools.progress.DefaultCacheProgress
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.cs2.PackCs2
import dev.openrune.cache.tools.tasks.impl.PackGameVals
import dev.openrune.cache.tools.tasks.impl.RemoveBzip
import dev.openrune.cache.tools.tasks.impl.RemoveXteas
import dev.openrune.cache.tools.tasks.impl.defs.PackAutoCert
import dev.openrune.cache.tools.cs2.UnpackDefaultCs2
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.filesystem.Cache
import java.io.File

fun cacheTool(block: CacheToolDsl.() -> Unit): CacheTool {
    return CacheToolDsl().apply(block).build()
}

class CacheToolDsl {
    var taskType: TaskType? = null
    var revision: Int? = null
    private var cache: File? = null
    private var serverCache: File? = null

    private val addedTasks = mutableListOf<CacheTask>()
    private val removedTasks = mutableListOf<CacheTask>()

    private var rscmDir: File? = null

    var autoAssignGameVals: Boolean = false

    var incremental: Boolean = true

    /**
     * How the cache is checked against recorded state. Set to [CacheVerification.OUTPUT_CRC] for a cache
     * that is legitimately rewritten between builds, such as a server cache reseeded from a live one.
     */
    var verification: CacheVerification = CacheVerification.FINGERPRINT

    /**
     * Where progress is reported. Replace it to combine every task into a single bar, to render into a UI,
     * or use [dev.openrune.cache.tools.progress.SilentCacheProgress] to report nothing.
     */
    var progress: CacheProgress = DefaultCacheProgress()

    private var incrementalDatabase: File? = null

    var autoCert: Boolean = false

    var autoCertIds: Map<String, Int> = emptyMap()
        private set

    fun tasks(block: TaskBuilder.() -> Unit) {
        TaskBuilder().apply(block).also {
            addedTasks += it.addedTasks
            removedTasks += it.removedTasks
        }
    }

    fun cache(path: String) {
        cache = File(path)
    }

    fun serverCache(path: String) {
        serverCache = File(path)
    }

    fun revision(revision: Int) {
        this.revision = revision
    }


    fun rscm(path: String) {
        rscmDir = File(path)
    }

    fun incrementalDatabase(path: String) {
        incrementalDatabase = File(path)
    }

    fun build(): CacheTool {
        val type = taskType ?: error("`taskType` must be set")
        val revision = revision ?: error("`revision` must be set")
        val cacheLocation = cache ?: error("`cache` must be set")

        rscmDir?.let(ConstantProvider::load)

        val certSettings = if (autoCert) AutoCertSettings(packConfigDirectories()) else null
        if (autoAssignGameVals || certSettings != null) {
            val candidates = certSettings?.let { scanCertCandidates(it, cacheLocation, revision) }.orEmpty()
            autoCertIds = GameValAssigner.assign(candidates, certSettings).certIds
        }

        if (certSettings != null && removedTasks.none { it is PackAutoCert }) {
            addedTasks += PackAutoCert(certSettings)
        }

        val defaultGameVals = PackGameVals()

        if (removedTasks.none { it is PackGameVals }) {
            addedTasks += defaultGameVals
        }

        if (type == TaskType.FRESH_INSTALL) {
            val xteasFile = File(cacheLocation, "xteas.json")
            if ((revision < 237) && removedTasks.none { it is RemoveXteas }) {
                addedTasks += RemoveXteas(xteasFile)
            }
            if (removedTasks.none { it is RemoveBzip }) {
                addedTasks += RemoveBzip()
            }
        }

        val finalTasks = addedTasks.filter { added ->
            removedTasks.none { removed -> removed::class == added::class }
        }

        val cleanedTasks = finalTasks.filterNot { it is RemoveXteas || it is RemoveBzip }

        validateCs2UnpackPackPairing(cleanedTasks)

        return CacheTool(
            type = type,
            revision = revision,
            cacheLocation = cacheLocation,
            serverCacheLocation = serverCache,
            extraTasks = cleanedTasks,
            autoCertIds = autoCertIds,
            incremental = incremental,
            incrementalDatabase = incrementalDatabase,
            verification = verification,
            progress = progress
        )
    }

    private fun scanCertCandidates(
        settings: AutoCertSettings,
        cacheLocation: File,
        revision: Int,
    ): List<CertCandidate> {
        val cache = runCatching { Cache.load(cacheLocation.toPath()) }.getOrNull()
        return try {
            CertCandidateScanner(settings).scan(cache, revision)
        } finally {
            runCatching { cache?.close() }
        }
    }

    private fun packConfigDirectories(): List<File> {
        val directories = addedTasks.filterIsInstance<PackConfig>().map { it.directory }.distinct()
        require(directories.isNotEmpty()) {
            "autoCert needs at least one PackConfig task to know which configs to scan"
        }
        return directories
    }

    private fun validateCs2UnpackPackPairing(tasks: List<CacheTask>) {
        val packers = tasks.filterIsInstance<PackCs2>()
        if (packers.isEmpty()) return
        val unpackers = tasks.filterIsInstance<UnpackDefaultCs2>()
        require(unpackers.isNotEmpty()) {
            "PackCs2 requires UnpackDefaultCs2 in the same tasks { } block. Add +UnpackDefaultCs2(File(\"...\")) before PackCs2."
        }
        for (pack in packers) {
            val packRoot = pack.cs2Root.canonicalFile
            val ok = unpackers.any { it.cs2Root.canonicalFile == packRoot }
            require(ok) {
                "PackCs2(${pack.cs2Root}) requires UnpackDefaultCs2(File(\"${pack.cs2Root}\")) " +
                    "(same CS2 directory)."
            }
        }
    }

    class TaskBuilder {
        internal val addedTasks = mutableListOf<CacheTask>()
        internal val removedTasks = mutableListOf<CacheTask>()

        operator fun CacheTask.unaryPlus() {
            addedTasks += this
        }

        operator fun CacheTask.unaryMinus() {
            removedTasks += this
        }
    }
}
