package dev.openrune.cache.tools.tasks.impl.defs

import dev.openrune.toml.model.TomlValue
import dev.openrune.toml.rsconfig.*
import dev.openrune.toml.tomlMapper
import dev.openrune.toml.util.InternalAPI
import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.*
import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.incremental.PackUnit
import dev.openrune.cache.tools.incremental.RecordingCache
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.getFiles
import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.EntityOpsDefinition
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.codec.*
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.type.*
import dev.openrune.definition.util.toArray
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import java.io.File
import java.lang.reflect.Modifier
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class PackType(
    val index: Int,
    val archive: Int,
    val codecClass: KClass<*>,
    val name: String,
    val gameValGroup: GameValGroupTypes? = null,
    val tomlMapper: dev.openrune.toml.TomlMapper,
    val kType: KType,
)

class PackConfig(
    internal val directory: File,
    private val tokenizedReplacements: Map<String, String> = emptyMap(),
    private val tokenizedFile: Path? = null,
) : CacheTask() {

    private data class DefToPack(
        val fileName: String,
        val tableKey: String,
        val definition: Definition,
        val raw: Map<String, TomlValue>,
        val packType: PackType,
    )

    val mapper = tomlMapper {
        rsconfig {
            enableConstantProvider()
            enabledTokenizedReplacement(tokenizedReplacements, tokenizedFile)
        }
    }

    init {
        registerPackType(ITEM, ItemCodec::class, "item", GameValGroupTypes.OBJTYPES, kType = typeOf<List<ItemType>>())
        registerPackType(index = TEXTURES, archive = 0, codec = TextureCodec::class, name = "texture", kType = typeOf<List<TextureType>>())
        registerPackType(OBJECT, ObjectCodec::class, "object", GameValGroupTypes.LOCTYPES, kType = typeOf<List<ObjectType>>())
        registerPackType(ENUM, EnumCodec::class, "enum", kType = typeOf<List<EnumType>>())
        registerPackType(SPOTANIM, SpotAnimCodec::class, "graphics", GameValGroupTypes.SPOTTYPES, kType = typeOf<List<SpotAnimType>>())
        registerPackType(SPOTANIM, SpotAnimCodec::class, "graphic", GameValGroupTypes.SPOTTYPES, kType = typeOf<List<SpotAnimType>>())
        registerPackType(SEQUENCE, SequenceCodec::class, "animation", GameValGroupTypes.SEQTYPES, kType = typeOf<List<SequenceType>>())
        registerPackType(NPC, NPCCodec::class, "npc", GameValGroupTypes.NPCTYPES, kType = typeOf<List<NpcType>>())
        registerPackType(VARBIT, VarBitCodec::class, "varbit", GameValGroupTypes.VARBITTYPES, kType = typeOf<List<VarBitType>>())
        registerPackType(MAP_ELEMENT, MapElementCodec::class, "mapelement", kType = typeOf<List<MapElementType>>())
        registerPackType(HEALTHBAR, HealthBarCodec::class, "health", kType = typeOf<List<HealthBarType>>())
        registerPackType(AMBIENCE, AmbienceCodec::class, "ambience", kType = typeOf<List<AmbienceType>>())
        registerPackType(HITSPLAT, HitSplatCodec::class, "hitsplat", kType = typeOf<List<HitSplatType>>())
        registerPackType(IDENTKIT, IdentityKitCodec::class, "idk", kType = typeOf<List<IdentityKitType>>())
        registerPackType(INV, InventoryCodec::class, "inventory", GameValGroupTypes.INVTYPES, kType = typeOf<List<InventoryType>>())
        registerPackType(OVERLAY, OverlayCodec::class, "overlay", kType = typeOf<List<OverlayType>>())
        registerPackType(UNDERLAY, OverlayCodec::class, "underlay", kType = typeOf<List<UnderlayType>>())
        registerPackType(PARAMS, ParamCodec::class, "params", kType = typeOf<List<ParamType>>())
        registerPackType(VARPLAYER, VarCodec::class, "varp", GameValGroupTypes.VARPTYPES, kType = typeOf<List<VarpType>>())
        registerPackType(VARCLIENT, VarClientCodec::class, "varclient", kType = typeOf<List<VarClientType>>())
    }

    internal val logger = InlineLogger()

    @OptIn(InternalAPI::class)
    override fun init(cache: Cache) {
        val files = getFiles(directory, "toml")
        if (files.isEmpty()) return

        val blocks = files.flatMap(ConfigBlocks::scan).filter { packTypes.containsKey(it.name) }
        if (blocks.isEmpty()) return

        val (varpBlocks, otherBlocks) = blocks.partition { it.name == "varp" }
        val varpConfigById = varpBlocks.mapNotNull { block -> block.id?.let { it to block.serverOnly } }.toMap()

        val order = ConfigBlocks.inheritOrder(blocks)

        val tokens = tokenizedReplacements.entries.sortedBy { it.key }.joinToString(",") { "${it.key}=${it.value}" }
        val tokenScope = if (tokens.isEmpty()) "" else "|tokens=${tokens.hashCode()}"

        val decoded = DecodedBlocks()

        listOf(VARP_PASS to varpBlocks, OTHER_PASS to otherBlocks).forEach { (pass, passBlocks) ->
            if (passBlocks.isEmpty()) return@forEach
            val byKey = passBlocks.associateBy { it.unitKey }
            val units = passBlocks.sortedBy(order).map { block ->
                PackUnit(
                    key = block.unitKey,
                    sources = listOf(block.file),
                    label = block.label,
                    fingerprint = block.fingerprint,
                )
            }
            incremental.run(
                task = this,
                scope = "${directory.absolutePath}$tokenScope|$pass",
                label = if (pass == VARP_PASS) "Packing Varps" else "Packing Configs",
                cache = cache,
                units = units,
                extraDeps = listOfNotNull(tokenizedFile?.toFile()),
            ) { packCache, unit ->
                packBlock(packCache, byKey.getValue(unit.key), decoded, varpConfigById)
            }
        }
    }

    @OptIn(InternalAPI::class)
    private inner class DecodedBlocks {
        private val byFile = HashMap<File, Map<String, Map<String, TomlValue>>>()

        fun propertiesOf(block: RawBlock): Map<String, TomlValue>? =
            byFile.getOrPut(block.file) { decode(block.file) }[block.matchKey]

        private fun decode(file: File): Map<String, Map<String, TomlValue>> {
            val listener = ConstantProvider.lookupListener
            ConstantProvider.lookupListener = null
            try {
                return decodeBlocks(file)
            } finally {
                ConstantProvider.lookupListener = listener
            }
        }

        private fun decodeBlocks(file: File): Map<String, Map<String, TomlValue>> {
            val result = HashMap<String, Map<String, TomlValue>>()
            val ordinals = HashMap<String, Int>()
            mapper.decodeRuneScapeBlocks(file.toPath()).forEach { decodedBlock ->
                val properties = decodedBlock.map.properties
                val ordinal = ordinals.merge(decodedBlock.name, 1, Int::plus)!! - 1
                val id = (properties["id"] as? TomlValue.Integer)?.value?.toInt()
                val key = if (id != null) "${decodedBlock.name}#$id" else "${decodedBlock.name}@$ordinal"
                result.putIfAbsent(key, properties)
            }
            return result
        }
    }

    @OptIn(InternalAPI::class)
    private fun packBlock(
        cache: Cache,
        block: RawBlock,
        decoded: DecodedBlocks,
        varpConfigById: Map<Int, Boolean>,
    ) {
        val packType = packTypes[block.name] ?: return

        block.constants.forEach { token ->
            ConstantProvider.notifyLookup(token, ConstantProvider.peekMapping(token))
        }

        val properties = decoded.propertiesOf(block)
            ?: error("Block ${block.label} was found by the scanner but not in the decoded file")

        val serverOnly = (properties["isServerOnly"] as? TomlValue.Bool)?.value ?: false
        if (serverOnly && !serverPass) return

        val def = packType.tomlMapper.decodeRuneScape(packType.kType, properties) as Definition
        val entry = DefToPack(block.file.name, block.name, def, properties, packType)

        val inherit = (properties["inherit"] as? TomlValue.Integer)?.value?.toInt() ?: -1
        val debugName = (properties["debugName"] as? TomlValue.String)?.value ?: ""

        if (entry.tableKey == "varbit") {
            validateVarbitVarp(entry, cache, varpConfigById)
        }

        try {
            val codecInstance = createCodecInstance(packType)
            packDefinition(packType, def, codecInstance, cache, inherit, debugName)
        } catch (e: Exception) {
            println("Unable to pack ${packType.name} with ID ${def.id} due to an error: ${e.message}")
        }
    }

    private fun validateVarbitVarp(
        entry: DefToPack,
        cache: Cache,
        varpConfigById: Map<Int, Boolean>,
    ) {
        val varbit = entry.definition as VarBitType
        val varpId = varbit.varp
        val varbitServerOnly = (entry.raw["isServerOnly"] as? TomlValue.Bool)?.value ?: false
        val varpServerOnly = varpConfigById[varpId]

        if (varpServerOnly == true && !serverPass && !varbitServerOnly) {
            error(
                "Varbit ${varbit.id} in ${entry.fileName} references varp $varpId which is marked isServerOnly, " +
                    "but the varbit is not marked isServerOnly",
            )
        }

        // Existence check only: the varbit's own bytes do not depend on the varp's contents, so this read
        // must not become a dependency. Recording it would repack every varbit whenever its varp is
        // repacked, which on a server cache is every build.
        val varpPresent = RecordingCache.unrecorded(cache) { cache.data(CONFIGS, VARPLAYER, varpId) } != null

        if (!varpPresent) {
            val configHint = when {
                varpServerOnly == true -> " (varp is marked isServerOnly and was not packed on this pass)"
                varpServerOnly == false -> " (varp config exists but was not found in cache after packing varps)"
                else -> ""
            }
            error(
                "Varbit ${varbit.id} in ${entry.fileName} references varp $varpId which does not exist in cache$configHint",
            )
        }
    }

    private fun <T : Definition> packDefinition(
        packType: PackType,
        def: Definition,
        codec: DefinitionCodec<T>,
        cache: Cache,
        inherit: Int,
        debugName: String,
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
            val inheritedDef = getInheritedDefinition(inherit, codec, archive, index, cache)
            inheritedDef?.let {
                definition = mergeDefinitions(it, def as T, codec)
            } ?: run {
                logger.warn { "No inherited definition found for ID (${def.id} inheritId $inherit) [${def::class.simpleName}]" }
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
        cache.write(index, archive, defId, writer.toArray())
    }

    private fun <T : Definition> getInheritedDefinition(
        inherit: Int,
        codec: DefinitionCodec<T>,
        archive: Int,
        index: Int,
        cache: Cache,
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
                val differsFromParent = !mergeFieldValuesEqual(childValue, parentValue)
                val differsFromDefault = !mergeFieldValuesEqual(childValue, defaultValue)

                if (differsFromParent && differsFromDefault) {
                    if (field.name == "params" && parentValue is Map<*, *> && childValue is Map<*, *>) {
                        val mergedParams = parentValue.toMutableMap().apply {
                            putAll(childValue)
                        }
                        field.set(parentDef, mergedParams)
                    } else {
                        field.set(parentDef, childValue)
                    }
                }
            }
        }

        return parentDef
    }

    private fun mergeFieldValuesEqual(a: Any?, b: Any?): Boolean {
        if (a === b) return true
        if ((a == null && b is Map<*, *> && b.isEmpty()) || (b == null && a is Map<*, *> && a.isEmpty())) return true
        if ((a == null && b is Collection<*> && b.isEmpty()) || (b == null && a is Collection<*> && a.isEmpty())) return true
        if (a is EntityOpsDefinition && b is EntityOpsDefinition) return a.contentEquals(b)
        return a == b
    }

    private fun createCodecInstance(codec: PackType): DefinitionCodec<*> {
        val constructor = codec.codecClass.constructors.first()
        val params = constructor.parameters.size

        return if (params == 0) {
            constructor.call() as DefinitionCodec<*>
        } else {
            constructor.call(revision) as DefinitionCodec<*>
        }
    }

    companion object {
        private const val VARP_PASS = "varp"
        private const val OTHER_PASS = "other"

        private val tomlMapperDefault = tomlMapper { }
        val packTypes = mutableMapOf<String, PackType>()

        fun registerPackType(
            archive: Int,
            codec: KClass<*>,
            name: String,
            gameValGroup: GameValGroupTypes? = null,
            index: Int = CONFIGS,
            tomlMapper: dev.openrune.toml.TomlMapper = tomlMapperDefault,
            kType: KType,
        ) {
            val packType = PackType(index, archive, codec, name, gameValGroup, tomlMapper, kType)
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
            val packType = PackType(index, archive, codec, name, gameValGroup, tomlMapperDefault, kType)
            packTypes[packType.name] = packType
        }
    }
}
