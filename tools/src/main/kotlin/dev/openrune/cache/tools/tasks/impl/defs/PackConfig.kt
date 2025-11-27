package dev.openrune.cache.tools.tasks.impl.defs

import com.github.michaelbull.logging.InlineLogger
import cc.ekblad.toml.TomlMapper
import cc.ekblad.toml.model.TomlValue
import cc.ekblad.toml.serialization.from
import cc.ekblad.toml.tomlMapper
import cc.ekblad.toml.util.InternalAPI
import dev.openrune.cache.*
import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.item.ItemSlotType
import dev.openrune.definition.util.toArray
import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.*
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.ItemParam
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.constants.ConstantProvider
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
    val gameValGroup: GameValGroupTypes? = null,
    val tomlMapper: TomlMapper,
    val kType: KType
)

class PackConfig(
    private val directory : File,
    private val tokenizedReplacement: Map<String,String> = emptyMap()
) : CacheTask() {

    val skillNameToId = mapOf(
        "ATTACK" to 0,
        "DEFENCE" to 1,
        "STRENGTH" to 2,
        "HITPOINTS" to 3,
        "RANGED" to 4,
        "PRAYER" to 5,
        "MAGIC" to 6,
        "COOKING" to 7,
        "WOODCUTTING" to 8,
        "FLETCHING" to 9,
        "FISHING" to 10,
        "FIREMAKING" to 11,
        "CRAFTING" to 12,
        "SMITHING" to 13,
        "MINING" to 14,
        "HERBLORE" to 15,
        "AGILITY" to 16,
        "THIEVING" to 17,
        "SLAYER" to 18,
        "FARMING" to 19,
        "RUNECRAFTING" to 20,
        "HUNTER" to 21,
        "CONSTRUCTION" to 22
    )

    private fun MutableList<String?>.fromOptions(  id: Int, name: String,keyName: String, content: Map<String, Any?>) {
        val context = buildString {
            append("[$id]")
            if (name.isNotBlank()) append(" [$name]")
        }

        val invalidKeys = content.keys.filter { it.startsWith(keyName) }.mapNotNull {
            val suffix = it.removePrefix(keyName).toIntOrNull()
            if (suffix != null && (suffix < 1 || suffix > 5)) suffix else null
        }

        if (invalidKeys.isNotEmpty()) {
            val sorted = invalidKeys.sorted()
            val keyString = sorted.joinToString()
            when {
                sorted.any { it < 1 } -> println("$context Warning: Invalid keys for '$keyName': $keyString — indices must start at 1.")
                sorted.any { it > 5 } -> println("$context Warning: Invalid keys for '$keyName': $keyString — indices must not exceed 5.")
                else -> println("Warning: Invalid option key(s): ${sorted.joinToString()}.")
            }
            return
        }

        for (i in 1..5) {
            val key = "$keyName$i"
            val value = content[key]
            if (value != null) {
                this[i - 1] = (value as? TomlValue.String)?.value
            }
        }
    }

    init {

        registerPackType(ITEM, ItemCodec::class, "item",GameValGroupTypes.OBJTYPES, tomlMapper = tomlMapper {
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
                (content["params"] as? TomlValue.Map)?.properties?.forEach { (key, value) ->
                    val param = ItemParam.entries.find { it.formattedName == key } ?: return@forEach
                    def.params?.remove(key)

                    val paramId = param.id.toString()

                    if (param.isSkillParam) {
                        val skillList = (value as? TomlValue.List)?.elements?.toList() ?: error("Expected a list for skill param '$paramId'.")

                        require(skillList.size >= 2) {
                            "Skill param '$paramId' must contain at least 2 elements (skill name/id and level)."
                        }

                        val skillId = when (val skillValue = skillList[0]) {
                            is TomlValue.Integer -> skillValue.value.toInt()
                            is TomlValue.String -> skillNameToId[skillValue.value.uppercase()] ?: error("Unknown skill name: '${skillValue.value}' in param '$paramId'.")
                            else -> error("First skill param must be an integer or string in param '$paramId'.")
                        }

                        val level = (skillList[1] as? TomlValue.Integer)?.value
                            ?: error("Second skill param must be an integer level in param '$paramId'.")

                        def.params?.apply {
                            put(paramId, skillId)
                            put(param.linkedLevelReqId.toString(), level)
                        }
                    } else {
                        val tomlValue = when (value) {
                            is TomlValue.Integer -> value.value.toString()
                            is TomlValue.String -> value.value
                            else -> ""
                        }
                        def.params?.put(paramId, tomlValue)
                    }
                }

                def.options.fromOptions(def.id,def.name,"option", content)
                def.interfaceOptions.fromOptions(def.id,def.name,"ioption", content)
            }
        }, kType = typeOf<List<ItemType>>())

        registerPackType(index = TEXTURES, archive = 0, codec = TextureCodec::class, name = "texture", kType = typeOf<List<TextureType>>())

        registerPackType(OBJECT, ObjectCodec::class, "object",GameValGroupTypes.LOCTYPES, tomlMapper = tomlMapper {
            addDecoder<ObjectType> { content , def: ObjectType ->
                def.actions.fromOptions(def.id,def.name,"option", content)
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

        registerPackType(SPOTANIM, SpotAnimCodec::class, "graphics", GameValGroupTypes.SPOTTYPES, kType = typeOf<List<SpotAnimType>>())
        registerPackType(SPOTANIM, SpotAnimCodec::class, "graphic", GameValGroupTypes.SPOTTYPES, kType = typeOf<List<SpotAnimType>>())
        registerPackType(SEQUENCE, SequenceCodec::class, "animation", GameValGroupTypes.SEQTYPES, kType = typeOf<List<SequenceType>>())

        registerPackType(NPC, NPCCodec::class, "npc",GameValGroupTypes.NPCTYPES, tomlMapper = tomlMapper {
            addDecoder<NpcType> { content , def: NpcType ->
                def.actions.fromOptions(def.id,def.name,"option", content)
            }
        }, kType = typeOf<List<NpcType>>())

        registerPackType(VARBIT, VarBitCodec::class, "varbit",GameValGroupTypes.VARBITTYPES, kType = typeOf<List<VarBitType>>())

        registerPackType(AREA, AreaCodec::class, "area", tomlMapper = tomlMapper {
            addDecoder<AreaType> { content , def: AreaType ->
                def.options.fromOptions(def.id,def.name,"option", content)
            }
        }, kType = typeOf<List<AreaType>>())

        registerPackType(HEALTHBAR, HealthBarCodec::class, "health", kType = typeOf<List<HealthBarType>>())
        registerPackType(HITSPLAT, HitSplatCodec::class, "hitsplat", kType = typeOf<List<HitSplatType>>())
        registerPackType(IDENTKIT, IdentityKitCodec::class, "idk", kType = typeOf<List<IdentityKitType>>())
        registerPackType(INV, InventoryCodec::class, "inventory",GameValGroupTypes.INVTYPES, kType = typeOf<List<InventoryType>>())
        registerPackType(OVERLAY, OverlayCodec::class, "overlay", kType = typeOf<List<OverlayType>>())
        registerPackType(UNDERLAY, OverlayCodec::class, "underlay", kType = typeOf<List<UnderlayType>>())
        registerPackType(PARAMS, ParamCodec::class, "params", kType = typeOf<List<ParamType>>())
        registerPackType(VARPLAYER, VarCodec::class, "varp",GameValGroupTypes.VARPTYPES, kType = typeOf<List<VarpType>>())
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
            val name = debugName.ifEmpty {
                when (packType.gameValGroup) {
                    GameValGroupTypes.OBJTYPES -> (def as ItemType).name
                    GameValGroupTypes.NPCTYPES -> (def as NpcType).name
                    GameValGroupTypes.LOCTYPES -> (def as ObjectType).name
                    else -> null
                }
            }
            if (!name.isNullOrBlank() && name != "null") {
                CacheTool.addGameValMapping(packType.gameValGroup, GameValElement(name, defId))
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
        val allowedPrefixes = ConstantProvider.types
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
                    val resolved = ConstantProvider.getMapping(fullValue) ?: error("Invalid RSCM reference: \"$fullValue\" not found")
                    modifiedLine = modifiedLine.replace("\"$fullValue\"", resolved.toString())

                    if (!debugNameAdded && trimmed.startsWith("id") && fullValue == match.groupValues[1]) {
                        output.appendLine("debugName = \"${fullValue.substringAfter(".")}\"")
                        debugNameAdded = true
                    }
                }
            }

            output.appendLine(modifiedLine)
        }

        return output.toString()
    }

    companion object {

        private val tomlMapperDefault = tomlMapper {  }
        val packTypes = mutableMapOf<String, PackType>()

        fun registerPackType(
            archive: Int, codec: KClass<*>,
            name: String,
            gameValGroup: GameValGroupTypes? = null,
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
            gameValGroup: GameValGroupTypes? = null,
            kType: KType,
        ) {
            val packType = PackType(index,archive, codec, name,gameValGroup,tomlMapperDefault,kType)
            packTypes[packType.name] = packType
        }

    }

}
