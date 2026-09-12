package dev.openrune.definition.util

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
