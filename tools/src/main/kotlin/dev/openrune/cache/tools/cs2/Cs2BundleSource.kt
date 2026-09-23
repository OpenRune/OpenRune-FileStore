package dev.openrune.cache.tools.cs2

import com.github.michaelbull.logging.InlineLogger
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import dev.openrune.cache.tools.CachePaths
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Where the default CS2 project bundles (`<rev>.<sub>.zip`) come from.
 *
 * Bundles are hosted on the OpenRune CDN and described by a `manifest.json` next to them, so the jar no
 * longer ships them. A bundle is downloaded once into the OpenRune app data directory and verified
 * against the manifest's sha256 before use. A zip under `packcs2/install/` on the classpath still wins,
 * which is how a locally built bundle can be tried before it is uploaded.
 *
 * Override the CDN with `-Dopenrune.cs2.bundles=<url>`.
 */
internal object Cs2BundleSource {

    private val logger = InlineLogger()

    const val DEFAULT_BASE_URL = "https://cdn.openrune.dev/cs2-bundles/"
    const val MANIFEST_FILE = "manifest.json"
    const val CLASSPATH_DIR = "packcs2/install"

    val baseUrl: String
        get() = (System.getProperty("openrune.cs2.bundles") ?: DEFAULT_BASE_URL).let { if (it.endsWith("/")) it else "$it/" }

    /**
     * Where downloaded bundles are kept: `cs2-bundles` under the same OpenRune directory the cache
     * downloads go to (`%APPDATA%/openrune` on Windows), so every project on the machine shares one
     * copy of each bundle.
     */
    val downloadDir: File
        get() = File(CachePaths.baseDir(), DOWNLOAD_DIR_NAME)

    const val DOWNLOAD_DIR_NAME = "cs2-bundles"

    data class Bundle(val name: String, val rev: Int, val sub: Int, val file: String, val size: Long, val sha256: String?)

    private var manifest: List<Bundle>? = null
    private var manifestFailed = false

    /** Every bundle known for [major]: manifest entries plus whatever is on the classpath. */
    fun available(major: Int): List<Bundle> {
        val fromManifest = loadManifest().filter { it.rev == major }
        val fromClasspath = classpathBundles(major).filter { cp -> fromManifest.none { it.name == cp.name } }
        return fromManifest + fromClasspath
    }

    /**
     * The bundle zip for [bundle], ready to read. Classpath first, then [downloadDir], then the CDN
     * (saved into [downloadDir] for next time). Returns null if it cannot be obtained.
     */
    fun fetch(bundle: Bundle): File? {
        val loader = Cs2BundleSource::class.java.classLoader
        loader.getResourceAsStream("$CLASSPATH_DIR/${bundle.file}")?.let { stream ->
            val temp = File.createTempFile("openrune-cs2-bundle-", ".zip")
            stream.use { input -> temp.outputStream().use { input.copyTo(it) } }
            temp.deleteOnExit()
            return temp
        }

        val target = File(downloadDir, bundle.file)
        if (target.isFile && verified(target, bundle)) {
            logger.debug { "Using cached CS2 bundle ${target.absolutePath}" }
            return target
        }

        val url = baseUrl + bundle.file
        logger.info { "Downloading CS2 bundle ${bundle.name} from $url" }
        target.parentFile.mkdirs()
        val partial = File(target.path + ".part")
        try {
            open(url).use { connection ->
                connection.inputStream.use { input -> partial.outputStream().use { input.copyTo(it) } }
            }
            if (!verified(partial, bundle)) {
                logger.warn { "CS2 bundle ${bundle.name} did not match the manifest checksum; discarding download" }
                partial.delete()
                return null
            }
            target.delete()
            if (!partial.renameTo(target)) partial.copyTo(target, overwrite = true).also { partial.delete() }
            logger.info { "Downloaded CS2 bundle ${bundle.name} (${target.length() / 1024 / 1024} MB) to ${target.absolutePath}" }
            return target
        } catch (e: Exception) {
            logger.warn(e) { "Could not download CS2 bundle ${bundle.name} from $url" }
            partial.delete()
            return null
        }
    }

