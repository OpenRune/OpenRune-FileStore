package dev.openrune.definition.util

/**
 * A `MutableList<Int>` stored as a plain `IntArray`: four bytes per element instead of a boxed
 * `Integer` and a reference. The definition types keep their declared `MutableList<Int>` fields,
 * so nothing downstream changes - decode just hands back this implementation. Equality and hash
 * follow the `List` contract via [AbstractMutableList], so it compares equal to any other list
 * with the same values, in both directions.
 *
 * Reads and writes through the generic `List` surface still box transiently; only the retained
 * storage is primitive, which is the part that matters for a cache held in memory.
 */
class IntBackedList private constructor(
    private var elements: IntArray,
    private var count: Int
) : AbstractMutableList<Int>(), RandomAccess {

    constructor(elements: IntArray) : this(elements, elements.size)

    constructor(initialCapacity: Int) : this(IntArray(initialCapacity), 0)

    override val size: Int get() = count

    override fun get(index: Int): Int {
        checkIndex(index)
        return elements[index]
    }

    override fun set(index: Int, element: Int): Int {
        checkIndex(index)
        val previous = elements[index]
        elements[index] = element
        return previous
    }

    override fun add(index: Int, element: Int) {
        if (index < 0 || index > count) throw IndexOutOfBoundsException("index $index, size $count")
        if (count == elements.size) {
            elements = elements.copyOf(if (elements.isEmpty()) 4 else elements.size * 2)
        }
        System.arraycopy(elements, index, elements, index + 1, count - index)
        elements[index] = element
        count++
    }

    override fun removeAt(index: Int): Int {
        checkIndex(index)
        val removed = elements[index]
        System.arraycopy(elements, index + 1, elements, index, count - index - 1)
        count--
        return removed
    }

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= count) throw IndexOutOfBoundsException("index $index, size $count")
    }
}

/**
 * Reads [length] values into an array-backed `Int` list, so decoded id, colour and frame lists
 * hold no boxed elements at all.
 */
inline fun readIntList(length: Int, read: (Int) -> Int): MutableList<Int> {
    val values = IntArray(length)
    for (i in 0 until length) {
        values[i] = read(i)
    }
    return IntBackedList(values)
}

