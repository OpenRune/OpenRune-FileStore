package dev.openrune.cache.tools

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.tools.OpenRS2.findRevision
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
    val tasks: MutableList<CacheTask> = mutableListOf(),
    val revision: Int = -1,
    val subRev: Int = -1,
    val cacheEnvironment: CacheEnvironment = CacheEnvironment.LIVE
) {

    private val logger = InlineLogger()
    private lateinit var revisionInfo: CacheInfo

    fun initialize() {
        OpenRS2.loadCaches()
        revisionInfo = findRevision(revision, subRev, GameType.OLDSCHOOL, cacheEnvironment)

        val uuid = revisionInfo.id.toString()
        val cacheDir = CachePaths.cacheDir(GameType.OLDSCHOOL, revision)

        if (!isCacheValid(cacheDir, uuid)) {
            if (cacheDir.exists()) {
                logger.warn { "Cache corrupted or incomplete. Deleting ${cacheDir.absolutePath}" }
                cacheDir.deleteRecursively()
            }
        }

        if (isCachePresent(cacheDir, uuid)) {
            logger.info { "Using cached zip for revision $revision ($uuid)" }

            prepareWorkingDirectory()

            unzip(File(cacheDir, "$uuid-disk.zip"), cacheOutput)

            File(cacheDir, "$uuid-xteas.json")
                .copyTo(File(cacheOutput, "xteas.json"), overwrite = true)

            runTasks()
            return
        }

        download(cacheDir)
    }

    private fun isCachePresent(dir: File, uuid: String): Boolean {
        return File(dir, "$uuid-disk.zip").exists() &&
                File(dir, "$uuid-xteas.json").exists()
    }

    private fun isCacheValid(dir: File, uuid: String): Boolean {
        val zip = File(dir, "$uuid-disk.zip")
        val xteas = File(dir, "$uuid-xteas.json")

        if (!zip.exists() || !xteas.exists()) return false
        if (zip.length() < 1024) return false

        return isZipValid(zip)
    }

    private fun isZipValid(zipFile: File): Boolean {
        return try {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var hasEntries = false

                while (zis.nextEntry != null) {
                    hasEntries = true
                    zis.closeEntry()
                }

                hasEntries
            }
        } catch (e: Exception) {
            logger.warn { "Invalid zip detected: ${zipFile.absolutePath}" }
            false
        }
    }

    private fun download(cacheDir: File) {
        val time = measureTimeMillis {
            logger.info {
                "Downloading Cache (revision=$revision, id=${revisionInfo.id}, Tasks=${
                    tasks.joinToString(", ") { it.javaClass.simpleName }
                })"
            }

            cacheDir.mkdirs()

            OpenRS2.downloadByInternalID(
                target = revisionInfo.id,
                directory = cacheDir,
                listener = downloadListener,
                remoteFileName = "disk.zip",
                localFileName = "${revisionInfo.id}-disk.zip"
            )

            OpenRS2.downloadByInternalID(
                target = revisionInfo.id,
                directory = cacheDir,
                listener = downloadListener,
                remoteFileName = "keys.json",
                localFileName = "${revisionInfo.id}-xteas.json"
            )
        }

        val hours = time / 3600000
        val minutes = (time % 3600000) / 60000
        val seconds = (time % 60000) / 1000

        val timeString = buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (seconds > 0) append("${seconds}s")
        }

        logger.info { "Download Finished In: $timeString" }
    }

    private fun prepareWorkingDirectory() {
        if (cacheOutput.exists()) {
            cacheOutput.deleteRecursively()
        }
        cacheOutput.mkdirs()
    }

    fun unzip(zipFile: File, destDir: File): Boolean {
        return try {
            if (!destDir.exists()) destDir.mkdirs()

            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry?

                while (zis.nextEntry.also { entry = it } != null) {
                    val normalizedName = entry!!.name.substringAfter('/')
                    val outFile = File(destDir, normalizedName)

                    if (entry!!.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }

                    zis.closeEntry()
                }
            }

            logger.info { "Unzipped successfully" }
            true
        } catch (e: IOException) {
            logger.error { "Error while unzipping: ${e.message}" }
            false
        }
    }

    private fun runTasks() {
        if (tasks.isNotEmpty()) {
            BuildCache(
                cacheLocation = cacheOutput,
                tasks = tasks,
                revision = revision
            ).initialize()
        }
    }

    private val downloadListener = object : DownloadListener {

        var progressBar: ProgressBar? = null
        private var completedDownloads = 0

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

            completedDownloads++
            if (completedDownloads < 2) return

            val uuid = revisionInfo.id.toString()
            val cacheDir = CachePaths.cacheDir(GameType.OLDSCHOOL, revision)

            val zip = File(cacheDir, "$uuid-disk.zip")
            val xteas = File(cacheDir, "$uuid-xteas.json")

            try {
                prepareWorkingDirectory()

                unzip(zip, cacheOutput)

                xteas.copyTo(File(cacheOutput, "xteas.json"), overwrite = true)

                runTasks()
            } catch (e: Exception) {
                logger.error(e) { "Error preparing cache" }
                error("Error Preparing Cache: ${e.message}")
            }
        }
    }
}

object CachePaths {

    fun baseDir(): File {
        val os = System.getProperty("os.name").lowercase()
        val home = System.getProperty("user.home")

        return when {
            os.contains("win") -> File(System.getenv("APPDATA"), "openrune")
            os.contains("mac") -> File(home, "Library/Application Support/openrune")
            else -> File(home, ".openrune")
        }
    }

    fun cacheDir(gameType: GameType, revision: Int): File {
        return File(baseDir(), "caches/${gameType.name.lowercase()}/$revision")
    }
}