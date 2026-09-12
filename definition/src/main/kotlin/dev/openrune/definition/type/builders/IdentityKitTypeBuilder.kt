package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

import dev.openrune.definition.MutableRecolourable
import dev.openrune.definition.util.IntListPool

class IdentityKitTypeBuilder(var id: Int = -1) : MutableRecolourable {

    override var originalColours: MutableList<Int>? = null
    override var modifiedColours: MutableList<Int>? = null
    override var originalTextureColours: MutableList<Int>? = null
    override var modifiedTextureColours: MutableList<Int>? = null
    var bodyPartId: Int = -1
    var models: MutableList<Int>? = null
    var chatheadModels: MutableList<Int> = mutableListOf(-1, -1, -1, -1, -1)
    var nonSelectable: Boolean = false

    fun build(): IdentityKitType = IdentityKitType(
        id = id,
        originalColours = IntListPool.of(originalColours),
        modifiedColours = IntListPool.of(modifiedColours),
        originalTextureColours = IntListPool.of(originalTextureColours),
        modifiedTextureColours = IntListPool.of(modifiedTextureColours),
        bodyPartId = bodyPartId,
        models = IntListPool.of(models),
        chatheadModels = IntListPool.of(chatheadModels)!!,
        nonSelectable = nonSelectable,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. List contents are copied too. */
        fun from(type: IdentityKitType): IdentityKitTypeBuilder {
            val builder = IdentityKitTypeBuilder(type.id)
            builder.originalColours = type.originalColours?.toMutableList()
            builder.modifiedColours = type.modifiedColours?.toMutableList()
            builder.originalTextureColours = type.originalTextureColours?.toMutableList()
            builder.modifiedTextureColours = type.modifiedTextureColours?.toMutableList()
            builder.bodyPartId = type.bodyPartId
            builder.models = type.models?.toMutableList()
            builder.chatheadModels = type.chatheadModels.toMutableList()
            builder.nonSelectable = type.nonSelectable
            return builder
        }
    }
}
