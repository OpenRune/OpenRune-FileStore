package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

import dev.openrune.definition.MutableRecolourable
import dev.openrune.definition.util.IntListPool

class SpotAnimTypeBuilder(var id: Int = -1) : MutableRecolourable {

    override var originalColours: MutableList<Int>? = null
    override var modifiedColours: MutableList<Int>? = null
    override var originalTextureColours: MutableList<Int>? = null
    override var modifiedTextureColours: MutableList<Int>? = null
    var resizeY: Int = 128
    var resizeX: Int = 128
    var rotation: Int = 0
    var rotate: Boolean = true
    var animationId: Int = -1
    var modelId: Int = 0
    var ambient: Int = 0
    var contrast: Int = 0
    var debugName: String = ""

    fun build(): SpotAnimType = SpotAnimType(
        id = id,
        originalColours = IntListPool.of(originalColours),
        modifiedColours = IntListPool.of(modifiedColours),
        originalTextureColours = IntListPool.of(originalTextureColours),
        modifiedTextureColours = IntListPool.of(modifiedTextureColours),
        resizeY = resizeY,
        resizeX = resizeX,
        rotation = rotation,
        rotate = rotate,
        animationId = animationId,
        modelId = modelId,
        ambient = ambient,
        contrast = contrast,
        debugName = debugName,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. List contents are copied too. */
        fun from(type: SpotAnimType): SpotAnimTypeBuilder {
            val builder = SpotAnimTypeBuilder(type.id)
            builder.originalColours = type.originalColours?.toMutableList()
            builder.modifiedColours = type.modifiedColours?.toMutableList()
            builder.originalTextureColours = type.originalTextureColours?.toMutableList()
            builder.modifiedTextureColours = type.modifiedTextureColours?.toMutableList()
            builder.resizeY = type.resizeY
            builder.resizeX = type.resizeX
            builder.rotation = type.rotation
            builder.rotate = type.rotate
            builder.animationId = type.animationId
            builder.modelId = type.modelId
            builder.ambient = type.ambient
            builder.contrast = type.contrast
            builder.debugName = type.debugName
            return builder
        }
    }
}
