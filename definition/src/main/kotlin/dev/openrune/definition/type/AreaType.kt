package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class AreaType(
    override var id: Int = -1,
    override var inherit: Int = 1,
    override var debugName : String = "",

    var sprite1: Int = -1,

    var sprite2: Int = -1,

    var name: String = "null",

    var fontColor: Int = 0,

    var textSize: Int = 0,

    var renderOnWorldMap: Boolean = true,

    var renderOnMinimap: Boolean = false,

    var options : MutableList<String?> = mutableListOf(null, null, null, null, null),

    var menuTargetName: String = "null",

    var field1933: MutableList<Int>? = null,

    var horizontalAlignment: Int = 1,

    var verticalAlignment: Int = 1,

    var field1930: MutableList<Int> = emptyList<Int>().toMutableList(),

    var field1948: MutableList<Int> = emptyList<Int>().toMutableList(),

    var category: Int = 0
) : Definition