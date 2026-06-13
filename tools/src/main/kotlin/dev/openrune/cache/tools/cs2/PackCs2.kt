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
 * Compiles Neptune CS2 sources and writes client scripts into the cache via [CacheDelegate.library].
 *
 * Expects [cs2Dir] to contain `neptune.toml` and the directories its `sources`, `symbols`, `libraries`, and `excluded` arrays name.
 * Pair with [UnpackDefaultCs2] on the same directory in the cache tool DSL.
 */
class PackCs2(private val cs2Dir: File) : CacheTask() {

    private val logger = InlineLogger()

    override val priority: TaskPriority get() = TaskPriority.CS2_LAST

    val cs2Root: File get() = cs2Dir

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

            val library = (cache as CacheDelegate).library
            val configFile = File(cs2Dir, "neptune.toml")
            if (!validateNeptuneLayout(configFile)) {
                return
            }

            NeptuneTomlClientVersion.patch(configFile, revision)

            SymDumper.dumpCacheVals(File(cs2Dir, "symbols"), cache, revision)

            val scripts = ClientScripts.compileTask(configFile.toPath(), revision)
            val progress = progress("Packing Cs2 Scripts", scripts.size)

            scripts.forEach { script ->

                if (!script.archiveName.contains(script.id.toString())) {
                    library.put(CLIENTSCRIPT, script.id, script.archiveName, script.bytes)
                } else {
                    library.put(CLIENTSCRIPT, script.id, script.bytes)
                }
                progress.step()
            }

            progress.close()
        } catch (e: Exception) {
            logger.error(e) { "PackCs2 failed" }
        }
    }

    private fun validateNeptuneLayout(configFile: File): Boolean {
        if (!configFile.exists()) {
            logger.warn {
                "PackCs2: neptune.toml not found at ${configFile.absolutePath}. " +
                    "Run a fresh install: add UnpackDefaultCs2 (or your own CS2 bootstrap) before PackCs2 in your cache " +
                    "task list, or create neptune.toml and the CS2 directory layout under ${cs2Dir.absolutePath}."
            }
            return false
        }

        val text = runCatching { configFile.readText() }.getOrElse {
            logger.warn { "PackCs2: could not read neptune.toml: ${it.message}" }
            return false
        }

        NeptuneTomlClientVersion.ensureExcludedDirectories(cs2Dir, text)

        for (key in NeptuneTomlClientVersion.neptuneDirectoryArrayKeys) {
            for (rel in parseNeptuneStringArray(text, key)) {
                val dir = File(cs2Dir, rel.trimEnd('/', ' '))
                if (dir.isDirectory) continue
                if (key == "sources") {
                    if (!dir.mkdirs()) {
                        logger.warn {
                            "PackCs2: neptune.toml lists sources path \"$rel\" but could not create directory " +
                                "${dir.absolutePath} (exists as a file or permission denied)."
                        }
                        return false
                    }
                    logger.info { "PackCs2: created missing sources directory ${dir.absolutePath}" }
                } else {
                    logger.warn {
                        "PackCs2: neptune.toml lists `$key` path \"$rel\" but that directory is missing " +
                            "(expected ${dir.absolutePath}). Run a fresh install (UnpackDefaultCs2 before PackCs2) " +
                            "or create the folders your config references."
                    }
                    return false
                }
            }
        }

        return true
    }

    private fun parseNeptuneStringArray(text: String, key: String): List<String> {
        val m = Regex("""(?m)^\s*$key\s*=\s*\[(.*?)]\s*(?:#.*)?$""").find(text) ?: return emptyList()
        return m.groupValues[1].split(',')
            .map { it.trim().removeSurrounding("\"").removeSurrounding("'").trim() }
            .filter { it.isNotEmpty() }
    }
}
