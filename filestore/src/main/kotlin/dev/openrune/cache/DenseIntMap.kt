package dev.openrune.cache

import java.util.Arrays

/**
 * A `MutableMap<Int, V>` for definition ids, stored as a plain array indexed by key: a slot is
 * the value or null for a gap. That is 4 bytes per id — half of what the previous sorted
 * key/value pair layout cost per entry, and far less than a `LinkedHashMap`'s hash node plus
 * boxed `Integer` key (~50 bytes of structure per entry across 112 000+ definitions). Gaps cost
 * one null slot each, which at game-content densities (even stock items ending at 32k with
 * custom content resuming at 64k) stays cheaper than per-entry bookkeeping.
 *
 * Lookups are a direct array index. The `Map` API is unchanged, so every consumer keeps working,
 * and iteration is in ascending key order. Keys must be >= 0: `get`/`containsKey` on a negative
 * key simply miss, `put` throws.
 */
class DenseIntMap<V : Any>(initialCapacity: Int = 16) : AbstractMutableMap<Int, V>() {

    private var slots = arrayOfNulls<Any>(maxOf(initialCapacity, 1))
    private var count = 0

    /** Highest key ever written, so iteration does not scan unused capacity. */
    private var maxKey = -1

    override val size: Int get() = count

    @Suppress("UNCHECKED_CAST")
    private fun slot(key: Int): V? = if (key in 0..maxKey) slots[key] as V? else null

    override fun get(key: Int): V? = slot(key)

    override fun containsKey(key: Int): Boolean = slot(key) != null

    override fun put(key: Int, value: V): V? {
        require(key >= 0) { "DenseIntMap keys must be >= 0, got $key" }
        if (key >= slots.size) {
            slots = slots.copyOf(maxOf(key + 1, slots.size * 2))
        }
        val previous = slot(key)
        slots[key] = value
        if (previous == null) count++
        if (key > maxKey) maxKey = key
        return previous
    }

    override fun remove(key: Int): V? {
        val previous = slot(key) ?: return null
        slots[key] = null
        count--
        return previous
    }

    override fun clear() {
        Arrays.fill(slots, 0, maxKey + 1, null)
        count = 0
        maxKey = -1
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
                private var next = -1
                private var current = -1

                init {
                    advance(0)
                }

                private fun advance(from: Int) {
                    var index = from
                    val limit = maxKey
                    while (index <= limit && slots[index] == null) index++
                    next = if (index <= limit) index else -1
                }

                override fun hasNext(): Boolean = next != -1

                override fun next(): MutableMap.MutableEntry<Int, V> {
                    val index = next
                    if (index == -1) throw NoSuchElementException()
                    current = index
                    advance(index + 1)
                    return Entry(index)
                }

                override fun remove() {
                    check(current != -1) { "next() has not been called" }
                    this@DenseIntMap.remove(current)
                    current = -1
                }
            }
    }

    private inner class Entry(override val key: Int) : MutableMap.MutableEntry<Int, V> {

        @Suppress("UNCHECKED_CAST")
        override val value: V get() = slots[key] as V

        override fun setValue(newValue: V): V {
            val previous = value
            slots[key] = newValue
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
class ReadOnlyIntMap<V : Any>(private val backing: DenseIntMap<V>) : Map<Int, V> by backing
