package dev.openrune.cache

import dev.openrune.definition.Definition
import dev.openrune.definition.type.*


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

    return this.mapKeys { (key, def) ->
        val newId = key + offset
        def.id = newId
        newId
    }.toMutableMap()
}

object CacheManager {

    private val npcs = mutableMapOf<Int, NpcType>()
    private val objects = mutableMapOf<Int, ObjectType>()
    private val items = mutableMapOf<Int, ItemType>()
    private val varbits = mutableMapOf<Int, VarBitType>()
    private val varps = mutableMapOf<Int, VarpType>()
    private val anims = mutableMapOf<Int, SequenceType>()
    private val enums = mutableMapOf<Int, EnumType>()
    private val healthBars = mutableMapOf<Int, HealthBarType>()
    private val hitsplats = mutableMapOf<Int, HitSplatType>()
    private val structs = mutableMapOf<Int, StructType>()
    private val dbrows = mutableMapOf<Int, DBRowType>()
    private val dbtables = mutableMapOf<Int, DBTableType>()

    @JvmStatic
    fun init(cacheStore : CacheStore) {
        cacheStore.init()
        npcs.putAll(cacheStore.npcs)
        objects.putAll(cacheStore.objects)
        items.putAll(cacheStore.items)
        varbits.putAll(cacheStore.varbits)
        varps.putAll(cacheStore.varps)
        anims.putAll(cacheStore.anims)
        enums.putAll(cacheStore.enums)
        healthBars.putAll(cacheStore.healthBars)
        hitsplats.putAll(cacheStore.hitsplats)
        structs.putAll(cacheStore.structs)
        dbrows.putAll(cacheStore.dbrows)
        dbtables.putAll(cacheStore.dbtables)
    }

    private fun <T> getOrDefault(map: Map<Int, T>, id: Int, default: T, typeName: String): T {
        return map.getOrDefault(id, default).also {
            if (id == -1) println("$typeName with id $id is missing.")
        }
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

    fun getNpcOrDefault(id: Int) = getOrDefault(npcs, id, NpcType(), "Npc")
    fun getObjectOrDefault(id: Int) = getOrDefault(objects, id, ObjectType(), "Object")
    fun getItemOrDefault(id: Int) = getOrDefault(items, id, ItemType(), "Item")
    fun getVarbitOrDefault(id: Int) = getOrDefault(varbits, id, VarBitType(), "Varbit")
    fun getVarpOrDefault(id: Int) = getOrDefault(varps, id, VarpType(), "Varp")
    fun getAnimOrDefault(id: Int) = getOrDefault(anims, id, SequenceType(), "Anim")
    fun getEnumOrDefault(id: Int) = getOrDefault(enums, id, EnumType(), "Enum")
    fun getHealthBarOrDefault(id: Int) = getOrDefault(healthBars, id, HealthBarType(), "HealthBar")
    fun getHitsplatOrDefault(id: Int) = getOrDefault(hitsplats, id, HitSplatType(), "Hitsplat")
    fun getStructOrDefault(id: Int) = getOrDefault(structs, id, StructType(), "Struct")
    fun getDbrowOrDefault(id: Int) = getOrDefault(dbrows, id, DBRowType(), "DBRow")
    fun getDbtableOrDefault(id: Int) = getOrDefault(dbtables, id, DBTableType(), "DBTable")

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

    // Bulk getters
    fun getNpcs() = npcs.toMap()
    fun getObjects() = objects.toMap()
    fun getItems() = items.toMap()
    fun getVarbits() = varbits.toMap()
    fun getVarps() = varps.toMap()
    fun getAnims() = anims.toMap()
    fun getEnums() = enums.toMap()
    fun getHealthBars() = healthBars.toMap()
    fun getHitsplats() = hitsplats.toMap()
    fun getStructs() = structs.toMap()
    fun getRows() = dbrows.toMap()

    fun revisionIsOrAfter(cacheRevision : Int,rev: Int) = rev <= cacheRevision
    fun revisionIsOrBefore(cacheRevision : Int,rev: Int) = rev >= cacheRevision

}
