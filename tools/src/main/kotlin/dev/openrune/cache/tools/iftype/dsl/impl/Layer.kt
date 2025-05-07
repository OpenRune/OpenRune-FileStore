package dev.openrune.cache.tools.iftype.dsl.impl

import dev.openrune.cache.tools.iftype.dsl.InterfaceBuilder
import dev.openrune.cache.tools.iftype.dsl.InterfaceComponent
import dev.openrune.cache.tools.iftype.dsl.impl.Graphic.applyGraphic
import dev.openrune.cache.tools.iftype.dsl.impl.Layer.applyLayer
import dev.openrune.cache.tools.iftype.dsl.impl.Line.applyLine
import dev.openrune.cache.tools.iftype.dsl.impl.Model.applyModel
import dev.openrune.cache.tools.iftype.dsl.impl.Rectangle.applyRectangle
import dev.openrune.cache.tools.iftype.dsl.impl.Text.TextComponent
import dev.openrune.cache.tools.iftype.dsl.impl.Text.applyText
import dev.openrune.definition.type.widget.ComponentType


object Layer {

    fun applyLayer(componentName: String, bld: LayerComponent) = bld.apply(componentName)


    open class LayerComponent : InterfaceComponent() {
        val layerComponents = mutableListOf<ComponentType>()

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

        fun apply(componentName : String): ComponentType {
            val component = ComponentType()
            applyTo(component)
            component.type = 0
            component.scrollWidth = scrollWidth
            component.scrollHeight = scrollHeight
            component.noClickThrough = noClickThrough
            component.name = componentName
            return component
        }

        fun LayerComponent.layer(componentName: String, block: LayerComponent.() -> Unit) : Int {
            return InterfaceBuilder().layer(componentName, layerComponents, block)
        }

        fun LayerComponent.text(componentName: String, block: TextComponent.() -> Unit) {
            InterfaceBuilder().text(componentName, layerComponents, block)
        }

        fun LayerComponent.model(componentName: String, block: Model.ModelComponent.() -> Unit) {
            InterfaceBuilder().model(componentName, layerComponents, block)
        }

        fun LayerComponent.rectangle(componentName: String, block: Rectangle.RectangleComponent.() -> Unit) {
            InterfaceBuilder().rectangle(componentName, layerComponents, block)
        }

        fun LayerComponent.line(componentName: String, block: Line.LineComponent.() -> Unit) {
            InterfaceBuilder().line(componentName, layerComponents, block)
        }

        fun LayerComponent.graphic(componentName: String, block: Graphic.GraphicComponent.() -> Unit) {
            InterfaceBuilder().graphic(componentName, layerComponents, block)
        }

    }

}


fun InterfaceBuilder.text(
    componentName: String,
    targetList: MutableList<ComponentType> = components,
    block: TextComponent.() -> Unit
) {
    val bld = TextComponent().apply(block)
    val component = applyText(componentName, bld)
    bld.repeatType?.generateComponents(0,0,componentName,component)?.forEach {
        targetList.add(it)
    }?: targetList.add(component)
}

fun InterfaceBuilder.layer(
    componentName: String,
    targetList: MutableList<ComponentType> = components,
    block: Layer.LayerComponent.() -> Unit
) : Int {

    val index = components.size + 1

    val bld = Layer.LayerComponent().apply(block)
    val component = applyLayer(componentName, bld)
    bld.repeatType?.generateComponents(bld.width,bld.height,componentName,component)?.forEach {
        targetList.add(it)
    }?: targetList.add(component)

    bld.layerComponents.forEach {
        it.parentId = index
        components.add(it)
    }

    return index
}


fun InterfaceBuilder.model(
    componentName: String,
    targetList: MutableList<ComponentType> = components,
    block: Model.ModelComponent.() -> Unit
) {
    val bld = Model.ModelComponent().apply(block)
    val component = applyModel(componentName, bld)
    bld.repeatType?.generateComponents(bld.width,bld.height,componentName,component)?.forEach {
        targetList.add(it)
    }?: targetList.add(component)
}

fun InterfaceBuilder.rectangle(
    componentName: String,
    targetList: MutableList<ComponentType> = components,
    block: Rectangle.RectangleComponent.() -> Unit
) {
    val bld = Rectangle.RectangleComponent().apply(block)
    val component = applyRectangle(componentName, bld)
    bld.repeatType?.generateComponents(bld.width,bld.height,componentName,component)?.forEach {
        targetList.add(it)
    }?: targetList.add(component)
}

fun InterfaceBuilder.line(
    componentName: String,
    targetList: MutableList<ComponentType> = components,
    block: Line.LineComponent.() -> Unit
) {
    val bld = Line.LineComponent().apply(block)
    val component = applyLine(componentName, bld)
    bld.repeatType?.generateComponents(bld.width,bld.height,componentName,component)?.forEach {
        targetList.add(it)
    }?: targetList.add(component)
}

fun InterfaceBuilder.graphic(
    componentName: String,
    targetList: MutableList<ComponentType> = components,
    block: Graphic.GraphicComponent.() -> Unit
) {
    val bld = Graphic.GraphicComponent().apply(block)
    val component = applyGraphic(componentName, bld)
    bld.repeatType?.generateComponents(bld.width,bld.height,componentName,component)?.forEach {
        targetList.add(it)
    }?: targetList.add(component)

}