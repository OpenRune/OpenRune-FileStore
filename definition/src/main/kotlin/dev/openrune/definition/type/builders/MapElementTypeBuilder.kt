package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

import dev.openrune.definition.util.IntListPool

/**
 * The mutable side of [MapElementType]. Codecs decode into one of these and the packing tools
 * edit one — either fresh or via [MapElementType.toBuilder] — then [build] produces the
 * immutable definition everything else reads.
 */
class MapElementTypeBuilder(var id: Int = -1) {

    var sprite1: Int = -1
    var sprite2: Int = -1
    var name: String = "null"
    var fontColor: Int = 0
    var textSize: Int = 0
    var renderOnWorldMap: Boolean = true
    var renderOnMinimap: Boolean = false
    var options: MutableList<String?> = mutableListOf(null, null, null, null, null)
    var menuTargetName: String = "null"
    var field1933: MutableList<Int>? = null
    var horizontalAlignment: Int = 1
    var verticalAlignment: Int = 1
    var field1930: MutableList<Int> = mutableListOf()
    var field1948: MutableList<Int> = mutableListOf()
    var category: Int = 0

    fun build(): MapElementType = MapElementType(
        id = id,
        sprite1 = sprite1,
        sprite2 = sprite2,
        name = name,
        fontColor = fontColor,
        textSize = textSize,
        renderOnWorldMap = renderOnWorldMap,
        renderOnMinimap = renderOnMinimap,
        options = options,
        menuTargetName = menuTargetName,
        field1933 = IntListPool.of(field1933),
        horizontalAlignment = horizontalAlignment,
        verticalAlignment = verticalAlignment,
        field1930 = IntListPool.of(field1930)!!,
        field1948 = IntListPool.of(field1948)!!,
        category = category,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. List contents are copied too. */
        fun from(type: MapElementType): MapElementTypeBuilder {
            val builder = MapElementTypeBuilder(type.id)
            builder.sprite1 = type.sprite1
            builder.sprite2 = type.sprite2
            builder.name = type.name
            builder.fontColor = type.fontColor
            builder.textSize = type.textSize
            builder.renderOnWorldMap = type.renderOnWorldMap
            builder.renderOnMinimap = type.renderOnMinimap
            builder.options = type.options.toMutableList()
            builder.menuTargetName = type.menuTargetName
            builder.field1933 = type.field1933?.toMutableList()
            builder.horizontalAlignment = type.horizontalAlignment
            builder.verticalAlignment = type.verticalAlignment
            builder.field1930 = type.field1930.toMutableList()
            builder.field1948 = type.field1948.toMutableList()
            builder.category = type.category
            return builder
        }
    }
}
