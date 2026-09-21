package dev.openrune.rs2

import dev.openrune.Rs2Cache
import dev.openrune.definition.type.SpriteType


class SpriteDecoder {

    fun spriteIds(cache: Rs2Cache): List<Int> = cache.spriteGroups()

    fun spriteCount(cache: Rs2Cache): Int = cache.spriteGroups().size

    fun load(cache: Rs2Cache, definitions: MutableMap<Int, SpriteType>) {
        load(cache, definitions, cache.spriteGroups())
    }

    fun load(cache: Rs2Cache, definitions: MutableMap<Int, SpriteType>, ids: Iterable<Int>) {
        for (group in ids) {
            try {
                definitions[group] = cache.readSpriteType(group = group)
            } catch (e: Exception) {
                // matches DefinitionDecoder's (OSRS) skip-on-error behaviour
            }
        }
    }

    fun loadFirst(cache: Rs2Cache, definitions: MutableMap<Int, SpriteType>, count: Int) =
        load(cache, definitions, spriteIds(cache).take(count))

    fun loadLast(cache: Rs2Cache, definitions: MutableMap<Int, SpriteType>, count: Int) =
        load(cache, definitions, spriteIds(cache).takeLast(count))

}
