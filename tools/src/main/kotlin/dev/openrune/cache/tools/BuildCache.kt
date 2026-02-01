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
                "Building ${if (serverPass) "Server " else ""}Cache (revision=$revision, tasks=${tasks.joinToString { it.javaClass.simpleName }})"
            }

            val time = measureTimeMillis {
                tasks.forEach { task ->
                    task.revision = revision
                    task.serverPass = serverPass
                    task.init(CacheDelegate(library))
                }
            }



            logger.info { "Tasks Finished In: ${formatTime(time)}" }
            logger.info { "Cleaning Up..." }

            library.update()
            library.rebuild(File(tempLocation, "rebuilt"))
            library.close()


            File(tempLocation, "rebuilt").listFiles { file -> file.extension in listOf("dat", "idx") }?.forEach { file ->
                file.copyTo(File(tempLocation, file.name), overwrite = true)
            }

            logger.info { "Build finished in ${formatTime(time)}" }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            tempLocation.deleteRecursively()
        }
    }

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