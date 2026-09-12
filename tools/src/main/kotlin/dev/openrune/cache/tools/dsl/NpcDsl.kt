package dev.openrune.cache.tools.dsl

import dev.openrune.definition.EntityOpsBuilder
import dev.openrune.definition.type.NpcType
import dev.openrune.definition.type.builders.NpcTypeBuilder

fun npcType(id: Int = -1, block: NpcTypeBuilder.() -> Unit = {}): NpcType =
    NpcTypeBuilder(id).apply(block).build()

fun NpcType.edit(block: NpcTypeBuilder.() -> Unit): NpcType =
    toBuilder().apply(block).build()

fun NpcTypeBuilder.actions(block: EntityOpsBuilder.() -> Unit) {
    actions = actions.toBuilder().apply(block).build()
}
