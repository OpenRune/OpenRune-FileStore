package dev.openrune.cache.tools.iftype.dsl

import dev.openrune.cache.tools.iftype.dsl.impl.Model
import dev.openrune.definition.type.widget.ComponentTypeBuilder

/**
 * Base class for all component DSL classes that provides common properties and methods
 */
open class BaseComponent {
    var x: Int = 0
    var y: Int = 0
    var width: Int = 0
    var height: Int = 0
    var type: Int = 0
    var contentType: Int = 0
    var xMode: Int = 0
    var yMode: Int = 0
    var widthMode: Int = 0
    var heightMode: Int = 0
    var layer: Int? = null
    var hide: Boolean = false
    var opBase: String? = ""
    var targetVerb: String? = ""
    var dragDeadZone: Int = 0
    var dragDeadTime: Int = 0
    var draggableBehavior: Boolean = false
    var onLoad: Array<Any>? = null
    var onClick: Array<Any>? = null
    var onClickRepeat: Array<Any>? = null
    var onRelease: Array<Any>? = null
    var onHold: Array<Any>? = null
    var onMouseOver: Array<Any>? = null
    var onMouseRepeat: Array<Any>? = null
    var onMouseLeave: Array<Any>? = null
    var onDrag: Array<Any>? = null
    var onDragComplete: Array<Any>? = null
    var onTargetEnter: Array<Any>? = null
    var onTargetLeave: Array<Any>? = null
    var onVarTransmit: Array<Any>? = null
    var onInvTransmit: Array<Any>? = null
    var onStatTransmit: Array<Any>? = null
    var onTimer: Array<Any>? = null
    var onOp: Array<Any>? = null
    var onScrollWheel: Array<Any>? = null
    var onVarTransmitList: IntArray? = null
    var onInvTransmitList: IntArray? = null
    var onStatTransmitList: IntArray? = null
    var repeatType: RepeatType? = null
    var events: Int? = null

    fun size(block: () -> Pair<Int, Int>) {
        val (newX, newY) = block()
        width = newX
        height = newY
    }

    fun size(pair: Pair<Int, Int>) {
        width = pair.first
        height = pair.second
    }

    fun width(block: () -> Int) { width = block() }
    fun height(block: () -> Int) { height = block() }
    fun type(block: () -> Int) { type = block() }
    fun contentType(block: () -> Int) { contentType = block() }
    fun xMode(block: () -> Int) { xMode = block() }
    fun yMode(block: () -> Int) { yMode = block() }
    fun widthMode(block: () -> Int) { widthMode = block() }
    fun heightMode(block: () -> Int) { heightMode = block() }
    fun layer(block: () -> Int) { layer = block() }
    fun hide(block: () -> Boolean) { hide = block() }
    fun opBase(block: () -> String?) { opBase = block() }
    fun targetVerb(block: () -> String?) { targetVerb = block() }
    fun dragDeadZone(block: () -> Int) { dragDeadZone = block() }
    fun dragDeadTime(block: () -> Int) { dragDeadTime = block() }
    fun draggableBehavior(block: () -> Boolean) { draggableBehavior = block() }

    fun onLoad(block: () -> Array<Any>?) { onLoad = block() }
    fun onClick(block: () -> Array<Any>?) { onClick = block() }
    fun onClickRepeat(block: () -> Array<Any>?) { onClickRepeat = block() }
    fun onRelease(block: () -> Array<Any>?) { onRelease = block() }
    fun onHold(block: () -> Array<Any>?) { onHold = block() }
    fun onMouseOver(block: () -> Array<Any>?) { onMouseOver = block() }
    fun onMouseRepeat(block: () -> Array<Any>?) { onMouseRepeat = block() }
    fun onMouseLeave(block: () -> Array<Any>?) { onMouseLeave = block() }
    fun onDrag(block: () -> Array<Any>?) { onDrag = block() }
    fun onDragComplete(block: () -> Array<Any>?) { onDragComplete = block() }
    fun onTargetEnter(block: () -> Array<Any>?) { onTargetEnter = block() }
    fun onTargetLeave(block: () -> Array<Any>?) { onTargetLeave = block() }
    fun onVarTransmit(block: () -> Array<Any>?) { onVarTransmit = block() }
    fun onInvTransmit(block: () -> Array<Any>?) { onInvTransmit = block() }
    fun onStatTransmit(block: () -> Array<Any>?) { onStatTransmit = block() }
    fun onTimer(block: () -> Array<Any>?) { onTimer = block() }
    fun onOp(block: () -> Array<Any>?) { onOp = block() }
    fun onScrollWheel(block: () -> Array<Any>?) { onScrollWheel = block() }

