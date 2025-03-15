package dev.openrune.cache.tools

import com.google.gson.Gson
import dev.openrune.cache.util.stringToTimestamp
import dev.openrune.cache.util.toEchochUTC
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

enum class GameType {
    DARKSCAPE, DOTD, OLDSCHOOL, RUNESCAPE;

    fun formatName() = name.lowercase()
}

interface DownloadListener {
    fun onProgress(progress: Int, max : Long, current : Long)
    fun onError(exception: Exception)
    fun onComplete()
    fun onFinished()
}

object OpenRS2 {
    private const val CACHE_DOWNLOAD_LOCATION = "https://archive.openrs2.org/caches.json"
    private var allCaches: Array<CacheInfo> = emptyArray()

    private fun loadCaches() {
        if (allCaches.isEmpty()) {
            val json = URL(CACHE_DOWNLOAD_LOCATION).readText()
            allCaches = Gson().fromJson(json, Array<CacheInfo>::class.java)
        }
    }

    fun downloadCacheByRevision(revision: Int, directory: File,type: GameType = GameType.OLDSCHOOL, listener: DownloadListener? = null) {
        loadCaches()
        downloadCacheByInternalID(findRevision(revision,type).id,directory, listener)
    }

    fun downloadKeysByRevision(revision: Int, directory: File, type: GameType = GameType.OLDSCHOOL, listener: DownloadListener? = null) {
        loadCaches()
        downloadKeysByInternalID(directory, findRevision(revision,type).id, listener)
    }

    fun downloadCacheByInternalID(target: Int, directory: File, listener: DownloadListener? = null) {
        if (!directory.exists()) {
            directory.mkdirs()
        }
        downloadZip(target,"https://archive.openrs2.org/caches/runescape/${target}/disk.zip", directory.path, listener)
    }

    fun downloadKeysByInternalID(directory: File, target: Int, listener: DownloadListener? = null) {
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = directory.resolve("${target}-keys.json")
        if (!file.exists()) {
            val url = URL("https://archive.openrs2.org/caches/runescape/${target}/keys.json")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            try {
                connection.inputStream.bufferedReader().use { reader ->
                    val text = reader.readText()
                    file.writeText(text)
                }
                listener?.onFinished()
            } catch (e: IOException) {
                listener?.onError(e)
            } finally {
                listener?.onComplete()
                connection.disconnect()
            }
        }
    }


    private fun downloadZip(target: Int,url: String, destinationDirectory: String, listener: DownloadListener? = null) {
        try {
            loadCaches()
            val urlConnection = URL(url).openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            urlConnection.connect()

            val contentLength = allCaches.first { it.id == target }.size
            val inputStream = BufferedInputStream(urlConnection.inputStream)
            val file = File(destinationDirectory, url.substringAfterLast("/"))
            val outputStream = FileOutputStream(file)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                val progress = ((totalBytesRead * 100) / contentLength).toInt()
                listener?.onProgress(progress,contentLength, totalBytesRead)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            urlConnection.disconnect()

            listener?.onComplete()
        } catch (e: Exception) {
            listener?.onError(e)
        } finally {
            listener?.onFinished()
        }
    }

    private fun getLatest(caches: Array<CacheInfo>, game: GameType = GameType.OLDSCHOOL) =
        caches
            .filter { it.game.contains(game.formatName()) }
            .filter { it.builds.isNotEmpty() }
            .filter { it.timestamp != null }
            .maxByOrNull { it.timestamp.stringToTimestamp().toEchochUTC() }
            ?: error("Unable to find Latest Revision")

    private fun findRevision(rev: Int, game: GameType = GameType.OLDSCHOOL) =
        allCaches
            .filter { it.game.contains(game.formatName()) }
            .filter { it.builds.isNotEmpty() && it.builds[0].major == rev }
            .filter { it.timestamp != null }
            .maxByOrNull { it.timestamp.stringToTimestamp().toEchochUTC() }
            ?: error("Unable to find Revision: $rev")
}
