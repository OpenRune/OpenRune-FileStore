package dev.openrune.cache.tools.iftype.dsl.impl

import dev.openrune.cache.tools.iftype.dsl.InterfaceComponent
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.type.widget.toJagexColor
import java.awt.Color

object Line {

    fun applyLine(name: String, bld: LineComponent) = bld.apply(name)

    open class LineComponent : InterfaceComponent() {

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

        fun apply(componentName : String): ComponentType {
            val component = ComponentType()
            applyTo(component)
            component.type = 9
            component.lineWidth = lineWidth
            component.color = color
            component.lineDirection = lineDirection
            component.name = componentName
            return component
        }

    }

}