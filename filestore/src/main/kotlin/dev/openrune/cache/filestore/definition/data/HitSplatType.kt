package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.Transforms
import dev.openrune.cache.filestore.serialization.toIntWithMaxCheck

import kotlinx.serialization.Serializable

@Serializable
data class HitSplatType(
    override var id: Int = -1,
    var font: Int = -1,
    var textColour: Int = 16777215,
    var icon: Int = -1,
    var left: Int = -1,
    var middle: Int = -1,
    var right: Int = -1,
    var offsetX: UShort = 0u,
    var amount: String = "",
    var duration: UShort = 70u,
    var offsetY: Short = 0,
    var fade: Short = -1,
    var comparisonType: UByte = UByte.MAX_VALUE,
    var damageYOfset: Short = 0,
    override var varbit: Int = -1,
    override var varp: Int = -1,
    override var transforms: MutableList<Int>? = null,
    //Custom
    override var inherit: Int = -1
) : Definition, Transforms {

    fun getComparisonType(): Int = comparisonType.toIntWithMaxCheck()
    fun getOffsetX(): Int = offsetX.toIntWithMaxCheck()
    fun getDuration(): Int = duration.toIntWithMaxCheck()

    fun getOffsetY(): Int = offsetY.toIntWithMaxCheck()
    fun getFade(): Int = fade.toIntWithMaxCheck()
    fun getDamageYOfset(): Int = damageYOfset.toIntWithMaxCheck()

}