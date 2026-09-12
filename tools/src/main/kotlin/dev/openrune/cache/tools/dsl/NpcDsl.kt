package dev.openrune.cache.tools.dsl

import dev.openrune.definition.EntityOpsBuilder
import dev.openrune.definition.type.NpcType
import dev.openrune.definition.type.builders.NpcTypeBuilder

/**
 * Builder DSL for [NpcType]. Definitions are immutable, so tools assemble one here and get the
 * built instance back:
 *
 * ```kotlin
 * val goblin = npcType(1234) {
 *     name = "Angry goblin"
 *     size = 1
 *     models = mutableListOf(4567)
 *     actions {
 *         op(0, "Talk-to")
 *         op(1, "Attack")
 *     }
 * }
 *
 * val renamed = goblin.edit { name = "Angrier goblin" }
 * ```
 */
fun npcType(id: Int = -1, block: NpcTypeBuilder.() -> Unit = {}): NpcType =
    NpcTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun NpcType.edit(block: NpcTypeBuilder.() -> Unit): NpcType =
    toBuilder().apply(block).build()

/** Op-set sugar inside an [npcType] or [edit] block. */
fun NpcTypeBuilder.actions(block: EntityOpsBuilder.() -> Unit) {
    actions = actions.toBuilder().apply(block).build()
}
