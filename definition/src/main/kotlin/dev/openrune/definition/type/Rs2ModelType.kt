package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class Rs2ModelRender(
    var flags: Int = 0,
    var material: Int = -1,
    var lod: Int = 0,
    var unknown: Int = 0,
    var indexCount: Int = 0,
) {
    val hidden: Boolean get() = flags and Rs2ModelType.FLAG_HIDDEN != 0
}

data class Rs2ModelType(
    override var id: Int = -1,
    var format: Int = 0,
    var version: Int = 0,
    var vertexCount: Int = 0,
    var renders: MutableList<Rs2ModelRender> = mutableListOf(),
    var billboardCount: Int = 0,
    var emitterCount: Int = 0,
    var effectorCount: Int = 0,
) : Definition {

    val materials: List<Int> get() = renders.map { it.material }.filter { it != -1 }

    companion object {
        const val FLAG_HAS_VERTICES = 1
        const val FLAG_HAS_FACE_BONES = 1 shl 2
        const val FLAG_HAS_BONE_IDS = 1 shl 3
        const val FLAG_HIDDEN = 1 shl 4
        const val FLAG_HAS_SKIN = 1 shl 5
    }
}
