package dev.openrune.cache

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.lookup
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.filesystem.Cache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import java.io.File
import java.net.URL
import java.nio.file.Path
import java.security.MessageDigest
import kotlinx.coroutines.sync.withPermit

data class ModelDefinition(
    val id: Int,
    val name: String?,
    val gameval: String,
    val animationId: Int,
    val objectModels: Array<Int>?,
    val objectTypes: Array<Int>?,
    val modelSizeX: Int,
    val modelSizeY: Int,
    val modelSizeZ: Int,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val offsetZ: Int = 0,
    val ambient: Int,
    val contrast: Int,
    val recolorToReplace: Array<Int>?,
    val recolorToFind: Array<Int>?,
    val textureToReplace: Array<Int>?,
    val retextureToFind: Array<Int>?,
    val rotated: Boolean = false
)

fun main() = runBlocking {

    val cache = Cache.load(Path.of("C:\\Users\\chris\\Desktop\\Alter\\data\\cache"))
    val gamevals = GameValHandler.readGameVal(GameValGroupTypes.LOCTYPES, cache)

    CacheManager.init(OsrsCacheProvider(cache, 232))

    val objects = mutableListOf<ModelDefinition>()

    CacheManager.getObjects().forEach { (_, objectDef) ->

        if (objectDef.objectModels != null) {
            objects += ModelDefinition(
                id = objectDef.id,
                name = objectDef.name,
                gameval = gamevals.lookup(objectDef.id)?.name ?: "this is rlly broken",
                animationId = objectDef.animationId,
                objectModels = objectDef.objectModels?.toTypedArray(),
                objectTypes = objectDef.objectTypes?.toTypedArray(),
                modelSizeX = objectDef.modelSizeX,
                modelSizeY = objectDef.modelSizeY,
                modelSizeZ = objectDef.modelSizeZ,
                offsetX = objectDef.offsetX,
                offsetY = objectDef.offsetY,
                offsetZ = objectDef.offsetZ,
                ambient = objectDef.ambient,
                contrast = objectDef.contrast,
                recolorToReplace = objectDef.originalColours?.toTypedArray(),
                recolorToFind = objectDef.modifiedColours?.toTypedArray(),
                textureToReplace = objectDef.originalTextureColours?.toTypedArray(),
                retextureToFind = objectDef.modifiedTextureColours?.toTypedArray(),
                rotated = objectDef.isRotated
            )
        }
    }

    val gson = GsonBuilder()
        .registerTypeAdapter(ModelDefinition::class.java, object : TypeAdapter<ModelDefinition>() {

            override fun write(out: JsonWriter, v: ModelDefinition) {
                out.beginObject()

                out.name("i").value(v.id)           // id
                out.name("g").value(v.gameval)      // gameval
                v.name?.let { out.name("n").value(it) }

                if (v.animationId != 0)
                    out.name("a").value(v.animationId)

                // objectModels
                v.objectModels?.takeIf { it.isNotEmpty() }?.let {
                    out.name("om")
                    out.beginArray()
                    it.forEach(out::value)
                    out.endArray()
                }

                // objectTypes
                v.objectTypes?.takeIf { it.isNotEmpty() }?.let {
                    out.name("ot")
                    out.beginArray()
                    it.forEach(out::value)
                    out.endArray()
                }

                // model sizes
                if (v.modelSizeX != 0) out.name("sx").value(v.modelSizeX)
                if (v.modelSizeY != 0) out.name("sy").value(v.modelSizeY)
                if (v.modelSizeZ != 0) out.name("sz").value(v.modelSizeZ)

                // offsets
                if (v.offsetX != 0) out.name("ox").value(v.offsetX)
                if (v.offsetY != 0) out.name("oy").value(v.offsetY)
                if (v.offsetZ != 0) out.name("oz").value(v.offsetZ)

                // lighting
                if (v.ambient != 0) out.name("am").value(v.ambient)
                if (v.contrast != 0) out.name("co").value(v.contrast)

                // recolors
                v.recolorToReplace?.takeIf { it.isNotEmpty() }?.let {
                    out.name("rr")
                    out.beginArray()
                    it.forEach(out::value)
                    out.endArray()
                }

                v.recolorToFind?.takeIf { it.isNotEmpty() }?.let {
                    out.name("rf")
                    out.beginArray()
                    it.forEach(out::value)
                    out.endArray()
                }

                // retextures
                v.textureToReplace?.takeIf { it.isNotEmpty() }?.let {
                    out.name("tr")
                    out.beginArray()
                    it.forEach(out::value)
                    out.endArray()
                }

                v.retextureToFind?.takeIf { it.isNotEmpty() }?.let {
                    out.name("tf")
                    out.beginArray()
                    it.forEach(out::value)
                    out.endArray()
                }

                if (v.rotated) out.name("r").value(true)

                out.endObject()
            }

            override fun read(reader: JsonReader): ModelDefinition {
                throw UnsupportedOperationException("Deserialization not supported")
            }
        })
        .create()

    val objectsFile = File("D:\\Pimp My House\\PMH-Assets\\objects\\objects.json")
    objectsFile.writeText(gson.toJson(objects))

    updateManifest(
        manifestFile = File("D:\\Pimp My House\\PMH-Assets\\manifest.json"),
        fileName = "objects.json",
        metadata = mapOf(
            "sha256" to sha256(objectsFile.readBytes()),
            "sizeBytes" to objectsFile.length(),
            "objectCount" to objects.size
        ),
        gson = gson
    )

    // --- Download images in parallel ---
    val imageDir = File("D:\\Pimp My House\\PMH-Assets\\objects\\images")
    val manifestFile = File("D:\\Pimp My House\\PMH-Assets\\manifest.json")

    println("Downloading object images...")
    val progress = ProgressBar(objects.size)

    val maxConcurrentDownloads = 50
    val semaphore = Semaphore(maxConcurrentDownloads)
    val delayMillis = 700L // 200ms delay between downloads

    coroutineScope {
        objects.map { obj ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    // Skip if all orientation images already exist
                    val allExist = (1..3).all { orient ->
                        File(imageDir, "${obj.id}_orient$orient.png").exists()
                    }
                    if (!allExist) {
                        downloadObjectImages(obj.id, imageDir, manifestFile, gson)
                    }
                    progress.increment()
                    kotlinx.coroutines.delay(delayMillis)
                }
            }
        }.awaitAll()
    }

    println("\nAll images downloaded.")

}

