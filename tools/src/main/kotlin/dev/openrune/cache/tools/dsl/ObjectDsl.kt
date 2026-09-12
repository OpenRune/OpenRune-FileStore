package dev.openrune.cache.tools.dsl

import dev.openrune.definition.EntityOpsBuilder
import dev.openrune.definition.type.ObjectType
import dev.openrune.definition.type.ObjectTypeBuilder

/**
 * Builder DSL for [ObjectType]. Definitions are immutable, so tools assemble one here and get the
 * built instance back:
 *
 * ```kotlin
 * val door = objectType(1234) {
 *     name = "Fancy door"
 *     sizeX = 1
 *     objectModels = mutableListOf(4567)
 *     actions {
 *         op(0, "Open")
 *         op(1, "Close")
 *     }
 * }
 *
 * val renamed = door.edit { name = "Fancier door" }
 * ```
 */
fun objectType(id: Int = -1, block: ObjectTypeBuilder.() -> Unit = {}): ObjectType =
    ObjectTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun ObjectType.edit(block: ObjectTypeBuilder.() -> Unit): ObjectType =
    toBuilder().apply(block).build()

/** Op-set sugar inside an [objectType] or [edit] block. */
fun ObjectTypeBuilder.actions(block: EntityOpsBuilder.() -> Unit) {
    actions.block()
}
