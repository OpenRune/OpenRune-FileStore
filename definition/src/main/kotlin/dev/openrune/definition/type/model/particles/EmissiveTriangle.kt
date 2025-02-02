package dev.openrune.definition.type.model.particles

data class EmissiveTriangle(
    val emitter: Int,
    val face: Int,
    val s: Int,
    val t: Int,
    val u: Int,
    val priority: Int
)