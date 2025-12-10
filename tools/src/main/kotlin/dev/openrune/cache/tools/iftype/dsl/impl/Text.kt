package dev.openrune.cache.tools.iftype.dsl.impl

import dev.openrune.cache.tools.iftype.dsl.BaseComponent
import dev.openrune.cache.tools.iftype.dsl.setOption
import dev.openrune.cache.tools.iftype.dsl.toJagexColor
import dev.openrune.definition.type.widget.ComponentTypeBuilder
import dev.openrune.definition.type.widget.IfEvent
import java.awt.Color

enum class TextAlignment(val value: Int) {
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

    open class TextComponent : BaseComponent() {
        var text: String = ""
        var font: FontType = FontType.FONT_REGULAR
        var lineHeight: Int = 0
        var xAllignment: Int = TextAlignment.CENTER.value
        var yAllignment: Int = TextAlignment.CENTER.value
        var textShadowed: Boolean = true
        var color: Int = 0
        private val options = mutableListOf<String>()
        private var hoverNormalColor: Color? = null
        private var hoverHoverColor: Color? = null

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

        fun verticalAlignment(bld: () -> TextAlignment) {
            yAllignment = bld().value
        }

        fun horizontalAlignment(bld: () -> TextAlignment) {
            xAllignment = bld().value
        }

        fun addOption(option: String, addAccessMask: Boolean = true) {
            options.add(option)
            if (addAccessMask) {
                events = (events ?: 0) or IfEvent.DeprecatedOp1.bitmask.toInt()
            }
        }

        fun effectHover(colorNormal: Color, colorHover: Color) {
            hoverNormalColor = colorNormal
            hoverHoverColor = colorHover
        }

        fun apply(componentName : String): ComponentTypeBuilder {
            return ComponentTypeBuilder(componentName).apply {
                applyCommonProperties(this)
                type = 4
                text = this@TextComponent.text
                textFont = this@TextComponent.font.value
                textLineHeight = this@TextComponent.lineHeight
                textAlignH = this@TextComponent.xAllignment
                textAlignV = this@TextComponent.yAllignment
                textShadow = this@TextComponent.textShadowed
                colour1 = this@TextComponent.color
                
                // Apply options
                this@TextComponent.options.forEachIndexed { index, option ->
                    setOption(index, option)
                }
                
                // Apply hover effect
                this@TextComponent.hoverNormalColor?.let { normal ->
                    this@TextComponent.hoverHoverColor?.let { hover ->
                        onMouseOver = arrayOf(45, "component:self", normal.toJagexColor())
                        onMouseLeave = arrayOf(45, "component:self", hover.toJagexColor())
                    }
                }
            }
        }
    }
}
