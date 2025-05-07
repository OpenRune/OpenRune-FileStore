package dev.openrune.cache.tools

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.impl.RemoveBzip
import dev.openrune.cache.tools.tasks.impl.RemoveXteas
import dev.openrune.cache.util.progress
import me.tongfei.progressbar.ProgressBar
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.system.measureTimeMillis

class FreshCache(
    private val downloadLocation: File,
    private val output: File = downloadLocation,
    val tasks: MutableList<CacheTask> = mutableListOf(),
    val revision: Int = -1,
    private val removeXteas: Boolean = false,
    private val removeBzip: Boolean = false
) {

    private val logger = InlineLogger()

    fun initialize() {
        val time = measureTimeMillis {
            if (removeBzip) tasks.add(0, RemoveBzip())
            if (removeXteas) tasks.add(1, RemoveXteas(File(downloadLocation, "xteas.json")))

            logger.info { "Downloading Cache (revision=$revision, Tasks: ${tasks.joinToString(", ") { it.javaClass.simpleName }})" }
            logger.info { "Getting Xteas..." }

            OpenRS2.downloadKeysByRevision(revision = revision, directory = downloadLocation)
            OpenRS2.downloadCacheByRevision(revision = revision, directory = downloadLocation, listener = downloadListener)
        }
        val hours = time / 3600000
        val minutes = (time % 3600000) / 60000
        val seconds = (time % 60000) / 1000

        val timeString = buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (seconds > 0) append("${seconds}s")
        }

        logger.info { "Fresh Cache Finished In: $timeString" }
    }

    fun unzip(zipFile: File, destDir: File): Boolean {
        return try {
            if (!destDir.exists()) destDir.mkdirs()

            val zipInputStream = ZipInputStream(FileInputStream(zipFile))
            var zipEntry: ZipEntry?

            while (zipInputStream.nextEntry.also { zipEntry = it } != null) {
                val outputFile = File(destDir, zipEntry!!.name.substringAfterLast('/'))
                if (!zipEntry!!.isDirectory) {
                    outputFile.parentFile?.mkdirs()
                    FileOutputStream(outputFile).use { fileOutputStream ->
                        zipInputStream.copyTo(fileOutputStream)
                    }
                }
                zipInputStream.closeEntry()
            }
            zipInputStream.close()
            logger.info { "Unzipped successfully" }
            true
        } catch (e: IOException) {
            logger.error { "Error while unzipping: ${e.message}" }
            false
        }
    }

    private val downloadListener = object : DownloadListener {
        var progressBar: ProgressBar? = null

        override fun onProgress(progress: Int, max: Long, current: Long) {
            if (progressBar == null) {
                progressBar = progress("Downloading Cache", max)
            } else {
                progressBar?.stepTo(current)
            }
        }

        override fun onError(exception: Exception) {
            error("Error Downloading: $exception")
        }

        override fun onFinished() {
            progressBar?.close()
            val zipLoc = File(downloadLocation, "disk.zip")
            if (unzip(zipLoc, downloadLocation)) {
                logger.info { "Cache downloaded and unzipped successfully." }
                if (tasks.isNotEmpty()) {
                    BuildCache(input = downloadLocation, output = output, tempLocation = File(output, "temp"), tasks = tasks).initialize()
                }
                zipLoc.delete()
            } else {
                error("Error Unzipping")
            }
        }
    }

}