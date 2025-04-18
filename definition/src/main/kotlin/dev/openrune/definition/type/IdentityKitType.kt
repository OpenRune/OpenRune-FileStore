package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Recolourable
import dev.openrune.definition.serialization.ListRscm
import dev.openrune.definition.serialization.Rscm
import kotlinx.serialization.Serializable

@Serializable
data class IdentityKitType(
    override var id: Rscm = -1,
    override var originalColours: MutableList<Int>? = null,
    override var modifiedColours: MutableList<Int>? = null,
    override var originalTextureColours: MutableList<Int>? = null,
    override var modifiedTextureColours: MutableList<Int>? = null,
    var bodyPartId : Int = -1,
    var models: ListRscm? = null,
    var chatheadModels : ListRscm = mutableListOf(-1, -1, -1, -1, -1),
    var nonSelectable : Boolean = false
) : Definition, Recolourable