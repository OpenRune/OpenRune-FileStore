package dev.openrune.cache

import dev.openrune.OsrsCacheProvider
import dev.openrune.definition.type.AreaType
import dev.openrune.definition.type.ObjectType
import dev.openrune.filesystem.Cache
import java.nio.file.Path


fun main() {
    val cache = Cache.load(Path.of("C:\\Users\\chris\\Desktop\\Alter\\data\\cache"))

    // Area test
    println("=== Testing AreaCodec ===")
    val areaType: MutableMap<Int, AreaType> = emptyMap<Int, AreaType>().toMutableMap()
    OsrsCacheProvider.AreaDecoder().load(cache, areaType)

    val areaType2: MutableMap<Int, AreaType> = emptyMap<Int, AreaType>().toMutableMap()
    OsrsCacheProvider.AreaDecoderNew().load(cache, areaType2)

    println("AreaDecoder size: ${areaType.size}")
    println("AreaDecoderNew size: ${areaType2.size}")

    val allAreaIds = (areaType.keys + areaType2.keys).sorted()
    var areaDifferences = 0

    for (id in allAreaIds) {
        val oldArea = areaType[id]
        val newArea = areaType2[id]

        if (oldArea?.hashCode() != newArea?.hashCode() || oldArea != newArea) {
            println("Area ID $id differs")
            areaDifferences++
        }
    }

    if (areaDifferences == 0 && areaType.keys == areaType2.keys) {
        println("SUCCESS: All Area data is 1:1 identical!")
    } else {
        println("FAILED: $areaDifferences Area differences found")
    }

    // Object test
    println("\n=== Testing ObjectCodec ===")

    val objectType11: MutableMap<Int, ObjectType> = emptyMap<Int, ObjectType>().toMutableMap()
    OsrsCacheProvider.ObjectDecoder(235).load(cache, objectType11)

    val objectType: MutableMap<Int, ObjectType> = emptyMap<Int, ObjectType>().toMutableMap()
    OsrsCacheProvider.ObjectDecoder(235).load(cache, objectType)

    val objectType2: MutableMap<Int, ObjectType> = emptyMap<Int, ObjectType>().toMutableMap()
    OsrsCacheProvider.ObjectDecoderNew(235).load(cache, objectType2)

    println("ObjectDecoder size: ${objectType.size}")
    println("ObjectDecoderNew size: ${objectType2.size}")

    val allObjectIds = (objectType.keys + objectType2.keys).sorted()
    var objectDifferences = 0

    for (id in allObjectIds) {
        val oldObject = objectType[id]
        val newObject = objectType2[id]

        if (oldObject?.hashCode() != newObject?.hashCode() || oldObject != newObject) {
            println("Object ID $id differs")
            objectDifferences++
        }
    }

    if (objectDifferences == 0 && objectType.keys == objectType2.keys) {
        println("SUCCESS: All Object data is 1:1 identical!")
    } else {
        println("FAILED: $objectDifferences Object differences found")
    }
}
