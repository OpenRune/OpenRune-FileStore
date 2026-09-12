package dev.openrune.definition.type

import dev.openrune.definition.type.builders.IdentityKitTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.definition.Definition
import dev.openrune.definition.Recolourable

@RsTableHeaders("idk")
data class IdentityKitType(
    override var id: Int = -1,
    override val originalColours: List<Int>? = null,
    override val modifiedColours: List<Int>? = null,
    override val originalTextureColours: List<Int>? = null,
    override val modifiedTextureColours: List<Int>? = null,
    val bodyPartId : Int = -1,
    val models: List<Int>? = null,
    val chatheadModels : List<Int> = mutableListOf(-1, -1, -1, -1, -1),
    val nonSelectable : Boolean = false
) : Definition, Recolourable {

    fun toBuilder(): IdentityKitTypeBuilder = IdentityKitTypeBuilder.from(this)
}
