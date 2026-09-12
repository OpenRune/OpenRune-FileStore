package dev.openrune.cache

import dev.openrune.definition.Definition
import dev.openrune.definition.type.*
import java.util.Collections


/**
 * Returns a new map where all keys (IDs) are incremented by the given [offset],
 * and each definition's internal `id` property is updated accordingly.
 *
 * This is useful when loading multiple sets of definitions (e.g., items, NPCs)
 * that may have overlapping IDs and need to be shifted to avoid conflicts.
 *
 */
fun <T : Definition> MutableMap<Int, T>.withOffset(offset: Int): MutableMap<Int, T> {
    if (offset == 0) return this.toMutableMap()

    val result = LinkedHashMap<Int, T>(capacityFor(size))
    for ((key, def) in this) {
        val newId = key + offset
        def.id = newId
        result[newId] = def
    }
    return result
}

/**
 * Table capacity that holds [size] entries without a rehash at the default 0.75 load factor.
 */
internal fun capacityFor(size: Int): Int = if (size < 3) 4 else (size / 0.75f).toInt() + 1

/**
 * Retrieves a value from the given [map] by [id], or returns the specified [default] if not found.
 * Additionally logs a warning message if the [id] is -1, indicating a potentially missing or invalid entry.
 *
 * @param map The map to retrieve the value from.
 * @param id The key to look up in the map.
 * @param default The value to return if the key is not present in the map.
 * @param typeName The name of the type being looked up (used in the warning message).
 * @return The value associated with [id], or [default] if not present.
 */
fun <T> getOrDefault(map: Map<Int, T>, id: Int, default: T, typeName: String): T {
    return map.getOrDefault(id, default).also {
        if (id == -1) println("$typeName with id $id is missing.")
    }
}

object CacheManager {

    private var npcs: MutableMap<Int, NpcType> = mutableMapOf()
    private var objects: MutableMap<Int, ObjectType> = mutableMapOf()
    private var items: MutableMap<Int, ItemType> = mutableMapOf()
    private var varbits: MutableMap<Int, VarBitType> = mutableMapOf()
    private var varps: MutableMap<Int, VarpType> = mutableMapOf()
    private var anims: MutableMap<Int, SequenceType> = mutableMapOf()
    private var enums: MutableMap<Int, EnumType> = mutableMapOf()
    private var healthBars: MutableMap<Int, HealthBarType> = mutableMapOf()
    private var hitsplats: MutableMap<Int, HitSplatType> = mutableMapOf()
    private var structs: MutableMap<Int, StructType> = mutableMapOf()
    private var dbrows: MutableMap<Int, DBRowType> = mutableMapOf()
    private var dbtables: MutableMap<Int, DBTableType> = mutableMapOf()

    /** Adopts [source] outright on the first init, otherwise merges into the existing map. */
    private fun <T> adopt(current: MutableMap<Int, T>, source: MutableMap<Int, T>): MutableMap<Int, T> {
        if (current.isEmpty()) {
            return source
        }
        current.putAll(source)
        return current
    }

    @JvmStatic
    fun init(cacheStore : CacheStore) {
        cacheStore.init()
        npcs = adopt(npcs, cacheStore.npcs)
        objects = adopt(objects, cacheStore.objects)
        DefinitionCompactor.compactObjects(objects)
        DefinitionCompactor.compactNpcs(npcs)
        items = adopt(items, cacheStore.items)
        DefinitionCompactor.compactItems(items)
        varbits = adopt(varbits, cacheStore.varbits)
        varps = adopt(varps, cacheStore.varps)
        anims = adopt(anims, cacheStore.anims)
        enums = adopt(enums, cacheStore.enums)
        healthBars = adopt(healthBars, cacheStore.healthBars)
        hitsplats = adopt(hitsplats, cacheStore.hitsplats)
        structs = adopt(structs, cacheStore.structs)
        dbrows = adopt(dbrows, cacheStore.dbrows)
        dbtables = adopt(dbtables, cacheStore.dbtables)
    }

