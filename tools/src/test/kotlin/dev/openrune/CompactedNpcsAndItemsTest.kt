package dev.openrune

import dev.openrune.cache.CONFIGS
import dev.openrune.cache.DefinitionCompactor
import dev.openrune.cache.ITEM
import dev.openrune.cache.NPC
import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.codec.ItemCodec
import dev.openrune.definition.codec.NPCCodec
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.type.NpcType
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** The same compact-and-freeze locks as [CompactedObjectsTest], for the npc and item passes. */
class CompactedNpcsAndItemsTest {

    private fun <T : Definition> loadAll(codec: DefinitionCodec<T>, archive: Int): LinkedHashMap<Int, T> {
        val cache = Cache.load(File("../data/cache").toPath())
        val definitions = LinkedHashMap<Int, T>()
        try {
            for (id in cache.files(CONFIGS, archive)) {
                val data = cache.data(CONFIGS, archive, id) ?: continue
                definitions[id] = codec.loadData(id, data)
            }
        } finally {
            cache.close()
        }
        return definitions
    }

    private fun <T : Definition> encode(codec: DefinitionCodec<T>, definition: T): ByteArray {
        val buffer = Unpooled.buffer(256)
        try {
            with(codec) { buffer.encode(definition) }
            return ByteArray(buffer.readableBytes()).also { buffer.readBytes(it) }
        } finally {
            buffer.release()
        }
    }

    @Test
    fun `npc compaction changes no values and freezes the shared state`() {
        val codec = NPCCodec(240)
        val npcs = loadAll(codec, NPC)
        val before = npcs.mapValues { (_, npc) -> encode(codec, npc) }

        DefinitionCompactor.compactNpcs(npcs)

        for ((id, npc) in npcs) {
            assertArrayEquals(before[id], encode(codec, npc), "npc $id")
        }

        val modelLists = npcs.values.mapNotNull { it.models }
        assertTrue(modelLists.toSet().size < modelLists.size) { "no npc model lists were shared" }

        val withModels = npcs.values.first { !it.models.isNullOrEmpty() }
        assertThrows(UnsupportedOperationException::class.java) { withModels.models!!.add(1) }
        val withOps = npcs.values.first { !it.actions.isEmpty() }
        assertThrows(UnsupportedOperationException::class.java) { withOps.actions.setOp(0, "Nope") }
    }

    @Test
    fun `item compaction changes no values and freezes the shared state`() {
        val codec = ItemCodec(240)
        val items = loadAll(codec, ITEM)
        val before = items.mapValues { (_, item) -> encode(codec, item) }

        DefinitionCompactor.compactItems(items)

        for ((id, item) in items) {
            assertArrayEquals(before[id], encode(codec, item), "item $id")
        }

        // Every stock item shares the default Take op set once compacted.
        val optionSets = items.values.map { it.options }.toSet()
        assertTrue(optionSets.size < items.size / 2) { "item op sets were not shared (${optionSets.size})" }

        val withOps = items.values.first()
        assertThrows(UnsupportedOperationException::class.java) { withOps.options.setOp(0, "Nope") }

        // The linked-template path assigns whole fields on a copy; that must stay legal.
        val copy = items.values.first { it.noteTemplateId != -1 }.copy()
        copy.name = "renamed"
        copy.stacks = copy.stacks
    }
}
