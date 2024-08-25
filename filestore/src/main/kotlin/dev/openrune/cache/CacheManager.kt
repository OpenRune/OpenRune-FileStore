package dev.openrune.cache

import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.data.*
import java.nio.file.Path

object CacheManager {

    lateinit var cache: Cache

    private val combinedNpcs: MutableMap<Int, NpcType> = mutableMapOf()
    private val combinedObjects: MutableMap<Int, ObjectType> = mutableMapOf()
    private val combinedItems: MutableMap<Int, ItemType> = mutableMapOf()
    private val combinedVarbits: MutableMap<Int, VarBitType> = mutableMapOf()
    private val combinedVarps: MutableMap<Int, VarpType> = mutableMapOf()
    private val combinedAnims: MutableMap<Int, SequenceType> = mutableMapOf()
    private val combinedEnums: MutableMap<Int, EnumType> = mutableMapOf()
    private val combinedHealthBars: MutableMap<Int, HealthBarType> = mutableMapOf()
    private val combinedHitsplats: MutableMap<Int, HitSplatType> = mutableMapOf()
    private val combinedStructs: MutableMap<Int, StructType> = mutableMapOf()

    private var cacheRevision = -1

    fun init(vararg dataSources : GameDataSource) {
        for (data in dataSources) {
            data.init()
            combinedNpcs.putAll(applyIdOffset(data.npcs, data.npcOffset))
            combinedObjects.putAll(applyIdOffset(data.objects, data.objectOffset))
            combinedItems.putAll(applyIdOffset(data.items, data.itemOffset))
            combinedVarbits.putAll(applyIdOffset(data.varbits, data.varbitOffset))
            combinedVarps.putAll(applyIdOffset(data.varps, data.varpOffset))
            combinedAnims.putAll(applyIdOffset(data.anims, data.animOffset))
            combinedEnums.putAll(applyIdOffset(data.enums, data.enumOffset))
            combinedHealthBars.putAll(applyIdOffset(data.healthBars, data.healthBarOffset))
            combinedHitsplats.putAll(applyIdOffset(data.hitsplats, data.hitsplatOffset))
            combinedStructs.putAll(applyIdOffset(data.structs, data.structOffset))
        }
    }

    fun <T : Definition> applyIdOffset(definitions: MutableMap<Int, T>, offset: Int): MutableMap<Int, T> {
        return if (offset != 0) {
            definitions.mapKeys { (key, definition) ->
                val newKey = key + offset
                definition.id = newKey
                newKey
            }.toMutableMap()
        } else {
            definitions.toMutableMap()
        }
    }

    private inline fun <reified T> getFromCombinedMap(
        map: Map<Int, T>,
        id: Int,
        typeName: String
    ): T {
        // Get the value from the map based on the provided id, not by index
        return map.getOrDefault(id, createMissingType<T>(id)).also {
            // If the object retrieved doesn't match the id (likely in the case of the default), print a message
            if (it.toString() != id.toString()) {
                //println("$typeName with id $id is missing.")
            }
        }
    }

    // Implementations for specific types, method names are kept the same
    fun getNpc(id: Int): NpcType {
        return getFromCombinedMap(combinedNpcs, id, "Npc")
    }

    fun getObject(id: Int): ObjectType {
        return getFromCombinedMap(combinedObjects, id, "Object")
    }

    fun getItem(id: Int): ItemType {
        return getFromCombinedMap(combinedItems, id, "Item")
    }

    fun getVarbit(id: Int): VarBitType {
        return getFromCombinedMap(combinedVarbits, id, "Varbit")
    }

    fun getVarp(id: Int): VarpType {
        return getFromCombinedMap(combinedVarps, id, "Varp")
    }

    fun getAnim(id: Int): SequenceType {
        return getFromCombinedMap(combinedAnims, id, "Anim")
    }

    fun getEnum(id: Int): EnumType {
        return getFromCombinedMap(combinedEnums, id, "Enum")
    }

    fun getHealthBar(id: Int): HealthBarType {
        return getFromCombinedMap(combinedHealthBars, id, "HealthBar")
    }

    fun getHitsplat(id: Int): HitSplatType {
        return getFromCombinedMap(combinedHitsplats, id, "Hitsplat")
    }

    fun getStruct(id: Int): StructType {
        return getFromCombinedMap(combinedStructs, id, "Struct")
    }

    // Methods to retrieve the sizes of the combined maps
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

    // Methods to retrieve the entire combined maps
    fun getNpcs(): Map<Int, NpcType> = combinedNpcs
    fun getObjects(): Map<Int, ObjectType> = combinedObjects
    fun getItems(): Map<Int, ItemType> = combinedItems
    fun getVarbits(): Map<Int, VarBitType> = combinedVarbits
    fun getVarps(): Map<Int, VarpType> = combinedVarps
    fun getAnims(): Map<Int, SequenceType> = combinedAnims
    fun getEnums(): Map<Int, EnumType> = combinedEnums
    fun getHealthBars(): Map<Int, HealthBarType> = combinedHealthBars
    fun getHitsplats(): Map<Int, HitSplatType> = combinedHitsplats
    fun getStructs(): Map<Int, StructType> = combinedStructs

    // Helper function to create a default instance if the ID is missing
    private inline fun <reified T> createMissingType(id: Int): T {
        return when (T::class) {
            NpcType::class -> NpcType(id) as T
            ObjectType::class -> ObjectType(id) as T
            ItemType::class -> ItemType(id) as T
            VarBitType::class -> VarBitType(id) as T
            VarpType::class -> VarpType(id) as T
            SequenceType::class -> SequenceType(id) as T
            EnumType::class -> EnumType(id) as T
            HealthBarType::class -> HealthBarType(id) as T
            HitSplatType::class -> HitSplatType(id) as T
            StructType::class -> StructType(id) as T
            else -> throw IllegalArgumentException("Unknown type: ${T::class}")
        }
    }

    fun revisionIsOrAfter(rev : Int) = rev <= 223
    fun revisionIsOrBefore(rev : Int) = rev >= 223


}
