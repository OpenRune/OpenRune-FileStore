package dev.openrune.cache.tools.iftype.dsl.impl

import dev.openrune.cache.tools.iftype.dsl.BaseComponent
import dev.openrune.cache.tools.iftype.dsl.toJagexColor
import dev.openrune.definition.type.widget.ComponentTypeBuilder
import java.awt.Color

object Line {

    fun applyLine(name: String, bld: LineComponent) = bld.apply(name)

    open class LineComponent : BaseComponent() {
        var lineWidth: Int = 1
        var color: Int = 0
        var lineDirection: Boolean = false

        fun lineWidth(bld: () -> Int) {
            this.lineWidth = bld()
        }

        fun color(value: Int) {
            this.color = value
        }

        fun color(value: Color) {
            this.color = value.toJagexColor()
        }

        fun lineDirection(bld: () -> Boolean) {
            this.lineDirection = bld()
        }

        fun apply(componentName : String): ComponentTypeBuilder {
            return ComponentTypeBuilder(componentName).apply {
                applyCommonProperties(this)
                type = 9
                lineWid = this@LineComponent.lineWidth
                colour1 = this@LineComponent.color
                lineDirection = this@LineComponent.lineDirection
            }
        }

    }

}