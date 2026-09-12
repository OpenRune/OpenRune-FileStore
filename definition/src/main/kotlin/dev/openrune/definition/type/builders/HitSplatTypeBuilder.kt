package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

import dev.openrune.definition.MutableTransforms
import dev.openrune.definition.util.IntListPool

/**
 * The mutable side of [HitSplatType]. Codecs decode into one of these and the packing tools edit
 * one — either fresh or via [HitSplatType.toBuilder] — then [build] produces the immutable
 * definition everything else reads.
 */
class HitSplatTypeBuilder(var id: Int = -1) : MutableTransforms {

    var font: Int = -1
    var textColour: Int = 16777215
    var icon: Int = -1
    var left: Int = -1
    var middle: Int = -1
    var right: Int = -1
    var offsetX: Int = 0
    var amount: String = ""
    var duration: Int = 70
    var offsetY: Int = 0
    var fade: Int = -1
    var comparisonType: Int = -1
    var damageYOfset: Int = 0

    override var multiVarBit: Int = -1
    override var multiVarp: Int = -1
    override var multiDefault: Int = -1
    override var transforms: MutableList<Int>? = null

    fun build(): HitSplatType = HitSplatType(
        id = id,
        font = font,
        textColour = textColour,
        icon = icon,
        left = left,
        middle = middle,
        right = right,
        offsetX = offsetX,
        amount = amount,
        duration = duration,
        offsetY = offsetY,
        fade = fade,
        comparisonType = comparisonType,
        damageYOfset = damageYOfset,
        multiVarBit = multiVarBit,
        multiVarp = multiVarp,
        multiDefault = multiDefault,
        transforms = IntListPool.of(transforms),
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. List contents are copied too. */
        fun from(type: HitSplatType): HitSplatTypeBuilder {
            val builder = HitSplatTypeBuilder(type.id)
            builder.font = type.font
            builder.textColour = type.textColour
            builder.icon = type.icon
            builder.left = type.left
            builder.middle = type.middle
            builder.right = type.right
            builder.offsetX = type.offsetX
            builder.amount = type.amount
            builder.duration = type.duration
            builder.offsetY = type.offsetY
            builder.fade = type.fade
            builder.comparisonType = type.comparisonType
            builder.damageYOfset = type.damageYOfset
            builder.multiVarBit = type.multiVarBit
            builder.multiVarp = type.multiVarp
            builder.multiDefault = type.multiDefault
            builder.transforms = type.transforms?.toMutableList()
            return builder
        }
    }
}
