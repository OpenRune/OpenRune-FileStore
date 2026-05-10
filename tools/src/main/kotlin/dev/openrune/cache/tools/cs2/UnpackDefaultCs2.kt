package dev.openrune.cache.tools.cs2

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.tools.TaskPriority
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.filesystem.Cache
import net.lingala.zip4j.ZipFile
import java.io.File

/**
 * Optional cache task: unpacks a bundled default Neptune CS2 project from the tools JAR into [cs2Directory].
 *
 * ```
 * tasks {
 *     +UnpackDefaultCs2(File("C:/work/my-cs2"))
 *     +PackCs2(File("C:/work/my-cs2"))
 * }
 * ```
 *
 * [PackCs2] must be registered in the same `tasks { }` block (enforced by [dev.openrune.cache.tools.CacheToolDsl]).
 * Cache revision must be at least [CacheTask.CS2_MIN_CACHE_REVISION].
 *
 * Bundles live at classpath `packcs2/install/{major}.zip` or `packcs2/install/{major}.{sub}.zip`.
 *
 * When [force] is false, unpacking is skipped only if [cs2Directory]/`neptune.toml` exists, its `client_version`
 * equals the cache [revision], and every path in `sources`, `symbols`, `libraries`, and `excluded` already exists
 * as a directory. Otherwise the bundle is extracted. Set [force] to true to always re-unpack.
 */
class UnpackDefaultCs2(
    private val cs2Directory: File,
    private val subRevision: Int? = null,
    private val force: Boolean = false,
) : CacheTask() {

    private val logger = InlineLogger()

    override val priority: TaskPriority get() = TaskPriority.CS2_LAST

    val cs2Root: File get() = cs2Directory

    override fun init(cache: Cache) {
        val major = revision
        if (major < 0) {
            logger.warn {
                "UnpackDefaultCs2: cache revision is unset (revision=$major); cannot choose a CS2 bundle. " +
                    "Set revision on the cache tool or ensure version.dat can be read."
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
        if (!force) {
            val text = runCatching { neptune.readText() }.getOrNull()
            if (text != null) {
                val existingRev = NeptuneTomlClientVersion.readClientVersionFromText(text)
                val layoutOk = NeptuneTomlClientVersion.allNeptunePathDirectoriesExist(cs2Directory, text)
                if (existingRev == major && layoutOk) {
                    logger.info {
                        "UnpackDefaultCs2: skip unpack; ${neptune.absolutePath} has client_version=$major and " +
                            "all neptune.toml directory paths exist (use force=true to re-unpack)."
                    }
                    ensureExcludedFromNeptune(neptune)
                    return
                }
            }
        }

        val loader = UnpackDefaultCs2::class.java.classLoader
        val bundleKey = Cs2InstallBundles.resolveBundleKey(major, subRevision, loader)
        if (bundleKey == null) {
            logger.warn {
                "UnpackDefaultCs2: no default CS2 bundle for revision $major on the classpath " +
                    "(expected packcs2/install/$major.zip or packcs2/install/$major.<sub>.zip). " +
                    "Add the zip under resources (packcs2/install/) " +
                    "or install a CS2 project manually under ${cs2Directory.absolutePath}"
            }
            return
        }

        val resourcePath = "packcs2/install/$bundleKey.zip"
        val stream = loader.getResourceAsStream(resourcePath)
        if (stream == null) {
            logger.warn {
                "UnpackDefaultCs2: classpath entry missing for $resourcePath " +
                    "(bundle id was resolved but the zip is not packaged). Install CS2 files manually."
            }
            return
        }

        cs2Directory.mkdirs()
        val tempZip = File.createTempFile("openrune-cs2-bundle-", ".zip")
        try {
            stream.use { input -> tempZip.outputStream().use { input.copyTo(it) } }
            ZipFile(tempZip).extractAll(cs2Directory.absolutePath)
            syncNeptuneClientVersion(major)
            logger.info { "UnpackDefaultCs2: unpacked $bundleKey into ${cs2Directory.absolutePath}" }
        } catch (e: Exception) {
            logger.error(e) { "UnpackDefaultCs2: failed to unpack $bundleKey: ${e.message}" }
        } finally {
            if (tempZip.exists()) tempZip.delete()
        }
    }

    private fun syncNeptuneClientVersion(major: Int) {
        val neptune = File(cs2Directory, "neptune.toml")
        NeptuneTomlClientVersion.ensureNeptuneToml(neptune, major)
        ensureExcludedFromNeptune(neptune)
    }

    private fun ensureExcludedFromNeptune(neptune: File) {
        val text = runCatching { neptune.readText() }.getOrNull() ?: return
        NeptuneTomlClientVersion.ensureExcludedDirectories(cs2Directory, text)
    }

}

internal object Cs2InstallBundles {

    fun resolveBundleKey(major: Int, subRevision: Int?, loader: ClassLoader): String? {
        val candidates = probeZips(major, loader)
        if (candidates.isEmpty()) return null

        if (subRevision != null) {
            val hit = candidates.find { it.sub == subRevision } ?: return null
            return hit.stem
        }

        val bestSub = candidates.maxOf { it.sub }
        return candidates.first { it.sub == bestSub }.stem
    }

    private data class BundleRef(val major: Int, val sub: Int, val stem: String)

    private fun probeZips(major: Int, loader: ClassLoader): List<BundleRef> = buildList {
        if (loader.getResource("packcs2/install/$major.zip") != null) {
            add(BundleRef(major, 0, "$major"))
        }
        for (sub in 1..999) {
            if (loader.getResource("packcs2/install/$major.$sub.zip") != null) {
                add(BundleRef(major, sub, "$major.$sub"))
            }
        }
    }
}
