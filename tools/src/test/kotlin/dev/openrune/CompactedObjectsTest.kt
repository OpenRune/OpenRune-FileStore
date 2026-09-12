package dev.openrune

import dev.openrune.cache.CONFIGS
import dev.openrune.cache.DefinitionCompactor
import dev.openrune.cache.OBJECT
import dev.openrune.definition.EntityOpsDefinition
import dev.openrune.definition.codec.ObjectCodec
import dev.openrune.definition.type.ObjectType
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

/**
 * Locks the compact-and-freeze pass the manager runs over loaded objects: values must be
 * byte-identical to an uncompacted decode, equal lists and op sets must collapse to shared
 * instances, and mutating anything the manager holds must throw. Fresh instances stay fully
 * mutable — they are what the codecs and packing tools build with.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompactedObjectsTest {

    private val codec = ObjectCodec(240)
    private val objects = LinkedHashMap<Int, ObjectType>()
    private val encodedBefore = HashMap<Int, ByteArray>()

    @BeforeAll
    fun load() {
        val cache = Cache.load(File("../data/cache").toPath())
        try {
            for (id in cache.files(CONFIGS, OBJECT)) {
                val data = cache.data(CONFIGS, OBJECT, id) ?: continue
                objects[id] = codec.loadData(id, data)
            }
        } finally {
            cache.close()
        }

        for ((id, definition) in objects) {
            encodedBefore[id] = encode(definition)
        }

        DefinitionCompactor.compactObjects(objects)
    }

    private fun encode(definition: ObjectType): ByteArray {
        val buffer = Unpooled.buffer(256)
        try {
            with(codec) { buffer.encode(definition) }
            return ByteArray(buffer.readableBytes()).also { buffer.readBytes(it) }
        } finally {
            buffer.release()
        }
    }

    @Test
    fun `compaction changes no values`() {
        for ((id, definition) in objects) {
            assertArrayEquals(encodedBefore[id], encode(definition), "object $id")
        }
    }

    @Test
    fun `equal lists collapse to one shared instance`() {
        val byContent = HashMap<List<Int>, MutableList<Int>>()
        for (definition in objects.values) {
            val models = definition.objectModels ?: continue
            val canonical = byContent.getOrPut(models) { models }
            assertSame(canonical, models, "object ${definition.id} models")
        }
        assertTrue(byContent.size < objects.values.count { it.objectModels != null }) {
            "no model lists were shared at all"
        }
    }

    @Test
    fun `equal op sets collapse and empty ops share the singleton`() {
        val empty = objects.values.first { it.actions.isEmpty() }
        assertSame(EntityOpsDefinition.EMPTY, empty.actions, "object ${empty.id}")

        val byRender = HashMap<String, EntityOpsDefinition>()
        for (definition in objects.values) {
            if (definition.actions.isEmpty()) continue
            val canonical = byRender.getOrPut(definition.actions.toString()) { definition.actions }
            assertSame(canonical, definition.actions, "object ${definition.id} actions")
        }
        assertTrue(byRender.size < objects.values.count { !it.actions.isEmpty() }) {
            "no op sets were shared at all"
        }
    }

    @Test
    fun `loaded lists and ops refuse mutation`() {
        val withModels = objects.values.first { !it.objectModels.isNullOrEmpty() }
        assertThrows(UnsupportedOperationException::class.java) { withModels.objectModels!!.add(1) }
        assertThrows(UnsupportedOperationException::class.java) { withModels.objectModels!![0] = 1 }

        val withOps = objects.values.first { !it.actions.isEmpty() }
        assertThrows(UnsupportedOperationException::class.java) { withOps.actions.setOp(0, "Nope") }
        assertThrows(UnsupportedOperationException::class.java) { withOps.actions.ops.add(null) }
        assertThrows(UnsupportedOperationException::class.java) { EntityOpsDefinition.EMPTY.setOp(0, "Nope") }
    }

    @Test
    fun `fresh definitions stay mutable for the packing tools`() {
        val fresh = ObjectType()
        fresh.objectModels = mutableListOf(1, 2)
        fresh.objectModels!!.add(3)
        fresh.actions.setOp(0, "Open")

        assertEquals(listOf(1, 2, 3), fresh.objectModels)
        assertEquals("Open", fresh.actions.getOpOrNull(0))
        assertNotNull(encode(fresh))
    }
}
