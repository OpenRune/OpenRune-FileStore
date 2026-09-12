package dev.openrune

import dev.openrune.cache.tools.dsl.actions
import dev.openrune.cache.tools.dsl.edit
import dev.openrune.cache.tools.dsl.objectType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ObjectDslTest {

    @Test
    fun `builds an immutable definition from the dsl`() {
        val door = objectType(1234) {
            name = "Fancy door"
            sizeX = 2
            objectModels = mutableListOf(4567, 4568)
            actions {
                op(0, "Open")
                op(1, "Close")
            }
        }

        assertEquals(1234, door.id)
        assertEquals("Fancy door", door.name)
        assertEquals(2, door.sizeX)
        assertEquals(listOf(4567, 4568), door.objectModels)
        assertEquals("Open", door.actions.getOpOrNull(0))
        assertEquals("Close", door.actions.getOpOrNull(1))
    }

    @Test
    fun `edit copies and applies changes without touching the original`() {
        val door = objectType(1) {
            name = "Door"
            objectModels = mutableListOf(10)
            actions { op(0, "Open") }
        }

        val renamed = door.edit {
            name = "Gate"
            actions { op(1, "Close") }
        }

        assertNotSame(door, renamed)
        assertEquals("Door", door.name)
        assertEquals("Gate", renamed.name)
        assertEquals(listOf(10), renamed.objectModels)
        assertEquals("Open", renamed.actions.getOpOrNull(0))
        assertEquals("Close", renamed.actions.getOpOrNull(1))
        assertEquals(null, door.actions.getOpOrNull(1))
    }

    @Test
    fun `equal lists and op sets are shared between built definitions`() {
        val first = objectType(1) {
            objectModels = mutableListOf(42, 43)
            actions { op(0, "Chop down") }
        }
        val second = objectType(2) {
            objectModels = mutableListOf(42, 43)
            actions { op(0, "Chop down") }
        }

        assertSame(first.objectModels, second.objectModels)
        assertSame(first.actions, second.actions)
    }
}
