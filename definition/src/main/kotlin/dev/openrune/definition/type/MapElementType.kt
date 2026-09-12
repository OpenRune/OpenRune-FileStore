package dev.openrune.definition.type

import dev.openrune.definition.type.builders.MapElementTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.definition.Definition
import dev.openrune.seralizer.MapElementTypeOptionsTableHook

@RsTableHeaders(
    "mapelement",
    rowPostDecode = MapElementTypeOptionsTableHook::class,
)
data class MapElementType(
    override var id: Int = -1,

    val sprite1: Int = -1,

    val sprite2: Int = -1,

    val name: String = "null",

    val fontColor: Int = 0,

    val textSize: Int = 0,

    val renderOnWorldMap: Boolean = true,

    val renderOnMinimap: Boolean = false,

    // The one non-val: [MapElementTypeOptionsTableHook] runs after TOML construction and fills
    // this list in place, so it must stay a mutable holder. By convention it is not modified once
    // a definition has been built.
    var options : MutableList<String?> = mutableListOf(null, null, null, null, null),

    val menuTargetName: String = "null",

    val field1933: List<Int>? = null,

    val horizontalAlignment: Int = 1,

    val verticalAlignment: Int = 1,

    val field1930: List<Int> = emptyList(),

    val field1948: List<Int> = emptyList(),

    val category: Int = 0
) : Definition {

    /** A mutable copy of this definition, for tools that need to edit and re-pack it. */
    fun toBuilder(): MapElementTypeBuilder = MapElementTypeBuilder.from(this)
}
