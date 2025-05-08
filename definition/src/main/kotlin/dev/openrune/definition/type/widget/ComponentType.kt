package dev.openrune.definition.type.widget

import dev.openrune.definition.Definition
import java.awt.Color

open class ComponentType : Definition {

    var name: String? = null
    var debugInterfaceName = ""
    override var id: Int = -1
    override var inherit: Int = 1
    override var debugName : String = ""
    var child: Int = -1
    var isIf3: Boolean = false
    var type: Int = 0
    var contentType: Int = 0
    var xMode: Int = 0
    var yMode: Int = 0
    var widthMode: Int = 0
    var heightMode: Int = 0
    var x: Int = 0
    var y: Int = 0
    open var width: Int = 0
    open var height: Int = 0
    var parentId: Int = -1
    var hidden: Boolean = false
    var scrollWidth: Int = 0
    var scrollHeight: Int = 0
    var color: Int = 0
    var filled: Boolean = false
    var opacity: Int = 0
    var lineWidth: Int = 1
    var spriteId: Int = -1
    var textureId: Int = 0
    var spriteTiling: Boolean = false
    var borderType: Int = 0
    var shadowColor: Int = 0
    var flippedVertically: Boolean = false
    var flippedHorizontally: Boolean = false
    var modelType: Int = 1
    var modelId: Int = -1
    var offsetX2d: Int = 0
    var offsetY2d: Int = 0
    var rotationX: Int = 0
    var rotationZ: Int = 0
    var rotationY: Int = 0
    var modelZoom: Int = 100
    var font: Int = -1
    var text: String? = ""
    var alternateText: String? = ""
    var textShadowed: Boolean = false
    var xPitch: Int = 0
    var yPitch: Int = 0
    var xOffsets: IntArray? = null
    var configActions: Array<String>? = null
    var accessMask: Int = 0
    var opBase: String? = ""
    var actions: Array<String?>? = null
    var dragDeadZone: Int = 0
    var dragDeadTime: Int = 0
    var dragRenderBehavior: Boolean = false
    var targetVerb: String? = ""
    var onLoadListener: Array<Any>? = null
    var onClickListener: Array<Any>? = null
    var onClickRepeatListener: Array<Any>? = null
    var onReleaseListener: Array<Any>? = null
    var onHoldListener: Array<Any>? = null
    var onMouseOverListener: Array<Any>? = null
    var onMouseRepeatListener: Array<Any>? = null
    var onMouseLeaveListener: Array<Any>? = null
    var onDragListener: Array<Any>? = null
    var onDragCompleteListener: Array<Any>? = null
    var onTargetEnterListener: Array<Any>? = null
    var onTargetLeaveListener: Array<Any>? = null
    var onVarTransmitListener: Array<Any>? = null
    var varTransmitTriggers: IntArray? = null
    var onInvTransmitListener: Array<Any>? = null
    var invTransmitTriggers: IntArray? = null
    var onStatTransmitListener: Array<Any>? = null
    var statTransmitTriggers: IntArray? = null
    var onTimerListener: Array<Any>? = null
    var onOpListener: Array<Any>? = null
    var onScrollWheelListener: Array<Any>? = null
    var dynamicValues: Array<IntArray>? = null
    var valueCompareType: IntArray? = null
    var spellName: String? = ""
    var tooltip: String = "Ok"
    var itemIds: IntArray? = null
    var itemQuantities: IntArray? = null
    var children: MutableList<ComponentType> = mutableListOf()
    var noClickThrough: Boolean = false
    var menuType: Int = 0
    var alternateTextColor: Int = 0
    var hoveredTextColor: Int = 0
    var alternateHoveredTextColor: Int = 0
    var lineDirection: Boolean = false
    var alternateSpriteId: Int = -1
    var field2840: Int = 1
    var alternateModelId: Int = -1
    var animation: Int = -1
    var alternateAnimation: Int = -1
    var modelHeightOverride: Int = 0
    var orthogonal: Boolean = false
    var lineHeight: Int = 0
    var xAllignment: Int = 0
    var yAllignment: Int = 0
    var yOffsets: IntArray? = null
    var sprites: IntArray? = null
    var requiredValues: IntArray? = null
    var hoveredSiblingId: Int = -1
    var hooks: MutableMap<String, Array<Any>>? = null
    var gameVals : List<Pair<String, Int>> = emptyList()
    val layer: String = ""

