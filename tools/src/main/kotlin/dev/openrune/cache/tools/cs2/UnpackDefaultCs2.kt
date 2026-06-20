package dev.openrune.cache.tools.cs2

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.tools.TaskPriority
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.filesystem.Cache
import net.lingala.zip4j.ZipFile
import java.io.File

class UnpackDefaultCs2(
    private val cs2Directory: File,
    private val subRevision: Int? = null,
    private val force: Boolean = false,
) : CacheTask() {

    private val logger = InlineLogger()

    override val priority: TaskPriority
        get() = TaskPriority.CS2_LAST

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

                val layoutOk =
                    NeptuneTomlClientVersion
                        .allNeptunePathDirectoriesExist(
                            cs2Directory,
                            text
                        )

                if (existingRev == major && layoutOk) {
                    logger.info {
                        "UnpackDefaultCs2: skip unpack; ${neptune.absolutePath} has client_version=$major and all neptune.toml directory paths exist."
                    }

                    ensureExcludedFromNeptune(neptune)
                    return
                }

                versionChanged = existingRev != major
            } else {
                versionChanged = true
            }
        }

        if (firstInstall || versionChanged || force) {
            logger.info {
                "UnpackDefaultCs2: performing fresh install " +
                        "(firstInstall=$firstInstall, " +
                        "versionChanged=$versionChanged, " +
                        "force=$force)"
            }

            wipeDirectory(cs2Directory)
            cs2Directory.mkdirs()
        }

        val loader = UnpackDefaultCs2::class.java.classLoader

        val bundleKey = Cs2InstallBundles.resolveBundleKey(
            major,
            subRevision,
            loader
        )

        if (bundleKey == null) {
            logger.warn {
                "UnpackDefaultCs2: no default CS2 bundle for revision $major on the classpath " +
                        "(expected packcs2/install/$major.zip or packcs2/install/$major.<sub>.zip). " +
                        "Install a CS2 project manually under ${cs2Directory.absolutePath}"
            }
            return
        }

        val resourcePath = "packcs2/install/$bundleKey.zip"

        val stream = loader.getResourceAsStream(resourcePath)
        if (stream == null) {
            logger.warn {
                "UnpackDefaultCs2: classpath entry missing for $resourcePath."
            }
            return
        }

        val tempZip =
            File.createTempFile(
                "openrune-cs2-bundle-",
                ".zip"
            )

        try {
            stream.use { input ->
                tempZip.outputStream().use {
                    input.copyTo(it)
                }
            }

            ZipFile(tempZip).use { zip ->
                Cs2BundleExtract.extract(
                    zip,
                    cs2Directory
                )
            }

            syncNeptuneClientVersion(major)

            logger.info {
                "UnpackDefaultCs2: unpacked $bundleKey into ${cs2Directory.absolutePath}"
            }
        } catch (e: Exception) {
            logger.error(e) {
                "UnpackDefaultCs2: failed to unpack $bundleKey: ${e.message}"
            }
        } finally {
            tempZip.delete()
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

            if (!it.delete()) {
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

    fun resolveBundleKey(
        major: Int,
        subRevision: Int?,
        loader: ClassLoader
    ): String? {
        val candidates = probeZips(major, loader)
        if (candidates.isEmpty()) {
            return null
        }

        if (subRevision != null) {
            return candidates.firstOrNull { it.sub == subRevision }?.stem
        }

        candidates.firstOrNull { it.sub == 0 }?.let { return it.stem }

        return candidates
            .maxByOrNull { it.sub }
            ?.stem
    }

    private data class BundleRef(
        val major: Int,
        val sub: Int,
        val stem: String
    )

    private fun probeZips(
        major: Int,
        loader: ClassLoader
    ): List<BundleRef> {
        val bundles = mutableListOf<BundleRef>()

        // packcs2/install/225.zip
        if (loader.getResource("packcs2/install/$major.zip") != null) {
            bundles += BundleRef(
                major = major,
                sub = 0,
                stem = "$major"
            )
        }

        for (sub in 1..999) {
            val path = "packcs2/install/$major.$sub.zip"

            if (loader.getResource(path) != null) {
                bundles += BundleRef(
                    major = major,
                    sub = sub,
                    stem = "$major.$sub"
                )
            }
        }

        return bundles
    }
}