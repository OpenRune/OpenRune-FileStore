package dev.openrune.definition.type.model.texture

data class SimpleTexture(
    override val faces: List<TexturedFace>,
    override val renderType: Int,
    override val coordinate: Int,
    override val vertex1: Int,
    override val vertex2: Int,
    override val vertex3: Int,
) : Texture