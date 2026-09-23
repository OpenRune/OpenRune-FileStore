package dev.openrune.cache.tools.cs2

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.tools.TaskPriority
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.filesystem.Cache
import net.lingala.zip4j.ZipFile
import java.io.File

class UnpackDefaultCs2(
    private val cs2Directory: File,
    private val subRevisionOverride: Int? = null,
    private val force: Boolean = false,
    /** Snapshot the pre-build symbols for Neptune's library baseline if the project has none; see [PackCs2.snapshotBaselineSymbols]. */
    private val recordBaseline: Boolean = true,
) : CacheTask() {

    private val logger = InlineLogger()

    /**
     * Runs first, not in the CS2 stage: installing the project touches no cache data, and doing it before
     * any packer lets the library baseline be recorded against the cache's original ids.
     */
    override val priority: TaskPriority
        get() = TaskPriority.GAMEVALS_COLLECT

    val cs2Root: File
        get() = cs2Directory

    override fun init(cache: Cache) {
        val major = revision

        if (major < 0) {
            logger.warn {
                "UnpackDefaultCs2: cache revision is unset (revision=$major); cannot choose a CS2 bundle."
            }
            return
        }

        if (major < CacheTask.CS2_MIN_CACHE_REVISION) {
            logger.warn {
                "UnpackDefaultCs2: cache revision $major is not supported (minimum ${CacheTask.CS2_MIN_CACHE_REVISION})."
            }
            return
        }

        val neptune = File(cs2Directory, "neptune.toml")

        val firstInstall = !neptune.exists()
        var versionChanged = false

        if (!firstInstall && !force) {
            val text = runCatching {
                neptune.readText()
            }.getOrNull()

            if (text != null) {
                val existingRev =
                    NeptuneTomlClientVersion.readClientVersionFromText(text)

                if (existingRev == major) {
                    // Same revision: the project is current. A directory neptune.toml lists but that is
                    // missing (a fresh checkout without symbols_custom, say) is recreated, not reinstalled
                    // over; reinstalling would wipe the user's scripts.
                    NeptuneTomlClientVersion.ensureListedDirectories(cs2Directory, text)
                    ensureExcludedFromNeptune(neptune)
                    if (recordBaseline) PackCs2.snapshotBaselineSymbols(cs2Directory, cache, major, progress)
                    return
                }

                versionChanged = true
            } else {
                versionChanged = true
            }
        }

        if (firstInstall || versionChanged || force) {
            logger.debug {
                "UnpackDefaultCs2: performing fresh install " +
                        "(firstInstall=$firstInstall, " +
                        "versionChanged=$versionChanged, " +
                        "force=$force)"
            }

            wipeDirectory(cs2Directory)
            cs2Directory.mkdirs()
        }

        val wantedSub = subRevisionOverride ?: subRevision.takeIf { it > 0 }

        val bundle = Cs2InstallBundles.resolve(major, wantedSub)
            ?: error(
                "UnpackDefaultCs2: no CS2 bundle for revision $major in the manifest at " +
                    "${Cs2BundleSource.baseUrl}${Cs2BundleSource.MANIFEST_FILE} or on the classpath " +
                    "(${Cs2BundleSource.CLASSPATH_DIR}/$major.<sub>.zip). " +
                    "Install a CS2 project manually under ${cs2Directory.absolutePath}"
            )
        val bundleKey = bundle.name

        val wantedLabel = if (wantedSub != null) "$major.$wantedSub" else "$major"
        if (bundleKey != wantedLabel) {
            logger.debug { "UnpackDefaultCs2: no bundle for $wantedLabel, using the closest one in revision $major: $bundleKey" }
        }
        logger.info { "Installing CS2 project $bundleKey into ${cs2Directory.absolutePath}" }

        val zipFile = Cs2BundleSource.fetch(bundle)
        if (zipFile == null) {
            logger.warn { "UnpackDefaultCs2: could not obtain bundle $bundleKey." }
            return
        }

        try {
            ZipFile(zipFile).use { zip ->
                Cs2BundleExtract.extract(
                    zip,
                    cs2Directory,
                    progress,
                )
            }

            syncNeptuneClientVersion(major)

            logger.debug {
                "UnpackDefaultCs2: unpacked $bundleKey into ${cs2Directory.absolutePath}"
            }

            if (recordBaseline) PackCs2.snapshotBaselineSymbols(cs2Directory, cache, major, progress)
        } catch (e: Exception) {
            logger.error(e) {
                "UnpackDefaultCs2: failed to unpack $bundleKey: ${e.message}"
            }
        }
    }

    private fun wipeDirectory(dir: File) {
        if (!dir.exists()) {
            return
        }

        dir.walkBottomUp().forEach {
            if (it.absolutePath.contains("custom")) {
                return@forEach
            }

            // A directory that still holds kept `custom` entries (or the root itself) cannot go; that is
            // expected, not a failure worth reporting.
            if (!it.delete() && !it.isDirectory) {
                logger.warn {
                    "UnpackDefaultCs2: failed to delete ${it.absolutePath}"
                }
            }
        }
    }

    private fun syncNeptuneClientVersion(major: Int) {
        val neptune = File(cs2Directory, "neptune.toml")

        NeptuneTomlClientVersion.ensureNeptuneToml(
            neptune,
            major
        )

        ensureExcludedFromNeptune(neptune)
    }

    private fun ensureExcludedFromNeptune(neptune: File) {
        val text = runCatching {
            neptune.readText()
        }.getOrNull() ?: return

        NeptuneTomlClientVersion.ensureExcludedDirectories(
            cs2Directory,
            text
        )
    }
}

internal object Cs2InstallBundles {

    /** The bundle to install for [major] / [subRevision], chosen from everything [Cs2BundleSource] knows. */
    fun resolve(major: Int, subRevision: Int?): Cs2BundleSource.Bundle? =
        pick(Cs2BundleSource.available(major), subRevision)

    fun pick(candidates: List<Cs2BundleSource.Bundle>, subRevision: Int?): Cs2BundleSource.Bundle? {
        if (candidates.isEmpty()) {
            return null
        }

        // Sub revision unknown: the generic <major>.zip if there is one, else the newest sub rev.
        if (subRevision == null || subRevision <= 0) {
            return candidates.firstOrNull { it.sub == 0 } ?: candidates.maxByOrNull { it.sub }
        }

        candidates.firstOrNull { it.sub == subRevision }?.let { return it }

        // A sub revision only gets a bundle when its client update changed scripts, so a missing one
        // means the scripts are still those of the sub revision before it. Going back is therefore
        // exact, while going forward would pull in changes this cache does not have; forward is only
        // a last resort for a sub revision older than every bundle. The candidates are for this major
        // revision alone, so the search can never cross into another one.
        val nearestBelow = candidates.filter { it.sub in 1 until subRevision }.maxByOrNull { it.sub }
        val nearestAbove = candidates.filter { it.sub > subRevision }.minByOrNull { it.sub }
        return nearestBelow ?: candidates.firstOrNull { it.sub == 0 } ?: nearestAbove
    }
}