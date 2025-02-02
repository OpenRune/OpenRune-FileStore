package dev.openrune.definition.type.model.texture

data class CubeTexture(
    override val faces: List<TexturedFace>,
    override val renderType: Int,
    override val coordinate: Int,
    override val vertex1: Int,
    override val vertex2: Int,
    override val vertex3: Int,
    override val scaleX: Int,
    override val scaleY: Int,
    override val scaleZ: Int,
    override val rotation: Int,
    override val direction: Int,
    override val speed: Int,
    val transU: Int,
    val transV: Int,
) : ComplexTexture