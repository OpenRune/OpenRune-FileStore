package dev.openrune.cache.tools.iftype.dsl

import dev.openrune.definition.type.widget.ComponentType

sealed class RepeatType {
    open fun generateComponents(widthCom : Int, heightCom : Int, namePrefix: String, component: ComponentType): List<ComponentType> = emptyList()
}

data class Grid(
    val count: Int,
    val rowSize: Int,
    val padX: Int = 0,
    val padY: Int = 0,
    val additive: Boolean = true
) : RepeatType() {
    override fun generateComponents(
        widthCom: Int,
        heightCom: Int,
        namePrefix: String,
        component: ComponentType
    ): List<ComponentType> {
        val components = mutableListOf<ComponentType>()

        repeat(count) { index ->
            val com = component.clone()
            val row = index / rowSize
            val col = index % rowSize
            val width = if (additive) widthCom + padX else widthCom - padX
            val height = if (additive) heightCom + padY else heightCom - padY
            val offsetX = col * width
            val offsetY = row * height

            com.name = "${namePrefix}_$index"
            com.setPosition(component.x + offsetX, component.y + offsetY)

            components.add(com)
        }

        return components
    }

}

class InterfaceBuilder(
    override var id: Int = -1,
    var interfaceName: String = "",
    override var width: Int = -1,
    override var height: Int = -1
) : InterfaceComponent() {

    fun apply(componentName : String): ComponentType {
        val component = ComponentType()
        applyTo(component)
        component.id = id
        component.debugInterfaceName = interfaceName
        component.width = width
        component.height = height
        component.name = componentName
        return component
    }

    var offset = Pair(0,0)
    val components = mutableListOf<ComponentType>()

    fun setOffset(block: () -> Pair<Int, Int>) {
        val (newX, newY) = block()
        offset = Pair(newX,newY)
    }

    private fun totalChildren(): Int {
        var total = components.size
        return total
    }

}

fun buildInterface(id: Int, interfaceName: String, width: Int, height: Int,builder: InterfaceBuilder.() -> Unit): List<ComponentType> {
    val bld = InterfaceBuilder(id, interfaceName,width,height)
    builder.invoke(bld)

    val component = bld.apply(interfaceName)

    bld.components.add(0,component)

    bld.components.forEach {
        it.id = id
        it.isIf3 = true
        it.x += bld.offset.first
        it.y += bld.offset.second
    }

    return bld.components
}
