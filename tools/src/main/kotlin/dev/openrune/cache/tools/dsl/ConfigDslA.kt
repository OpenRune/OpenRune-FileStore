package dev.openrune.cache.tools.dsl

import dev.openrune.definition.type.HealthBarType
import dev.openrune.definition.type.builders.HealthBarTypeBuilder
import dev.openrune.definition.type.HitSplatType
import dev.openrune.definition.type.builders.HitSplatTypeBuilder
import dev.openrune.definition.type.InventoryType
import dev.openrune.definition.type.builders.InventoryTypeBuilder
import dev.openrune.definition.type.StructType
import dev.openrune.definition.type.builders.StructTypeBuilder
import dev.openrune.definition.type.VarBitType
import dev.openrune.definition.type.builders.VarBitTypeBuilder
import dev.openrune.definition.type.VarpType
import dev.openrune.definition.type.builders.VarpTypeBuilder

/**
 * Builder DSLs for [VarBitType], [VarpType], [StructType], [HealthBarType], [HitSplatType] and
 * [InventoryType]. Definitions are immutable, so tools assemble one here and get the built
 * instance back:
 *
 * ```kotlin
 * val bit = varBitType(8119) {
 *     varp = 1631
 *     startBit = 0
 *     endBit = 7
 * }
 *
 * val widened = bit.edit { endBit = 15 }
 * ```
 */
fun varBitType(id: Int = -1, block: VarBitTypeBuilder.() -> Unit = {}): VarBitType =
    VarBitTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun VarBitType.edit(block: VarBitTypeBuilder.() -> Unit): VarBitType =
    toBuilder().apply(block).build()

fun varpType(id: Int = -1, block: VarpTypeBuilder.() -> Unit = {}): VarpType =
    VarpTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun VarpType.edit(block: VarpTypeBuilder.() -> Unit): VarpType =
    toBuilder().apply(block).build()

fun structType(id: Int = -1, block: StructTypeBuilder.() -> Unit = {}): StructType =
    StructTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun StructType.edit(block: StructTypeBuilder.() -> Unit): StructType =
    toBuilder().apply(block).build()

fun healthBarType(id: Int = -1, block: HealthBarTypeBuilder.() -> Unit = {}): HealthBarType =
    HealthBarTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun HealthBarType.edit(block: HealthBarTypeBuilder.() -> Unit): HealthBarType =
    toBuilder().apply(block).build()

fun hitSplatType(id: Int = -1, block: HitSplatTypeBuilder.() -> Unit = {}): HitSplatType =
    HitSplatTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun HitSplatType.edit(block: HitSplatTypeBuilder.() -> Unit): HitSplatType =
    toBuilder().apply(block).build()

fun inventoryType(id: Int = -1, block: InventoryTypeBuilder.() -> Unit = {}): InventoryType =
    InventoryTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun InventoryType.edit(block: InventoryTypeBuilder.() -> Unit): InventoryType =
    toBuilder().apply(block).build()