/* ----------------------- Helpers ----------------------- */

fun sha256(data: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(data).joinToString("") { "%02x".format(it) }
}

fun updateManifest(
    manifestFile: File,
    fileName: String,
    metadata: Map<String, Any>,
    gson: Gson
) {
    // Read existing manifest leniently
    val manifest: MutableMap<String, Any> = if (manifestFile.exists()) {
        try {
            val reader = JsonReader(manifestFile.reader())
            reader.isLenient = true
            gson.fromJson(reader, MutableMap::class.java) as MutableMap<String, Any>
        } catch (e: Exception) {
            println("Warning: Failed to read manifest. Creating a new one. Reason: ${e.message}")
            mutableMapOf()
        }
    } else {
        mutableMapOf()
    }

    // Update manifest
    manifest[fileName] = metadata

    // Write manifest pretty-printed
    val gsonPretty = GsonBuilder().setPrettyPrinting().create()
    manifestFile.writeText(gsonPretty.toJson(manifest))
}

/* ---------------- ModelDefinition JSON Adapter ---------------- */

class ModelDefinitionAdapter : TypeAdapter<ModelDefinition>() {

    override fun write(out: JsonWriter, v: ModelDefinition) {
        out.beginObject()

        out.name("id").value(v.id)
        out.name("gameval").value(v.gameval)

        v.name?.let { out.name("name").value(it) }
        if (v.animationId != 0) out.name("animationId").value(v.animationId)

        writeArray(out, "objectModels", v.objectModels)
        writeArray(out, "objectTypes", v.objectTypes)

        if (v.modelSizeX != 0) out.name("modelSizeX").value(v.modelSizeX)
        if (v.modelSizeY != 0) out.name("modelSizeY").value(v.modelSizeY)
        if (v.modelSizeZ != 0) out.name("modelSizeZ").value(v.modelSizeZ)

        if (v.offsetX != 0) out.name("offsetX").value(v.offsetX)
        if (v.offsetY != 0) out.name("offsetY").value(v.offsetY)
        if (v.offsetZ != 0) out.name("offsetZ").value(v.offsetZ)

        if (v.ambient != 0) out.name("ambient").value(v.ambient)
        if (v.contrast != 0) out.name("contrast").value(v.contrast)

        writeArray(out, "recolorToReplace", v.recolorToReplace)
        writeArray(out, "recolorToFind", v.recolorToFind)
        writeArray(out, "textureToReplace", v.textureToReplace)
        writeArray(out, "retextureToFind", v.retextureToFind)

        if (v.rotated) out.name("rotated").value(true)

        out.endObject()
    }

    private fun writeArray(out: JsonWriter, name: String, arr: Array<Int>?) {
        if (!arr.isNullOrEmpty()) {
            out.name(name)
            out.beginArray()
            arr.forEach(out::value)
            out.endArray()
        }
    }

    override fun read(reader: JsonReader): ModelDefinition {
        throw UnsupportedOperationException("Deserialization is not supported")
    }
}

suspend fun downloadObjectImages(objectId: Int, outDir: File, manifestFile: File, gson: Gson) {
    outDir.mkdirs()
    val orientations = listOf(1, 2, 3)
    val downloadedFiles = mutableListOf<File>()

    for (orient in orientations) {
        val url = "https://chisel.weirdgloop.org/static/img/osrs-object/${objectId}_orient$orient.png"
        val outFile = File(outDir, "${objectId}_orient$orient.png")
        try {
            URL(url).openStream().use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
            downloadedFiles += outFile
        } catch (e: Exception) {
            // skip missing orientations
        }
    }

    if (downloadedFiles.isEmpty()) return

}

class ProgressBar(private val total: Int) {
    private var current = 0
    @Synchronized
    fun increment() {
        current++
        val percent = (current * 100 / total)
        print("\rProgress: $percent% ($current/$total)")
    }
}