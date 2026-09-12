package dev.openrune.definition.type

import dev.openrune.definition.type.builders.HealthBarTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.definition.Definition

@RsTableHeaders("health")
data class HealthBarType(
    override var id: Int = -1,
    val int1: Int = 255,
    val int2: Int = 255,
    val int3: Int = -1,
    val int4: Int = 70,
    val frontSpriteId: Int = -1,
    val backSpriteId: Int = -1,
    val width: Int = 30,
    val widthPadding: Int = 0,

) : Definition {

    fun toBuilder(): HealthBarTypeBuilder = HealthBarTypeBuilder.from(this)
}
