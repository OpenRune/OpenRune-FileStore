package dev.openrune.cache.util


import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.lookup
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.type.AreaType
import dev.openrune.filesystem.Cache
import java.io.File

import com.google.gson.GsonBuilder
import dev.openrune.cache.CacheManager
import dev.openrune.cache.gameval.GameValElement
import dev.openrune.definition.type.EnumType

data class MapIconGroup(
    val category: String,
    val objects: List<Int>
)

fun orderCategoriesByPriorityAndSize(categoryMap: Map<String, List<Int>>): List<String> {
    val stores = listOf("store", "shop")

    val nonStores = categoryMap.keys
        .filter { stores.none { s -> it.lowercase().contains(s) } }
        .sortedBy { it.lowercase() } // alphabetical

    val storeCats = categoryMap.keys
        .filter { stores.any { s -> it.lowercase().contains(s) } }
        .sortedBy { it.lowercase() } // alphabetical, but at the end

    return nonStores + storeCats
}

fun main() {
    val cache = Cache.load(File("C:\\Users\\Home\\Desktop\\New folder").toPath())
    CacheManager.init(OsrsCacheProvider(cache))

    val area: MutableMap<Int, AreaType> = mutableMapOf()
    val enum: MutableMap<Int, EnumType> = mutableMapOf()
    val objectGameVals = GameValHandler.readGameVal(GameValGroupTypes.LOCTYPES, cache)
    OsrsCacheProvider.AreaDecoder().load(cache, area)
    OsrsCacheProvider.EnumDecoder().load(cache, enum)

    writeMapFunctionData(objectGameVals,area,enum)
    writeMapIconsData(objectGameVals)

}

fun writeMapIconsData(objectGameVals: List<GameValElement>) {
    val objectsByMapArea = CacheManager.getObjects()
        .asSequence()
        .filterNot { it.value.mapSceneID == -1 }
        .groupBy { it.value.mapSceneID }

    val categoryMap = mutableMapOf<String, MutableList<Int>>()

    objectsByMapArea.forEach { (mapId, entries) ->
        val categoryName = "icon $mapId"
        val objList = categoryMap.getOrPut(categoryName) { mutableListOf() }
        objList.addAll(entries.map { it.key })
    }

    val finalCategories = orderCategoriesByPriorityAndSize(categoryMap)

    val result = finalCategories.map { cat ->
        MapIconGroup(cat, categoryMap[cat]?.distinct() ?: emptyList())
    }

    val gson = GsonBuilder().setPrettyPrinting().create()
    println(gson.toJson(result))
}

fun writeMapFunctionData(
    objectGameVals: List<GameValElement>,
    area: MutableMap<Int, AreaType>,
    enum: MutableMap<Int, EnumType>
) {
    val objectsByMapArea = CacheManager.getObjects()
        .asSequence()
        .filterNot { it.value.mapAreaId == -1 }
        .groupBy { it.value.mapAreaId }

    val categoryMap = mutableMapOf<String, MutableList<Int>>()
    val realNameEnum = enum[1713]!!

    val transportRegex = Regex(
        "^(Transportation\\s+[A-Za-z]{1,3}|transportation_icon_[a-z]+)$",
        RegexOption.IGNORE_CASE
    )

    objectsByMapArea.forEach { (mapId, entries) ->
        val areaType = area[mapId] ?: return@forEach
        if (!areaType.renderOnMinimap) return@forEach

        var categoryName = realNameEnum.values[areaType.category.toString()]?.toString() ?: "Unknown"

        val firstObjectName = entries.mapNotNull { objectGameVals.lookup(it.key)?.name }
            .firstOrNull { it.isNotBlank() }

        if (firstObjectName != null) {
            when {
                firstObjectName.contains("Tutor", ignoreCase = true) -> {
                    categoryName = "Tutors"
                }
                transportRegex.matches(firstObjectName) -> {
                    categoryName = "Fairy Rings"
                }
            }
        }

        val objList = categoryMap.getOrPut(categoryName) { mutableListOf() }
        objList.addAll(entries.map { it.key })
    }

    val finalCategories = orderCategoriesByPriorityAndSize(categoryMap)

    val result = finalCategories.map { cat ->
        MapIconGroup(cat, categoryMap[cat]?.distinct() ?: emptyList())
    }

    val gson = GsonBuilder().setPrettyPrinting().create()
    println(gson.toJson(result))
}