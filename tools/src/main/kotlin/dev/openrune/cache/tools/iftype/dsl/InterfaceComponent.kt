package dev.openrune.cache.tools.iftype.dsl

import dev.openrune.definition.type.widget.ComponentType

open class InterfaceComponent {
    var isIf3 : Boolean = true
    var debugInterfaceName : String = ""
    var name : String = ""
    open var id : Int = -1
    var x : Int = 0
    var y : Int = 0
    var type: Int = 0
    var contentType: Int = 0
    open var width: Int = 0
    open var height: Int = 0
    var xMode: Int = 0
    var yMode: Int = 0
    var widthMode: Int = 0
    var heightMode: Int = 0
    var parentId: Int = -1
    var hidden: Boolean = false
    var accessMask: Int = 0
    var actions: Array<String?>? = null
    var opBase: String? = ""
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
    var repeatType: RepeatType? = null

    open fun applyTo(target: ComponentType) {
        target.isIf3 = isIf3
        target.debugInterfaceName = debugInterfaceName
        target.name = name
        target.id = id
        target.x = x
        target.y = y
        target.type = type
        target.contentType = contentType
        target.width = width
        target.height = height
        target.xMode = xMode
        target.yMode = yMode
        target.widthMode = widthMode
        target.heightMode = heightMode

        target.hidden = hidden
        target.accessMask = accessMask
        target.actions = actions
        target.opBase = opBase
        target.dragDeadZone = dragDeadZone
        target.dragDeadTime = dragDeadTime
        target.dragRenderBehavior = dragRenderBehavior
        target.targetVerb = targetVerb

        target.onLoadListener = onLoadListener
        target.onClickListener = onClickListener
        target.onClickRepeatListener = onClickRepeatListener
        target.onReleaseListener = onReleaseListener
        target.onHoldListener = onHoldListener
        target.onMouseOverListener = onMouseOverListener
        target.onMouseRepeatListener = onMouseRepeatListener
        target.onMouseLeaveListener = onMouseLeaveListener
        target.onDragListener = onDragListener
        target.onDragCompleteListener = onDragCompleteListener
        target.onTargetEnterListener = onTargetEnterListener
        target.onTargetLeaveListener = onTargetLeaveListener
        target.onVarTransmitListener = onVarTransmitListener
        target.varTransmitTriggers = varTransmitTriggers
        target.onInvTransmitListener = onInvTransmitListener
        target.invTransmitTriggers = invTransmitTriggers
        target.onStatTransmitListener = onStatTransmitListener
        target.statTransmitTriggers = statTransmitTriggers
        target.onTimerListener = onTimerListener
        target.onOpListener = onOpListener
        target.onScrollWheelListener = onScrollWheelListener
        target.dynamicValues = dynamicValues
        target.valueCompareType = valueCompareType
    }

    fun size(block: () -> Pair<Int, Int>) {
        val (newX, newY) = block()
        width = newX
        height = newY
    }
    fun width(block: () -> Int) { width = block() }
    fun height(block: () -> Int) { height = block() }
    fun type(block: () -> Int) { type = block() }
    fun contentType(block: () -> Int) { contentType = block() }
    fun xMode(block: () -> Int) { xMode = block() }
    fun yMode(block: () -> Int) { yMode = block() }
    fun widthMode(block: () -> Int) { widthMode = block() }
    fun heightMode(block: () -> Int) { heightMode = block() }
    fun parentId(block: () -> Int) { parentId = block() }
    fun hidden(block: () -> Boolean) { hidden = block() }
    fun accessMask(block: () -> Int) { accessMask = block() }
    fun opBase(block: () -> String?) { opBase = block() }
    fun targetVerb(block: () -> String?) { targetVerb = block() }
    fun dragDeadZone(block: () -> Int) { dragDeadZone = block() }
    fun dragDeadTime(block: () -> Int) { dragDeadTime = block() }
    fun dragRenderBehavior(block: () -> Boolean) { dragRenderBehavior = block() }

    fun actions(block: () -> Array<String?>?) { actions = block() }

    fun onLoadListener(block: () -> Array<Any>?) { onLoadListener = block() }
    fun onClickListener(block: () -> Array<Any>?) { onClickListener = block() }
    fun onClickRepeatListener(block: () -> Array<Any>?) { onClickRepeatListener = block() }
    fun onReleaseListener(block: () -> Array<Any>?) { onReleaseListener = block() }
    fun onHoldListener(block: () -> Array<Any>?) { onHoldListener = block() }
    fun onMouseOverListener(block: () -> Array<Any>?) { onMouseOverListener = block() }
    fun onMouseRepeatListener(block: () -> Array<Any>?) { onMouseRepeatListener = block() }
    fun onMouseLeaveListener(block: () -> Array<Any>?) { onMouseLeaveListener = block() }
    fun onDragListener(block: () -> Array<Any>?) { onDragListener = block() }
    fun onDragCompleteListener(block: () -> Array<Any>?) { onDragCompleteListener = block() }
    fun onTargetEnterListener(block: () -> Array<Any>?) { onTargetEnterListener = block() }
    fun onTargetLeaveListener(block: () -> Array<Any>?) { onTargetLeaveListener = block() }
    fun onVarTransmitListener(block: () -> Array<Any>?) { onVarTransmitListener = block() }
    fun onInvTransmitListener(block: () -> Array<Any>?) { onInvTransmitListener = block() }
    fun onStatTransmitListener(block: () -> Array<Any>?) { onStatTransmitListener = block() }
    fun onTimerListener(block: () -> Array<Any>?) { onTimerListener = block() }
    fun onOpListener(block: () -> Array<Any>?) { onOpListener = block() }
    fun onScrollWheelListener(block: () -> Array<Any>?) { onScrollWheelListener = block() }

    fun varTransmitTriggers(block: () -> IntArray?) { varTransmitTriggers = block() }
    fun invTransmitTriggers(block: () -> IntArray?) { invTransmitTriggers = block() }
    fun statTransmitTriggers(block: () -> IntArray?) { statTransmitTriggers = block() }
    fun dynamicValues(block: () -> Array<IntArray>?) { dynamicValues = block() }
    fun valueCompareType(block: () -> IntArray?) { valueCompareType = block() }

    fun position(block: () -> Pair<Int, Int>) {
        val (newX, newY) = block()
        x = newX
        y = newY
    }

    fun setOption(index: Int, option: String?) {
        if (actions == null || index >= actions!!.size) {
            val newSize = index + 1
            actions = Array(newSize) { actions?.getOrNull(it) }
        }
        actions!![index] = option?.takeIf { it.isNotEmpty() && it != "null" }
    }

    fun repeatType(bld: () -> RepeatType) {
        repeatType = bld()
    }

}
