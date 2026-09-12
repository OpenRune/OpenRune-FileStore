package dev.openrune.cache.tools.dsl

import dev.openrune.definition.type.EnumType
import dev.openrune.definition.type.builders.EnumTypeBuilder
import dev.openrune.definition.type.OverlayType
import dev.openrune.definition.type.builders.OverlayTypeBuilder
import dev.openrune.definition.type.SequenceType
import dev.openrune.definition.type.builders.SequenceTypeBuilder

/** Builder DSL for [EnumType]. */
fun enumType(id: Int = -1, block: EnumTypeBuilder.() -> Unit = {}): EnumType =
    EnumTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun EnumType.edit(block: EnumTypeBuilder.() -> Unit): EnumType =
    toBuilder().apply(block).build()

/** Builder DSL for [SequenceType]. */
fun sequenceType(id: Int = -1, block: SequenceTypeBuilder.() -> Unit = {}): SequenceType =
    SequenceTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun SequenceType.edit(block: SequenceTypeBuilder.() -> Unit): SequenceType =
    toBuilder().apply(block).build()

/** Builder DSL for [OverlayType]; the HSL fields are derived in `build()`. */
fun overlayType(id: Int = -1, block: OverlayTypeBuilder.() -> Unit = {}): OverlayType =
    OverlayTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun OverlayType.edit(block: OverlayTypeBuilder.() -> Unit): OverlayType =
    toBuilder().apply(block).build()
