package dev.openrune.cache.tools.tasks.impl

import com.google.gson.GsonBuilder
import dev.openrune.cache.MAPS
import dev.openrune.cache.util.XteaLoader
import dev.openrune.cache.util.decompressGzipToBytes
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.logger
import dev.openrune.cache.util.progress
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.filesystem.Cache
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readBytes
import kotlin.random.Random

enum class XteaType {
    NO_KEYS,
    EMPTY_KEYS,
    RANDOM_KEYS
}

class PackMaps(
    private val mapsDirectory: File,
    private val xteaLocation: File = File("xteas.json"),
    private val xteaType: XteaType = XteaType.NO_KEYS
) : CacheTask() {

    override fun init(cache: Cache) {

        val supportsMapXteas = revision < 237

        if (!supportsMapXteas && xteaType != XteaType.NO_KEYS) {
            error("XTEA map packing is deprecated on revision 237+.")
        }

        val encodeXteas = supportsMapXteas && xteaType != XteaType.NO_KEYS

        if (encodeXteas) {
            if (!xteaLocation.exists()) {
                logger.info { "Unable to find Xteas File at $xteaLocation" }
                return
            }

            XteaLoader.load(xteaLocation)
        }

        packRegularMaps(cache, encodeXteas)
        packPackFiles(cache, encodeXteas)

        if (encodeXteas) {
            val valuesList = XteaLoader.xteas.values.toList()
            xteaLocation.writeText(
                GsonBuilder()
                    .setPrettyPrinting()
                    .create()
                    .toJson(valuesList)
            )
        }
    }

    private fun packRegularMaps(cache: Cache, encodeXteas: Boolean) {

        val mapFiles = getFiles(mapsDirectory, "gz", "dat")
            .filter { it.name.startsWith("l") }

        val progressMaps = progress("Packing Maps", mapFiles.size)

        mapFiles.forEach { mapFile ->

            val objectFile = File(
                mapFile.parent,
                mapFile.name.replaceFirstChar { "m" }
            )

            if (!objectFile.exists()) {
                println("MISSING MAP FILE: $objectFile")
                return@forEach
            }

            val loc = mapFile.nameWithoutExtension
                .replace("m", "")
                .replace("l", "")
                .split("_")

            val regionX = loc[0].toInt()
            val regionY = loc[1].toInt()

            val regionId = (regionX shl 8) or regionY

            var tileData = Files.readAllBytes(mapFile.toPath())
            var objData = Files.readAllBytes(objectFile.toPath())

            if (mapFile.name.endsWith(".gz")) {
                tileData = decompressGzipToBytes(mapFile.toPath())
            }

            if (objectFile.name.endsWith(".gz")) {
                objData = decompressGzipToBytes(objectFile.toPath())
            }

            val keys: IntArray? = when (xteaType) {
                XteaType.NO_KEYS -> null
                XteaType.EMPTY_KEYS -> intArrayOf(0, 0, 0, 0)
                XteaType.RANDOM_KEYS -> generateRandomIntArray()
            }

            if (encodeXteas && keys != null) {
                XteaLoader.xteas[regionId]?.key = keys
            }

            packMap(
                cache,
                regionX,
                regionY,
                tileData,
                objData,
                if (encodeXteas) keys else null
            )

            progressMaps.step()
        }

        progressMaps.close()
    }

    private fun packPackFiles(cache: Cache, encodeXteas: Boolean) {

        val packFiles = mapsDirectory.listFiles()?.filter { it.extension == "pack" } ?: return

        if (packFiles.isEmpty()) {
            return
        }

        val progressPacks = progress("Packing .pack Maps", packFiles.size)

        packFiles.forEach { file ->
            val baseRegionId = parseBaseRegionId(file) ?: error("Unable to determine base region id from ${file.name}")
            packRSPSiFile(
                cache,
                baseRegionId,
                file.toPath(),
                encodeXteas
            )

            progressPacks.step()
        }

        progressPacks.close()
    }

    private fun parseBaseRegionId(file: File): Int? {
        val name = file.nameWithoutExtension
        val regex = Regex("(\\d+)$")

        return regex.find(name)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
    }

    private fun packRSPSiFile(
        cache: Cache,
        baseRegionId: Int,
        packFile: Path,
        encodeXteas: Boolean
    ): Set<Int> {

        val writtenRegions = mutableSetOf<Int>()

        val bytes = packFile.readBytes()
        val buffer = ByteBuffer.wrap(bytes)

        val baseRegionX = (baseRegionId shr 8) and 0xFF
        val baseRegionY = baseRegionId and 0xFF

        val mapSquareCount = buffer.getInt()

        repeat(mapSquareCount) {

            buffer.getInt()
            buffer.getInt()

            val localX = buffer.getInt()
            val localY = buffer.getInt()

            val locsLen = buffer.getInt()
            val locsBlock = ByteArray(locsLen)
            buffer.get(locsBlock)

            val mapLen = buffer.getInt()
            val mapBlock = ByteArray(mapLen)
            buffer.get(mapBlock)

            val regionX = baseRegionX + localX
            val regionY = baseRegionY + localY

            val regionId = (regionX shl 8) or regionY

            writtenRegions += regionId

            val keys: IntArray? = when (xteaType) {
                XteaType.NO_KEYS -> null
                XteaType.EMPTY_KEYS -> intArrayOf(0, 0, 0, 0)
                XteaType.RANDOM_KEYS -> generateRandomIntArray()
            }

            if (encodeXteas && keys != null) {
                XteaLoader.xteas[regionId]?.key = keys
            }

            packMap(
                cache,
                regionX,
                regionY,
                mapBlock,
                locsBlock,
                if (encodeXteas) keys else null
            )

            println(
                "Packed region $regionId " +
                        "(${regionX}_${regionY}) " +
                        "[tiles=${mapBlock.size}, locs=${locsBlock.size}]"
            )
        }

        return writtenRegions
    }

    fun packMap(
        cache: Cache,
        regionX: Int,
        regionY: Int,
        tileData: ByteArray,
        objData: ByteArray,
        keys: IntArray?
    ) {

        if (revision < 237) {

            val mapArchiveName = "m${regionX}_${regionY}"
            val landArchiveName = "l${regionX}_${regionY}"

            cache.write(MAPS, mapArchiveName, tileData)
            cache.write(MAPS, landArchiveName, objData, keys)

        } else {

            val groupId = (regionX shl 8) or (regionY and 0xFF)

            cache.write(MAPS, groupId, 0, tileData)
            cache.write(MAPS, groupId, 1, objData)
        }
    }

    fun generateRandomIntArray(): IntArray {
        return IntArray(4) {
            Random.nextInt(Int.MIN_VALUE, Int.MAX_VALUE)
        }
    }
}