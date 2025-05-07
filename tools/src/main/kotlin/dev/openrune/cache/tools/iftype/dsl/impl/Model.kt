package dev.openrune.cache.tools.iftype.dsl.impl

import dev.openrune.cache.tools.iftype.dsl.InterfaceComponent
import dev.openrune.definition.type.widget.ComponentType

object Model {

    fun applyModel(name: String, bld: ModelComponent) = bld.apply(name)

    open class ModelComponent : InterfaceComponent() {

        var modelId: Int = -1
        var offsetX2d: Int = 0
        var offsetY2d: Int = 0
        var rotationX: Int = 0
        var rotationZ: Int = 0
        var rotationY: Int = 0
        var modelZoom: Int = 100
        var animation: Int = -1
        var modelHeightOverride: Int = 0
        var orthogonal: Boolean = false

        fun modelId(bld: () -> Int) {
            modelId = bld()
        }

        fun offsetX2d(bld: () -> Int) {
            offsetX2d = bld()
        }

        fun offsetY2d(bld: () -> Int) {
            offsetY2d = bld()
        }

        fun rotationX(bld: () -> Int) {
            rotationX = bld()
        }

        fun rotationZ(bld: () -> Int) {
            rotationZ = bld()
        }

        fun rotationY(bld: () -> Int) {
            rotationY = bld()
        }

        fun modelZoom(bld: () -> Int) {
            modelZoom = bld()
        }

        fun animation(bld: () -> Int) {
            animation = bld()
        }

        fun modelHeightOverride(bld: () -> Int) {
            modelHeightOverride = bld()
        }

        fun orthogonal(bld: () -> Boolean) {
            orthogonal = bld()
        }

        fun apply(componentName : String): ComponentType {
            val component = ComponentType()
            applyTo(component)
            component.type = 6
            component.modelId = modelId
            component.offsetX2d = offsetX2d
            component.offsetY2d = offsetY2d
            component.rotationX = rotationX
            component.rotationZ = rotationZ
            component.rotationY = rotationY
            component.modelZoom = modelZoom
            component.animation = animation
            component.modelHeightOverride = modelHeightOverride
            component.orthogonal = orthogonal
            component.name = componentName
            return component
        }

    }
}