    fun onLoadListener(block: () -> Array<Any>?) { onLoad = block() }
    fun onClickListener(block: () -> Array<Any>?) { onClick = block() }
    fun onClickRepeatListener(block: () -> Array<Any>?) { onClickRepeat = block() }
    fun onReleaseListener(block: () -> Array<Any>?) { onRelease = block() }
    fun onHoldListener(block: () -> Array<Any>?) { onHold = block() }
    fun onMouseOverListener(block: () -> Array<Any>?) { onMouseOver = block() }
    fun onMouseRepeatListener(block: () -> Array<Any>?) { onMouseRepeat = block() }
    fun onMouseLeaveListener(block: () -> Array<Any>?) { onMouseLeave = block() }
    fun onDragListener(block: () -> Array<Any>?) { onDrag = block() }
    fun onDragCompleteListener(block: () -> Array<Any>?) { onDragComplete = block() }
    fun onTargetEnterListener(block: () -> Array<Any>?) { onTargetEnter = block() }
    fun onTargetLeaveListener(block: () -> Array<Any>?) { onTargetLeave = block() }
    fun onVarTransmitListener(block: () -> Array<Any>?) { onVarTransmit = block() }
    fun onInvTransmitListener(block: () -> Array<Any>?) { onInvTransmit = block() }
    fun onStatTransmitListener(block: () -> Array<Any>?) { onStatTransmit = block() }
    fun onTimerListener(block: () -> Array<Any>?) { onTimer = block() }
    fun onOpListener(block: () -> Array<Any>?) { onOp = block() }
    fun onScrollWheelListener(block: () -> Array<Any>?) { onScrollWheel = block() }

    fun onVarTransmitList(block: () -> IntArray?) { onVarTransmitList = block() }
    fun onInvTransmitList(block: () -> IntArray?) { onInvTransmitList = block() }
    fun onStatTransmitList(block: () -> IntArray?) { onStatTransmitList = block() }

    fun position(block: () -> Pair<Int, Int>) {
        val (newX, newY) = block()
        x = newX
        y = newY
    }

    fun position(pair: Pair<Int, Int>) {
        x = pair.first
        y = pair.second
    }

    fun repeatType(block: () -> RepeatType) {
        repeatType = block()
    }

    /**
     * Applies common properties from this component to a ComponentTypeBuilder
     */
    protected fun applyCommonProperties(builder: ComponentTypeBuilder) {
        builder.x = x
        builder.y = y
        builder.width = width
        builder.height = height
        builder.type = type
        builder.contentType = contentType
        builder.xMode = xMode
        builder.yMode = yMode
        builder.widthMode = widthMode
        builder.heightMode = heightMode
        builder.layer = layer
        builder.hide = hide
        builder.opBase = opBase
        builder.targetVerb = targetVerb
        builder.dragDeadZone = dragDeadZone
        builder.dragDeadTime = dragDeadTime
        builder.draggableBehavior = draggableBehavior
        builder.onLoad = onLoad
        builder.onClick = onClick
        builder.onClickRepeat = onClickRepeat
        builder.onRelease = onRelease
        builder.onHold = onHold
        builder.onMouseOver = onMouseOver
        builder.onMouseRepeat = onMouseRepeat
        builder.onMouseLeave = onMouseLeave
        builder.onDrag = onDrag
        builder.onDragComplete = onDragComplete
        builder.onTargetEnter = onTargetEnter
        builder.onTargetLeave = onTargetLeave
        builder.onVarTransmit = onVarTransmit
        builder.onInvTransmit = onInvTransmit
        builder.onStatTransmit = onStatTransmit
        builder.onTimer = onTimer
        builder.onOp = onOp
        builder.onScrollWheel = onScrollWheel
        builder.onVarTransmitList = onVarTransmitList
        builder.onInvTransmitList = onInvTransmitList
        builder.onStatTransmitList = onStatTransmitList
        builder.events = events
    }
}

