package dev.openrune.cache

import Tool
import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import dev.openrune.cache.tools.CacheTool.Constants.library

import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.*
import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import io.netty.buffer.Unpooled
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

val Item = PackType(ITEM, ObjectType::class.java, "object")
val Object = PackType(OBJECT, ItemType::class.java, "item")

class PackType(val id: Int, private val clazz: Class<*>, private val name: String)

fun parseItemsToMap(types: List<String>, tomlContent: String): MutableMap<String, MutableList<String>> {
    val combinedMap = mutableMapOf<String, MutableList<String>>()
    val sectionRegex = """\[\[(\w+)\]\](.*?)(?=\[\[|\Z)""".toRegex(RegexOption.DOT_MATCHES_ALL)
    val matches = sectionRegex.findAll(tomlContent)
    matches.forEach { match ->
        val sectionName = match.groupValues[1]
        println(sectionName)
        val sectionContent = match.groupValues[2].trim()
        if (types.contains(sectionName)) {
            combinedMap.computeIfAbsent(sectionName) { mutableListOf() }.add(sectionContent)
        }
    }

    return combinedMap
}

val data = """
[[object]]
id = 3434

sizeX = 4
sizeY = 4
option1 = "Pray-at"
animationId = 3030

name = "Statue of Armadyl"

[[item]]
id = 32204
name = "Bow of the Last Guardian"
description = "A beautiful bow, imbued with divine energy. Perfectly balanced, as all things should be."
inventoryModel = 63142
zoom2d = 2670
yan2d = 617
xan2d = 450
zan2d = 0
xOffset2d = -4
yOffset2d = 12
maleModel = 63143
femaleModel = 63143
contrast = 40
ambient = 30
ioption1 = "Wield"

[[item]]
id = 32204
name = "Test333"
description = "A beautiful bow, imbued with divine energy. Perfectly balanced, as all things should be."
inventoryModel = 63142
zoom2d = 2670
yan2d = 617
xan2d = 450
zan2d = 0
xOffset2d = -4
yOffset2d = 12
maleModel = 63143
femaleModel = 63143
contrast = 40
ambient = 30
ioption1 = "Wield"
""".trimIndent()

fun main() {

    val resultMap = findDefinitionCodecs("dev.openrune.definition.codec")
    val defs = parseItemsToMap(resultMap.keys.toList(), data)

    defs.forEach { (typeName, items) ->
        val codec: Pair<KClass<*>, KClass<*>>? = resultMap[typeName]
        codec?.let { (typeClass, codecClass) ->
            items.forEach { item ->
                val constructor = codecClass.constructors.first()
                val params = constructor.parameters.size
                val codecInstance =
                if (params == 0) {
                    constructor.call() as DefinitionCodec<*>
                } else {
                    constructor.call(228) as DefinitionCodec<*>
                }
                packDefinitions(item, typeClass, codecInstance, -1)
            }
        }
    }

}

@OptIn(InternalSerializationApi::class)
private fun <T : Definition> packDefinitions(
    tomlContent: String,
    clazz: KClass<*>,
    codec: DefinitionCodec<T>,
    archive: Int
) {
    val toml = Toml(TomlInputConfig(true))
    var def = toml.decodeFromString(clazz.serializer(), tomlContent) as T

    if (def.id == -1) {
        return
    }

    val defId = def.id

    if (def.inherit != -1) {
        val data = library.data(CONFIGS, archive, def.inherit)
        if (data != null) {
            val inheritedDef = codec.loadData(def.inherit, data)
            def = mergeDefinitions(inheritedDef, def, codec)
        } else {
//            logger.warn { "No inherited definition found for ID ${def.inherit}" }
            return
        }
    }

    println(def)

    val writer = Unpooled.buffer(4096)
    with(codec) { writer.encode(def) }
    println(writer.writerIndex())
}

fun <T : Definition> mergeDefinitions(baseDef: T, inheritedDef: T, codec: DefinitionCodec<T>): T {
    val ignoreFields = setOf("inherit")
    val defaultDef = codec.createDefinition()

    defaultDef::class.java.declaredFields.forEach { field ->
        if (!Modifier.isStatic(field.modifiers) && !ignoreFields.contains(field.name)) {
            field.isAccessible = true
            val baseValue = field.get(baseDef)
            val inheritedValue = field.get(inheritedDef)
            val defaultValue = field.get(defaultDef)

            // Only overwrite the base value if the inherited value is different from both the base and default values
            if (inheritedValue != baseValue && inheritedValue != defaultValue) {
                field.set(baseDef, inheritedValue)
            }
        }
    }

    return baseDef
}

fun findDefinitionCodecs(packageName: String): Map<String, Pair<KClass<*>, KClass<*>>> {
    val resultMap = mutableMapOf<String, Pair<KClass<*>, KClass<*>>>()

    ClassGraph().enableAllInfo()
        .acceptPackages(packageName)
        .scan().use { scanResult: ScanResult ->

            val codecSubclasses = scanResult.allClasses

            for (subclass in codecSubclasses) {
                try {
                    val subclassKClass = subclass.loadClass().kotlin
                    val typeClass = subclassKClass.supertypes.firstOrNull()?.arguments?.firstOrNull()?.type?.classifier as? KClass<*>

                    typeClass?.annotations?.filterIsInstance<Tool>()?.forEach { toolAnnotation ->
                        resultMap[toolAnnotation.name] = typeClass to subclassKClass
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    return resultMap
}

