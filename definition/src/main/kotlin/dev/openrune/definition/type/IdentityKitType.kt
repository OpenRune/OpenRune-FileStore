package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Recolourable

data class IdentityKitType(
    override var id: Int = -1,
    override var originalColours: MutableList<Int>? = null,
    override var modifiedColours: MutableList<Int>? = null,
    override var originalTextureColours: MutableList<Int>? = null,
    override var modifiedTextureColours: MutableList<Int>? = null,
    var bodyPartId : Int = -1,
    var models: MutableList<Int>? = null,
    var chatheadModels : MutableList<Int> = mutableListOf(-1, -1, -1, -1, -1),
    var nonSelectable : Boolean = false
) : Definition, Recolourable