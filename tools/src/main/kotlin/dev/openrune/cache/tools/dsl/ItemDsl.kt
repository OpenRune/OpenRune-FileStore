package dev.openrune.cache.tools.dsl

import dev.openrune.definition.EntityOpsBuilder
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.type.builders.ItemTypeBuilder

/**
 * Builder DSL for [ItemType]:
 *
 * ```kotlin
 * val sword = itemType(4151) {
 *     name = "Fancy whip"
 *     cost = 120_001
 *     options {
 *         op(1, "Wield")
 *     }
 * }
 * val renamed = sword.edit { name = "Fancier whip" }
 * ```
 */
fun itemType(id: Int = -1, block: ItemTypeBuilder.() -> Unit = {}): ItemType =
    ItemTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun ItemType.edit(block: ItemTypeBuilder.() -> Unit): ItemType =
    toBuilder().apply(block).build()

/** Op-set sugar inside an [itemType] or [edit] block. */
fun ItemTypeBuilder.options(block: EntityOpsBuilder.() -> Unit) {
    options = options.toBuilder().apply(block).build()
}
