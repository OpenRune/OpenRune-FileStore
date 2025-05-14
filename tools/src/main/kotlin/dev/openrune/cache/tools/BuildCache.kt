package dev.openrune.cache.tools

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.tools.tasks.CacheTask
import java.io.File
import kotlin.system.measureTimeMillis

class BuildCache(
    private val input: File,
    private val output: File = input,
    private val tempLocation: File = File(output, "temp"),
    val tasks: MutableList<CacheTask> = mutableListOf(),
    val revision: Int = -1,
) {

    private val logger = InlineLogger()

    fun initialize() {
        try {
            logger.info { "Building Cache (Tasks: ${tasks.joinToString(", ") { it.javaClass.simpleName }})" }

            input.listFiles { file -> file.extension in listOf("dat", "idx") }?.forEach { file ->
                file.copyTo(File(tempLocation, file.name), overwrite = true)
            }

            val library = CacheLibrary(input.absolutePath)

            val time = measureTimeMillis {
                tasks.forEach { task ->
                    task.revision = revision
                    task.init(CacheDelegate(library))
                }
            }

            val hours = time / 3600000
            val minutes = (time % 3600000) / 60000
            val seconds = (time % 60000) / 1000

            val timeString = buildString {
                if (hours > 0) append("${hours}h ")
                if (minutes > 0) append("${minutes}m ")
                if (seconds > 0) append("${seconds}s")
            }

            logger.info { "Tasks Finished In: $timeString" }
            logger.info { "Cleaning Up..." }

            library.update()
            library.rebuild(File(tempLocation, "rebuilt"))
            library.close()

            File(tempLocation, "rebuilt").listFiles { file -> file.extension in listOf("dat", "idx") }?.forEach { file ->
                file.copyTo(File(tempLocation, file.name), overwrite = true)
            }

        } catch (ex: Exception) {
            println("Error occurred during initialization: ${ex.message}")
        } finally {
            tempLocation.deleteRecursively()
        }
    }
}