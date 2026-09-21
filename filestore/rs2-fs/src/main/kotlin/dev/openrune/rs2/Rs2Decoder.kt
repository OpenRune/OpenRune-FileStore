package dev.openrune.rs2

import dev.openrune.Rs2Cache

interface Rs2Decoder<T> {

    fun ids(cache: Rs2Cache): List<Int>

    fun read(cache: Rs2Cache, id: Int): T

    fun count(cache: Rs2Cache): Int = ids(cache).size

    fun load(cache: Rs2Cache, definitions: MutableMap<Int, T>) {
        load(cache, definitions, ids(cache))
    }

    /** Loads only [ids] - a specific list, a range (`100..200`), or any other [Iterable<Int>]. */
    fun load(cache: Rs2Cache, definitions: MutableMap<Int, T>, ids: Iterable<Int>) {
        for (id in ids) {
            try {
                definitions[id] = read(cache, id)
            } catch (e: Exception) {
                // skip on error, matches DefinitionDecoder's (OSRS) behaviour
            }
        }
    }

    fun loadFirst(cache: Rs2Cache, definitions: MutableMap<Int, T>, count: Int) =
        load(cache, definitions, ids(cache).take(count))

    fun loadLast(cache: Rs2Cache, definitions: MutableMap<Int, T>, count: Int) =
        load(cache, definitions, ids(cache).takeLast(count))

}
