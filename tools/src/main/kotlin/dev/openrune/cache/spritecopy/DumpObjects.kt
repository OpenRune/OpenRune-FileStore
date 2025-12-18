package dev.openrune.cache.spritecopy

import cc.ekblad.toml.TomlMapper
import cc.ekblad.toml.model.TomlDocument
import cc.ekblad.toml.model.TomlException
import cc.ekblad.toml.model.TomlValue
import cc.ekblad.toml.serialization.write
import dev.openrune.cache.OBJECT
import dev.openrune.cache.filestore.definition.ConfigDefinitionDecoder
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.cache.tools.tasks.impl.defs.PackType
import dev.openrune.definition.Definition
import dev.openrune.definition.codec.ObjectCodec
import dev.openrune.definition.type.ObjectType
import dev.openrune.filesystem.Cache
import java.io.File
import java.nio.file.Path
import kotlin.collections.forEach
import kotlin.collections.iterator

object DumpObjects {
    val sourceDir =
        ""
    private val outputDir: File = File("./tomlgen/")
    val revision = 235
    var map: Map<Int, PackType> = mutableMapOf()

    @JvmStatic
    fun main(args: Array<String>) {
        val config = PackConfig(
            directory = outputDir,
            tokenizedReplacement = emptyMap()
        )
        map = PackConfig.packTypes.values.associateBy { it.archive }
        val packType = map[OBJECT]
            ?: error("Unknown pack type: ${OBJECT}")

        val codec = ObjectCodec(235)

        class ObjectDecoder() : ConfigDefinitionDecoder<ObjectType>(codec, OBJECT)

        val cache = Cache.load(Path.of(sourceDir))


        val objects: MutableMap<Int, ObjectType> = mutableMapOf()

        ObjectDecoder().load(cache, objects)

        val defaultValues = codec.createDefinition()

        val packName = "object"
        val tomlString = packType.tomlMapper.encodeToString(packName, objects.values.toList(), defaultValues)

        val outputFile = outputDir.resolve("$packName.toml")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(tomlString)

        println("Exported $packName to ${outputFile.absolutePath}")
    }

    private fun TomlMapper.defToMap(def: Definition): MutableMap<String, TomlValue> {
        val tomlValue = encode(def)
        val document = tomlValue as? TomlDocument ?: throw TomlException.SerializationError.NotAMap(def, tomlValue)
        val element: Map<String, TomlValue> = document.properties
        return HashMap(element)
    }

    fun TomlMapper.encodeToString(packName: String, definitions: List<Definition>, defaultValues: Definition): String {
        val defaults = defToMap(defaultValues)
        val defMaps = mutableListOf<Map<String, TomlValue>>()
        definitions.forEach { def ->
            val element: MutableMap<String, TomlValue> = defToMap(def)
            element.entries.removeIf { (key, value) ->
                defaults[key] == value
            }
//            println(element)

            val map1 = reorderAndRestructureMap(element)
            println(map1)

            defMaps.add(map1)
        }
        val value: Map<String, List<Map<String, TomlValue>>> = mapOf(packName to defMaps)

        val tomlValue = encode(value)
        val document = tomlValue as? TomlDocument ?: throw TomlException.SerializationError.NotAMap(value, tomlValue)

        val map: Map<String, TomlValue> = document.properties

        val document2 = TomlDocument(map)
        val stringBuffer = StringBuffer()
        document.write(stringBuffer)
        return stringBuffer.toString()
    }

    fun TomlMapper.reorderAndRestructureMap(flat: Map<String, TomlValue>): Map<String, TomlValue> {
        val result = LinkedHashMap<String, TomlValue>()

        listOf("id", "name").forEach { key ->
            flat[key]?.let { result[key] = it }
        }

        val normalKeys = flat.keys
            .filterNot { it in listOf("id", "name") || '.' in it }
            .sorted()

        for (key in normalKeys) {
            result[key] = flat[key]!!
        }

        val grouped: MutableMap<String, MutableMap<String, TomlValue>> = mutableMapOf()

        for ((key, value) in flat) {
            if (!key.contains('.')) continue
            val (group, subKey) = key.split('.', limit = 2)
            grouped.computeIfAbsent(group) { mutableMapOf() }[subKey] = value
        }

        for ((group, entries) in grouped) {
            result[group] = encode(entries)
        }

        return result
    }
}
