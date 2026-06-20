package dev.openrune.cache.tools.cs2

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.CLIENTSCRIPT
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.tools.TaskPriority
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.progress
import dev.openrune.clientscript.compiler.ClientScripts
import dev.openrune.filesystem.Cache
import java.io.File

/**
 * Compiles Neptune CS2 sources and writes client scripts into the cache.
 *
 * Automatically installs or refreshes the bundled default CS2 project when:
 * - this is the first run,
 * - neptune.toml is missing,
 * - client_version differs from the cache revision,
 * - required directories from neptune.toml are missing.
 */
class PackCs2(private val cs2Dir: File) : CacheTask() {

    private val logger = InlineLogger()

    override val priority: TaskPriority
        get() = TaskPriority.CS2_LAST

    val cs2Root: File
        get() = cs2Dir

    override fun init(cache: Cache) {
        try {
            if (revision < 0) {
                logger.warn {
                    "PackCs2: cache revision is unset (-1). BuildCache must assign revision before CS2 tasks run."
                }
                return
            }

            if (revision < CacheTask.CS2_MIN_CACHE_REVISION) {
                logger.warn {
                    "PackCs2: cache revision $revision is not supported (minimum ${CacheTask.CS2_MIN_CACHE_REVISION})."
                }
                return
            }

            ensureInstalled(cache)

            val library = (cache as CacheDelegate).library
            val configFile = File(cs2Dir, "neptune.toml")

            if (!validateNeptuneLayout(configFile)) {
                return
            }

            NeptuneTomlClientVersion.patch(configFile, revision)

            SymDumper.dumpCacheVals(File(cs2Dir, "symbols"), cache, revision)

            CustomCs2OverrideSync(cs2Dir, revision).sync()

            val scripts = ClientScripts.compileTask(
                configFile.toPath(),
                revision
            )

            val progress = progress("Packing Cs2 Scripts", scripts.size)

            scripts.forEach { script ->
                if (!script.archiveName.contains(script.id.toString())) {
                    library.put(CLIENTSCRIPT, script.archiveName, script.bytes)
                } else {
                    library.put(CLIENTSCRIPT, script.id, script.bytes)
                }

                progress.step()
            }

            progress.close()
        } catch (e: Exception) {
            logger.error(e) {
                "PackCs2 failed"
            }
        }
    }

    private fun ensureInstalled(cache: Cache) {
        val neptune = File(cs2Dir, "neptune.toml")

        var needsInstall = !neptune.exists()

        if (!needsInstall) {
            val text = runCatching {
                neptune.readText()
            }.getOrNull()

            if (text == null) {
                needsInstall = true
            } else {
                val existingVersion =
                    NeptuneTomlClientVersion
                        .readClientVersionFromText(text)

                val layoutOk =
                    NeptuneTomlClientVersion
                        .allNeptunePathDirectoriesExist(
                            cs2Dir,
                            text
                        )

                needsInstall =
                    existingVersion != revision ||
                            !layoutOk
            }
        }

        if (!needsInstall) {
            return
        }

        logger.info {
            "PackCs2: CS2 project missing or outdated, unpacking bundled defaults."
        }

        val unpack = UnpackDefaultCs2(
            cs2Directory = cs2Dir,
            force = true
        )

        unpack.revision = revision
        unpack.init(cache)
    }

    private fun validateNeptuneLayout(
        configFile: File
    ): Boolean {
        if (!configFile.exists()) {
            logger.warn {
                "PackCs2: neptune.toml does not exist after installation."
            }
            return false
        }

        val text = runCatching {
            configFile.readText()
        }.getOrElse {
            logger.warn {
                "PackCs2: could not read neptune.toml: ${it.message}"
            }
            return false
        }

        NeptuneTomlClientVersion.ensureExcludedDirectories(
            cs2Dir,
            text
        )

        for (key in NeptuneTomlClientVersion.neptuneDirectoryArrayKeys) {
            for (rel in parseNeptuneStringArray(text, key)) {
                val dir = File(
                    cs2Dir,
                    rel.trimEnd('/', ' ')
                )

                if (dir.isDirectory) {
                    continue
                }

                if (!dir.mkdirs()) {
                    logger.warn {
                        "PackCs2: could not create $key directory ${dir.absolutePath}."
                    }
                    return false
                }

                logger.info {
                    "PackCs2: created missing $key directory ${dir.absolutePath}"
                }
            }
        }

        return true
    }

    private fun parseNeptuneStringArray(
        text: String,
        key: String
    ): List<String> {
        val match = Regex(
                """(?m)^\s*$key\s*=\s*\[(.*?)]\s*(?:#.*)?$"""
            ).find(text)
                ?: return emptyList()

        return match.groupValues[1]
            .split(',')
            .map {
                it.trim()
                    .removeSurrounding("\"")
                    .removeSurrounding("'")
                    .trim()
            }
            .filter {
                it.isNotEmpty()
            }
    }
}