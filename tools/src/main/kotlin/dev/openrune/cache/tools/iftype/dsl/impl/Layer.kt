package dev.openrune.cache.tools.iftype.dsl.impl

import dev.openrune.cache.tools.iftype.dsl.BaseComponent
import dev.openrune.cache.tools.iftype.dsl.InterfaceBuilder
import dev.openrune.cache.tools.iftype.dsl.impl.Graphic.applyGraphic
import dev.openrune.cache.tools.iftype.dsl.impl.Input.applyInput
import dev.openrune.cache.tools.iftype.dsl.impl.Layer.applyLayer
import dev.openrune.cache.tools.iftype.dsl.impl.Line.applyLine
import dev.openrune.cache.tools.iftype.dsl.impl.Model.applyModel
import dev.openrune.cache.tools.iftype.dsl.impl.Rectangle.applyRectangle
import dev.openrune.cache.tools.iftype.dsl.impl.Text.TextComponent
import dev.openrune.cache.tools.iftype.dsl.impl.Text.applyText
import dev.openrune.definition.type.widget.ComponentTypeBuilder


object Layer {

    fun applyLayer(componentName: String, bld: LayerComponent) = bld.apply(componentName)


    open class LayerComponent(internal val host: InterfaceBuilder) : BaseComponent() {
        val layerComponents = mutableListOf<ComponentTypeBuilder>()

        var scrollWidth: Int = 0
        var scrollHeight: Int = 0
        var noClickThrough: Boolean = false

        fun scrollWidth(bld: () -> Int) {
            this.scrollWidth = bld()
        }

        fun scrollHeight(bld: () -> Int) {
            this.scrollHeight = bld()
        }

        fun noClickThrough(bld: () -> Boolean) {
            this.noClickThrough = bld()
        }

        fun apply(componentName : String): ComponentTypeBuilder {
            return ComponentTypeBuilder(componentName).apply {
                applyCommonProperties(this)
                type = 0
                scrollWidth = this@LayerComponent.scrollWidth
                scrollHeight = this@LayerComponent.scrollHeight
                noClickThrough = this@LayerComponent.noClickThrough
            }
        }

        fun layer(componentName: String, block: LayerComponent.() -> Unit): Int =
            host.layer(componentName, layerComponents, block)

        fun text(componentName: String, block: TextComponent.() -> Unit) {
            host.text(componentName, layerComponents, block)
        }

        fun model(componentName: String, block: Model.ModelComponent.() -> Unit) {
            host.model(componentName, layerComponents, block)
        }

        fun rectangle(componentName: String, block: Rectangle.RectangleComponent.() -> Unit) {
            host.rectangle(componentName, layerComponents, block)
        }

        fun line(componentName: String, block: Line.LineComponent.() -> Unit) {
            host.line(componentName, layerComponents, block)
        }

        fun graphic(componentName: String, block: Graphic.GraphicComponent.() -> Unit) {
            host.graphic(componentName, layerComponents, block)
        }

    }

}


fun InterfaceBuilder.text(
    componentName: String,
    targetList: MutableList<ComponentTypeBuilder> = components,
    block: TextComponent.() -> Unit
) {
    val bld = TextComponent().apply(block)
    val component = applyText(componentName, bld)
    bld.repeatType?.generateComponents(component.width ?: 0, component.height ?: 0, componentName, component)?.forEach {
        targetList.add(it)
    } ?: targetList.add(component)
}

/**
 * Depth-first flatten of layer children into [InterfaceBuilder.components], matching client ordering
 * (parent row first, then descendants). [parentOneBased] matches the historical `components.size + 1`
 * convention used for packed `layer` refs.
 */
private fun InterfaceBuilder.flattenLayerSubtree(parentOneBased: Int, children: List<ComponentTypeBuilder>) {
    for (child in children) {
        if (child.layer == null) {
            child.layer = (id shl 16) or parentOneBased
        }
        components.add(child)
        val nested = layerNestedChildren.remove(child)
        if (!nested.isNullOrEmpty()) {
            flattenLayerSubtree(components.size, nested)
        }
    }
}

fun InterfaceBuilder.layer(
    componentName: String,
    targetList: MutableList<ComponentTypeBuilder> = components,
    block: Layer.LayerComponent.() -> Unit
) : Int {

    val parentOneBased = components.size + 1

    val bld = Layer.LayerComponent(this).apply(block)
    val component = applyLayer(componentName, bld)
    bld.repeatType?.generateComponents(component.width ?: 0, component.height ?: 0, componentName, component)?.forEach {
        targetList.add(it)
    } ?: targetList.add(component)

    if (bld.layerComponents.isEmpty()) {
        return parentOneBased
    }

    if (bld.repeatType != null) {
        bld.layerComponents.forEach {
            if (it.layer == null) {
                it.layer = (id shl 16) or parentOneBased
            }
            components.add(it)
        }
        return parentOneBased
    }

    val children = bld.layerComponents.toMutableList()
    if (targetList === components) {
        flattenLayerSubtree(parentOneBased, children)
    } else {
        layerNestedChildren[component] = children
    }

    return parentOneBased
}


fun InterfaceBuilder.model(
    componentName: String,
    targetList: MutableList<ComponentTypeBuilder> = components,
    block: Model.ModelComponent.() -> Unit
) {
    val bld = Model.ModelComponent().apply(block)
    val component = applyModel(componentName, bld)
    bld.repeatType?.generateComponents(component.width ?: 0, component.height ?: 0, componentName, component)?.forEach {
        targetList.add(it)
    } ?: targetList.add(component)
}

fun InterfaceBuilder.input(
    componentName: String,
    targetList: MutableList<ComponentTypeBuilder> = components,
    block: Input.InputComponent.() -> Unit
) {
    val bld = Input.InputComponent().apply(block)
    val component = applyInput(componentName, bld)
    bld.repeatType?.generateComponents(component.width ?: 0, component.height ?: 0, componentName, component)?.forEach {
        targetList.add(it)
    } ?: targetList.add(component)
}

fun InterfaceBuilder.rectangle(
    componentName: String,
    targetList: MutableList<ComponentTypeBuilder> = components,
    block: Rectangle.RectangleComponent.() -> Unit
) {
    val bld = Rectangle.RectangleComponent().apply(block)
    val component = applyRectangle(componentName, bld)
    bld.repeatType?.generateComponents(component.width ?: 0, component.height ?: 0, componentName, component)?.forEach {
        targetList.add(it)
    } ?: targetList.add(component)
}

fun InterfaceBuilder.line(
    componentName: String,
    targetList: MutableList<ComponentTypeBuilder> = components,
    block: Line.LineComponent.() -> Unit
) {
    val bld = Line.LineComponent().apply(block)
    val component = applyLine(componentName, bld)
    bld.repeatType?.generateComponents(component.width ?: 0, component.height ?: 0, componentName, component)?.forEach {
        targetList.add(it)
    } ?: targetList.add(component)
}

fun InterfaceBuilder.graphic(
    componentName: String,
    targetList: MutableList<ComponentTypeBuilder> = components,
    block: Graphic.GraphicComponent.() -> Unit
) {
    val bld = Graphic.GraphicComponent().apply(block)
    val component = applyGraphic(componentName, bld)
    bld.repeatType?.generateComponents(component.width ?: 0, component.height ?: 0, componentName, component)?.forEach {
        targetList.add(it)
    } ?: targetList.add(component)
}