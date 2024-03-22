package dev.openrune.cache.tools.tasks.impl

import com.displee.cache.CacheLibrary
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.XteaLoader
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.FileUtil
import dev.openrune.cache.util.decompressGzipToBytes
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import java.io.File
import java.io.FileWriter
import java.nio.file.Files

class PackMaps(private val mapsDirectory : File,private val xteaLocation : File = File(FileUtil.getTemp(), "xteas.json")) : CacheTask() {
    override fun init(library: CacheLibrary) {
        XteaLoader.load(xteaLocation)
        val mapSize = getFiles(mapsDirectory,"gz","dat").size
        val progressMaps = progress("Packing Maps", mapSize * 1)
        if (mapSize != 0) {
            getFiles(mapsDirectory,"gz","dat").filter { it.name.startsWith("l") }.forEach { mapFile ->
                if (mapFile.name.first().toString() == "l") {

                    val objectFile = File(mapFile.parent,mapFile.name.replaceFirstChar { "m" })

                    if (objectFile.exists()) {

                        val loc = mapFile.nameWithoutExtension.replace(
                            "m",""
                        ).replace("l","").split("_")

                        val regionId = loc[0].toInt() shl 8 or loc[1].toInt()

                        var tileData = Files.readAllBytes(mapFile.toPath())
                        var objData = Files.readAllBytes(objectFile.toPath())

                        if (mapFile.name.endsWith(".gz")) {
                            tileData = decompressGzipToBytes(mapFile.toPath())
                        }
                        if (objectFile.name.endsWith(".gz")) {
                            objData = decompressGzipToBytes(objectFile.toPath())
                        }

                        packMap(library,loc[0].toInt(), loc[1].toInt(), tileData, objData)
                        XteaLoader.xteas[regionId]!!.key = intArrayOf(0,0,0,0)
                    } else {
                        println("MISSING MAP FILE: $objectFile")
                    }

                }

                progressMaps.stepBy(3)

            }
            // Create a FileWriter to write into the file
            val fileWriter = FileWriter(xteaLocation)

            val valuesList = XteaLoader.xteas.values.toList()

            fileWriter.write(GsonBuilder().setPrettyPrinting().create().toJson(valuesList))

            fileWriter.close()
            progressMaps.close()
        }
    }

    fun packMap(library: CacheLibrary, regionX : Int, regionY : Int, tileData : ByteArray, objData : ByteArray) {
        val mapArchiveName = "m" + regionX + "_" + regionY
        val landArchiveName = "l" + regionX + "_" + regionY

        library.put(MAPS, mapArchiveName, tileData)
        library.put(MAPS, landArchiveName, objData)

    }

}