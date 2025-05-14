package dev.openrune.cache.tools.tasks.impl.defs

import com.github.michaelbull.logging.InlineLogger
import cc.ekblad.toml.TomlMapper
import cc.ekblad.toml.model.TomlValue
import cc.ekblad.toml.serialization.from
import cc.ekblad.toml.tomlMapper
import cc.ekblad.toml.util.InternalAPI
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
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import java.io.File
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class PackType(
    val index: Int,
    val archive: Int,
    val codecClass: KClass<*>,
    val name: String,
    val gameValGroup: Js5GameValGroup? = null,
    val tomlMapper: TomlMapper,
    val kType: KType
)

class PackConfig(
    private val directory : File,
    private val tokenizedReplacement: Map<String,String> = emptyMap()
) : CacheTask() {


    fun MutableList<String?>.fromOptions(keyName: String, content: Map<String, Any?>) {
        (1..5).mapNotNull { content["${keyName}$it"] as? TomlValue.String }
            .forEachIndexed { index, option -> this[index] = option.value }
    }


    init {

        registerPackType(ITEM, ItemCodec::class, "item",Js5GameValGroup.OBJTYPES, tomlMapper = tomlMapper {
            addDecoder<ItemType> { content , def: ItemType ->
                content["equipmentType"]?.let { typeValue ->
                    val type = (typeValue as? TomlValue.String)?.value ?: error("equipmentType must be a string")
                    val slotType = ItemSlotType.fetchType(type) ?: error("Unable to find slot type for $type")
                    def.apply {
                        equipSlot = slotType.slot
                        appearanceOverride1 = slotType.override1
                        appearanceOverride2 = slotType.override2
                    }
                }
                def.options.fromOptions("option", content)
                def.interfaceOptions.fromOptions("ioption", content)
            }
        }, kType = typeOf<List<ItemType>>())

        registerPackType(index = TEXTURES, archive = 0, codec = TextureCodec::class, name = "texture", kType = typeOf<List<TextureType>>())

        registerPackType(OBJECT, ObjectCodec::class, "object",Js5GameValGroup.LOCTYPES, tomlMapper = tomlMapper {
            addDecoder<ObjectType> { content , def: ObjectType ->
                def.actions.fromOptions("option", content)
            }
        }, kType = typeOf<List<ObjectType>>())

        registerPackType(ENUM, EnumCodec::class, "enum", tomlMapper =  tomlMapper {
            addDecoder<EnumType> { content , def: EnumType ->

                if (content.containsKey("clear")) {
                    def.values.clear()
                }

                val value = (content["default"] as? TomlValue.String)?.value

                if (value.isNullOrEmpty()) {
                    def.defaultString = ""
                } else {
                    when {
                        value.all { it.isDigit() } -> def.defaultInt = value.toInt()
                        else -> def.defaultString = value
                    }
                }
            }
        }, kType = typeOf<List<EnumType>>())

        registerPackType(SPOTANIM, SpotAnimCodec::class, "graphics", Js5GameValGroup.SPOTTYPES, kType = typeOf<List<SpotAnimType>>())
        registerPackType(SPOTANIM, SpotAnimCodec::class, "graphic", Js5GameValGroup.SPOTTYPES, kType = typeOf<List<SpotAnimType>>())
        registerPackType(SEQUENCE, SequenceCodec::class, "animation", Js5GameValGroup.SEQTYPES, kType = typeOf<List<SequenceType>>())

        registerPackType(NPC, NPCCodec::class, "npc",Js5GameValGroup.NPCTYPES, tomlMapper = tomlMapper {
            addDecoder<NpcType> { content , def: NpcType ->
                def.actions.fromOptions("option", content)
            }
        }, kType = typeOf<List<NpcType>>())

        registerPackType(VARBIT, VarBitCodec::class, "varbit",Js5GameValGroup.VARBITTYPES, kType = typeOf<List<VarBitType>>())

        registerPackType(AREA, AreaCodec::class, "area", tomlMapper = tomlMapper {
            addDecoder<AreaType> { content , def: AreaType ->
                def.options.fromOptions("option", content)
            }
        }, kType = typeOf<List<AreaType>>())

        registerPackType(HEALTHBAR, HealthBarCodec::class, "health", kType = typeOf<List<HealthBarType>>())
        registerPackType(HITSPLAT, HitSplatCodec::class, "hitsplat", kType = typeOf<List<HitSplatType>>())
        registerPackType(IDENTKIT, IdentityKitCodec::class, "idk", kType = typeOf<List<IdentityKitType>>())
        registerPackType(INV, InventoryCodec::class, "inventory",Js5GameValGroup.INVTYPES, kType = typeOf<List<InventoryType>>())
        registerPackType(OVERLAY, OverlayCodec::class, "overlay", kType = typeOf<List<OverlayType>>())
        registerPackType(UNDERLAY, OverlayCodec::class, "underlay", kType = typeOf<List<UnderlayType>>())
        registerPackType(PARAMS, ParamCodec::class, "params", kType = typeOf<List<ParamType>>())
        registerPackType(VARPLAYER, VarCodec::class, "varp",Js5GameValGroup.VARPTYPES, kType = typeOf<List<VarpType>>())
        registerPackType(VARCLIENT, VarClientCodec::class, "varclient", kType = typeOf<List<VarClientType>>())

    }


    internal val logger = InlineLogger()

    @OptIn(InternalAPI::class)
    override fun init(cache: Cache) {
        val size = getFiles(directory, "toml").size
        val progress = progress("Packing Configs", size)
        if (size != 0) {
            getFiles(directory, "toml").forEach {
                progress.extraMessage = it.name


                val document = TomlValue.from(processDocumentChanges(it.readText()))
                document.properties.forEach { prop ->
                    val packType: PackType? = packTypes[prop.key]
                    packType?.let {
                        val decodedDefinitions : List<Definition> = packType.tomlMapper.decode(packType.kType, prop.value)
                        val decodedDefinitionsRaw : List<Map<String, Any>> = packType.tomlMapper.decode(prop.value)
                        val codecInstance = createCodecInstance(packType)
                        decodedDefinitions.forEachIndexed { index, definition ->
                            val def = decodedDefinitionsRaw[index]
                            val inherit = def["inherit"]?.toString()?.toIntOrNull() ?: -1
                            val debugName = def["debugName"]?.toString() ?: ""
                            try {
                                packDefinition(packType, definition, codecInstance,cache,inherit,debugName)
                            }catch (e : Exception) {
                                println("Unable to pack ${packType.name} with ID ${definition.id} due to an error: ${e.message}")
                            }
                        }
                    }
                }

                progress.step()
            }
            progress.close()
        }
    }

    private fun <T : Definition> packDefinition(
        packType: PackType,
        def : Definition,
        codec: DefinitionCodec<T>,
        cache: Cache,
        inherit : Int,
        debugName : String
    ) {
        val index = packType.index
        val archive = packType.archive
        var definition = def
        if (def.id == -1) {
            logger.info { "Unable to pack as the ID is -1 or has not been defined" }
            return
        }

        val defId = def.id

        if (inherit != -1) {
            val inheritedDef = getInheritedDefinition(inherit, codec,archive, index,cache)
            inheritedDef?.let {
                definition = mergeDefinitions(it, def as T, codec)
            } ?: run {
                logger.warn { "No inherited definition found for ID (${def.id} inheritdID ${inherit}) [${def::class.simpleName}]" }
                return
            }
        }



        if (packType.gameValGroup != null) {
            val matchingName : String = debugName
            val name = when {
                matchingName.isEmpty() || !matchingName.contains(".") -> {
                    when (packType.gameValGroup) {
                        Js5GameValGroup.OBJTYPES -> (def as ItemType).name
                        Js5GameValGroup.NPCTYPES -> (def as NpcType).name
                        Js5GameValGroup.LOCTYPES -> (def as ObjectType).name
                        else -> null
                    }
                }
                else -> matchingName.substringAfter(".")
            }

            if (!name.isNullOrBlank() && name != "null") {
                CacheTool.addGameValMapping(packType.gameValGroup, name.lowercase(), defId)
            }
        }
        
        val writer = Unpooled.buffer(4096)
        with(codec) { writer.encode(definition as T) }
        cache.write(index,archive,defId,writer.toArray())
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

    private fun processDocumentChanges(content: String): String {
        val tokenMap = tokenizedReplacement.toMutableMap()
        val regex = Regex("""\[\[tokenizedReplacement]](.*?)(?=\n\[\[|\z)""", RegexOption.DOT_MATCHES_ALL)

        regex.find(content)?.groups?.get(1)?.value
            ?.lineSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.forEach { parseInlineTableString(it).forEach { (k, v) -> tokenMap[k] = v.removeSurrounding("\"") } }

        var updated = content
        tokenMap.forEach { (key, value) ->
            updated = updated.replace(Regex("%${Regex.escape(key)}%", RegexOption.IGNORE_CASE), value)
        }

        return processRSCMModifier(updated)
    }

    private fun parseInlineTableString(input: String): List<Pair<String, String>> =
        input.removePrefix("{").removeSuffix("}")
            .split(",")
            .map { it.split("=").map { it.trim() } }
            .map { it[0].lowercase() to it[1] }

    private fun createCodecInstance(codec: PackType): DefinitionCodec<*> {
        val constructor = codec.codecClass.constructors.first()
        val params = constructor.parameters.size

        return if (params == 0) {
            constructor.call() as DefinitionCodec<*>
        } else {
            constructor.call(revision) as DefinitionCodec<*>
        }
    }


    private fun processRSCMModifier(input: String): String {
        val allowedPrefixes = RSCMHandler.rscmTypes
        val output = StringBuilder()
        var debugNameAdded = false

        val quotedStringRegex = Regex(""""([^"]+)"""")

        input.lines().forEach { line ->
            val trimmed = line.trim()

            if (trimmed.startsWith("[[")) {
                debugNameAdded = false
                output.appendLine(line)
                return@forEach
            }

            var modifiedLine = line
            val matches = quotedStringRegex.findAll(line)

            for (match in matches) {
                val fullValue = match.groupValues[1]
                if (allowedPrefixes.any { fullValue.startsWith(it) }) {
                    val resolved = RSCMHandler.getMapping(fullValue) ?: error("Invalid RSCM reference: \"$fullValue\" not found")
                    modifiedLine = modifiedLine.replace("\"$fullValue\"", resolved.toString())

                    if (!debugNameAdded && trimmed.startsWith("id") && fullValue == match.groupValues[1]) {
                        output.appendLine("debugName = \"$fullValue\"")
                        debugNameAdded = true
                    }
                }
            }

            output.appendLine(modifiedLine)
        }

        return output.toString()
    }

    companion object {

        val tomlMapperDefault = tomlMapper {  }
        val packTypes = mutableMapOf<String, PackType>()

        fun registerPackType(
            archive: Int, codec: KClass<*>,
            name: String,
            gameValGroup: Js5GameValGroup? = null,
            index: Int = CONFIGS,
            tomlMapper: TomlMapper = tomlMapperDefault,
            kType: KType,
        ) {
            val packType = PackType(index,archive, codec, name, gameValGroup,tomlMapper,kType)
            packTypes[packType.name] = packType
        }

        fun registerPackType(
            index: Int,
            archive: Int,
            codec: KClass<*>,
            name: String,
            gameValGroup: Js5GameValGroup? = null,
            kType: KType,
        ) {
            val packType = PackType(index,archive, codec, name,gameValGroup,tomlMapperDefault,kType)
            packTypes[packType.name] = packType
        }

    }

}
