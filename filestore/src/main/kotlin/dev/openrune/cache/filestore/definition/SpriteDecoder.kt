package dev.openrune.cache.filestore.definition

import dev.openrune.cache.SPRITES
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.definition.codec.SpriteCodec
import dev.openrune.definition.type.SpriteType
import dev.openrune.filesystem.Cache

class SpriteDecoder : DefinitionDecoder<SpriteType>(SPRITES, SpriteCodec()) {
    override fun getFile(id: Int) = 0

    fun spriteIds(cache: Cache): List<Int> = cache.archives(SPRITES).sorted()

    fun spriteCount(cache: Cache): Int = cache.archives(SPRITES).size

    fun load(cache: Cache, definitions: MutableMap<Int, SpriteType>, ids: Iterable<Int>) {
        for (id in ids) {
            try {
                val data = cache.data(index, getArchive(id), getFile(id)) ?: continue
                definitions[id] = codec.loadData(id, data)
            } catch (e: Exception) {
                // matches DefinitionDecoder's skip-on-error behaviour
            }
        }
    }

    fun loadFirst(cache: Cache, definitions: MutableMap<Int, SpriteType>, count: Int) =
        load(cache, definitions, spriteIds(cache).take(count))

    fun loadLast(cache: Cache, definitions: MutableMap<Int, SpriteType>, count: Int) =
        load(cache, definitions, spriteIds(cache).takeLast(count))
}