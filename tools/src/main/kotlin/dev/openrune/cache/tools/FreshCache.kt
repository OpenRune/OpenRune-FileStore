package dev.openrune.cache.tools

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.tools.tasks.CacheTask
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
    private val cacheOutput: File,
    private val serverOutput: File? = null,
    val tasks: MutableList<CacheTask> = mutableListOf(),
    val revision: Int = -1,
    val subRev: Int = -1,
    val cacheEnvironment : CacheEnvironment = CacheEnvironment.LIVE
) {

    private val logger = InlineLogger()

    fun initialize() {
        val time = measureTimeMillis {

            logger.info { "Downloading Cache (revision=$revision, Tasks: ${tasks.joinToString(", ") { it.javaClass.simpleName }})" }

            OpenRS2.downloadKeysByRevision(revision = revision, directory = cacheOutput,environment = cacheEnvironment, subRev = subRev)
            OpenRS2.downloadCacheByRevision(revision = revision, directory = cacheOutput, environment = cacheEnvironment,subRev = subRev,listener = downloadListener)
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
            val zipLoc = File(cacheOutput, "disk.zip")
            try {

                val success = unzip(zipLoc, cacheOutput)
                if (success) {
                    zipLoc.delete()
                    if (tasks.isNotEmpty()) {
                        BuildCache(
                            cacheLocation = cacheOutput,
                            tasks = tasks,
                            revision = revision
                        ).initialize()
                    }
                } else {
                    logger.error { "Failed to unzip ${zipLoc.absolutePath} to $cacheOutput" }
                }
            } catch (e: Exception) {
                logger.error(e) { "Exception while unzipping ${zipLoc.absolutePath}" }
                error("Error Unzipping: ${e.message}")
            }
        }
    }

}