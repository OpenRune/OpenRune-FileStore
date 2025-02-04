package dev.openrune.definition.game.render.util

data class Vector3f(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {

    fun length(): Float = kotlin.math.sqrt(x * x + y * y + z * z)

    fun normalize(): Vector3f {
        val len = length().coerceAtLeast(1f)
        return Vector3f(x / len, y / len, z / len)
    }
}