    fun setColor(hex: String): Int {
        color = if (hex.isEmpty()) 0 else hex.removePrefix("#").toInt(16)
        return color
    }

    fun setShadowColor(hex: String): Int {
        shadowColor = if (hex.isEmpty()) 0 else hex.removePrefix("#").toInt(16)
        return shadowColor
    }

    fun setOption(index: Int, option: String?) {
        if (actions == null || index >= actions!!.size) {
            val newSize = index + 1
            actions = Array(newSize) { actions?.getOrNull(it) }
        }
        actions!![index] = option?.takeIf { it.isNotEmpty() && it != "null" }
    }

    fun setClickMask(mask: AccessMask) {
        accessMask = mask.value
    }

    fun add(component: ComponentType) {
        add("",component)
    }

    fun add(debugName: String = "", component: ComponentType, inherit : Boolean = false) {
        if (debugName.isBlank()) {
            val indexAdded = children.size
            component.name = "COM_${indexAdded}"
            children.add(component)
        } else {
            component.name = debugName
            val componentID = getComponentIdFromName(debugName)
            if (componentID != null && !inherit) {
                children[componentID] = component
            } else {
                children.add(component)
            }
        }
    }

    private fun getComponentIdFromName(name : String): Int? {
        if (name.contains(".")) {
            //TODO RSCM
        } else {
            return gameVals.find { it.first == name }?.second
        }
        return -1
    }

    fun clone(): ComponentType {
        val copy = ComponentType()
        copy.name = this.name
        copy.debugInterfaceName = this.debugInterfaceName
        copy.id = this.id
        copy.child = this.child
        copy.isIf3 = this.isIf3
        copy.type = this.type
        copy.contentType = this.contentType
        copy.xMode = this.xMode
        copy.yMode = this.yMode
        copy.widthMode = this.widthMode
        copy.heightMode = this.heightMode
        copy.x = this.x
        copy.y = this.y
        copy.width = this.width
        copy.height = this.height
        copy.parentId = this.parentId
        copy.hidden = this.hidden
        copy.scrollWidth = this.scrollWidth
        copy.scrollHeight = this.scrollHeight
        copy.color = this.color
        copy.filled = this.filled
        copy.opacity = this.opacity
        copy.lineWidth = this.lineWidth
        copy.spriteId = this.spriteId
        copy.textureId = this.textureId
        copy.spriteTiling = this.spriteTiling
        copy.borderType = this.borderType
        copy.shadowColor = this.shadowColor
        copy.flippedVertically = this.flippedVertically
        copy.flippedHorizontally = this.flippedHorizontally
        copy.modelType = this.modelType
        copy.modelId = this.modelId
        copy.offsetX2d = this.offsetX2d
        copy.offsetY2d = this.offsetY2d
        copy.rotationX = this.rotationX
        copy.rotationZ = this.rotationZ
        copy.rotationY = this.rotationY
        copy.modelZoom = this.modelZoom
        copy.font = this.font
        copy.text = this.text
        copy.alternateText = this.alternateText
        copy.textShadowed = this.textShadowed
        copy.xPitch = this.xPitch
        copy.yPitch = this.yPitch
        copy.xOffsets = this.xOffsets?.clone()
        copy.configActions = this.configActions?.clone()
        copy.accessMask = this.accessMask
        copy.opBase = this.opBase
        copy.actions = this.actions?.clone()
        copy.dragDeadZone = this.dragDeadZone
        copy.dragDeadTime = this.dragDeadTime
        copy.dragRenderBehavior = this.dragRenderBehavior
        copy.targetVerb = this.targetVerb
        copy.onLoadListener = this.onLoadListener?.clone()
        copy.onClickListener = this.onClickListener?.clone()
        copy.onClickRepeatListener = this.onClickRepeatListener?.clone()
        copy.onReleaseListener = this.onReleaseListener?.clone()
        copy.onHoldListener = this.onHoldListener?.clone()
        copy.onMouseOverListener = this.onMouseOverListener?.clone()
        copy.onMouseRepeatListener = this.onMouseRepeatListener?.clone()
        copy.onMouseLeaveListener = this.onMouseLeaveListener?.clone()
        copy.onDragListener = this.onDragListener?.clone()
        copy.onDragCompleteListener = this.onDragCompleteListener?.clone()
        copy.onTargetEnterListener = this.onTargetEnterListener?.clone()
        copy.onTargetLeaveListener = this.onTargetLeaveListener?.clone()
        copy.onVarTransmitListener = this.onVarTransmitListener?.clone()
        copy.varTransmitTriggers = this.varTransmitTriggers?.clone()
        copy.onInvTransmitListener = this.onInvTransmitListener?.clone()
        copy.invTransmitTriggers = this.invTransmitTriggers?.clone()
        copy.onStatTransmitListener = this.onStatTransmitListener?.clone()
        copy.statTransmitTriggers = this.statTransmitTriggers?.clone()
        copy.onTimerListener = this.onTimerListener?.clone()
        copy.onOpListener = this.onOpListener?.clone()
        copy.onScrollWheelListener = this.onScrollWheelListener?.clone()
        copy.dynamicValues = this.dynamicValues?.map { it.clone() }?.toTypedArray()
        copy.valueCompareType = this.valueCompareType?.clone()
        copy.spellName = this.spellName
        copy.tooltip = this.tooltip
        copy.itemIds = this.itemIds?.clone()
        copy.itemQuantities = this.itemQuantities?.clone()
        copy.children = this.children.map { it.clone() }.toMutableList()
        copy.noClickThrough = this.noClickThrough
        copy.menuType = this.menuType
        copy.alternateTextColor = this.alternateTextColor
        copy.hoveredTextColor = this.hoveredTextColor
        copy.alternateHoveredTextColor = this.alternateHoveredTextColor
        copy.lineDirection = this.lineDirection
        copy.alternateSpriteId = this.alternateSpriteId
        copy.field2840 = this.field2840
        copy.alternateModelId = this.alternateModelId
        copy.animation = this.animation
        copy.alternateAnimation = this.alternateAnimation
        copy.modelHeightOverride = this.modelHeightOverride
        copy.orthogonal = this.orthogonal
        copy.lineHeight = this.lineHeight
        copy.xAllignment = this.xAllignment
        copy.yAllignment = this.yAllignment
        copy.yOffsets = this.yOffsets?.clone()
        copy.sprites = this.sprites?.clone()
        copy.requiredValues = this.requiredValues?.clone()
        copy.hoveredSiblingId = this.hoveredSiblingId
        copy.hooks = this.hooks?.mapValues { it.value.clone() }?.toMutableMap()
        copy.gameVals = this.gameVals.toList()
        return copy
    }

    fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun setDynamicSize(width: Int, height: Int) {
        widthMode = width
        heightMode = height
    }

    fun setPosition(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    fun setDynamicPosition(x: Int, y: Int) {
        xMode = x
        yMode = y
    }

    override fun toString(): String {
        return "ComponentDefinitions(" +
                "name=$name, " +
                "interfaceId=$id, " +
                "isIf3=$isIf3, " +
                "type=$type, " +
                "contentType=$contentType, " +
                "xMode=$xMode, " +
                "yMode=$yMode, " +
                "widthMode=$widthMode, " +
                "heightMode=$heightMode, " +
                "x=$x, " +
                "y=$y, " +
                "width=$width, " +
                "height=$height, " +
                "parentId=$parentId, " +
                "hidden=$hidden, " +
                "scrollWidth=$scrollWidth, " +
                "scrollHeight=$scrollHeight, " +
                "color=$color, " +
                "filled=$filled, " +
                "opacity=$opacity, " +
                "lineWidth=$lineWidth, " +
                "spriteId=$spriteId, " +
                "textureId=$textureId, " +
                "spriteTiling=$spriteTiling, " +
                "borderType=$borderType, " +
                "shadowColor=$shadowColor, " +
                "flippedVertically=$flippedVertically, " +
                "flippedHorizontally=$flippedHorizontally, " +
                "modelType=$modelType, " +
                "modelId=$modelId, " +
                "offsetX2d=$offsetX2d, " +
                "offsetY2d=$offsetY2d, " +
                "rotationX=$rotationX, " +
                "rotationZ=$rotationZ, " +
                "rotationY=$rotationY, " +
                "modelZoom=$modelZoom, " +
                "font=$font, " +
                "text=$text, " +
                "alternateText=$alternateText, " +
                "textShadowed=$textShadowed, " +
                "xPitch=$xPitch, " +
                "yPitch=$yPitch, " +
                "xOffsets=${xOffsets?.joinToString()}, " +
                "configActions=${configActions?.joinToString()}, " +
                "accessMask=$accessMask, " +
                "opBase=$opBase, " +
                "actions=${actions.contentDeepToString()}, " +
                "dragDeadZone=$dragDeadZone, " +
                "dragDeadTime=$dragDeadTime, " +
                "dragRenderBehavior=$dragRenderBehavior, " +
                "targetVerb=$targetVerb, " +
                "onLoadListener=${onLoadListener}, " +
                "onClickListener=${onClickListener.contentDeepToString()}, " +
                "onClickRepeatListener=${onClickRepeatListener.contentDeepToString()}, " +
                "onReleaseListener=${onReleaseListener.contentDeepToString()}, " +
                "onHoldListener=${onHoldListener.contentDeepToString()}, " +
                "onMouseOverListener=${onMouseOverListener.contentDeepToString()}, " +
                "onMouseRepeatListener=${onMouseRepeatListener.contentDeepToString()}, " +
                "onMouseLeaveListener=${onMouseLeaveListener.contentDeepToString()}, " +
                "onDragListener=${onDragListener.contentDeepToString()}, " +
                "onDragCompleteListener=${onDragCompleteListener.contentDeepToString()}, " +
                "onTargetEnterListener=${onTargetEnterListener.contentDeepToString()}, " +
                "onTargetLeaveListener=${onTargetLeaveListener.contentDeepToString()}, " +
                "onVarTransmitListener=${onVarTransmitListener.contentDeepToString()}, " +
                "varTransmitTriggers=${varTransmitTriggers?.joinToString(", ")}, " +
                "onInvTransmitListener=${onInvTransmitListener.contentDeepToString()}, " +
                "invTransmitTriggers=${invTransmitTriggers?.joinToString(", ")}, " +
                "onStatTransmitListener=${onStatTransmitListener.contentDeepToString()}, " +
                "statTransmitTriggers=${statTransmitTriggers?.joinToString(", ")}, " +
                "onTimerListener=${onTimerListener.contentDeepToString()}, " +
                "onOpListener=${onOpListener.contentDeepToString()}, " +
                "onScrollWheelListener=${onScrollWheelListener.contentDeepToString()}, " +
                "dynamicValues=${dynamicValues.contentDeepToString()}, " +
                "valueCompareType=${valueCompareType?.joinToString(", ")}, " +
                "spellName=$spellName, " +
                "tooltip=$tooltip, " +
                "itemIds=${itemIds?.joinToString()}, " +
                "itemQuantities=${itemQuantities?.joinToString()}, " +
                "children=$children, " +
                "noClickThrough=$noClickThrough, " +
                "menuType=$menuType, " +
                "alternateTextColor=$alternateTextColor, " +
                "hoveredTextColor=$hoveredTextColor, " +
                "alternateHoveredTextColor=$alternateHoveredTextColor, " +
                "lineDirection=$lineDirection, " +
                "alternateSpriteId=$alternateSpriteId, " +
                "field2840=$field2840, " +
                "alternateModelId=$alternateModelId, " +
                "animation=$animation, " +
                "alternateAnimation=$alternateAnimation, " +
                "modelHeightOverride=$modelHeightOverride, " +
                "orthogonal=$orthogonal, " +
                "lineHeight=$lineHeight, " +
                "xAllignment=$xAllignment, " +
                "yAllignment=$yAllignment, " +
                "yOffsets=${yOffsets?.joinToString()}, " +
                "sprites=${sprites?.joinToString()}, " +
                "requiredValues=${requiredValues?.joinToString()}, " +
                "hoveredSiblingId=$hoveredSiblingId, " +
                "hooks=$hooks)"
    }

}

fun Color.toJagexColor(): Int {
    val hex = "#${String.format("%02X%02X%02X", red, green, blue)}"
    val color = if (hex.isEmpty()) 0 else hex.removePrefix("#").toInt(16)
    return color
}