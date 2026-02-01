package dev.openrune.cache.tools

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.CLIENTSCRIPT
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.tools.tasks.CacheTask
import readCacheRevision
import java.io.File
import java.nio.ByteBuffer
import kotlin.system.measureTimeMillis

class BuildCache(
    private val cacheLocation: File,
    private val tempLocation: File = File(cacheLocation, "temp"),
    val tasks: MutableList<CacheTask> = mutableListOf(),
    var revision: Int = -1,
    var serverPass: Boolean = false
) {

    private val logger = InlineLogger()

    /**
     * Entry point to build the cache.
     * Builds LIVE cache first, then optionally copies to SERVER cache.
     */
    fun initialize() {
        runBuild(cacheLocation, tempLocation,serverPass)
    }

    /**
     * Main cache build routine.
     * Handles temp backup, running tasks, rebuilding, and safe file replacement.
     */
    private fun runBuild(outputDir: File, temp: File, serverPass : Boolean) {
        temp.deleteRecursively()
        temp.mkdirs()

        try {
            outputDir.listFiles { f -> f.extension in listOf("dat", "idx") }
                ?.forEach { file ->
                    file.copyTo(File(temp, file.name), overwrite = true)
                }

            val library = CacheLibrary(outputDir.absolutePath)

            if (revision == -1) {
                val data = library.data(CLIENTSCRIPT, "version.dat")
                    ?: error("version.dat missing – set revision manually.")
                revision = readCacheRevision(
                    ByteBuffer.wrap(data),
                    "version.dat is invalid – set revision manually."
                )
            }

            logger.info {
                "Building ${if (serverPass) "Server " else ""}Cache (revision=$revision, tasks=${tasks.joinToString { it.javaClass.simpleName }})"
            }

            val time = measureTimeMillis {
                tasks.forEach { task ->
                    task.revision = revision
                    task.serverPass = serverPass
                    task.init(CacheDelegate(library))
                }
            }

            library.update()
            library.rebuild(File(temp, "rebuilt"))
            library.close()

            // Replace rebuilt files safely
            File(temp, "rebuilt")
                .listFiles { f -> f.extension in listOf("dat", "idx") }
                ?.forEach { rebuilt ->
                    replaceFile(rebuilt, File(outputDir, rebuilt.name))
                }

            logger.info { "Build finished in ${time}ms" }
        } finally {
            temp.deleteRecursively()
        }
    }

    private fun replaceFile(src: File, dst: File) {
        if (dst.exists() && !dst.delete()) {
            error("Failed to delete locked file: ${dst.absolutePath}")
        }
        src.copyTo(dst)
    }


}