package dev.openrune.cache

import dev.openrune.definition.EntityOpsDefinition
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.type.NpcType
import dev.openrune.definition.type.ObjectType
import dev.openrune.definition.util.IntBackedList

/**
 * Compacts and freezes loaded definitions once they reach [CacheManager].
 *
 * A cache is full of copies: the same object placed at four rotations is four ids with identical
 * model, type and colour lists, and thousands of doors share the exact same op set. While a
 * definition is being decoded or packed it has to be mutable, so nothing can be shared — but the
 * moment the manager adopts the tables the instances are final. This pass interns equal lists and
 * op sets to one frozen instance each; mutating anything shared afterwards throws.
 *
 * The mutable types themselves stay as they are — a freshly constructed [ObjectType] is the
 * builder the codecs and packing tools work with. Only what the manager holds is frozen.
 */
object DefinitionCompactor {

    fun compactObjects(objects: Map<Int, ObjectType>) {
        val lists = HashMap<List<Int>, IntBackedList>()
        val ops = HashMap<String, EntityOpsDefinition>()

        for (definition in objects.values) {
            definition.objectModels = intern(lists, definition.objectModels)
            definition.objectTypes = intern(lists, definition.objectTypes)
            definition.ambientSoundIds = intern(lists, definition.ambientSoundIds)
            definition.originalColours = intern(lists, definition.originalColours)
            definition.modifiedColours = intern(lists, definition.modifiedColours)
            definition.originalTextureColours = intern(lists, definition.originalTextureColours)
            definition.modifiedTextureColours = intern(lists, definition.modifiedTextureColours)
            definition.transforms = intern(lists, definition.transforms)
            definition.actions = intern(ops, definition.actions)
        }
    }

    fun compactNpcs(npcs: Map<Int, NpcType>) {
        val lists = HashMap<List<Int>, IntBackedList>()
        val ops = HashMap<String, EntityOpsDefinition>()

        for (definition in npcs.values) {
            definition.models = intern(lists, definition.models)
            definition.chatheadModels = intern(lists, definition.chatheadModels)
            definition.headIconGraphics = intern(lists, definition.headIconGraphics)
            definition.headIconIndexes = intern(lists, definition.headIconIndexes)
            definition.originalColours = intern(lists, definition.originalColours)
            definition.modifiedColours = intern(lists, definition.modifiedColours)
            definition.originalTextureColours = intern(lists, definition.originalTextureColours)
            definition.modifiedTextureColours = intern(lists, definition.modifiedTextureColours)
            definition.transforms = intern(lists, definition.transforms)
            definition.actions = intern(ops, definition.actions)
        }
    }

    fun compactItems(items: Map<Int, ItemType>) {
        val lists = HashMap<List<Int>, IntBackedList>()
        val ops = HashMap<String, EntityOpsDefinition>()

        for (definition in items.values) {
            definition.countObj = intern(lists, definition.countObj)
            definition.countCo = intern(lists, definition.countCo)
            definition.originalColours = intern(lists, definition.originalColours)
            definition.modifiedColours = intern(lists, definition.modifiedColours)
            definition.originalTextureColours = intern(lists, definition.originalTextureColours)
            definition.modifiedTextureColours = intern(lists, definition.modifiedTextureColours)
            definition.options = intern(ops, definition.options)
        }
    }

    /**
     * Returns the one frozen instance for this list's contents. Non-primitive lists (for example
     * from TOML merges) are converted, so everything the manager holds is array backed.
     */
    private fun intern(pool: HashMap<List<Int>, IntBackedList>, list: MutableList<Int>?): MutableList<Int>? {
        if (list == null) return null
        return pool.getOrPut(list) {
            (list as? IntBackedList ?: IntBackedList(list.toIntArray())).freeze()
        }
    }

    /**
     * Returns the one frozen instance for this op set's contents. [EntityOpsDefinition.toString]
     * is a full render of the recorded ops, so it serves as the content key; [contentEquals]
     * verifies the match so a colliding render can never alias two different op sets.
     */
    private fun intern(pool: HashMap<String, EntityOpsDefinition>, ops: EntityOpsDefinition): EntityOpsDefinition {
        if (ops.isEmpty()) return EntityOpsDefinition.EMPTY

        val key = ops.toString()
        val existing = pool[key]
        if (existing != null && existing.contentEquals(ops)) {
            return existing
        }
        pool[key] = ops.freeze()
        return ops
    }
}
