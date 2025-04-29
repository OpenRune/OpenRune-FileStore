package dev.openrune.cache

import dev.openrune.definition.Definition
import dev.openrune.definition.type.*

fun <T : Definition> MutableMap<Int, T>.withOffset(offset: Int): MutableMap<Int, T> {
    if (offset == 0) return this.toMutableMap()

    return this.mapKeys { (key, def) ->
        val newId = key + offset
        def.id = newId
        newId
    }.toMutableMap()
}

object CacheManager {

    private val combinedNpcs = mutableMapOf<Int, NpcType>()
    private val combinedObjects = mutableMapOf<Int, ObjectType>()
    private val combinedItems = mutableMapOf<Int, ItemType>()
    private val combinedVarbits = mutableMapOf<Int, VarBitType>()
    private val combinedVarps = mutableMapOf<Int, VarpType>()
    private val combinedAnims = mutableMapOf<Int, SequenceType>()
    private val combinedEnums = mutableMapOf<Int, EnumType>()
    private val combinedHealthBars = mutableMapOf<Int, HealthBarType>()
    private val combinedHitsplats = mutableMapOf<Int, HitSplatType>()
    private val combinedStructs = mutableMapOf<Int, StructType>()
    private val combinedDbrows = mutableMapOf<Int, DBRowType>()
    private val combinedDbtables = mutableMapOf<Int, DBTableType>()

    @JvmStatic
    fun init(vararg dataSources : CacheStore) {
        for (data in dataSources) {
            data.init()
            combinedNpcs.putAll(data.npcs.withOffset(data.npcOffset))
            combinedObjects.putAll(data.objects.withOffset(data.objectOffset))
            combinedItems.putAll(data.items.withOffset(data.itemOffset))
            combinedVarbits.putAll(data.varbits.withOffset(data.varbitOffset))
            combinedVarps.putAll(data.varps.withOffset(data.varpOffset))
            combinedAnims.putAll(data.anims.withOffset(data.animOffset))
            combinedEnums.putAll(data.enums.withOffset(data.enumOffset))
            combinedHealthBars.putAll(data.healthBars.withOffset(data.healthBarOffset))
            combinedHitsplats.putAll(data.hitsplats.withOffset(data.hitsplatOffset))
            combinedStructs.putAll(data.structs.withOffset(data.structOffset))
            combinedDbrows.putAll(data.dbrows.withOffset(data.dbrowOffset))
            combinedDbtables.putAll(data.dbtables.withOffset(data.dbtableOffset))
        }
    }

    private fun <T> getOrDefault(map: Map<Int, T>, id: Int, default: T, typeName: String): T {
        return map.getOrDefault(id, default).also {
            if (id == -1) println("$typeName with id $id is missing.")
        }
    }

    fun getNpc(id: Int) = combinedNpcs[id]
    fun getObject(id: Int) = combinedObjects[id]
    fun getItem(id: Int) = combinedItems[id]
    fun getVarbit(id: Int) = combinedVarbits[id]
    fun getVarp(id: Int) = combinedVarps[id]
    fun getAnim(id: Int) = combinedAnims[id]
    fun getEnum(id: Int) = combinedEnums[id]
    fun getHealthBar(id: Int) = combinedHealthBars[id]
    fun getHitsplat(id: Int) = combinedHitsplats[id]
    fun getStruct(id: Int) = combinedStructs[id]
    fun getDbrow(id: Int) = combinedDbrows[id]
    fun getDbtable(id: Int) = combinedDbtables[id]

    fun getNpcOrDefault(id: Int) = getOrDefault(combinedNpcs, id, NpcType(), "Npc")
    fun getObjectOrDefault(id: Int) = getOrDefault(combinedObjects, id, ObjectType(), "Object")
    fun getItemOrDefault(id: Int) = getOrDefault(combinedItems, id, ItemType(), "Item")
    fun getVarbitOrDefault(id: Int) = getOrDefault(combinedVarbits, id, VarBitType(), "Varbit")
    fun getVarpOrDefault(id: Int) = getOrDefault(combinedVarps, id, VarpType(), "Varp")
    fun getAnimOrDefault(id: Int) = getOrDefault(combinedAnims, id, SequenceType(), "Anim")
    fun getEnumOrDefault(id: Int) = getOrDefault(combinedEnums, id, EnumType(), "Enum")
    fun getHealthBarOrDefault(id: Int) = getOrDefault(combinedHealthBars, id, HealthBarType(), "HealthBar")
    fun getHitsplatOrDefault(id: Int) = getOrDefault(combinedHitsplats, id, HitSplatType(), "Hitsplat")
    fun getStructOrDefault(id: Int) = getOrDefault(combinedStructs, id, StructType(), "Struct")
    fun getDbrowOrDefault(id: Int) = getOrDefault(combinedDbrows, id, DBRowType(), "DBRow")
    fun getDbtableOrDefault(id: Int) = getOrDefault(combinedDbtables, id, DBTableType(), "DBTable")

    // Size methods
    fun npcSize() = combinedNpcs.size
    fun objectSize() = combinedObjects.size
    fun itemSize() = combinedItems.size
    fun varbitSize() = combinedVarbits.size
    fun varpSize() = combinedVarps.size
    fun animSize() = combinedAnims.size
    fun enumSize() = combinedEnums.size
    fun healthBarSize() = combinedHealthBars.size
    fun hitsplatSize() = combinedHitsplats.size
    fun structSize() = combinedStructs.size

    // Bulk getters
    fun getNpcs() = combinedNpcs.toMap()
    fun getObjects() = combinedObjects.toMap()
    fun getItems() = combinedItems.toMap()
    fun getVarbits() = combinedVarbits.toMap()
    fun getVarps() = combinedVarps.toMap()
    fun getAnims() = combinedAnims.toMap()
    fun getEnums() = combinedEnums.toMap()
    fun getHealthBars() = combinedHealthBars.toMap()
    fun getHitsplats() = combinedHitsplats.toMap()
    fun getStructs() = combinedStructs.toMap()
    fun getRows() = combinedDbrows.toMap()

    fun revisionIsOrAfter(cacheRevision : Int,rev: Int) = rev <= cacheRevision
    fun revisionIsOrBefore(cacheRevision : Int,rev: Int) = rev >= cacheRevision

}
