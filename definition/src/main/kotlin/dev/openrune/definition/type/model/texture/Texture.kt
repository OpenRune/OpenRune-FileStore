package dev.openrune.definition.type.model.texture

sealed interface Texture {
    val faces: List<TexturedFace>
    val renderType: Int
    val coordinate: Int
    val vertex1: Int
    val vertex2: Int
    val vertex3: Int
}