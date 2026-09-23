package dev.openrune.cache.tools.gameval

import dev.openrune.cache.DBROW
import dev.openrune.cache.DBTABLE
import dev.openrune.cache.ENUM
import dev.openrune.cache.ITEM
import dev.openrune.cache.NPC
import dev.openrune.cache.OBJECT
import dev.openrune.cache.PARAMS
import dev.openrune.cache.STRUCT
import dev.openrune.cache.CONFIGS
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.gameval.impl.Sprite
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.cache.tools.incremental.ConfigRef
import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.Parameterized
import dev.openrune.definition.codec.DBRowCodec
import dev.openrune.definition.codec.DBTableCodec
import dev.openrune.definition.codec.EnumCodec
import dev.openrune.definition.codec.ItemCodec
import dev.openrune.definition.codec.NPCCodec
import dev.openrune.definition.codec.ObjectCodec
import dev.openrune.definition.codec.ParamCodec
import dev.openrune.definition.codec.StructCodec
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.DBTableType
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.type.ParamType
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.definition.util.toArray
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled

/**
 * Config archives whose definitions carry [CacheVarLiteral]-typed values, and therefore may reference
 * gamevals by id. The name is what the reference index stores, so keep it stable.
 */
internal enum class ConfigRefKind(val archive: Int) {
    ENUM(dev.openrune.cache.ENUM),
    STRUCT(dev.openrune.cache.STRUCT),
    ITEM(dev.openrune.cache.ITEM),
    NPC(dev.openrune.cache.NPC),
    LOC(OBJECT),
    DBROW(dev.openrune.cache.DBROW),
    DBTABLE(dev.openrune.cache.DBTABLE),
    ;

    @Suppress("UNCHECKED_CAST")
    fun codec(revision: Int): DefinitionCodec<Definition> = when (this) {
        ENUM -> EnumCodec()
        STRUCT -> StructCodec()
        ITEM -> ItemCodec(revision)
        NPC -> NPCCodec(revision)
        LOC -> ObjectCodec(revision)
        DBROW -> DBRowCodec()
        DBTABLE -> DBTableCodec()
    } as DefinitionCodec<Definition>

    companion object {
        private val byArchive = entries.associateBy { it.archive }
        fun forArchive(archive: Int): ConfigRefKind? = byArchive[archive]
        fun forName(name: String): ConfigRefKind? = entries.firstOrNull { it.name == name }
    }
}

/** Resolves a gameval id back to its name, for one direction of the index. */
internal fun interface GameValNames {
    fun nameOf(table: String, id: Int): String?
}

/**
 * The gameval reference index: which typed values in which cache configs point at which gameval, by
 * name, so the configs can be re-encoded when the name is renumbered.
 *
 * Neptune does the same for library scripts, recording the symbols each one uses and rewriting the
 * script when a symbol's id changes. Here the "symbols" are the [CacheVarLiteral]-typed slots of a
 * config: enum keys and values, params on structs, items, npcs and locs, dbrow cells and dbtable
 * defaults. A config only has to exist in the cache to be tracked; it needs no TOML source.
 */
internal object GameValReferences {

    /** Literals whose gameval table is not simply the literal's lower-cased name. */
    private val TABLE_BY_LITERAL: Map<Int, String> = mapOf(
        CacheVarLiteral.COMPONENT.id to GameValPublisher.COMPONENT_TABLE,
        CacheVarLiteral.INTERFACE.id to "interface",
        CacheVarLiteral.TOPLEVELINTERFACE.id to "interface",
        CacheVarLiteral.OVERLAYINTERFACE.id to "interface",
        CacheVarLiteral.CLIENTINTERFACE.id to "interface",
        CacheVarLiteral.OBJ.id to "obj",
        CacheVarLiteral.NAMEDOBJ.id to "obj",
        CacheVarLiteral.NPC.id to "npc",
        CacheVarLiteral.LOC.id to "loc",
        CacheVarLiteral.SEQ.id to "seq",
        CacheVarLiteral.SPOTANIM.id to "spotanim",
        CacheVarLiteral.INV.id to "inv",
        CacheVarLiteral.JINGLE.id to "jingle",
        CacheVarLiteral.GRAPHIC.id to "sprites",
        CacheVarLiteral.DBROW.id to "dbrow",
        CacheVarLiteral.DBTABLE.id to "dbtable",
    )

    /**
     * Gameval table a literal refers to, or null for literals that are plain values (INT, STRING,
     * BOOLEAN, COORDGRID...). Beyond the fixed map, any literal whose lower-cased name is one of
     * [knownTables] counts, so VARBIT, STRUCT or STAT resolve when the provider has such a table.
     */
    fun tableFor(literal: CacheVarLiteral, knownTables: Collection<String>): String? =
        TABLE_BY_LITERAL[literal.id] ?: literal.name.lowercase().takeIf { it in knownTables }

