package dev.openrune.definition.util

object BoxedInts {
    private val pool = arrayOfNulls<Any>(8192)

    fun of(value: Int): Any {
        if (value in -128..127) return value
        val slot = value and (pool.size - 1)
        val cached = pool[slot]
        if (cached is Int && cached == value) return cached
        val boxed: Any = value
        pool[slot] = boxed
        return boxed
    }
}
