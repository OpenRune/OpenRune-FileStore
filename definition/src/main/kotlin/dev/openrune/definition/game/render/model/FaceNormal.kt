package dev.openrune.definition.game.render.model

import dev.openrune.definition.game.render.util.Vector3f

data class FaceNormal(
    var x: Int = 0,
    var y: Int = 0,
    var z: Int = 0
) {
    fun normalize(): Vector3f {
        val length = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat().coerceAtLeast(1f)
        return Vector3f(x / length, y / length, z / length)
    }
}