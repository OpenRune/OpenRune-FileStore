package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.serialization.toIntWithMaxCheck

import kotlinx.serialization.Serializable

@Serializable
data class TextureType(
    override var id : Int = -1,
    var isTransparent : Boolean = false,
    var fileIds : MutableList<Int> = emptyList<Int>().toMutableList(),
    var combineModes : MutableList<Int> = emptyList<Int>().toMutableList(),
    var field2440 : MutableList<Int> = emptyList<Int>().toMutableList(),
    var colourAdjustments : MutableList<Int> = emptyList<Int>().toMutableList(),
    var averageRgb : UShort = 0u,
    var animationDirection : UByte = 0u,
    var animationSpeed : UByte = 0u,
    override var inherit : Int = -1,
 ) : Definition {

     fun getAnimatedDirection() = animationDirection.toIntWithMaxCheck()

     fun getAnimatedSpeed() = animationSpeed.toIntWithMaxCheck()

    fun getAverageRgb() = averageRgb.toIntWithMaxCheck()

 }