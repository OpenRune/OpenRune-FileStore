package dev.openrune.cache

import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.filestore.definition.ConfigDefinitionDecoder
import dev.openrune.definition.codec.new.AreaCodecNew
import dev.openrune.definition.type.AreaType
import dev.openrune.filesystem.Cache
import java.nio.file.Path


fun main() {
    val cache = Cache.load(Path.of("C:\\Users\\chris\\Desktop\\Alter\\data\\cache"))

    val areaType3: MutableMap<Int, AreaType> = emptyMap<Int, AreaType>().toMutableMap()
    OsrsCacheProvider.AreaDecoder().load(cache, areaType3)

    val areaType: MutableMap<Int, AreaType> = emptyMap<Int, AreaType>().toMutableMap()
    OsrsCacheProvider.AreaDecoder().load(cache, areaType)

    val areaType2: MutableMap<Int, AreaType> = emptyMap<Int, AreaType>().toMutableMap()
    OsrsCacheProvider.AreaDecoderNew().load(cache, areaType2)

    // Compare the maps
    println("AreaDecoder size: ${areaType.size}")
    println("AreaDecoderNew size: ${areaType2.size}")

    val allIds = (areaType.keys + areaType2.keys).sorted()
    var differences = 0

    for (id in allIds) {
        val oldArea = areaType[id]
        val newArea = areaType2[id]

        if (oldArea?.hashCode() != newArea?.hashCode() || oldArea != newArea) {
            println("ID $id differs")
            differences++
        }
    }

    if (differences == 0 && areaType.keys == areaType2.keys) {
        println("\nSUCCESS: All data is 1:1 identical!")
    } else {
        println("\nFAILED: $differences differences found")
    }
}
