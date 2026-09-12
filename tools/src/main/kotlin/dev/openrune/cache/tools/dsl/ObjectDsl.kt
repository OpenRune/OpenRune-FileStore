package dev.openrune.cache.tools.dsl

import dev.openrune.definition.EntityOpsBuilder
import dev.openrune.definition.type.ObjectType
import dev.openrune.definition.type.builders.ObjectTypeBuilder

fun objectType(id: Int = -1, block: ObjectTypeBuilder.() -> Unit = {}): ObjectType =
    ObjectTypeBuilder(id).apply(block).build()

fun ObjectType.edit(block: ObjectTypeBuilder.() -> Unit): ObjectType =
    toBuilder().apply(block).build()

fun ObjectTypeBuilder.actions(block: EntityOpsBuilder.() -> Unit) {
    actions.block()
}
