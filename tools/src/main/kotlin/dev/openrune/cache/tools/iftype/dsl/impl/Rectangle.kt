package dev.openrune.cache.tools.iftype.dsl.impl

import dev.openrune.cache.tools.iftype.dsl.BaseComponent
import dev.openrune.cache.tools.iftype.dsl.toJagexColor
import dev.openrune.definition.type.widget.ComponentTypeBuilder
import java.awt.Color

object Rectangle {

    fun applyRectangle(name: String, bld: RectangleComponent) = bld.apply(name)

    open class RectangleComponent : BaseComponent() {
        var color: Int = 0
        var filled: Boolean = false
        var opacity: Int = 0

        fun color(value: Int) {
            this.color = value
        }

        fun color(value: Color) {
            this.color = value.toJagexColor()
        }

        fun opacity(bld: () -> Int) {
            val value = bld()
            require(value in 0..255) { "Opacity must be between 0 and 255, but was $value" }
            this.opacity = value
        }

        fun filled(bld: () -> Boolean) {
            this.filled = bld()
        }

        fun apply(componentName : String): ComponentTypeBuilder {
            return ComponentTypeBuilder(componentName).apply {
                applyCommonProperties(this)
                type = 3
                colour1 = this@RectangleComponent.color
                fill = this@RectangleComponent.filled
                trans1 = this@RectangleComponent.opacity
            }
        }

    }

}