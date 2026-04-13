package dev.openrune.cache.tools.tasks.impl.defs

import com.github.michaelbull.logging.InlineLogger
import cc.ekblad.toml.TomlMapper
import cc.ekblad.toml.decode
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
import dev.openrune.definition.EntityOpsDefinition
import dev.openrune.definition.type.*
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.ItemParam
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.codec.*
import dev.openrune.definition.util.CacheVarLiteral
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

    private fun EntityOpsDefinition.fromOptions(id: Int, name: String, keyName: String, content: Map<String, Any?>) {
        val context = buildString {
            append("[$id]")
            if (name.isNotBlank()) append(" [$name]")
        }

        // Old format support: option1..option5
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
            val text = (value as? TomlValue.String)?.value ?: continue
            setOp(i - 1, text)
        }

        // New format support:
        // option = [{ slot=1, text="Open" }]
        ((content[keyName] as? TomlValue.List)?.elements ?: emptyList()).forEach { element ->
            val map = (element as? TomlValue.Map)?.properties ?: return@forEach
            val slot = ((map["slot"] as? TomlValue.Integer)?.value?.toInt() ?: return@forEach) - 1
            val text = (map["text"] as? TomlValue.String)?.value ?: return@forEach
            if (slot in 0..4) {
                setOp(slot, text)
            }
        }

        // Slot-keyed subop format:
        // subop1 = [{ index = 1, text = "Use (sub)" }]
        // Slot is inferred from suffix (1..5 -> slots 0..4)
        val subOpKeyRegex = Regex("""^subop([1-5])$""")
        content.forEach { (rawKey, rawValue) ->
            val match = subOpKeyRegex.matchEntire(rawKey) ?: return@forEach
            val slot = match.groupValues[1].toInt() - 1
            val rows = (rawValue as? TomlValue.List)?.elements ?: return@forEach
            rows.forEach { row ->
                val map = (row as? TomlValue.Map)?.properties ?: return@forEach
                val subId = ((map["index"] as? TomlValue.Integer)?.value?.toInt() ?: return@forEach) - 1
                val text = (map["text"] as? TomlValue.String)?.value ?: return@forEach
                if (slot in 0..4 && subId >= 0) {
                    setSubOp(slot, subId, text)
                }
            }
        }

        // Slot-keyed conditional op format:
        // multiop1 = { text = "Activate", varp = 100, varbit = 200, min = 1, max = 9 }
        // or
        // multiop1 = [{ text = "Activate", varp = 100, varbit = 200, min = 1, max = 9 }]
        // Slot is inferred from suffix (1..5 -> slots 0..4)
        val conditionalOpKeyRegex = Regex("""^multiop([1-5])$""")
        content.forEach { (rawKey, rawValue) ->
            val match = conditionalOpKeyRegex.matchEntire(rawKey) ?: return@forEach
            val slot = match.groupValues[1].toInt() - 1
            val rows: List<TomlValue> = when (rawValue) {
                is TomlValue.Map -> listOf(rawValue)
                is TomlValue.List -> rawValue.elements
                else -> emptyList()
            }
            rows.forEach { row ->
                val map = (row as? TomlValue.Map)?.properties ?: return@forEach
                val text = (map["text"] as? TomlValue.String)?.value ?: return@forEach
                val varp = (map["varp"] as? TomlValue.Integer)?.value?.toInt() ?: 0
                val varbit = (map["varbit"] as? TomlValue.Integer)?.value?.toInt() ?: 0
                val min = (map["min"] as? TomlValue.Integer)?.value?.toInt() ?: 0
                val max = (map["max"] as? TomlValue.Integer)?.value?.toInt() ?: 0
                if (slot in 0..4) {
                    setConditionalOp(slot, text, varp, varbit, min, max)
                }
            }
        }

        // Compact conditional-sub format:
        // multisubop1 = [
        //   { index = 3, text = "Activate (sub)", varp = 300, varbit = 400, min = 2, max = 8 },
        //   ...
        // ]
        // Slot is inferred from suffix (1..5 -> slots 0..4)
        val compactKeyRegex = Regex("""^multisubop([1-5])$""")
        content.forEach { (rawKey, rawValue) ->
            val match = compactKeyRegex.matchEntire(rawKey) ?: return@forEach
            val slot = match.groupValues[1].toInt() - 1
            val rows = (rawValue as? TomlValue.List)?.elements ?: return@forEach
            rows.forEach { row ->
                val map = (row as? TomlValue.Map)?.properties ?: return@forEach
                val subId = ((map["index"] as? TomlValue.Integer)?.value?.toInt() ?: return@forEach) - 1
                val text = (map["text"] as? TomlValue.String)?.value ?: return@forEach
                val varp = (map["varp"] as? TomlValue.Integer)?.value?.toInt() ?: 0
                val varbit = (map["varbit"] as? TomlValue.Integer)?.value?.toInt() ?: 0
                val min = (map["min"] as? TomlValue.Integer)?.value?.toInt() ?: 0
                val max = (map["max"] as? TomlValue.Integer)?.value?.toInt() ?: 0
                if (slot in 0..4 && subId >= 0) {
                    setConditionalSubOp(slot, subId, text, varp, varbit, min, max)
                }
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
                    val param = ItemParam.entries.find { it.formattedName == key }
                    def.params?.remove(key)

                    val paramId = key

                    if (param?.isSkillParam == true) {
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
                            is TomlValue.Integer -> value.value.toInt()
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

                val keyLiteral = (content["type"] as? TomlValue.String)?.value
                val valueLiteral = (content["valueVarType"] as? TomlValue.String)?.value
                keyLiteral?.let { def.keyType = CacheVarLiteral.byName(it) }
                valueLiteral?.let { def.valueType = CacheVarLiteral.byName(it) }

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

                (content["params"] as? TomlValue.Map)?.properties?.forEach { (key, value) ->
                    def.params?.remove(key)

                    val paramId = key

                    val tomlValue = when (value) {
                        is TomlValue.Integer -> value.value.toInt()
                        is TomlValue.String -> value.value
                        else -> ""
                    }

                    def.params?.put(paramId, tomlValue)
                }
            }
        }, kType = typeOf<List<NpcType>>())

        registerPackType(VARBIT, VarBitCodec::class, "varbit",GameValGroupTypes.VARBITTYPES, kType = typeOf<List<VarBitType>>())

        registerPackType(MAP_ELEMENT, MapElementCodec::class, "mapelement", tomlMapper = tomlMapper {
            addDecoder<MapElementType> { content, def: MapElementType ->
                def.options.fromOptions(def.id,def.name,"option", content)
            }
        }, kType = typeOf<List<MapElementType>>())


        registerPackType(HEALTHBAR, HealthBarCodec::class, "health", kType = typeOf<List<HealthBarType>>())
        registerPackType(HITSPLAT, HitSplatCodec::class, "hitsplat", kType = typeOf<List<HitSplatType>>())
        registerPackType(IDENTKIT, IdentityKitCodec::class, "idk", kType = typeOf<List<IdentityKitType>>())
        registerPackType(INV, InventoryCodec::class, "inventory",GameValGroupTypes.INVTYPES, kType = typeOf<List<InventoryType>>())
        registerPackType(OVERLAY, OverlayCodec::class, "overlay", kType = typeOf<List<OverlayType>>())
        registerPackType(UNDERLAY, OverlayCodec::class, "underlay", kType = typeOf<List<UnderlayType>>())
        registerPackType(PARAMS, ParamCodec::class, "params", tomlMapper = tomlMapper {
            addDecoder<ParamType> { content, def: ParamType ->
                (content["varType"] as? TomlValue.String)?.value?.let { def.type = CacheVarLiteral.byName(it) }
            }
        }, kType = typeOf<List<ParamType>>())
        registerPackType(VARPLAYER, VarCodec::class, "varp",GameValGroupTypes.VARPTYPES, kType = typeOf<List<VarpType>>())
        registerPackType(VARCLIENT, VarClientCodec::class, "varclient", kType = typeOf<List<VarClientType>>())

    }
    data class DefToPack(
        val fileName: String,
        val tableKey: String,
        val definition: Definition,
        val raw: Map<String, Any>,
        val packType: PackType
    )


    internal val logger = InlineLogger()

    @OptIn(InternalAPI::class)
    override fun init(cache: Cache) {
        val files = getFiles(directory, "toml")

        data class DefToPack(
            val fileName: String,
            val tableKey: String,
            val definition: Definition,
            val raw: Map<String, Any>,
            val packType: PackType
        )

        val definitionsToPack = mutableListOf<DefToPack>()

        files.forEach { file ->
            val document = TomlValue.from(processDocumentChanges(file.readText()))
            document.properties.forEach { prop ->
                val packType = packTypes[prop.key] ?: return@forEach

                val decodedDefinitions: List<Definition> = packType.tomlMapper.decode(packType.kType, prop.value)
                val decodedDefinitionsRaw: List<Map<String, Any>> = packType.tomlMapper.decode(prop.value)

                decodedDefinitions.zip(decodedDefinitionsRaw).forEach { (definition, defRaw) ->
                    val serverOnly = defRaw["isServerOnly"]?.toString()?.toBoolean() ?: false
                    if (serverOnly && !serverPass) return@forEach

                    definitionsToPack += DefToPack(file.name, prop.key, definition, defRaw, packType)
                }
            }
        }

        if (definitionsToPack.isEmpty()) return

        val progress = progress("Packing Configs", definitionsToPack.size)

        definitionsToPack.forEach { entry ->
            val inherit = entry.raw["inherit"]?.toString()?.toIntOrNull() ?: -1
            val debugName = entry.raw["debugName"]?.toString() ?: ""

            progress.extraMessage = "${entry.fileName.replace(".toml","")} (${entry.definition.id})"

            try {
                val codecInstance = createCodecInstance(entry.packType)
                packDefinition(entry.packType, entry.definition, codecInstance, cache, inherit, debugName)
            } catch (e: Exception) {
                println("Unable to pack ${entry.packType.name} with ID ${entry.definition.id} due to an error: ${e.message}")
            }

            progress.step()
        }

        progress.close()
    }

    @OptIn(InternalAPI::class)
    fun <T : Definition> PackType.decodeWithRaw(propValue: String): List<Pair<T, Map<String, Any>>> {
        val rawList: List<Map<String, Any>> = this.tomlMapper.decode(propValue)
        return rawList.map { raw ->
            val def: T = this.tomlMapper.decode(this.kType, raw as TomlValue)
            def to raw
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

    /**
     * Merges [childDef] onto [parentDef] (call site passes parent from cache first, then child from TOML).
     * A field from the child replaces the parent's value only when the child differs from both the parent
     * and the codec default. Mutable nested types like [EntityOpsDefinition] must compare by content, not
     * reference, or an "empty" child instance wrongly overwrites a non-empty parent.
     */
    private fun <T : Definition> mergeDefinitions(parentDef: T, childDef: T, codec: DefinitionCodec<T>): T {
        val defaultDef = codec.createDefinition()

        defaultDef::class.java.declaredFields.forEach { field ->
            if (!Modifier.isStatic(field.modifiers)) {
                field.isAccessible = true
                val parentValue = field.get(parentDef)
                val childValue = field.get(childDef)
                val defaultValue = field.get(defaultDef)

                if (!mergeFieldValuesEqual(childValue, parentValue) && !mergeFieldValuesEqual(childValue, defaultValue)) {
                    field.set(parentDef, childValue)
                }
            }
        }

        return parentDef
    }

    private fun mergeFieldValuesEqual(a: Any?, b: Any?): Boolean {
        if (a === b) return true
        if (a is EntityOpsDefinition && b is EntityOpsDefinition) return a.contentEquals(b)
        return a == b
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

        return normalizeLegacyVarLiteralKeys(processRSCMModifier(updated))
    }

    private fun normalizeLegacyVarLiteralKeys(input: String): String {
        val out = StringBuilder()
        var currentTable: String? = null

        input.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("[[") && trimmed.endsWith("]]")) {
                currentTable = trimmed.removePrefix("[[").removeSuffix("]]").trim()
                out.appendLine(line)
                return@forEach
            }

            var modified = line
            when (currentTable) {
                "params" -> {
                    modified = modified.replace(Regex("""^(\s*)type(\s*=)"""), "$1varType$2")
                }
                "enum" -> {
                    modified = modified.replace(Regex("""^(\s*)keyType(\s*=)"""), "$1keyVarType$2")
                    modified = modified.replace(Regex("""^(\s*)valueType(\s*=)"""), "$1valueVarType$2")
                    modified = modified.replace(Regex("""^(\s*)keyLit(\s*=)"""), "$1keyVarType$2")
                    modified = modified.replace(Regex("""^(\s*)valueLit(\s*=)"""), "$1valueVarType$2")
                }
            }
            out.appendLine(modified)
        }

        return out.toString()
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
