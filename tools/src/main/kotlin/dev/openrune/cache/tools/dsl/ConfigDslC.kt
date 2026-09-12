package dev.openrune.cache.tools.dsl

import dev.openrune.definition.type.BugTemplateType
import dev.openrune.definition.type.builders.BugTemplateTypeBuilder
import dev.openrune.definition.type.DBTableIndexType
import dev.openrune.definition.type.builders.DBTableIndexTypeBuilder
import dev.openrune.definition.type.ParamType
import dev.openrune.definition.type.builders.ParamTypeBuilder
import dev.openrune.definition.type.StringVectorType
import dev.openrune.definition.type.builders.StringVectorTypeBuilder
import dev.openrune.definition.type.VarClanSettingsType
import dev.openrune.definition.type.builders.VarClanSettingsTypeBuilder
import dev.openrune.definition.type.VarClanType
import dev.openrune.definition.type.builders.VarClanTypeBuilder
import dev.openrune.definition.type.VarClientType
import dev.openrune.definition.type.builders.VarClientTypeBuilder
import dev.openrune.definition.type.WorldEntityType
import dev.openrune.definition.type.builders.WorldEntityTypeBuilder
import dev.openrune.definition.type.WorldMapAreaType
import dev.openrune.definition.type.builders.WorldMapAreaTypeBuilder

/**
 * Builder DSLs for the config-style definitions converted to the immutable-type + builder
 * pattern. Definitions are immutable, so tools assemble one here and get the built instance
 * back:
 *
 * ```kotlin
 * val param = paramType(1234) {
 *     defaultInt = 5
 * }
 *
 * val edited = param.edit { defaultInt = 10 }
 * ```
 */
fun varClanType(id: Int = -1, block: VarClanTypeBuilder.() -> Unit = {}): VarClanType =
    VarClanTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun VarClanType.edit(block: VarClanTypeBuilder.() -> Unit): VarClanType =
    toBuilder().apply(block).build()

fun varClanSettingsType(id: Int = -1, block: VarClanSettingsTypeBuilder.() -> Unit = {}): VarClanSettingsType =
    VarClanSettingsTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun VarClanSettingsType.edit(block: VarClanSettingsTypeBuilder.() -> Unit): VarClanSettingsType =
    toBuilder().apply(block).build()

fun varClientType(id: Int = -1, block: VarClientTypeBuilder.() -> Unit = {}): VarClientType =
    VarClientTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun VarClientType.edit(block: VarClientTypeBuilder.() -> Unit): VarClientType =
    toBuilder().apply(block).build()

fun worldEntityType(id: Int = -1, block: WorldEntityTypeBuilder.() -> Unit = {}): WorldEntityType =
    WorldEntityTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun WorldEntityType.edit(block: WorldEntityTypeBuilder.() -> Unit): WorldEntityType =
    toBuilder().apply(block).build()

fun worldMapAreaType(id: Int = -1, block: WorldMapAreaTypeBuilder.() -> Unit = {}): WorldMapAreaType =
    WorldMapAreaTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun WorldMapAreaType.edit(block: WorldMapAreaTypeBuilder.() -> Unit): WorldMapAreaType =
    toBuilder().apply(block).build()

fun stringVectorType(id: Int = -1, block: StringVectorTypeBuilder.() -> Unit = {}): StringVectorType =
    StringVectorTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun StringVectorType.edit(block: StringVectorTypeBuilder.() -> Unit): StringVectorType =
    toBuilder().apply(block).build()

fun bugTemplateType(id: Int = -1, block: BugTemplateTypeBuilder.() -> Unit = {}): BugTemplateType =
    BugTemplateTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun BugTemplateType.edit(block: BugTemplateTypeBuilder.() -> Unit): BugTemplateType =
    toBuilder().apply(block).build()

fun paramType(id: Int = -1, block: ParamTypeBuilder.() -> Unit = {}): ParamType =
    ParamTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun ParamType.edit(block: ParamTypeBuilder.() -> Unit): ParamType =
    toBuilder().apply(block).build()

fun dbTableIndexType(id: Int = -1, block: DBTableIndexTypeBuilder.() -> Unit = {}): DBTableIndexType =
    DBTableIndexTypeBuilder(id).apply(block).build()

/** An edited copy: every field of this definition, with [block]'s changes applied on top. */
fun DBTableIndexType.edit(block: DBTableIndexTypeBuilder.() -> Unit): DBTableIndexType =
    toBuilder().apply(block).build()
