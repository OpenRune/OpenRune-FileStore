package dev.openrune.definition.game.render.model

import dev.openrune.definition.game.render.util.Vector3f
import kotlin.math.sqrt

class VertexNormal {
    var x = 0
    var y = 0
    var z = 0
    var magnitude = 0

    fun normalize(): Vector3f {
        val length = sqrt((x * x + y * y + z * z).toDouble()).toFloat().coerceAtLeast(1f)
        return Vector3f(x / length, y / length, z / length).also {
            assert(it.x in -1f..1f)
            assert(it.y in -1f..1f)
            assert(it.z in -1f..1f)
        }
    }
}
