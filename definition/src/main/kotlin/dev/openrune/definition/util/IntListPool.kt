package dev.openrune.definition.util

/**
 * Shares equal int lists between immutable definitions. A cache is full of copies — one object
 * placed at four rotations is four ids with identical model, type and colour lists — so a
 * builder's `build()` routes its lists through here and equal contents collapse to one instance.
 *
 * Safe because the pooled list is always a fresh array-backed copy that only immutable types
 * hold, never the builder's own list. Direct-mapped and race tolerant like the other decode
 * pools: entries are equality-checked before reuse, so a collision or race only costs sharing.
 */
object IntListPool {

    private val pool = arrayOfNulls<IntBackedList>(32768)

    fun of(list: List<Int>?): List<Int>? {
        if (list == null) return null
        val slot = list.hashCode() and (pool.size - 1)
        val cached = pool[slot]
        if (cached != null && cached == list) return cached

        val detached = IntBackedList(list.toIntArray())
        pool[slot] = detached
        return detached
    }
}
