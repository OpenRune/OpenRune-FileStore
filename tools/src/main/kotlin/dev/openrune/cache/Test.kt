package dev.openrune.cache

import com.google.gson.GsonBuilder
import dev.openrune.OsrsCacheProvider
import dev.openrune.definition.Definition
import dev.openrune.definition.codec.new.*
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.type.*
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.nio.file.Path

private const val MAX_DECODE_DIFFERENCES = 20
private const val MAX_ENCODE_FAILURES = 10
private const val MAX_DETAILED_FAILURES = 3
private const val MAX_FIELD_DIFFS = 10

fun main() {
    val cachePath = System.getProperty("cache.path", "C:\\Users\\chris\\Desktop\\Alter\\data\\cache")
    val cache = Cache.load(Path.of(cachePath))
    val outputDir = File("codec_diff_output").apply { mkdirs() }
    val gson = GsonBuilder().setPrettyPrinting().create()

    println("Warming up JVM...")
    repeat(3) { OsrsCacheProvider.AreaDecoderNew().load(cache, mutableMapOf()) }
    println("Warmup complete\n")

    println("Testing Codecs...\n")
    val allTestsPassed = listOf(
        testCodec("AreaCodec", "area", cache, outputDir, gson,
            { OsrsCacheProvider.AreaDecoder().load(cache, it) },
            { OsrsCacheProvider.AreaDecoderNew().load(cache, it) },
            AreaCodecNew()),
        testCodec("StructCodec", "struct", cache, outputDir, gson,
            { OsrsCacheProvider.StructDecoder().load(cache, it) },
            { OsrsCacheProvider.StructDecoderNew().load(cache, it) },
            StructCodecNew()),
        testCodec("VarCodec", "var", cache, outputDir, gson,
            { OsrsCacheProvider.VarDecoder().load(cache, it) },
            { OsrsCacheProvider.VarDecoderNew().load(cache, it) },
            VarCodecNew()),
        testCodec("VarClientCodec", "varclient", cache, outputDir, gson,
            { OsrsCacheProvider.VarClientDecoder().load(cache, it) },
            { OsrsCacheProvider.VarClientDecoderNew().load(cache, it) },
            VarClientCodecNew()),
        testCodec("ParamCodec", "param", cache, outputDir, gson,
            { OsrsCacheProvider.ParamDecoder().load(cache, it) },
            { OsrsCacheProvider.ParamDecoderNew().load(cache, it) },
            ParamCodecNew()),
        testCodec("InventoryCodec", "inventory", cache, outputDir, gson,
            { OsrsCacheProvider.InventoryDecoder().load(cache, it) },
            { OsrsCacheProvider.InventoryDecoderNew().load(cache, it) },
            InventoryCodecNew()),
        testCodec("HealthBarCodec", "healthbar", cache, outputDir, gson,
            { OsrsCacheProvider.HealthBarDecoder().load(cache, it) },
            { OsrsCacheProvider.HealthBarDecoderNew().load(cache, it) },
            HealthBarCodecNew()),
        testCodec("UnderlayCodec", "underlay", cache, outputDir, gson,
            { OsrsCacheProvider.UnderlayDecoder().load(cache, it) },
            { OsrsCacheProvider.UnderlayDecoderNew().load(cache, it) },
            UnderlayCodecNew()),
        testCodec("OverlayCodec", "overlay", cache, outputDir, gson,
            { OsrsCacheProvider.OverlayDecoder().load(cache, it) },
            { OsrsCacheProvider.OverlayDecoderNew().load(cache, it) },
            OverlayCodecNew()),
        testCodec("SpotAnimCodec", "spotanim", cache, outputDir, gson,
            { OsrsCacheProvider.SpotAnimDecoder().load(cache, it) },
            { OsrsCacheProvider.SpotAnimDecoderNew().load(cache, it) },
            SpotAnimCodecNew()),
        testCodec("IdentityKitCodec", "identitykit", cache, outputDir, gson,
            { OsrsCacheProvider.IdentityKitDecoder().load(cache, it) },
            { OsrsCacheProvider.IdentityKitDecoderNew().load(cache, it) },
            IdentityKitCodecNew()),
        testCodec("WorldEntityCodec", "worldentity", cache, outputDir, gson,
            { OsrsCacheProvider.WorldEntityDecoder().load(cache, it) },
            { OsrsCacheProvider.WorldEntityDecoderNew().load(cache, it) },
            WorldEntityCodecNew()),
    ).all { it }

    println("\n${"=".repeat(60)}")
    println(if (allTestsPassed) "RESULT: ALL TESTS PASSED" else "RESULT: SOME TESTS FAILED")
    println("Output directory: ${outputDir.absolutePath}")
    println("=".repeat(60))
}


private fun formatValue(value: Any?): String = when (value) {
    is Collection<*> -> "${value.javaClass.simpleName}(${value.size})"
    is Map<*, *> -> "${value.javaClass.simpleName}(${value.size})"
    else -> value?.toString() ?: "null"
}

