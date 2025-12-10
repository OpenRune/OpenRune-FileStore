package dev.openrune.cache.tools.iftype.dsl

import dev.openrune.definition.type.widget.ComponentTypeBuilder
import java.awt.Color

/**
 * Extension functions for ComponentTypeBuilder DSL convenience methods
 */

fun ComponentTypeBuilder.size(block: () -> Pair<Int, Int>) {
    val (newX, newY) = block()
    width = newX
    height = newY
}

fun ComponentTypeBuilder.width(block: () -> Int) { width = block() }
fun ComponentTypeBuilder.height(block: () -> Int) { height = block() }
fun ComponentTypeBuilder.type(block: () -> Int) { type = block() }
fun ComponentTypeBuilder.contentType(block: () -> Int) { contentType = block() }
fun ComponentTypeBuilder.xMode(block: () -> Int) { xMode = block() }
fun ComponentTypeBuilder.yMode(block: () -> Int) { yMode = block() }
fun ComponentTypeBuilder.widthMode(block: () -> Int) { widthMode = block() }
fun ComponentTypeBuilder.heightMode(block: () -> Int) { heightMode = block() }
fun ComponentTypeBuilder.layer(block: () -> Int) { layer = block() }
fun ComponentTypeBuilder.hide(block: () -> Boolean) { hide = block() }
fun ComponentTypeBuilder.opBase(block: () -> String?) { opBase = block() }
fun ComponentTypeBuilder.targetVerb(block: () -> String?) { targetVerb = block() }
fun ComponentTypeBuilder.dragDeadZone(block: () -> Int) { dragDeadZone = block() }
fun ComponentTypeBuilder.dragDeadTime(block: () -> Int) { dragDeadTime = block() }
fun ComponentTypeBuilder.draggableBehavior(block: () -> Boolean) { draggableBehavior = block() }

fun ComponentTypeBuilder.onLoad(block: () -> Array<Any>?) { onLoad = block() }
fun ComponentTypeBuilder.onClick(block: () -> Array<Any>?) { onClick = block() }
fun ComponentTypeBuilder.onClickRepeat(block: () -> Array<Any>?) { onClickRepeat = block() }
fun ComponentTypeBuilder.onRelease(block: () -> Array<Any>?) { onRelease = block() }
fun ComponentTypeBuilder.onHold(block: () -> Array<Any>?) { onHold = block() }
fun ComponentTypeBuilder.onMouseOver(block: () -> Array<Any>?) { onMouseOver = block() }
fun ComponentTypeBuilder.onMouseRepeat(block: () -> Array<Any>?) { onMouseRepeat = block() }
fun ComponentTypeBuilder.onMouseLeave(block: () -> Array<Any>?) { onMouseLeave = block() }
fun ComponentTypeBuilder.onDrag(block: () -> Array<Any>?) { onDrag = block() }
fun ComponentTypeBuilder.onDragComplete(block: () -> Array<Any>?) { onDragComplete = block() }
fun ComponentTypeBuilder.onTargetEnter(block: () -> Array<Any>?) { onTargetEnter = block() }
fun ComponentTypeBuilder.onTargetLeave(block: () -> Array<Any>?) { onTargetLeave = block() }
fun ComponentTypeBuilder.onVarTransmit(block: () -> Array<Any>?) { onVarTransmit = block() }
fun ComponentTypeBuilder.onInvTransmit(block: () -> Array<Any>?) { onInvTransmit = block() }
fun ComponentTypeBuilder.onStatTransmit(block: () -> Array<Any>?) { onStatTransmit = block() }
fun ComponentTypeBuilder.onTimer(block: () -> Array<Any>?) { onTimer = block() }
fun ComponentTypeBuilder.onOp(block: () -> Array<Any>?) { onOp = block() }
fun ComponentTypeBuilder.onScrollWheel(block: () -> Array<Any>?) { onScrollWheel = block() }

fun ComponentTypeBuilder.onVarTransmitList(block: () -> IntArray?) { onVarTransmitList = block() }
fun ComponentTypeBuilder.onInvTransmitList(block: () -> IntArray?) { onInvTransmitList = block() }
fun ComponentTypeBuilder.onStatTransmitList(block: () -> IntArray?) { onStatTransmitList = block() }

fun ComponentTypeBuilder.position(block: () -> Pair<Int, Int>) {
    val (newX, newY) = block()
    x = newX
    y = newY
}

fun ComponentTypeBuilder.setOption(index: Int, option: String?) {
    val currentOp = op
    if (currentOp.isEmpty() || index >= currentOp.size) {
        val newSize = index + 1
        val newOp = Array(newSize) { currentOp.getOrNull(it) ?: "" }
        newOp[index] = option?.takeIf { it.isNotEmpty() && it != "null" } ?: ""
        op = newOp
    } else {
        val newOp = currentOp.copyOf()
        newOp[index] = option?.takeIf { it.isNotEmpty() && it != "null" } ?: ""
        op = newOp
    }
}

fun Color.toJagexColor(): Int {
    val hex = "#${String.format("%02X%02X%02X", red, green, blue)}"
    val color = if (hex.isEmpty()) 0 else hex.removePrefix("#").toInt(16)
    return color
}

