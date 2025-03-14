package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Transforms
import dev.openrune.definition.serialization.Rscm
import kotlinx.serialization.Serializable

@Serializable
data class HitSplatType(
    override var id: Rscm = -1,
    var font: Int = -1,
    var textColour: Int = 16777215,
    var icon: Int = -1,
    var left: Int = -1,
    var middle: Int = -1,
    var right: Int = -1,
    var offsetX: Int = 0,
    var amount: String = "",
    var duration: Int = 70,
    var offsetY: Int = 0,
    var fade: Int = -1,
    var comparisonType: Int = -1,
    var damageYOfset: Int = 0,
    override var varbit: Int = -1,
    override var varp: Int = -1,
    override var transforms: MutableList<Int>? = null,

) : Definition, Transforms