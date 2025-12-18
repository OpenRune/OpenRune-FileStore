package dev.openrune.cache

import com.google.gson.GsonBuilder
import dev.openrune.OsrsCacheProvider
import dev.openrune.definition.Definition
import dev.openrune.definition.type.*
import dev.openrune.filesystem.Cache
import java.io.File
import java.nio.file.Path

fun main() {
    val cache = Cache.load(Path.of("C:\\Users\\chris\\Desktop\\Alter\\data\\cache"))
    val cacheRevision = 235
    val outputDir = File("codec_diff_output").apply { mkdirs() }
    val gson = GsonBuilder().setPrettyPrinting().create()

    // Warmup
    println("Warming up JVM...")
    repeat(3) {
        OsrsCacheProvider.AreaDecoder().load(cache, mutableMapOf())
        OsrsCacheProvider.AreaDecoderNew().load(cache, mutableMapOf())
    }
    println("Warmup complete\n")

    var allTestsPassed = true

    // Area test
    allTestsPassed = testCodec<AreaType>(
        "AreaCodec", "area",
        { map -> OsrsCacheProvider.AreaDecoder().load(cache, map) },
        { map -> OsrsCacheProvider.AreaDecoderNew().load(cache, map) },
        { old, new -> old == new },
        outputDir, gson
    ) && allTestsPassed

    // Struct test
    allTestsPassed = testCodec<StructType>(
        "StructCodec", "struct",
        { map -> OsrsCacheProvider.StructDecoder().load(cache, map) },
        { map -> OsrsCacheProvider.StructDecoderNew().load(cache, map) },
        { old, new -> old == new },
        outputDir, gson
    ) && allTestsPassed

    // Var test
    allTestsPassed = testCodec<VarpType>(
        "VarCodec", "var",
        { map -> OsrsCacheProvider.VarDecoder().load(cache, map) },
        { map -> OsrsCacheProvider.VarDecoderNew().load(cache, map) },
        { old, new -> old == new },
        outputDir, gson
    ) && allTestsPassed

    // VarClient test
    allTestsPassed = testCodec<VarClientType>(
        "VarClientCodec", "varclient",
        { map -> OsrsCacheProvider.VarClientDecoder().load(cache, map) },
        { map -> OsrsCacheProvider.VarClientDecoderNew().load(cache, map) },
        { old, new -> old == new },
        outputDir, gson
    ) && allTestsPassed

    // Param test
    allTestsPassed = testCodec<ParamType>(
        "ParamCodec", "param",
        { map -> OsrsCacheProvider.ParamDecoder().load(cache, map) },
        { map -> OsrsCacheProvider.ParamDecoderNew().load(cache, map) },
        { old, new -> old == new },
        outputDir, gson
    ) && allTestsPassed

    // Inventory test
    allTestsPassed = testCodec<InventoryType>(
        "InventoryCodec", "inventory",
        { map -> OsrsCacheProvider.InventoryDecoder().load(cache, map) },
        { map -> OsrsCacheProvider.InventoryDecoderNew().load(cache, map) },
        { old, new -> old == new },
        outputDir, gson
    ) && allTestsPassed

    // HealthBar test
    allTestsPassed = testCodec<HealthBarType>(
        "HealthBarCodec", "healthbar",
        { map -> OsrsCacheProvider.HealthBarDecoder().load(cache, map) },
        { map -> OsrsCacheProvider.HealthBarDecoderNew().load(cache, map) },
        { old, new -> old == new },
        outputDir, gson
    ) && allTestsPassed

    // Underlay test
    allTestsPassed = testCodec<UnderlayType>(
        "UnderlayCodec", "underlay",
        { map -> OsrsCacheProvider.UnderlayDecoder().load(cache, map) },
        { map -> OsrsCacheProvider.UnderlayDecoderNew().load(cache, map) },
        { old, new -> old == new },
        outputDir, gson
    ) && allTestsPassed

    // Overlay test
    allTestsPassed = testCodec<OverlayType>(
        "OverlayCodec", "overlay",
        { map -> OsrsCacheProvider.OverlayDecoder().load(cache, map) },
        { map -> OsrsCacheProvider.OverlayDecoderNew().load(cache, map) },
        { old, new -> old == new },
        outputDir, gson
    ) && allTestsPassed

    // HitSplat test
    allTestsPassed = testCodec<HitSplatType>(
        "HitSplatCodec", "hitsplat",
        { map -> OsrsCacheProvider.HitSplatDecoder().load(cache, map) },
        { map -> OsrsCacheProvider.HitSplatDecoderNew().load(cache, map) },
        { old, new -> old == new },
        outputDir, gson
    ) && allTestsPassed

    // SpotAnim test
    allTestsPassed = testCodec<SpotAnimType>(
        "SpotAnimCodec", "spotanim",
        { map -> OsrsCacheProvider.SpotAnimDecoder().load(cache, map) },
        { map -> OsrsCacheProvider.SpotAnimDecoderNew().load(cache, map) },
        { old, new -> old == new },
        outputDir, gson
    ) && allTestsPassed

    // IdentityKit test
    allTestsPassed = testCodec<IdentityKitType>(
        "IdentityKitCodec", "identitykit",
        { map -> OsrsCacheProvider.IdentityKitDecoder().load(cache, map) },
        { map -> OsrsCacheProvider.IdentityKitDecoderNew().load(cache, map) },
        { old, new -> old == new },
        outputDir, gson
    ) && allTestsPassed

    // WorldEntity test
    allTestsPassed = testCodec<WorldEntityType>(
        "WorldEntityCodec", "worldentity",
        { map -> OsrsCacheProvider.WorldEntityDecoder().load(cache, map) },
        { map -> OsrsCacheProvider.WorldEntityDecoderNew().load(cache, map) },
        { old, new -> old == new },
        outputDir, gson
    ) && allTestsPassed

    // Sequence test
    allTestsPassed = testCodec<SequenceType>(
        "SequenceCodec", "sequence",
        { map -> OsrsCacheProvider.SequenceDecoder(cacheRevision).load(cache, map) },
        { map -> OsrsCacheProvider.SequenceDecoderNew(cacheRevision).load(cache, map) },
        { old, new -> old == new },
        outputDir, gson
    ) && allTestsPassed


    println("\n" + "=".repeat(50))
    if (allTestsPassed) {
        println("✅ ALL TESTS PASSED!")
    } else {
        println("❌ SOME TESTS FAILED!")
    }
    println("JSON files saved to: ${outputDir.absolutePath}")
    println("=".repeat(50))
}

