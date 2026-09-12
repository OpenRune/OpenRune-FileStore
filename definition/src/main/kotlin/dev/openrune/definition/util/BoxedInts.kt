package dev.openrune.definition.util

/**
 * Deduplicates boxed integers on the decode path. Enum values, db row cells and params box every
 * int into a fresh `Integer` once it leaves the JVM's tiny -128..127 cache, and the same ids come
 * up over and over across definitions. A direct-mapped table hands back the previous box when the
 * same value repeats. Bounded, lock free and race tolerant: boxes are immutable and checked by
 * value before reuse, so a race only costs a slot.
 */
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
