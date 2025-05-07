package dev.openrune.cache.tools.iftype.dsl.impl

import dev.openrune.cache.tools.iftype.dsl.InterfaceComponent
import dev.openrune.definition.type.widget.AccessMask
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.type.widget.toJagexColor
import java.awt.Color

object Graphic {

    fun applyGraphic(name: String, bld: GraphicComponent) : ComponentType {
        require(bld.spriteId != -1) { "spriteId must be set to a valid value before applying GraphicComponent." }
        return bld.apply(name)
    }

    open class GraphicComponent : InterfaceComponent() {

        var spriteId: Int = -1
        var textureId: Int = 0
        var spriteTiling: Boolean = false
        var borderType: Int = 0
        var shadowColor: Int = 0
        var flippedVertically: Boolean = false
        var flippedHorizontally: Boolean = false
        var opacity: Int = 0

        fun spriteId(bld: () -> Int) {
            val value = bld()
            require(value != -1) { "spriteId must be set to a valid value, but was -1" }
            this.spriteId = value
        }

        fun textureId(bld: () -> Int) {
            this.textureId = bld()
        }

        fun spriteTiling(bld: () -> Boolean) {
            this.spriteTiling = bld()
        }

        fun borderType(bld: () -> Int) {
            this.borderType = bld()
        }

        fun shadowColor(value: Int) {
            this.shadowColor = value
        }

        fun shadowColor(value: Color) {
            this.shadowColor = value.toJagexColor()
        }

        fun flippedVertically(bld: () -> Boolean) {
            this.flippedVertically = bld()
        }

        fun flippedHorizontally(bld: () -> Boolean) {
            this.flippedHorizontally = bld()
        }

        fun opacity(bld: () -> Int) {
            val value = bld()
            require(value in 0..255) { "Opacity must be between 0 and 255, but was $value" }
            this.opacity = value
        }

        fun addOption(option: String, addAccessMask: Boolean = true) {
            if (addAccessMask) {
                accessMask = AccessMask.CLICK_OP1.value
            }
            setOption(0, option)
        }

        fun effectHover(hover: Int, normal: Int, component: String = "component:self") {
            onMouseOverListener = arrayOf(44, component, hover)
            onMouseLeaveListener = arrayOf(44, component, normal)
        }

        fun apply(componentName : String): ComponentType {
            val component = ComponentType()
            applyTo(component)
            component.type = 5
            component.spriteId = spriteId
            component.textureId = textureId
            component.spriteTiling = spriteTiling
            component.borderType = borderType
            component.shadowColor = shadowColor
            component.flippedVertically = flippedVertically
            component.flippedHorizontally = flippedHorizontally
            component.opacity = opacity
            component.xMode = xMode
            component.yMode = yMode
            component.contentType = contentType
            component.isIf3 = isIf3
            component.name = componentName
            return component
        }

    }
}