    fun getNpc(id: Int) = npcs[id]
    fun getObject(id: Int) = objects[id]
    fun getItem(id: Int) = items[id]
    fun getVarbit(id: Int) = varbits[id]
    fun getVarp(id: Int) = varps[id]
    fun getAnim(id: Int) = anims[id]
    fun getEnum(id: Int) = enums[id]
    fun getHealthBar(id: Int) = healthBars[id]
    fun getHitsplat(id: Int) = hitsplats[id]
    fun getStruct(id: Int) = structs[id]
    fun getDbrow(id: Int) = dbrows[id]
    fun getDbtable(id: Int) = dbtables[id]

    /** Same contract as [getOrDefault], but the fallback is only built on a miss. */
    private inline fun <T> lookupOrDefault(map: Map<Int, T>, id: Int, typeName: String, default: () -> T): T {
        if (id == -1) println("$typeName with id $id is missing.")
        return map[id] ?: default()
    }

    fun getNpcOrDefault(id: Int) = lookupOrDefault(npcs, id, "Npc") { NpcType() }
    fun getObjectOrDefault(id: Int) = lookupOrDefault(objects, id, "Object") { ObjectType() }
    fun getItemOrDefault(id: Int) = lookupOrDefault(items, id, "Item") { ItemType() }
    fun getVarbitOrDefault(id: Int) = lookupOrDefault(varbits, id, "Varbit") { VarBitType() }
    fun getVarpOrDefault(id: Int) = lookupOrDefault(varps, id, "Varp") { VarpType() }
    fun getAnimOrDefault(id: Int) = lookupOrDefault(anims, id, "Anim") { SequenceType() }
    fun getEnumOrDefault(id: Int) = lookupOrDefault(enums, id, "Enum") { EnumType() }
    fun getHealthBarOrDefault(id: Int) = lookupOrDefault(healthBars, id, "HealthBar") { HealthBarType() }
    fun getHitsplatOrDefault(id: Int) = lookupOrDefault(hitsplats, id, "Hitsplat") { HitSplatType() }
    fun getStructOrDefault(id: Int) = lookupOrDefault(structs, id, "Struct") { StructType() }
    fun getDbrowOrDefault(id: Int) = lookupOrDefault(dbrows, id, "DBRow") { DBRowType() }
    fun getDbtableOrDefault(id: Int) = lookupOrDefault(dbtables, id, "DBTable") { DBTableType() }

    // Size methods
    fun npcSize() = npcs.size
    fun objectSize() = objects.size
    fun itemSize() = items.size
    fun varbitSize() = varbits.size
    fun varpSize() = varps.size
    fun animSize() = anims.size
    fun enumSize() = enums.size
    fun healthBarSize() = healthBars.size
    fun hitsplatSize() = hitsplats.size
    fun structSize() = structs.size

    // Bulk getters. Read-only views rather than a copy per call; the tables are fixed after `init`.
    fun getNpcs(): Map<Int, NpcType> = Collections.unmodifiableMap(npcs)
    fun getObjects(): Map<Int, ObjectType> = Collections.unmodifiableMap(objects)
    fun getItems(): Map<Int, ItemType> = Collections.unmodifiableMap(items)
    fun getVarbits(): Map<Int, VarBitType> = Collections.unmodifiableMap(varbits)
    fun getVarps(): Map<Int, VarpType> = Collections.unmodifiableMap(varps)
    fun getAnims(): Map<Int, SequenceType> = Collections.unmodifiableMap(anims)
    fun getEnums(): Map<Int, EnumType> = Collections.unmodifiableMap(enums)
    fun getHealthBars(): Map<Int, HealthBarType> = Collections.unmodifiableMap(healthBars)
    fun getHitsplats(): Map<Int, HitSplatType> = Collections.unmodifiableMap(hitsplats)
    fun getStructs(): Map<Int, StructType> = Collections.unmodifiableMap(structs)
    fun getRows(): Map<Int, DBRowType> = Collections.unmodifiableMap(dbrows)

    fun revisionIsOrAfter(cacheRevision : Int,rev: Int) = rev <= cacheRevision
    fun revisionIsOrBefore(cacheRevision : Int,rev: Int) = rev >= cacheRevision

}
