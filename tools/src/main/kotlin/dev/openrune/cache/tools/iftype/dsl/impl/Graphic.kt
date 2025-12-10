package dev.openrune.cache.tools.iftype.dsl.impl

import dev.openrune.cache.tools.iftype.dsl.BaseComponent
import dev.openrune.cache.tools.iftype.dsl.setOption
import dev.openrune.cache.tools.iftype.dsl.toJagexColor
import dev.openrune.definition.type.widget.ComponentTypeBuilder
import dev.openrune.definition.type.widget.IfEvent
import java.awt.Color

object Graphic {

    fun applyGraphic(name: String, bld: GraphicComponent) : ComponentTypeBuilder {
        require(bld.spriteId != -1) { "spriteId must be set to a valid value before applying GraphicComponent." }
        return bld.apply(name)
    }

    open class GraphicComponent : BaseComponent() {
        var spriteId: Int = -1
        var textureId: Int = 0
        var spriteTiling: Boolean = false
        var borderType: Int = 0
        var shadowColor: Int = 0
        var flippedVertically: Boolean = false
        var flippedHorizontally: Boolean = false
        var opacity: Int = 0
        private val options = mutableListOf<String>()
        private var hoverHoverSprite: Int? = null
        private var hoverNormalSprite: Int? = null
        private var hoverComponent: String = "component:self"

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
            options.add(option)
            if (addAccessMask) {
                events = (events ?: 0) or IfEvent.DeprecatedOp1.bitmask.toInt()
            }
        }

        fun effectHover(hover: Int, normal: Int, component: String = "component:self") {
            hoverHoverSprite = hover
            hoverNormalSprite = normal
            hoverComponent = component
        }

        fun apply(componentName : String): ComponentTypeBuilder {
            return ComponentTypeBuilder(componentName).apply {
                applyCommonProperties(this)
                type = 5
                graphic = this@GraphicComponent.spriteId
                angle2d = this@GraphicComponent.textureId
                tiling = this@GraphicComponent.spriteTiling
                outline = this@GraphicComponent.borderType
                graphicShadow = this@GraphicComponent.shadowColor
                vFlip = this@GraphicComponent.flippedVertically
                hFlip = this@GraphicComponent.flippedHorizontally
                trans1 = this@GraphicComponent.opacity
                
                // Apply options
                this@GraphicComponent.options.forEachIndexed { index, option ->
                    setOption(index, option)
                }
                
                // Apply hover effect
                this@GraphicComponent.hoverHoverSprite?.let { hover ->
                    this@GraphicComponent.hoverNormalSprite?.let { normal ->
                        onMouseOver = arrayOf(44, this@GraphicComponent.hoverComponent, hover)
                        onMouseLeave = arrayOf(44, this@GraphicComponent.hoverComponent, normal)
                    }
                }
            }
        }

    }
}