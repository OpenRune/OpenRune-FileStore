package dev.openrune.cache.tools.dsl

import dev.openrune.definition.type.AmbienceType
import dev.openrune.definition.type.builders.AmbienceTypeBuilder
import dev.openrune.definition.type.IdentityKitType
import dev.openrune.definition.type.builders.IdentityKitTypeBuilder
import dev.openrune.definition.type.MapElementType
import dev.openrune.definition.type.builders.MapElementTypeBuilder
import dev.openrune.definition.type.SpotAnimType
import dev.openrune.definition.type.builders.SpotAnimTypeBuilder
import dev.openrune.definition.type.TextureType
import dev.openrune.definition.type.builders.TextureTypeBuilder
import dev.openrune.definition.type.UnderlayType
import dev.openrune.definition.type.builders.UnderlayTypeBuilder

/**
 * Builder DSLs for the small config definitions. Definitions are immutable, so tools assemble one
 * here and get the built instance back:
 *
 * ```kotlin
 * val kit = identityKitType(5) {
 *     bodyPartId = 0
 *     models = mutableListOf(255)
 * }
 *
 * val renamed = kit.edit { nonSelectable = true }
 * ```
 */

fun identityKitType(id: Int = -1, block: IdentityKitTypeBuilder.() -> Unit = {}): IdentityKitType =
    IdentityKitTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun IdentityKitType.edit(block: IdentityKitTypeBuilder.() -> Unit): IdentityKitType =
    toBuilder().apply(block).build()

fun spotAnimType(id: Int = -1, block: SpotAnimTypeBuilder.() -> Unit = {}): SpotAnimType =
    SpotAnimTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun SpotAnimType.edit(block: SpotAnimTypeBuilder.() -> Unit): SpotAnimType =
    toBuilder().apply(block).build()

fun textureType(id: Int = -1, block: TextureTypeBuilder.() -> Unit = {}): TextureType =
    TextureTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun TextureType.edit(block: TextureTypeBuilder.() -> Unit): TextureType =
    toBuilder().apply(block).build()

fun underlayType(id: Int = -1, block: UnderlayTypeBuilder.() -> Unit = {}): UnderlayType =
    UnderlayTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun UnderlayType.edit(block: UnderlayTypeBuilder.() -> Unit): UnderlayType =
    toBuilder().apply(block).build()

fun ambienceType(id: Int = -1, block: AmbienceTypeBuilder.() -> Unit = {}): AmbienceType =
    AmbienceTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun AmbienceType.edit(block: AmbienceTypeBuilder.() -> Unit): AmbienceType =
    toBuilder().apply(block).build()

fun mapElementType(id: Int = -1, block: MapElementTypeBuilder.() -> Unit = {}): MapElementType =
    MapElementTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun MapElementType.edit(block: MapElementTypeBuilder.() -> Unit): MapElementType =
    toBuilder().apply(block).build()