private fun getFieldValue(obj: Any, field: Field): Any? = try {
    obj::class.java.getDeclaredField(field.name).apply { isAccessible = true }.get(obj)
} catch (_: Exception) {
    null
}

private fun <T : Any> findFieldDifferences(old: T, new: T, limit: Int = MAX_FIELD_DIFFS): List<String> {
    val fields = old::class.java.declaredFields.filterNot { Modifier.isStatic(it.modifiers) }
    val diffs = mutableListOf<String>()

    for (field in fields) {
        field.isAccessible = true
        val oldValue = field.get(old)
        val newValue = getFieldValue(new, field)

        if (oldValue != newValue) {
            diffs += "      ${field.name}: original=${formatValue(oldValue)}, encoded=${formatValue(newValue)}"
        }
    }

    return if (diffs.size > limit) {
        diffs.take(limit) + "      ... and ${diffs.size - limit} more differences"
    } else {
        diffs
    }
}

private fun <T : Any> printFieldDiff(old: T, new: T, limit: Int = MAX_FIELD_DIFFS) {
    findFieldDifferences(old, new, limit).forEach { println(it) }
}

private fun <T : Definition> testDecodeComparison(
    oldMap: Map<Int, T>,
    newMap: Map<Int, T>
): Int {
    var differences = 0
    val allIds = (oldMap.keys + newMap.keys).sorted()

    for (id in allIds) {
        val oldDef = oldMap[id]
        val newDef = newMap[id]

        when {
            oldDef == null || newDef == null -> {
                if (differences == 0) println()
                println("  [ID $id] Missing in one of the maps")
                differences++
            }
            oldDef != newDef -> {
                if (differences == 0) println()
                println("  [ID $id] Data differs")
                val diffFields = findFieldDifferences(oldDef, newDef, MAX_FIELD_DIFFS)
                diffFields.forEach { println(it) }
                differences++
            }
        }

        if (differences >= MAX_DECODE_DIFFERENCES) {
            println("  ... (stopping after $MAX_DECODE_DIFFERENCES differences)")
            break
        }
    }

    return differences
}

private fun <T : Definition> testEncodeRoundtrip(
    defs: Map<Int, T>,
    codec: OpcodeDefinitionCodec<T>
): Pair<Int, Int> {
    var failures = 0
    var tested = 0

    for ((id, original) in defs.toSortedMap()) {
        try {
            val buffer = Unpooled.buffer(4096)
            with(codec) { buffer.encode(original) }
            val decoded = codec.loadData(id, buffer)
            tested++

            if (original != decoded) {
                if (failures == 0) print("    Encode roundtrip: ")
                if (failures < MAX_DETAILED_FAILURES) {
                    println("\n      [ID $id] Roundtrip mismatch")
                    printFieldDiff(original, decoded, limit = 5)
                }
                failures++
            }
        } catch (e: Exception) {
            if (failures == 0) print("    Encode roundtrip: ")
            if (failures < MAX_DETAILED_FAILURES) {
                println("\n      [ID $id] Exception: ${e.message}")
            }
            failures++
        }

        if (failures >= MAX_ENCODE_FAILURES) {
            println("      ... (stopping after $MAX_ENCODE_FAILURES failures)")
            break
        }
    }

    return failures to tested
}

private fun <T : Definition> testCodec(
    codecName: String,
    filePrefix: String,
    cache: Cache,
    outputDir: File,
    gson: com.google.gson.Gson,
    loadOld: (MutableMap<Int, T>) -> Unit,
    loadNew: (MutableMap<Int, T>) -> Unit,
    encodeCodec: OpcodeDefinitionCodec<T>
): Boolean {
    print("Testing $codecName... ")

    val oldMap = mutableMapOf<Int, T>().also(loadOld)
    val newMap = mutableMapOf<Int, T>().also(loadNew)

    File(outputDir, "${filePrefix}_old.json").writeText(gson.toJson(oldMap.toSortedMap().values.toList()))
    File(outputDir, "${filePrefix}_new.json").writeText(gson.toJson(newMap.toSortedMap().values.toList()))

    val decodeDifferences = testDecodeComparison(oldMap, newMap)
    val decodePassed = decodeDifferences == 0 && oldMap.keys == newMap.keys

    val (encodeFailures, encodeTested) = testEncodeRoundtrip(newMap, encodeCodec)
    val encodePassed = encodeFailures == 0

    when {
        decodePassed && encodePassed -> {
            println("PASS (${oldMap.size} definitions, decode: identical, encode: $encodeTested roundtrips OK)")
            return true
        }
        else -> {
            val issues = buildList {
                if (!decodePassed) add("decode differences: $decodeDifferences")
                if (!encodePassed) add("encode roundtrip: $encodeFailures failures out of $encodeTested tested")
            }
            println("FAIL (${issues.joinToString(", ")})")
            return false
        }
    }
}