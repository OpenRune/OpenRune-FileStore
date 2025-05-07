package dev.openrune.cache.tools.tasks.impl.defs

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.github.michaelbull.logging.InlineLogger
import dev.openrune.OsrsCacheProvider.Companion.CACHE_REVISION
import dev.openrune.cache.*
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.item.ItemSlotType
import dev.openrune.definition.util.toArray
import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.*
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.impl.sprites.SpriteSet
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import dev.openrune.definition.Js5GameValGroup
import dev.openrune.definition.RSCMHandler
import dev.openrune.definition.codec.*
import dev.openrune.definition.util.Type
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import java.io.File
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

class PackType(
    val index: Int,
    val archive: Int,
    val codecClass: KClass<*>,
    val name: String,
    val gameValGroup: Js5GameValGroup? = null,
    val modify: ((Map<String, Any>, Any) -> Unit)? = null
) {
    val typeClass: KClass<*> = codecClass.supertypes.firstOrNull()
        ?.arguments?.firstOrNull()?.type?.classifier as? KClass<*>
        ?: throw IllegalArgumentException("Type class not found for codec $codecClass")
}

class PackConfig(
    private val directory : File,
    val tokenizedReplacement: Map<String,String> = emptyMap()
) : CacheTask() {

    fun MutableList<String?>.fromOptions(keyName: String, content: Map<String, Any?>) {
        (1..5).forEachIndexed { index, i ->
            content["${keyName}$i"]?.let { this[index] = it.toString().replace("\"", "") }
        }
    }

    init {

        packTypes.registerPackType(ITEM, ItemCodec::class, "item",Js5GameValGroup.OBJTYPES) { content, def: ItemType ->
            if (content["equipmentType"] != null) {
                content["equipmentType"]?.toString()?.replace("\"", "")?.takeIf { ItemSlotType.fetchTypes().contains(it) }?.let { type ->
                    ItemSlotType.fetchType(type)?.apply {
                        def.equipSlot = slot
                        def.appearanceOverride1 = override1
                        def.appearanceOverride2 = override2
                    }
                } ?: println("Unknown Slot: ${content["equipmentType"]}")

            }
            def.options.fromOptions("option", content)
            def.interfaceOptions.fromOptions("ioption", content)
        }

        packTypes.registerPackType(index = TEXTURES, archive = 0, codec = TextureCodec::class, name = "texture")
        packTypes.registerPackType(OBJECT, ObjectCodec::class, "object",Js5GameValGroup.LOCTYPES) { content, def: ObjectType ->
            def.actions.fromOptions("option", content)
        }
        packTypes.registerPackType(SPOTANIM, SpotAnimCodec::class, "graphics", Js5GameValGroup.SPOTTYPES)
        packTypes.registerPackType(SPOTANIM, SpotAnimCodec::class, "graphic", Js5GameValGroup.SPOTTYPES)

        packTypes.registerPackType(SEQUENCE, SequenceCodec::class, "animation", Js5GameValGroup.SEQTYPES)
        packTypes.registerPackType(STRUCT, StructCodec::class, "struct") { content, def: StructType ->
            val filteredMap = content.filterKeys { key ->
                key.toIntOrNull() != null
            }.mapKeys { (key, _) -> key.toInt() }

            filteredMap.forEach { (key, value) ->
                def.params?.set(key, value)
            }
        }
        packTypes.registerPackType(NPC, NPCCodec::class, "npc",Js5GameValGroup.NPCTYPES) { content, def: NpcType ->
            def.actions.fromOptions("option", content)
        }

        packTypes.registerPackType(ENUM, EnumCodec::class, "enum") { content, def: EnumType ->
            val filteredMap = content.filterKeys { key ->
                key.toIntOrNull() != null
            }.mapKeys { (key, _) -> key.toInt() }

            content["keytype"]?.let {
                def.valueType = Type.valueOf(it.toString().replace("\"", "").uppercase())
            }

            content["valuetype"]?.let {
                def.valueType = Type.valueOf(it.toString().replace("\"", "").uppercase())
            }

            content["clear"]?.takeIf { it == "true" }?.let {
                def.values.clear()
            }

            content["default"]?.let {
                val value = it.toString().replace("\"", "")
                if (value.matches(Regex("^[0-9]+$"))) {
                    def.defaultInt = value.toInt()
                } else {
                    def.defaultString = value
                }
            }

            filteredMap.forEach { (key, value) ->
                def.values[key] = value
            }
        }
        packTypes.registerPackType(VARBIT, VarBitCodec::class, "varbit",Js5GameValGroup.VARBITTYPES)
        packTypes.registerPackType(AREA, AreaCodec::class, "area") { content, def: AreaType ->
            def.options.fromOptions("option", content)
        }
        packTypes.registerPackType(HEALTHBAR, HealthBarCodec::class, "health")
        packTypes.registerPackType(HITSPLAT, HitSplatCodec::class, "hitsplat")
        packTypes.registerPackType(IDENTKIT, IdentityKitCodec::class, "idk")
        packTypes.registerPackType(INV, InventoryCodec::class, "inventory",Js5GameValGroup.INVTYPES)
        packTypes.registerPackType(OVERLAY, OverlayCodec::class, "overlay")
        packTypes.registerPackType(UNDERLAY, OverlayCodec::class, "underlay")
        packTypes.registerPackType(PARAMS, ParamCodec::class, "params")
        packTypes.registerPackType(VARPLAYER, VarCodec::class, "varp",Js5GameValGroup.VARPTYPES)
        packTypes.registerPackType(VARCLIENT, VarClientCodec::class, "varclient")

    }


    val logger = InlineLogger()

    override fun init(cache: Cache) {
        val size = getFiles(directory, "toml").size
        val progress = progress("Packing Configs", size)
        if (size != 0) {
            getFiles(directory, "toml").forEach {
                progress.extraMessage = it.name

                val defs = parseTomlSectionToMap(tokenizedReplacement.toMutableMap(),packTypes.keys.toList(), it.readText())

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
                            packDefinitions(item.section, item.lines, codec, codecInstance, cache)
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
        lines : Map<String, Any>,
        packType: PackType,
        codec: DefinitionCodec<T>,
        cache: Cache
    ) {
        val toml = Toml(TomlInputConfig(ignoreUnknownNames = true))
        var def = toml.decodeFromString(packType.typeClass.serializer(), tomlContent) as T
        val index = packType.index
        val archive = packType.archive

        if (def.id == -1) {
            logger.info { "Unable to pack as the ID is -1 or has not been defined" }
            return
        }

        val defId = def.id

        val inheritText = lines["inherit"].toString().replace("\"", "")

        val inherit = inheritText.let {
            it.toIntOrNull() ?: RSCMHandler.getMapping(it) ?: -1
        }

        if (inherit != -1) {
            val inheritedDef = getInheritedDefinition(inherit, codec,archive, index,cache)
            inheritedDef?.let {
                def = mergeDefinitions(it, def, codec)
            } ?: run {
                logger.warn { "No inherited definition found for ID $inherit" }
                return
            }
        }
        packType.modify?.let { it(lines, def) }

        if (packType.index == TEXTURES) {
            def = manageTexture(cache,def) as T
        }

        if (packType.gameValGroup != null) {
            val matchingName : String? = lines["id"]?.toString()?.takeIf { it.all { char -> char.isDigit() } }?.let { null } ?: lines["id"].toString()

            val name = matchingName?.substringAfter(".") ?: when (packType.gameValGroup) {
                Js5GameValGroup.OBJTYPES -> (def as ItemType).name
                Js5GameValGroup.NPCTYPES -> (def as NpcType).name
                Js5GameValGroup.LOCTYPES -> (def as ObjectType).name
                else -> null
            }

            if (!name.isNullOrBlank() && name != "null") {
                CacheTool.addGameValMapping(packType.gameValGroup, name, defId)
            }
        }

        val writer = Unpooled.buffer(4096)
        with(codec) { writer.encode(def) }
        cache.write(index,archive,defId,writer.toArray())
    }

    private fun <T : Definition> manageTexture(cache: Cache, inheritedDef: T): TextureType {
        val def = inheritedDef as TextureType
        val spriteID = def.fileIds.firstOrNull() ?: return def.copy(averageRgb = 0)
        val spriteBuff = cache.data(SPRITES, spriteID) ?: return def.copy(averageRgb = 0)
        val sprite = SpriteSet.decode(spriteID, Unpooled.wrappedBuffer(spriteBuff))
        val color = sprite.sprites.firstOrNull()?.averageColorForPixels() ?: 0
        return def.copy(averageRgb = color)
    }

    private fun <T : Definition> getInheritedDefinition(
        inherit : Int,
        codec: DefinitionCodec<T>,
        archive: Int,
        index : Int,
        cache: Cache
    ): T? {
        val data = cache.data(index, archive, inherit)
        return data?.let { codec.loadData(inherit, data) }
    }

    private fun <T : Definition> mergeDefinitions(baseDef: T, inheritedDef: T, codec: DefinitionCodec<T>): T {
        val defaultDef = codec.createDefinition()

        defaultDef::class.java.declaredFields.forEach { field ->
            if (!Modifier.isStatic(field.modifiers)) {
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

    data class TomlSection(var section: String, val lines: MutableMap<String, String>)

    private fun parseTomlSectionToMap(
        tokenizedReplacement: MutableMap<String, String>,
        types: List<String>,
        tomlContent: String
    ): MutableMap<String, MutableList<TomlSection>> {

        val combinedMap = mutableMapOf<String, MutableList<TomlSection>>()
        val sectionRegex = """\[\[([\w.]+)\]\](.*?)(?=\[\[|\Z)""".toRegex(RegexOption.DOT_MATCHES_ALL)

        sectionRegex.findAll(tomlContent).forEach { match ->
            val (fullSectionName, sectionContent) = match.destructured
            val sectionParts = fullSectionName.split(".") // Handle sub-tables

            val sectionMap = sectionContent.trim().lineSequence()
                .mapNotNull { it.split("=").map(String::trim).takeIf { it.size == 2 } }
                .associate { it[0] to it[1].removeSurrounding("\"") }
                .toMutableMap()

            val topLevelSection = sectionParts.first()
            val subTableName = sectionParts.drop(1).joinToString(".") // Capture sub-table name if present

            when (topLevelSection) {
                in types -> {
                    val sectionObject = TomlSection(sectionContent, sectionMap)
                    combinedMap.getOrPut(fullSectionName) { mutableListOf() }.add(sectionObject)
                }
                "tokenizedReplacement" -> {
                    sectionMap.forEach { (key, value) ->
                        tokenizedReplacement[key] = value
                    }
                }
            }
        }

        // Apply token replacements
        combinedMap.values.flatten().forEach { item ->
            item.lines.replaceAll { key, value ->
                tokenizedReplacement.entries.fold(value) { acc, (placeholder, replacement) ->
                    acc.replace("%$placeholder%", replacement)
                }
            }
            item.section = tokenizedReplacement.entries.fold(item.section) { acc, (placeholder, replacement) ->
                acc.replace("%$placeholder%", replacement)
            }
        }

        return combinedMap
    }

    companion object {
        val packTypes = mutableMapOf<String, PackType>()

        fun <T : Definition>MutableMap<String, PackType>.registerPackType(
            archive: Int, codec: KClass<*>,
            name: String,
            gameValGroup: Js5GameValGroup? = null,
            index: Int = CONFIGS,
            modify: ((Map<String, Any>, T) -> Unit)
        ) {
            val packType = PackType(index,archive, codec, name, gameValGroup,modify as ((Map<String, Any>, Any) -> Unit)?)
            this[packType.name] = packType
        }

        fun MutableMap<String, PackType>.registerPackType(
            index: Int,
            archive: Int,
            codec: KClass<*>,
            name: String,
            gameValGroup: Js5GameValGroup? = null
        ) {
            val packType = PackType(index,archive, codec, name, gameValGroup)
            this[packType.name] = packType
        }

        fun MutableMap<String, PackType>.registerPackType(archive: Int, codec: KClass<*>, name: String, gameValGroup: Js5GameValGroup? = null) {
            registerPackType(CONFIGS,archive, codec, name,gameValGroup)
        }
    }

}
