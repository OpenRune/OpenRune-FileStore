package dev.openrune.definition.type.model.texture

sealed interface ComplexTexture : Texture {
    val scaleX: Int
    val scaleY: Int
    val scaleZ: Int
    val rotation: Int
    val direction: Int
    val speed: Int
}