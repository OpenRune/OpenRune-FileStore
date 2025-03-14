package dev.openrune.cache.tools.tasks.impl.defs

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import dev.openrune.OsrsCacheProvider.Companion.CACHE_REVISION
import dev.openrune.cache.*
import dev.openrune.cache.tools.CacheTool.Constants.library
import dev.openrune.definition.util.toArray
import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.*
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.capitalizeFirstLetter
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import dev.openrune.definition.codec.*
import dev.openrune.filesystem.Cache
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.Unpooled
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import java.io.File
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

class PackType(val archive: Int, val codecClass: KClass<*>, val name: String) {
    val typeClass: KClass<*> = codecClass.supertypes.firstOrNull()
        ?.arguments?.firstOrNull()?.type?.classifier as? KClass<*>
        ?: throw IllegalArgumentException("Type class not found for codec $codecClass")
}

class PackConfig(private val directory : File) : CacheTask() {

    init {
        packTypes.registerPackType(ITEM, ItemCodec::class, "item")
        packTypes.registerPackType(OBJECT, ObjectCodec::class, "object")
    }


    val logger = KotlinLogging.logger {}

    override fun init(cache: Cache) {
        val size = getFiles(directory, "toml").size
        val progress = progress("Packing Configs", size)
        if (size != 0) {
            getFiles(directory, "toml").forEach {
                progress.extraMessage = it.name
                val defs = parseItemsToMap(packTypes.keys.toList(), it.readText())

                defs.forEach { (typeName, items) ->
                    val codec: PackType? = packTypes[typeName]
                    codec?.let {
                        items.forEach { item ->
                            val constructor = codec.codecClass.constructors.first()
                            val params = constructor.parameters.size
                            val codecInstance = if (params == 0) {
                                constructor.call() as DefinitionCodec<*>
                            } else {
                                constructor.call(CACHE_REVISION) as DefinitionCodec<*>
                            }
                            packDefinitions(item, codec.typeClass, codecInstance, cache, codec.archive)
                        }
                    }
                }

                progress.step()
            }
            progress.close()
        }
    }

    @OptIn(InternalSerializationApi::class)
    private fun <T : Definition> packDefinitions(
        tomlContent: String,
        clazz: KClass<*>,
        codec: DefinitionCodec<T>,
        cache: Cache,
        archive: Int
    ) {
        val toml = Toml(TomlInputConfig(true))
        var def = toml.decodeFromString(clazz.serializer(), tomlContent) as T

        if (def.id == -1) {
            dev.openrune.cache.util.logger.info { "Unable to pack as the ID is -1 or has not been defined" }
            return
        }

        val defId = def.id

        if (def.inherit != -1) {
            val inheritedDef = getInheritedDefinition(def, codec,archive, cache)
            inheritedDef?.let {
                def = mergeDefinitions(it, def, codec)
            } ?: run {
                logger.warn { "No inherited definition found for ID ${def.inherit}" }
                return
            }
        }

        val writer = Unpooled.buffer(4096)
        with(codec) { writer.encode(def) }
        library.index(CONFIGS).archive(archive)?.add(defId, writer.toArray())
    }

    private fun <T : Definition> getInheritedDefinition(
        def: T,
        codec: DefinitionCodec<T>,
        archive: Int,
        cache: Cache
    ): T? {
        val data = cache.data(CONFIGS, archive, def.inherit)
        return data?.let { codec.loadData(def.inherit, data) }
    }

    private fun <T : Definition> mergeDefinitions(baseDef: T, inheritedDef: T, codec: DefinitionCodec<T>): T {
        val ignoreFields = setOf("inherit")
        val defaultDef = codec.createDefinition()

        defaultDef::class.java.declaredFields.forEach { field ->
            if (!Modifier.isStatic(field.modifiers) && !ignoreFields.contains(field.name)) {
                field.isAccessible = true
                val baseValue = field.get(baseDef)
                val inheritedValue = field.get(inheritedDef)
                val defaultValue = field.get(defaultDef)

                if (inheritedValue != baseValue && inheritedValue != defaultValue) {
                    field.set(baseDef, inheritedValue)
                }
            }
        }

        return baseDef
    }

    fun parseItemsToMap(types: List<String>, tomlContent: String): MutableMap<String, MutableList<String>> {
        val combinedMap = mutableMapOf<String, MutableList<String>>()
        val sectionRegex = """\[\[(\w+)\]\](.*?)(?=\[\[|\Z)""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = sectionRegex.findAll(tomlContent)
        matches.forEach { match ->
            val sectionName = match.groupValues[1]
            val sectionContent = match.groupValues[2].trim()
            if (types.contains(sectionName)) {
                combinedMap.computeIfAbsent(sectionName) { mutableListOf() }.add(sectionContent)
            }
        }

        return combinedMap
    }



    companion object {
        val packTypes = mutableMapOf<String, PackType>()

        fun MutableMap<String, PackType>.registerPackType(id: Int, cclazz: KClass<*>, name: String) {
            val packType = PackType(id, cclazz, name)
            this[packType.name] = packType
        }


    }

}