    private fun verified(file: File, bundle: Bundle): Boolean {
        if (bundle.size > 0 && file.length() != bundle.size) return false
        val expected = bundle.sha256 ?: return true
        return sha256(file).equals(expected, ignoreCase = true)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Synchronized
    private fun loadManifest(): List<Bundle> {
        manifest?.let { return it }
        if (manifestFailed) return emptyList()
        val url = baseUrl + MANIFEST_FILE
        return try {
            val text = open(url).use { it.inputStream.bufferedReader().readText() }
            parseManifest(text).also { manifest = it }
        } catch (e: Exception) {
            manifestFailed = true
            logger.warn { "Could not read the CS2 bundle manifest at $url (${e.message}); only classpath bundles are available" }
            emptyList()
        }
    }

    fun parseManifest(text: String): List<Bundle> =
        JsonParser.parseString(text).asJsonObject.getAsJsonArray("bundles").map { element ->
            val obj = element.asJsonObject
            val name = obj["name"].asString
            Bundle(
                name = name,
                rev = obj["rev"]?.asInt ?: name.substringBefore('.').toInt(),
                sub = obj["sub"]?.asInt ?: name.substringAfter('.', "0").toInt(),
                file = obj["file"]?.asString ?: "$name.zip",
                size = obj["size"]?.asLong ?: -1L,
                sha256 = obj["sha256"]?.asString,
            )
        }

    private fun classpathBundles(major: Int): List<Bundle> {
        val loader = Cs2BundleSource::class.java.classLoader
        val bundles = mutableListOf<Bundle>()
        if (loader.getResource("$CLASSPATH_DIR/$major.zip") != null) {
            bundles += Bundle("$major", major, 0, "$major.zip", -1, null)
        }
        for (sub in 1..999) {
            if (loader.getResource("$CLASSPATH_DIR/$major.$sub.zip") != null) {
                bundles += Bundle("$major.$sub", major, sub, "$major.$sub.zip", -1, null)
            }
        }
        return bundles
    }

    /** Opens [url]; `file:` URLs work too, which is how a local bundle folder can stand in for the CDN. */
    private fun open(url: String): Connection {
        val connection = URL(url).openConnection().apply {
            connectTimeout = 15_000
            readTimeout = 120_000
            setRequestProperty("User-Agent", "OpenRune-FileStore")
        }
        if (connection is HttpURLConnection) {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            if (connection.responseCode != 200) {
                val code = connection.responseCode
                connection.disconnect()
                error("HTTP $code for $url")
            }
        }
        return Connection(connection)
    }

    private class Connection(private val connection: java.net.URLConnection) : AutoCloseable {
        val inputStream get() = connection.getInputStream()
        override fun close() { (connection as? HttpURLConnection)?.disconnect() }
    }

    /**
     * Writes `manifest.json` for every `<rev>.<sub>.zip` in [dir], the file the CDN serves next to the
     * bundles. Run after adding bundles to the upload folder.
     */
    fun writeManifest(dir: File, baseUrl: String = DEFAULT_BASE_URL): File {
        val zips = dir.listFiles { f -> f.isFile && f.extension == "zip" && BUNDLE_NAME.matches(f.nameWithoutExtension) }
            .orEmpty()
            .sortedWith(compareBy({ it.nameWithoutExtension.substringBefore('.').toInt() }, { it.nameWithoutExtension.substringAfter('.', "0").toInt() }))
        val entries = zips.map { zip ->
            val name = zip.nameWithoutExtension
            linkedMapOf(
                "name" to name,
                "rev" to name.substringBefore('.').toInt(),
                "sub" to name.substringAfter('.', "0").toInt(),
                "file" to zip.name,
                "size" to zip.length(),
                "sha256" to sha256(zip),
            )
        }
        val manifestFile = File(dir, MANIFEST_FILE)
        manifestFile.writeText(GsonBuilder().setPrettyPrinting().create().toJson(mapOf("baseUrl" to baseUrl, "bundles" to entries)))
        return manifestFile
    }

    private val BUNDLE_NAME = Regex("""\d+(\.\d+)?""")

    /** `Cs2BundleSource <bundleDir> [baseUrl]` regenerates the manifest for a folder of bundles. */
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.isNotEmpty()) { "Usage: Cs2BundleSource <bundleDir> [baseUrl]" }
        val file = writeManifest(File(args[0]), args.getOrNull(1) ?: DEFAULT_BASE_URL)
        println("Wrote ${file.absolutePath}")
    }
}
