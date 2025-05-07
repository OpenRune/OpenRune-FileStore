package dev.openrune.cache.tools.iftype.dsl.impl

import dev.openrune.cache.tools.iftype.dsl.InterfaceComponent
import dev.openrune.definition.type.widget.AccessMask
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.type.widget.toJagexColor
import java.awt.Color

enum class Alignment(val value: Int) {
    LEFT(0),
    CENTER(1),
    RIGHT(2);
}

enum class FontType(val value: Int) {
    FONT_SMALL(494),
    FONT_REGULAR(495),
    FONT_BOLD(496),
    FONT_LARGE_STYLE(497);
}


object Text {

    fun applyText(componentName: String, bld: TextComponent) = bld.apply(componentName)

    open class TextComponent : InterfaceComponent() {

        var text: String = ""
        var font: FontType = FontType.FONT_REGULAR
        var lineHeight: Int = 0
        var xAllignment: Int = Alignment.CENTER.value
        var yAllignment: Int = Alignment.CENTER.value
        var textShadowed: Boolean = true
        var color: Int = 0

        fun display(bld: () -> String) {
            text = bld()
        }

        fun font(bld: () -> FontType) {
            font = bld()
        }

        fun lineHeight(bld: () -> Int) {
            lineHeight = bld()
        }

        fun xAllignment(bld: () -> Int) {
            xAllignment = bld()
        }

        fun yAllignment(bld: () -> Int) {
            yAllignment = bld()
        }

        fun textShadowed(bld: () -> Boolean) {
            textShadowed = bld()
        }

        fun color(value: Int) {
            color = value
        }

        fun color(value: String) {
            color = if (value.isEmpty()) 0 else value.removePrefix("#").toInt(16)
        }

        fun color(value: Color) {
            color = value.toJagexColor()
        }

        fun verticalAlignment(bld: () -> Alignment) {
            yAllignment = bld().value
        }

        fun horizontalAlignment(bld: () -> Alignment) {
            xAllignment = bld().value
        }

        fun addOption(option: String, addAccessMask: Boolean = true) {
            if (addAccessMask) {
                accessMask = AccessMask.CLICK_OP1.value
            }
            setOption(0, option)
        }

        fun effectHover(colorNormal: Color, colorHover: Color) {
            onMouseOverListener = arrayOf(45, "component:self", colorNormal.toJagexColor())
            onMouseLeaveListener = arrayOf(45, "component:self", colorHover.toJagexColor())
        }

        fun apply(componentName : String): ComponentType {
            val component = ComponentType()
            applyTo(component)
            component.type = 4
            component.text = text
            component.font = font.value
            component.lineHeight = lineHeight
            component.xAllignment = xAllignment
            component.yAllignment = yAllignment
            component.textShadowed = textShadowed
            component.color = color
            component.name = componentName
            return component
        }


    }
}