    // ---- slots ----

    /** One typed integer inside a config, with a way to overwrite it in place. */
    internal class Slot(val id: String, val literal: CacheVarLiteral, val value: Int, val set: (Int) -> Unit)

    /**
     * Every gameval-capable slot of [definition]. Enum value slots come before key slots so a key
     * that moves does not pull its value's setter out from under it.
     */
    fun slots(kind: ConfigRefKind, definition: Definition, paramTypes: Map<Int, ParamType>): List<Slot> =
        when (kind) {
            ConfigRefKind.ENUM -> enumSlots(definition as EnumType)
            ConfigRefKind.STRUCT, ConfigRefKind.ITEM, ConfigRefKind.NPC, ConfigRefKind.LOC ->
                paramSlots(definition as Parameterized, paramTypes)
            ConfigRefKind.DBROW -> dbRowSlots(definition as DBRowType)
            ConfigRefKind.DBTABLE -> dbTableSlots(definition as DBTableType)
        }

    private fun enumSlots(enum: EnumType): List<Slot> {
        val slots = ArrayList<Slot>()
        val entries = enum.values.entries.map { it.key to it.value }
        for ((key, value) in entries) {
            if (value is Int) {
                slots += Slot("v:$key", enum.valueType, value) { enum.values[key] = it }
            }
        }
        for ((key, _) in entries) {
            // Carries whatever the entry holds when the key moves, so a value rewritten first is kept.
            slots += Slot("k:$key", enum.keyType, key) { newKey ->
                val current = enum.values.remove(key) ?: return@Slot
                enum.values[newKey] = current
            }
        }
        return slots
    }

    private fun paramSlots(definition: Parameterized, paramTypes: Map<Int, ParamType>): List<Slot> {
        val params = definition.params as? MutableMap<Int, Any> ?: return emptyList()
        return params.entries.mapNotNull { (id, value) ->
            val literal = paramTypes[id]?.type ?: return@mapNotNull null
            if (value !is Int) return@mapNotNull null
            Slot("p:$id", literal, value) { params[id] = it }
        }
    }

    private fun dbRowSlots(row: DBRowType): List<Slot> {
        val columns = row.columnTypes ?: return emptyList()
        val typeIds = row.field5306 ?: return emptyList()
        val slots = ArrayList<Slot>()
        for (column in columns.indices) {
            val cells = columns[column] ?: continue
            val ids = typeIds[column] ?: continue
            if (ids.isEmpty()) continue
            for (cell in cells.indices) {
                val value = cells[cell] as? Int ?: continue
                val literal = CacheVarLiteral.mappedIds[ids[cell % ids.size]] ?: continue
                slots += Slot("c:$column:$cell", literal, value) { cells[cell] = it }
            }
        }
        return slots
    }

    private fun dbTableSlots(table: DBTableType): List<Slot> {
        val slots = ArrayList<Slot>()
        for ((column, type) in table.columns) {
            val defaults = type.values ?: continue
            if (type.types.isEmpty()) continue
            for (cell in defaults.indices) {
                val value = defaults[cell] as? Int ?: continue
                val literal = type.types[cell % type.types.size]
                slots += Slot("c:$column:$cell", literal, value) { defaults[cell] = it }
            }
        }
        return slots
    }

    // ---- index / relink ----

    /** The named gameval references in [definition]; slots whose id has no name are left out. */
    fun extract(
        kind: ConfigRefKind,
        definition: Definition,
        paramTypes: Map<Int, ParamType>,
        names: GameValNames,
        knownTables: Collection<String>,
    ): List<ConfigRef> =
        slots(kind, definition, paramTypes).mapNotNull { slot ->
            val table = tableFor(slot.literal, knownTables) ?: return@mapNotNull null
            val name = names.nameOf(table, slot.value) ?: return@mapNotNull null
            ConfigRef(slot.id, table, name, slot.value)
        }

