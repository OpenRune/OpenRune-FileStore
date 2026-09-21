package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.MutableParameterized

enum class Rs2MapElementShow { NONE, MAP, MINIMAP, BOTH }
enum class Rs2MapElementAlign { START, CENTRE, END }

data class Rs2MapElementCondition(val varbit: Int? = null, val varp: Int? = null, val op: Int = 0, val value: Int = 0)

data class Rs2MapElementMulti(
    val varbit: Int? = null,
    val varp: Int? = null,
    val default: Int? = null,
    val values: List<Int?> = emptyList()
)

data class Rs2MapElementPolygonPoint(val x: Int, val y: Int, val outlineColour: Int = 0)

/**
 * RS3's "MEL" (map element) config type - world map/minimap icons, area
 * labels, and their click behaviour. Distinct from OSRS's [MapElementType]:
 * the two cache generations' MEL layouts have diverged too far to share one
 * representation (RS3 gained conditions, multi-variants, polygon outlines,
 * generic params, etc. that OSRS's format never had).
 */
data class Rs2MapElementType(
    override var id: Int = -1,
    var sprite: Int? = null,
    var mouseOverGraphic: Int? = null,
    var text: String? = null,
    var textColour: Int = 0,
    var textMouseOverColour: Int = 0,
    var textSize: Int = 0,
    var show: Rs2MapElementShow = Rs2MapElementShow.NONE,
    var mapFunction: Boolean = false,
    var condition: Rs2MapElementCondition? = null,
    var condition2: Rs2MapElementCondition? = null,
    var op1: String? = null,
    var op2: String? = null,
    var op3: String? = null,
    var op4: String? = null,
    var op5: String? = null,
    var polygon: List<Rs2MapElementPolygonPoint> = emptyList(),
    var polygonFill: Int = 0,
    var listable: Boolean = true,
    var opBase: String? = null,
    var worldMapArrow: Int? = null,
    var category: Int = -1,
    var textBackgroundOutline: Int = 0,
    var textBackgroundFill: Int = 0,
    var polygonOutlineDashLength: Int = 0,
    var polygonOutlineDashGap: Int = 0,
    var polygonOutlineDashPhase: Int = 0,
    var textOffsetX: Int = 0,
    var textOffsetY: Int = 0,
    var flashSprite: Int? = null,
    var multi: Rs2MapElementMulti? = null,
    var multiDefault: Rs2MapElementMulti? = null,
    var minimapIconScale: Int = 0,
    var horizontalAlign: Rs2MapElementAlign = Rs2MapElementAlign.START,
    var verticalAlign: Rs2MapElementAlign = Rs2MapElementAlign.START,
    override var params: MutableMap<Int, Any>? = null
) : Definition, MutableParameterized