inline fun <T : Definition> testCodec(
    codecName: String,
    filePrefix: String,
    loadOld: (MutableMap<Int, T>) -> Unit,
    loadNew: (MutableMap<Int, T>) -> Unit,
    compare: (T, T) -> Boolean,
    outputDir: File,
    gson: com.google.gson.Gson
): Boolean {
    println("=== Testing $codecName ===")
    
    val oldMap = mutableMapOf<Int, T>()
    val newMap = mutableMapOf<Int, T>()
    
    loadOld(oldMap)
    loadNew(newMap)
    
    println("Old size: ${oldMap.size}")
    println("New size: ${newMap.size}")
    
    // Serialize to JSON - sort by ID for cleaner diffs
    val oldJson = oldMap.toSortedMap().values.toList()
    val newJson = newMap.toSortedMap().values.toList()
    
    File(outputDir, "${filePrefix}_old.json").writeText(gson.toJson(oldJson))
    File(outputDir, "${filePrefix}_new.json").writeText(gson.toJson(newJson))
    
    println("  Saved to ${filePrefix}_old.json and ${filePrefix}_new.json")
    
    val allIds = (oldMap.keys + newMap.keys).sorted()
    var differences = 0
    
    for (id in allIds) {
        val oldDef = oldMap[id]
        val newDef = newMap[id]
        
        if (oldDef == null || newDef == null) {
            println("  ID $id: Missing in one of the maps")
            differences++
            continue
        }
        
        if (!compare(oldDef, newDef)) {
            println("  ID $id: Data differs")
            // Print actual differences (limit to first 10 for readability)
            val oldFields = oldDef::class.java.declaredFields
            val diffFields = mutableListOf<String>()
            for (field in oldFields) {
                if (java.lang.reflect.Modifier.isStatic(field.modifiers)) continue
                field.isAccessible = true
                val oldValue = field.get(oldDef)
                val newValue = try {
                    newDef::class.java.getDeclaredField(field.name).apply { isAccessible = true }.get(newDef)
                } catch (e: Exception) {
                    null
                }
                if (oldValue != newValue) {
                    val oldStr = when {
                        oldValue is Collection<*> -> "${oldValue.javaClass.simpleName}(${oldValue.size})"
                        oldValue is Map<*, *> -> "${oldValue.javaClass.simpleName}(${oldValue.size})"
                        else -> oldValue?.toString()
                    }
                    val newStr = when {
                        newValue is Collection<*> -> "${newValue.javaClass.simpleName}(${newValue.size})"
                        newValue is Map<*, *> -> "${newValue.javaClass.simpleName}(${newValue.size})"
                        else -> newValue?.toString()
                    }
                    diffFields.add("    ${field.name}: old=$oldStr, new=$newStr")
                }
            }
            if (diffFields.isNotEmpty()) {
                diffFields.take(10).forEach { println(it) }
                if (diffFields.size > 10) {
                    println("    ... and ${diffFields.size - 10} more differences")
                }
            }
            differences++
            if (differences >= 20) {
                println("  ... (stopping after 20 differences)")
                break
            }
        }
    }
    
    if (differences == 0 && oldMap.keys == newMap.keys) {
        println("✅ SUCCESS: All $codecName data is 1:1 identical!\n")
        return true
    } else {
        println("❌ FAILED: $differences $codecName differences found\n")
        return false
    }
}