    /**
     * Points every slot in [refs] at the id its name currently resolves to. Returns the refs as they
     * stand afterwards, or null when nothing needed changing. Names that no longer resolve are left as
     * they were rather than guessed at.
     */
    fun relink(
        kind: ConfigRefKind,
        definition: Definition,
        refs: List<ConfigRef>,
        paramTypes: Map<Int, ParamType>,
        idOf: (table: String, name: String) -> Int?,
    ): List<ConfigRef>? {
        val bySlot = slots(kind, definition, paramTypes).associateBy { it.id }
        var changed = false
        // Enum slots are addressed by key. When a key itself moves, every slot that used the old key is
        // addressed by the new one from now on, so the stored refs must follow or the next build
        // cannot find them.
        val keyMoves = HashMap<String, String>()
        // Values before keys, whatever order the refs were stored in: a value setter addresses its entry
        // by the key it was extracted with, so it must run before that key can move.
        val ordered = refs.sortedBy { it.slot.startsWith(KEY_SLOT) }
        val updated = ordered.map { ref ->
            val slot = bySlot[ref.slot] ?: return@map ref
            val current = idOf(ref.table, ref.name) ?: return@map ref
            if (current == slot.value) return@map ref.copy(id = current)
            slot.set(current)
            changed = true
            if (ref.slot.startsWith(KEY_SLOT)) keyMoves[ref.slot.removePrefix(KEY_SLOT)] = current.toString()
            ref.copy(id = current)
        }
        if (!changed) return null
        if (keyMoves.isEmpty()) return updated
        return updated.map { ref ->
            val prefix = ref.slot.substringBefore(':') + ":"
            val moved = keyMoves[ref.slot.removePrefix(prefix)] ?: return@map ref
            ref.copy(slot = prefix + moved)
        }
    }

    private const val KEY_SLOT = "k:"

    fun decode(kind: ConfigRefKind, id: Int, bytes: ByteArray, revision: Int): Definition =
        kind.codec(revision).loadData(id, bytes)

    fun encode(kind: ConfigRefKind, definition: Definition, revision: Int): ByteArray {
        val writer = Unpooled.buffer(4096)
        with(kind.codec(revision)) { writer.encode(definition) }
        return writer.toArray()
    }

    fun loadParamTypes(cache: Cache, revision: Int): Map<Int, ParamType> {
        val codec = ParamCodec(revision)
        val types = HashMap<Int, ParamType>()
        for (file in cache.files(CONFIGS, PARAMS)) {
            val bytes = cache.data(CONFIGS, PARAMS, file) ?: continue
            runCatching { codec.loadData(file, bytes) }.getOrNull()?.let { types[file] = it }
        }
        return types
    }

    // ---- name sources ----

    /**
     * Names as the cache itself records them in its gameval index. Used when the index is first built,
     * because the ids inside the cache's configs are consistent with the cache's own gamevals, not with
     * whatever the project has since redeclared.
     */
    fun namesFromCache(cache: Cache, revision: Int): GameValNames {
        val tables = HashMap<String, HashMap<Int, String>>()
        fun put(table: String, id: Int, name: String) {
            tables.getOrPut(table) { HashMap() }.putIfAbsent(id, name)
        }
        for (group in GameValGroupTypes.entries) {
            if (group == GameValGroupTypes.IFTYPES_V2) continue
            if (group.revision != -1 && revision < group.revision) continue
            val elements = runCatching { GameValHandler.readGameVal(group, cache, revision) }.getOrNull() ?: continue
            val table = GameValPublisher.tableOf(group)
            for (element in elements) {
                when (element) {
                    is Interface -> {
                        put(table, element.id, element.name)
                        element.components.forEach { put(GameValPublisher.COMPONENT_TABLE, it.packed, "${element.name}:${it.name}") }
                    }
                    is Table -> {
                        put(table, element.id, element.name)
                        element.columns.forEach { put(GameValPublisher.DBCOL_TABLE, (element.id shl 16) or it.id, "${element.name}:${it.name}") }
                    }
                    is Sprite -> put(table, element.id, if (element.index == -1) element.name else "${element.name}:${element.index}")
                    else -> put(table, element.id, element.name)
                }
            }
        }
        return CachedNames(tables)
    }

    /** Names as the constant provider currently knows them: cache gamevals plus everything declared or packed. */
    fun namesFromProvider(): GameValNames {
        val tables = HashMap<String, HashMap<Int, String>>()
        for ((table, entries) in ConstantProvider.mappings) {
            val reverse = tables.getOrPut(table) { HashMap(entries.size) }
            val prefix = "$table."
            for ((key, id) in entries) {
                // -1 is the unassigned placeholder. Anything else is real: packed component ids for
                // interfaces above 32767 are negative as an Int.
                if (id == UNASSIGNED) continue
                reverse.putIfAbsent(id, key.removePrefix(prefix))
            }
        }
        return CachedNames(tables)
    }

    fun currentId(table: String, name: String): Int? =
        ConstantProvider.peekMapping("$table.$name")?.takeIf { it != UNASSIGNED }

    private const val UNASSIGNED = -1

    private class CachedNames(private val tables: Map<String, Map<Int, String>>) : GameValNames {
        val knownTables: Set<String> get() = tables.keys
        override fun nameOf(table: String, id: Int): String? = tables[table]?.get(id)
    }

    fun knownTables(names: GameValNames): Collection<String> =
        (names as? CachedNames)?.knownTables ?: ConstantProvider.types
}
