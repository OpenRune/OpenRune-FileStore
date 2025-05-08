package dev.openrune

import com.github.michaelbull.logging.InlineLogger
import com.google.gson.Gson
import dev.openrune.cache.tools.CacheInfo
import dev.openrune.cache.util.progress
import dev.openrune.cache.util.stringToTimestamp
import dev.openrune.cache.util.toEchochUTC
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipException
import kotlin.system.exitProcess

object DownloadOSRS {

    private val logger = InlineLogger()

    private const val CACHE_DOWNLOAD_LOCATION = "https://archive.openrs2.org/caches.json"

    fun downloadCache(rev : Int, location : File) {
        val json = URL(CACHE_DOWNLOAD_LOCATION).readText()
        val caches: Array<CacheInfo> = Gson().fromJson(json, Array<CacheInfo>::class.java)
        val cacheInfo = if(rev == -1) getLatest(caches) else findRevision(rev,caches)

        downloadCache(location,cacheInfo)
    }


    private fun downloadCache(output : File,cache: CacheInfo) {
        try {
            val url = URL("https://archive.openrs2.org/caches/${cache.id}/disk.zip")
            val httpConnection = url.openConnection() as HttpURLConnection
            val completeFileSize = cache.size
            val input: InputStream = httpConnection.inputStream
            val out = FileOutputStream(output)

            val data = ByteArray(1024)
            var downloadedFileSize: Long = 0
            var count: Int

            val pb = progress("Downloading Cache", completeFileSize)

            while (input.read(data, 0, 1024).also { count = it } != -1) {
                downloadedFileSize += count.toLong()
                pb.stepBy(count.toLong())
                out.write(data, 0, count)
            }
            pb.close()
        } catch (e: Exception) {
            logger.error { "Unable to download Cache: $e" }
            exitProcess(0)
        }

    }

    fun unZip(path: File, output: String) {
        val zipFile = ZipFile(path)
        try {
            logger.info { "Unzipping Cache please wait" }
            println("OUTPUT: ${output}")
            zipFile.extractAll(output)
        } catch (e: ZipException) {
            logger.error { "Unable extract files from $path : $e" }
        }
    }

    private fun getLatest(caches: Array<CacheInfo>) = caches
        .filter { it.game.contains("oldschool") }
        .filter { it.builds.isNotEmpty() }
        .filter { it.timestamp != null }
        .maxByOrNull { it.timestamp.stringToTimestamp().toEchochUTC()  } ?: error("Unable to find Latest Revision")


    private fun findRevision(rev: Int, caches: Array<CacheInfo>) = caches
        .filter { it.game.contains("oldschool") }
        .filter { it.builds.isNotEmpty() && it.builds[0].major == rev }
        .filter { it.timestamp != null }
        .maxByOrNull { it.timestamp.stringToTimestamp().toEchochUTC()   } ?: error("Unable to find Latest Revision: $rev")

}