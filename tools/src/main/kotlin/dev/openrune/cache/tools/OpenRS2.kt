package dev.openrune.cache.tools

import com.google.gson.Gson
import dev.openrune.cache.util.stringToTimestamp
import dev.openrune.cache.util.toEchochUTC
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter

enum class GameType {
    DARKSCAPE, DOTD, OLDSCHOOL, RUNESCAPE;

    fun formatName() = name.lowercase()
}

interface DownloadListener {
    fun onProgress(progress: Int, max: Long, current: Long)
    fun onError(exception: Exception)
    fun onFinished()
}

object OpenRS2 {
    private const val CACHE_DOWNLOAD_LOCATION = "https://archive.openrs2.org/caches.json"
    private val gson = Gson()
    private val formatter = DateTimeFormatter.ISO_INSTANT

    @Volatile
    var allCaches: Array<CacheInfo> = emptyArray()

    fun loadCaches() {
        if (allCaches.isEmpty()) {
            synchronized(this) {
                if (allCaches.isEmpty()) {
                    val json = URL(CACHE_DOWNLOAD_LOCATION).readText()
                    allCaches = gson.fromJson(json, Array<CacheInfo>::class.java)
                }
            }
        }
    }

    fun downloadCacheByRevision(
        revision: Int,
        directory: File,
        type: GameType = GameType.OLDSCHOOL,
        environment: CacheEnvironment,
        subRev: Int = -1,
        listener: DownloadListener? = null
    ) {
        loadCaches()
        val info = findRevision(revision, subRev, type, environment)
        downloadByInternalID(info.id, directory, listener, "disk.zip")
    }

    fun downloadKeysByRevision(
        revision: Int,
        directory: File,
        type: GameType = GameType.OLDSCHOOL,
        environment: CacheEnvironment,
        subRev: Int,
        listener: DownloadListener? = null
    ) {
        loadCaches()
        val info = findRevision(revision, subRev, type, environment)
        downloadByInternalID(info.id, directory, listener, "xteas.json")
    }

    fun downloadByInternalID(
        target: Int,
        directory: File,
        listener: DownloadListener?,
        fileName: String
    ) {
        directory.mkdirs()
        val url = if (fileName.endsWith(".zip")) {
            "https://archive.openrs2.org/caches/runescape/$target/$fileName"
        } else {
            "https://archive.openrs2.org/caches/runescape/$target/keys.json"
        }
        val expectedLength = if (fileName.endsWith(".zip")) allCaches.firstOrNull { it.id == target }?.size ?: -1 else -1
        downloadFile(url, directory.resolve(fileName), expectedLength, listener)
    }

    private fun downloadFile(
        urlString: String,
        destination: File,
        expectedLength: Long,
        listener: DownloadListener?
    ) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 60_000
                connect()
            }

            val totalBytes = if (expectedLength > 0) expectedLength else connection.contentLengthLong

            // Only print warning for ZIPs
            if (fileIsZip(destination) && totalBytes <= 0) {
                println("Warning: Unknown content length for $urlString")
            }

            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var totalRead = 0L
                    var lastProgress = -1
                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val progress = ((totalRead * 100) / totalBytes).toInt()
                            if (progress != lastProgress) {
                                lastProgress = progress
                                listener?.onProgress(progress, totalBytes, totalRead)
                            }
                        }
                    }
                }
            }
            listener?.onFinished()
        } catch (e: Exception) {
            listener?.onError(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun fileIsZip(file: File) = file.extension.lowercase() == "zip"

    fun getLatest(caches: Array<CacheInfo>, game: GameType = GameType.OLDSCHOOL): CacheInfo =
        caches.asSequence()
            .filter { it.game.contains(game.formatName()) }
            .filter { it.builds.isNotEmpty() && it.timestamp.isNotBlank() && it.environment == "live" }
            .maxByOrNull { it.timestamp.stringToTimestamp().toEchochUTC() }
            ?: error("Unable to find latest revision for $game")

    fun findRevision(
        rev: Int,
        subRev: Int = -1,
        game: GameType = GameType.OLDSCHOOL,
        environment: CacheEnvironment = CacheEnvironment.LIVE
    ): CacheInfo {
        val candidates = allCaches.asSequence()
            .filter { it.game.contains(game.formatName()) }
            .filter { it.builds.isNotEmpty() && it.builds[0].major == rev }
            .filter { it.timestamp.isNotBlank() && it.size > 0 }
            .filter { it.environment == environment.toString().lowercase() }
            .sortedBy { Instant.from(formatter.parse(it.timestamp)).toEpochMilli() }
            .toList()

        if (candidates.isEmpty()) error("Unable to find Revision $rev for environment $environment")

        return when {
            subRev == -1 -> candidates.lastOrNull() ?: error("No candidates found for revision $rev")
            subRev == 0 -> candidates.getOrNull(0) ?: error("No sub revisions available for revision $rev")
            subRev in 1..candidates.size -> candidates[subRev - 1]
            else -> candidates.lastOrNull() ?: error("No latest candidate found for revision $rev")
        }
    }
}