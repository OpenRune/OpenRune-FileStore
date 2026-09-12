package dev.openrune.cache

import java.util.Arrays

/**
 * A `MutableMap<Int, V>` stored as two parallel arrays — sorted int keys and their values — for
 * definition ids. No hash nodes and no boxed `Integer` keys, which is roughly 50 bytes per entry
 * less than a `LinkedHashMap` across the hundred-thousand-plus definitions the cache manager
 * holds, and unlike a plain array-by-id it wastes nothing on id gaps (custom content often jumps
 * from the stock range to 64k+).
 *
 * Lookups are a binary search; appending in ascending key order — which is what a sequential
 * decode does — is O(1). The `Map` API is unchanged, so every consumer keeps working, and
 * iteration is in ascending key order.
 */
class SortedIntMap<V : Any>(initialCapacity: Int = 16) : AbstractMutableMap<Int, V>() {

    private var ids = IntArray(maxOf(initialCapacity, 1))
    private var slots = arrayOfNulls<Any>(maxOf(initialCapacity, 1))
    private var count = 0

    override val size: Int get() = count

    private fun indexOf(key: Int): Int = Arrays.binarySearch(ids, 0, count, key)

    @Suppress("UNCHECKED_CAST")
    private fun valueAt(index: Int): V = slots[index] as V

    @Suppress("UNCHECKED_CAST")
    override fun get(key: Int): V? {
        val index = indexOf(key)
        return if (index >= 0) slots[index] as V else null
    }

    override fun containsKey(key: Int): Boolean = indexOf(key) >= 0

    override fun put(key: Int, value: V): V? {
        val index = indexOf(key)
        if (index >= 0) {
            val previous = valueAt(index)
            slots[index] = value
            return previous
        }

        val insertAt = -(index + 1)
        if (count == ids.size) {
            val grown = maxOf(ids.size * 2, 8)
            ids = ids.copyOf(grown)
            slots = slots.copyOf(grown)
        }
        if (insertAt < count) {
            System.arraycopy(ids, insertAt, ids, insertAt + 1, count - insertAt)
            System.arraycopy(slots, insertAt, slots, insertAt + 1, count - insertAt)
        }
        ids[insertAt] = key
        slots[insertAt] = value
        count++
        return null
    }

    override fun remove(key: Int): V? {
        val index = indexOf(key)
        if (index < 0) return null
        val previous = valueAt(index)
        removeAt(index)
        return previous
    }

    private fun removeAt(index: Int) {
        val tail = count - index - 1
        if (tail > 0) {
            System.arraycopy(ids, index + 1, ids, index, tail)
            System.arraycopy(slots, index + 1, slots, index, tail)
        }
        count--
        slots[count] = null
    }

    override fun clear() {
        Arrays.fill(slots, 0, count, null)
        count = 0
    }

    /** A read-only view of this map; the returned instance cannot be cast back to a mutable one. */
    fun readOnly(): Map<Int, V> = ReadOnlyIntMap(this)

    override val entries: MutableSet<MutableMap.MutableEntry<Int, V>> = object :
        AbstractMutableSet<MutableMap.MutableEntry<Int, V>>() {

        override val size: Int get() = count

        override fun add(element: MutableMap.MutableEntry<Int, V>): Boolean {
            val previous = put(element.key, element.value)
            return previous != element.value
        }

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<Int, V>> =
            object : MutableIterator<MutableMap.MutableEntry<Int, V>> {
                private var cursor = 0
                private var lastKey = -1
                private var hasLast = false

                override fun hasNext(): Boolean = cursor < count

                override fun next(): MutableMap.MutableEntry<Int, V> {
                    if (cursor >= count) throw NoSuchElementException()
                    lastKey = ids[cursor]
                    hasLast = true
                    cursor++
                    return Entry(lastKey)
                }

                override fun remove() {
                    check(hasLast) { "next() has not been called" }
                    this@SortedIntMap.remove(lastKey)
                    cursor--
                    hasLast = false
                }
            }
    }

    private inner class Entry(override val key: Int) : MutableMap.MutableEntry<Int, V> {

        override val value: V get() = this@SortedIntMap[key] ?: error("entry $key was removed")

        override fun setValue(newValue: V): V {
            val previous = value
            put(key, newValue)
            return previous
        }

        override fun equals(other: Any?): Boolean =
            other is Map.Entry<*, *> && other.key == key && other.value == value

        override fun hashCode(): Int = key.hashCode() xor value.hashCode()

        override fun toString(): String = "$key=$value"
    }
}

/**
 * Read-only wrapper handed out by the cache manager: it only implements [Map], so a loaded table
 * cannot be mutated even by casting.
 */
class ReadOnlyIntMap<V : Any>(private val backing: SortedIntMap<V>) : Map<Int, V> by backing
