package dev.openrune.cache.tools.dsl

import dev.openrune.definition.EntityOpsBuilder
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.type.builders.ItemTypeBuilder

fun itemType(id: Int = -1, block: ItemTypeBuilder.() -> Unit = {}): ItemType =
    ItemTypeBuilder(id).apply(block).build()

fun ItemType.edit(block: ItemTypeBuilder.() -> Unit): ItemType =
    toBuilder().apply(block).build()

fun ItemTypeBuilder.options(block: EntityOpsBuilder.() -> Unit) {
    options = options.toBuilder().apply(block).build()
}
