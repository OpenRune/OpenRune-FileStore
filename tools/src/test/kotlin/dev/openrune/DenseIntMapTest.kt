package dev.openrune

import dev.openrune.cache.DenseIntMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

/** Contract locks for the sorted-array map the cache manager holds definitions in. */
class DenseIntMapTest {

    @Test
    fun `put get remove and size behave like a map`() {
        val map = DenseIntMap<String>()

        assertNull(map.put(5, "five"))
        assertEquals("five", map.put(5, "five2"))
        assertEquals("five2", map[5])
        assertEquals(1, map.size)
        assertTrue(map.containsKey(5))
        assertFalse(map.containsKey(4))
        assertNull(map[999])

        assertEquals("five2", map.remove(5))
        assertNull(map.remove(5))
        assertTrue(map.isEmpty())
    }

    @Test
    fun `gapped ids cost nothing and stay ordered`() {
        val map = DenseIntMap<String>()
        map[64_000] = "custom"
        map[3] = "stock"
        map[32_000] = "late-stock"

        assertEquals(listOf(3, 32_000, 64_000), map.keys.toList())
        assertEquals("custom", map[64_000])
        assertNull(map[40_000])
        assertEquals(3, map.size)
    }

    @Test
    fun `iteration is ascending and skips removed keys`() {
        val map = DenseIntMap<String>()
        map[10] = "ten"
        map[2] = "two"
        map[7] = "seven"
        map.remove(7)

        assertEquals(listOf(2 to "two", 10 to "ten"), map.entries.map { it.key to it.value })
        assertEquals(listOf(2, 10), map.keys.toList())
        assertEquals(listOf("two", "ten"), map.values.toList())
    }

    @Test
    fun `equals hashCode and copies interoperate with standard maps`() {
        val sorted = DenseIntMap<String>()
        val reference = LinkedHashMap<Int, String>()
        for (i in 0 until 50 step 3) {
            sorted[i] = "v$i"
            reference[i] = "v$i"
        }

        assertEquals(reference as Map<Int, String>, sorted)
        assertEquals(sorted as Map<Int, String>, reference)
        assertEquals(reference.hashCode(), sorted.hashCode())
        assertEquals(reference, HashMap(sorted))
        assertEquals(sorted, DenseIntMap<String>().apply { putAll(reference) })
    }

    @Test
    fun `random operations agree with a reference map`() {
        val random = Random(1234)
        val sorted = DenseIntMap<Int>()
        val reference = HashMap<Int, Int>()

        repeat(20_000) {
            val key = random.nextInt(0, 5000)
            when (random.nextInt(4)) {
                0, 1 -> {
                    val value = random.nextInt()
                    assertEquals(reference.put(key, value), sorted.put(key, value))
                }
                2 -> assertEquals(reference.remove(key), sorted.remove(key))
                else -> assertEquals(reference[key], sorted[key])
            }
        }

        assertEquals(reference.size, sorted.size)
        assertEquals(reference, HashMap(sorted))
    }

    @Test
    fun `entry iterator remove works`() {
        val map = DenseIntMap<String>()
        map[1] = "a"
        map[2] = "b"
        map[3] = "c"

        val iterator = map.entries.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().key == 2) iterator.remove()
        }

        assertEquals(setOf(1, 3), map.keys)
        assertEquals(2, map.size)
    }

    @Test
    fun `entry setValue writes through`() {
        val map = DenseIntMap<String>()
        map[4] = "before"
        map.entries.first().setValue("after")
        assertEquals("after", map[4])
    }

    @Test
    fun `readOnly view reads through and is not a MutableMap`() {
        val map = DenseIntMap<String>()
        map[7] = "seven"
        val view = map.readOnly()

        assertEquals("seven", view[7])
        assertEquals(1, view.size)
        assertFalse(view is MutableMap<*, *>)
    }

    @Test
    fun `clear resets and the map can be refilled`() {
        val map = DenseIntMap<String>()
        map[100] = "x"
        map.clear()
        assertTrue(map.isEmpty())
        assertNull(map[100])
        map[3] = "y"
        assertEquals(mapOf(3 to "y"), map)
    }
}
