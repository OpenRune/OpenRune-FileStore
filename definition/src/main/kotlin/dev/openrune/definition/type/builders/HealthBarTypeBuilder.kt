package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

class HealthBarTypeBuilder(var id: Int = -1) {

    var int1: Int = 255
    var int2: Int = 255
    var int3: Int = -1
    var int4: Int = 70
    var frontSpriteId: Int = -1
    var backSpriteId: Int = -1
    var width: Int = 30
    var widthPadding: Int = 0

    fun build(): HealthBarType = HealthBarType(
        id = id,
        int1 = int1,
        int2 = int2,
        int3 = int3,
        int4 = int4,
        frontSpriteId = frontSpriteId,
        backSpriteId = backSpriteId,
        width = width,
        widthPadding = widthPadding,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. */
        fun from(type: HealthBarType): HealthBarTypeBuilder {
            val builder = HealthBarTypeBuilder(type.id)
            builder.int1 = type.int1
            builder.int2 = type.int2
            builder.int3 = type.int3
            builder.int4 = type.int4
            builder.frontSpriteId = type.frontSpriteId
            builder.backSpriteId = type.backSpriteId
            builder.width = type.width
            builder.widthPadding = type.widthPadding
            return builder
        }
    }
}
