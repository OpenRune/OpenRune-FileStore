package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

class TextureTypeBuilder(var id: Int = -1) {

    var isTransparent: Boolean = false
    var fileId: Int = -1
    var averageRgb: Int = 0
    var animationDirection: Int = 0
    var animationSpeed: Int = 0
    var isLowDetail: Boolean = false

    fun build(): TextureType = TextureType(
        id = id,
        isTransparent = isTransparent,
        fileId = fileId,
        averageRgb = averageRgb,
        animationDirection = animationDirection,
        animationSpeed = animationSpeed,
        isLowDetail = isLowDetail,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. */
        fun from(type: TextureType): TextureTypeBuilder {
            val builder = TextureTypeBuilder(type.id)
            builder.isTransparent = type.isTransparent
            builder.fileId = type.fileId
            builder.averageRgb = type.averageRgb
            builder.animationDirection = type.animationDirection
            builder.animationSpeed = type.animationSpeed
            builder.isLowDetail = type.isLowDetail
            return builder
        }
    }
}
