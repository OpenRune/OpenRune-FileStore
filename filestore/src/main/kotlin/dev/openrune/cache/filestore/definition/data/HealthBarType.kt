package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.serialization.toIntWithMaxCheck

import kotlinx.serialization.Serializable

@Serializable
data class HealthBarType(
    override var id: Int = -1,
    var int1: UByte = 255u,
    var int2: UByte = 255u,
    var int3: UShort = UShort.MAX_VALUE,
    var int4: UShort = 70u,
    var frontSpriteId: UShort = UShort.MAX_VALUE,
    var backSpriteId: UShort = UShort.MAX_VALUE,
    var width: UByte = 30u,
    var widthPadding: UByte = 0u,
    //Custom
    override var inherit: Int = -1
) : Definition {

    fun getInt1(): Int = int1.toIntWithMaxCheck()
    fun getInt2(): Int = int2.toIntWithMaxCheck()
    fun getWidth(): Int = width.toIntWithMaxCheck()
    fun getWidthPadding(): Int = widthPadding.toIntWithMaxCheck()

    fun getInt3(): Int = int3.toIntWithMaxCheck()
    fun getInt4(): Int = int4.toIntWithMaxCheck()
    fun getFrontSpriteId(): Int = frontSpriteId.toIntWithMaxCheck()
    fun getBackSpriteId(): Int = backSpriteId.toIntWithMaxCheck()

}