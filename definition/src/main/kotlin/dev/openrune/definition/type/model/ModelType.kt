package dev.openrune.definition.type.model

import dev.openrune.definition.Definition
import dev.openrune.definition.game.render.model.FaceNormal
import dev.openrune.definition.game.render.model.Model
import dev.openrune.definition.game.render.model.VertexNormal
import dev.openrune.definition.type.model.particles.EffectiveVertex
import dev.openrune.definition.type.model.particles.EmissiveTriangle
import dev.openrune.definition.type.model.particles.FaceBillboard
import dev.openrune.definition.type.model.texture.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class MeshDecodingOption {

    /**
     * If this option is enabled, the original data is preserved for unversioned decode functions.
     * This means the code will not run the code that is found in the client, to strip away "unused"
     * textures and other meta-data. Those operations cause a loss of data, making it impossible to
     * generate a byte-for-byte identical output.
     *
     * An example of this would be setting material colour to 127 because it contains a texture.
     * We cannot get the old colour that was encoded on the mesh back as it could've been anything.
     */
    PreserveOriginalData,

    /**
     * Scales meshes with a version of above 12 down by a factor of 4 (shr 2).
     * Newer meshes in higher revision clients were up-scaled for easier modelling,
     * the client in those revisions performs a down-scaling operation.
     */
    ScaleVersionedMesh
}

enum class MeshType {
    Unversioned,
    Versioned,
    UnversionedSkeletal,
    VersionedSkeletal,
}

data class ModelType(
    override var id: Int = -1,
    var vertexCount: Int = 0,
    var triangleCount: Int = 0,
    var textureTriangleCount: Int = 0,
    var version: Int = UNVERSIONED,
    var type: MeshType = MeshType.VersionedSkeletal,
    var renderPriority: Int = 0,

    var triangleColors: ShortArray? = null,
    var triangleAlphas: IntArray? = null,
    var triangleSkins: IntArray? = null,
    var triangleRenderTypes: IntArray? = null,
    var triangleRenderPriorities: IntArray? = null,
    var triangleVertex1: IntArray? = null,
    var triangleVertex2: IntArray? = null,
    var triangleVertex3: IntArray? = null,

    var vertexPositionsX: IntArray? = null,
    var vertexPositionsY: IntArray? = null,
    var vertexPositionsZ: IntArray? = null,
    var vertexSkins: IntArray? = null,

    var triangleTextures: IntArray? = null,
    var textureRenderTypes: IntArray? = null,
    var textureTriangleVertex1: IntArray? = null,
    var textureTriangleVertex2: IntArray? = null,
    var textureTriangleVertex3: IntArray? = null,
    var textureScaleX: IntArray? = null,
    var textureScaleY: IntArray? = null,
    var textureScaleZ: IntArray? = null,
    var textureRotation: IntArray? = null,
    var textureDirection: IntArray? = null,
    var textureSpeed: IntArray? = null,
    var textureTransU: IntArray? = null,
    var textureTransV: IntArray? = null,
    var textureCoordinates: IntArray? = null,

    var skeletalBones: Array<IntArray?>? = null,
    var skeletalScales: Array<IntArray?>? = null,

    var emitters: Array<EmissiveTriangle>? = null,
    var effectors: Array<EffectiveVertex>? = null,

    var faceBillboards: Array<FaceBillboard>? = null,


) : Definition {


    var vertexNormals: Array<VertexNormal>? = null
    var faceNormals: Array<FaceNormal>? = null

    fun computeNormals() {
        if (this.vertexNormals != null) {
            return
        }

        this.vertexNormals = Array(this.vertexCount) { VertexNormal() }

        for (i in 0 until this.triangleCount) {
            val vertexA = this.triangleVertex1!![i]
            val vertexB = this.triangleVertex2!![i]
            val vertexC = this.triangleVertex3!![i]

            val edgeABx = this.vertexPositionsX!![vertexB] - this.vertexPositionsX!![vertexA]
            val edgeABy = this.vertexPositionsY!![vertexB] - this.vertexPositionsY!![vertexA]
            val edgeABz = this.vertexPositionsZ!![vertexB] - this.vertexPositionsZ!![vertexA]

            val edgeACx = this.vertexPositionsX!![vertexC] - this.vertexPositionsX!![vertexA]
            val edgeACy = this.vertexPositionsY!![vertexC] - this.vertexPositionsY!![vertexA]
            val edgeACz = this.vertexPositionsZ!![vertexC] - this.vertexPositionsZ!![vertexA]

            var normalX = edgeABy * edgeACz - edgeACy * edgeABz
            var normalY = edgeABz * edgeACx - edgeACz * edgeABx
            var normalZ = edgeABx * edgeACy - edgeACx * edgeABy

            while (normalX > 8192 || normalY > 8192 || normalZ > 8192 || normalX < -8192 || normalY < -8192 || normalZ < -8192) {
                normalX = normalX shr 1
                normalY = normalY shr 1
                normalZ = normalZ shr 1
            }

            var length = kotlin.math.sqrt((normalX * normalX + normalY * normalY + normalZ * normalZ).toDouble()).toInt()
            if (length <= 0) length = 1

            normalX = normalX * 256 / length
            normalY = normalY * 256 / length
            normalZ = normalZ * 256 / length

            val renderType = this.triangleRenderTypes?.get(i) ?: 0

            if (renderType == 0) {
                this.vertexNormals!![vertexA].apply {
                    x += normalX
                    y += normalY
                    z += normalZ
                    magnitude++
                }
                this.vertexNormals!![vertexB].apply {
                    x += normalX
                    y += normalY
                    z += normalZ
                    magnitude++
                }
                this.vertexNormals!![vertexC].apply {
                    x += normalX
                    y += normalY
                    z += normalZ
                    magnitude++
                }
            } else if (renderType == 1) {
                if (this.faceNormals == null) {
                    this.faceNormals = Array(this.triangleCount) { FaceNormal(0, 0, 0) }
                }
                this.faceNormals!![i] = FaceNormal(normalX, normalY, normalZ)
            }
        }
    }

    fun version(): Int {
        return this.version
    }

    fun resetVersion() {
        this.version = DEFAULT_VERSION
    }

    fun forcePriority(priority: Int) {
        this.renderPriority = priority
        this.triangleRenderPriorities = null
    }

    fun position(index: Int): VertexPoint {
        val posX = requireNotNull(this.vertexPositionsX)
        val posY = requireNotNull(this.vertexPositionsY)
        val posZ = requireNotNull(this.vertexPositionsZ)
        return VertexPoint(posX[index], posY[index], posZ[index])
    }

    fun updatePriority(old: Int, new: Int) {
        val priorities = this.triangleRenderPriorities ?: return
        for (i in priorities.indices) {
            val pri = priorities[i]
            if (pri == old) {
                priorities[i] = new
            }
        }
    }

    private fun computeCoordinateToFacesMap(): Map<Int, List<TexturedFace>> {
        val map = mutableMapOf<Int, MutableList<TexturedFace>>()
        for (i in 0 until triangleCount) {
            val coordinate = textureCoordinates!![i]
            if (coordinate == -1) continue
            val unsignedCoordinate = coordinate and 0xFF
            val texture = this.triangleTextures!![i]
            map.getOrPut(unsignedCoordinate, ::mutableListOf).add(TexturedFace(i, texture))
        }
        return map
    }

    fun removeTextures() {
        this.textureTriangleCount = 0
        this.triangleTextures = null
        this.textureRenderTypes = null
        this.textureTriangleVertex1 = null
        this.textureTriangleVertex2 = null
        this.textureTriangleVertex3 = null
        this.textureScaleX = null
        this.textureScaleY = null
        this.textureScaleZ = null
        this.textureRotation = null
        this.textureDirection = null
        this.textureSpeed = null
        this.textureTransU = null
        this.textureTransV = null
        this.textureCoordinates = null
    }

    fun removeSkeletalInformation() {
        this.skeletalBones = null
        this.skeletalScales = null
        if (type == MeshType.UnversionedSkeletal) {
            type = MeshType.Unversioned
        } else if (type == MeshType.VersionedSkeletal) {
            type = MeshType.Versioned
        }
    }

    fun setSkeletalInformation(bones: Array<IntArray?>, scales: Array<IntArray?>) {
        this.skeletalBones = bones
        this.skeletalScales = scales
        if (type == MeshType.Unversioned) {
            type = MeshType.UnversionedSkeletal
        } else if (type == MeshType.Versioned) {
            type = MeshType.VersionedSkeletal
        }
    }

    fun removeParticles() {
        this.emitters = null
        this.effectors = null
    }

    fun removeBillboards() {
        this.faceBillboards = null
    }

    private fun getTexturedFaces(): List<Texture> {
        if (textureTriangleCount == 0) return emptyList()
        val textures = mutableListOf<Texture>()
        val coordinateToFacesMap = computeCoordinateToFacesMap()

        for (coordinate in 0 until textureTriangleCount) {
            val faces = coordinateToFacesMap[coordinate] ?: emptyList()
            val type = this.textureRenderTypes!![coordinate]
            val vertex1 = this.textureTriangleVertex1!![coordinate]
            val vertex2 = this.textureTriangleVertex2!![coordinate]
            val vertex3 = this.textureTriangleVertex3!![coordinate]
            if (type == SIMPLE_TEXTURE) {
                textures += SimpleTexture(
                    faces,
                    type,
                    coordinate,
                    vertex1,
                    vertex2,
                    vertex3
                )
                continue
            }
            val scaleX = this.textureScaleX!![coordinate]
            val scaleY = this.textureScaleY!![coordinate]
            val scaleZ = this.textureScaleZ!![coordinate]
            val rotation = this.textureRotation!![coordinate]
            val direction = this.textureDirection!![coordinate]
            val speed = this.textureSpeed!![coordinate]
            if (type == CYLINDRICAL_TEXTURE) {
                textures += CylindricalTexture(
                    faces,
                    type,
                    coordinate,
                    vertex1,
                    vertex2,
                    vertex3,
                    scaleX,
                    scaleY,
                    scaleZ,
                    rotation,
                    direction,
                    speed
                )
                continue
            } else if (type == SPHERICAL_TEXTURE) {
                textures += SphericalTexture(
                    faces,
                    type,
                    coordinate,
                    vertex1,
                    vertex2,
                    vertex3,
                    scaleX,
                    scaleY,
                    scaleZ,
                    rotation,
                    direction,
                    speed
                )
                continue
            }
            val transU = this.textureTransU!![coordinate]
            val transV = this.textureTransV!![coordinate]
            require(type == CUBE_TEXTURE)
            textures += CubeTexture(
                faces,
                type,
                coordinate,
                vertex1,
                vertex2,
                vertex3,
                scaleX,
                scaleY,
                scaleZ,
                rotation,
                direction,
                speed,
                transU,
                transV
            )
        }
        return textures
    }

    fun retainTextures(predicate: TextureRetainPredicate) {
        if (textureTriangleCount == 0) return
        val textures = getTexturedFaces()
        val (retained, removed) = textures.partition(predicate)
        val cubeTextures = retained.count { it is CubeTexture }
        val complexTextures = retained.count { it is ComplexTexture }
        for (texture in removed) {
            for ((face, _) in texture.faces) {
                this.textureCoordinates!![face] = -1
                this.triangleTextures!![face] = -1
            }
        }
        this.textureTriangleCount = retained.size
        if (cubeTextures == 0) {
            this.textureTransU = null
            this.textureTransV = null
        }
        if (complexTextures == 0) {
            this.textureScaleX = null
            this.textureScaleY = null
            this.textureScaleZ = null
            this.textureRotation = null
            this.textureDirection = null
            this.textureSpeed = null
        }
        if (retained.isEmpty()) {
            this.textureRenderTypes = null
            this.textureTriangleVertex1 = null
            this.textureTriangleVertex2 = null
            this.textureTriangleVertex3 = null
            return
        }

        this.textureTriangleVertex1 = IntArray(textureTriangleCount)
        this.textureTriangleVertex2 = IntArray(textureTriangleCount)
        this.textureTriangleVertex3 = IntArray(textureTriangleCount)
        this.textureRenderTypes = IntArray(textureTriangleCount)
        if (complexTextures > 0) {
            this.textureScaleX = IntArray(complexTextures)
            this.textureScaleY = IntArray(complexTextures)
            this.textureScaleZ = IntArray(complexTextures)
            this.textureRotation = IntArray(complexTextures)
            this.textureDirection = IntArray(complexTextures)
            this.textureSpeed = IntArray(complexTextures)
            if (cubeTextures > 0) {
                this.textureTransU = IntArray(cubeTextures)
                this.textureTransV = IntArray(cubeTextures)
            }
        }
        for ((index, texture) in retained.withIndex()) {
            for ((face, _) in texture.faces) {
                this.textureCoordinates!![face] = index
            }
            this.textureRenderTypes!![index] = texture.renderType
            this.textureTriangleVertex1!![index] = texture.vertex1
            this.textureTriangleVertex2!![index] = texture.vertex2
            this.textureTriangleVertex3!![index] = texture.vertex3
            if (texture is ComplexTexture) {
                this.textureScaleX!![index] = texture.scaleX
                this.textureScaleY!![index] = texture.scaleY
                this.textureScaleZ!![index] = texture.scaleZ
                this.textureRotation!![index] = texture.rotation
                this.textureDirection!![index] = texture.direction
                this.textureSpeed!![index] = texture.speed
                if (texture is CubeTexture) {
                    this.textureTransU!![index] = texture.transU
                    this.textureTransV!![index] = texture.transV
                }
            }
        }
    }

    fun translatePoints(vararg points: Pair<VertexPoint, VertexPoint>) {
        val posX = requireNotNull(this.vertexPositionsX)
        val posY = requireNotNull(this.vertexPositionsY)
        val posZ = requireNotNull(this.vertexPositionsZ)
        for ((from, to) in points) {
            for (i in 0 until this.vertexCount) {
                val x = posX[i]
                val y = posY[i]
                val z = posZ[i]
                if (-from.x == x && -from.y == y && -from.z == z) {
                    posX[i] = -to.x
                    posY[i] = -to.y
                    posZ[i] = -to.z
                }
            }
        }
    }

    fun recolor(find: Short, replacements: Short) {
        triangleColors?.let { colors ->
            for (i in 0 until triangleCount) {
                if (colors[i] == find) {
                    colors[i] = replacements
                }
            }
        }
    }

    fun retexture(find: Short, replacements: Short) {
        triangleTextures?.let { textures ->
            for (i in 0 until triangleCount) {
                if (textures[i].toShort() == find) {
                    textures[i] = replacements.toInt()
                }
            }
        }
    }

    fun moveSeparatePartsCloserTogetherZAxis(splitPoint: Int, offset: Int) {
        val posZ = this.vertexPositionsZ ?: return
        for (i in posZ.indices) {
            val pos = posZ[i]
            if (pos < splitPoint) {
                posZ[i] += offset
            } else {
                posZ[i] -= offset
            }
        }
    }

    fun removeTrianglesByColour(colour: Int) {
        val colours = this.triangleColors ?: return
        val indices = mutableSetOf<Int>()
        for (i in colours.indices) {
            val col = colours[i].toInt()
            if (col and 0xFFFF == colour and 0xFFFF) {
                indices += i
            }
        }
        this.triangleCount -= indices.size
        this.triangleVertex1 = removeIndices(triangleVertex1, indices)
        this.triangleVertex2 = removeIndices(triangleVertex2, indices)
        this.triangleVertex3 = removeIndices(triangleVertex3, indices)
        this.triangleColors = removeIndices(triangleColors, indices)
        this.triangleRenderTypes = removeIndices(triangleRenderTypes, indices)
        this.triangleRenderPriorities = removeIndices(triangleRenderPriorities, indices)
        this.textureCoordinates = removeIndices(textureCoordinates, indices)
        this.triangleTextures = removeIndices(triangleTextures, indices)
        this.triangleAlphas = removeIndices(triangleAlphas, indices)
        this.triangleSkins = removeIndices(triangleSkins, indices)
    }

    fun removeTrianglesByPriority(condition: (priority: Int) -> Boolean) {
        val priorities = this.triangleRenderPriorities ?: return
        val indices = mutableSetOf<Int>()
        for (i in priorities.indices) {
            val pri = priorities[i]
            if (condition(pri and 0xFFFF)) {
                indices += i
            }
        }
        this.triangleCount -= indices.size
        this.triangleVertex1 = removeIndices(triangleVertex1, indices)
        this.triangleVertex2 = removeIndices(triangleVertex2, indices)
        this.triangleVertex3 = removeIndices(triangleVertex3, indices)
        this.triangleColors = removeIndices(triangleColors, indices)
        this.triangleRenderTypes = removeIndices(triangleRenderTypes, indices)
        this.triangleRenderPriorities = removeIndices(triangleRenderPriorities, indices)
        this.textureCoordinates = removeIndices(textureCoordinates, indices)
        this.triangleTextures = removeIndices(triangleTextures, indices)
        this.triangleAlphas = removeIndices(triangleAlphas, indices)
        this.triangleSkins = removeIndices(triangleSkins, indices)
    }

    fun removeTrianglesBySkins(condition: (skin: Int) -> Boolean) {
        val skins = this.triangleSkins ?: return
        val indices = mutableSetOf<Int>()
        for (i in skins.indices) {
            val pri = skins[i]
            if (condition(pri)) {
                indices += i
            }
        }
        this.triangleCount -= indices.size
        this.triangleVertex1 = removeIndices(triangleVertex1, indices)
        this.triangleVertex2 = removeIndices(triangleVertex2, indices)
        this.triangleVertex3 = removeIndices(triangleVertex3, indices)
        this.triangleColors = removeIndices(triangleColors, indices)
        this.triangleRenderTypes = removeIndices(triangleRenderTypes, indices)
        this.triangleRenderPriorities = removeIndices(triangleRenderPriorities, indices)
        this.textureCoordinates = removeIndices(textureCoordinates, indices)
        this.triangleTextures = removeIndices(triangleTextures, indices)
        this.triangleAlphas = removeIndices(triangleAlphas, indices)
        this.triangleSkins = removeIndices(triangleSkins, indices)
    }

    fun addTrianglesByColour(from: ModelType, colour: Int) {
        val colours = from.triangleColors ?: return
        val indices = mutableSetOf<Int>()
        for (i in colours.indices) {
            val col = colours[i].toInt()
            if (col and 0xFFFF == colour and 0xFFFF) {
                indices += i
            }
        }
        this.triangleCount += indices.size
        this.triangleVertex1 += filterIndices(from.triangleVertex1, indices)
        this.triangleVertex2 += filterIndices(from.triangleVertex2, indices)
        this.triangleVertex3 += filterIndices(from.triangleVertex3, indices)
        this.triangleColors += filterIndices(from.triangleColors, indices)
        this.triangleRenderTypes += filterIndices(from.triangleRenderTypes, indices)
        this.triangleRenderTypes = fill(triangleRenderTypes, triangleCount, 0)
        this.triangleRenderPriorities += filterIndices(from.triangleRenderPriorities, indices)
        this.triangleRenderPriorities = fill(triangleRenderPriorities, triangleCount, if (colour == 0) 0 else 7)
        this.textureCoordinates += filterIndices(from.textureCoordinates, indices)
        this.triangleTextures += filterIndices(from.triangleTextures, indices)
        this.triangleAlphas += filterIndices(from.triangleAlphas, indices)
        this.triangleSkins += filterIndices(from.triangleSkins, indices)
    }

    private fun fill(array: IntArray?, expected: Int, fill: Int): IntArray? {
        if (array == null || array.size == expected) return null
        val list = array.toMutableList()
        while (list.size < expected) {
            list += fill
        }
        return list.toIntArray()
    }

    private operator fun IntArray?.plus(other: IntArray?): IntArray? {
        if (this == null) return other
        return (this.toList() + (other?.toList() ?: emptyList())).toIntArray()
    }

    private operator fun ShortArray?.plus(other: ShortArray?): ShortArray? {
        if (this == null) return other
        return (this.toList() + (other?.toList() ?: emptyList())).toShortArray()
    }

    private fun removeIndices(array: IntArray?, indices: Set<Int>): IntArray? {
        if (array == null) return null
        val newResult = mutableListOf<Int>()
        for (i in array.indices) {
            if (i in indices) continue
            newResult += array[i]
        }
        return newResult.toIntArray()
    }

    private fun removeIndices(array: ShortArray?, indices: Set<Int>): ShortArray? {
        if (array == null) return null
        val newResult = mutableListOf<Short>()
        for (i in array.indices) {
            if (i in indices) continue
            newResult += array[i]
        }
        return newResult.toShortArray()
    }

    private fun filterIndices(array: IntArray?, indices: Set<Int>): IntArray? {
        if (array == null) return null
        val newResult = mutableListOf<Int>()
        for (i in array.indices) {
            if (i !in indices) continue
            newResult += array[i]
        }
        return newResult.toIntArray()
    }

    private fun filterIndices(array: ShortArray?, indices: Set<Int>): ShortArray? {
        if (array == null) return null
        val newResult = mutableListOf<Short>()
        for (i in array.indices) {
            if (i !in indices) continue
            newResult += array[i]
        }
        return newResult.toShortArray()
    }

    fun setVertexPositionByVertexIndex(index: Int, posX: Int, posY: Int, posZ: Int) {
        val triangleVertex1 = requireNotNull(this.triangleVertex1)
        val triangleVertex2 = requireNotNull(this.triangleVertex2)
        val triangleVertex3 = requireNotNull(this.triangleVertex3)
        val vertexPositionsX = requireNotNull(this.vertexPositionsX)
        val vertexPositionsY = requireNotNull(this.vertexPositionsY)
        val vertexPositionsZ = requireNotNull(this.vertexPositionsZ)
        /* All the arrays must be the same length so we can only check it on one of them. */
        require(index in triangleVertex1.indices)
        vertexPositionsX[triangleVertex1[index]] = posX
        vertexPositionsY[triangleVertex2[index]] = posY
        vertexPositionsZ[triangleVertex3[index]] = posZ
    }

    fun transformVertexSkins(transformer: (src: Int) -> Int) {
        val skins = requireNotNull(this.vertexSkins)
        for (i in skins.indices) {
            val current = skins[i]
            val result = transformer(current)
            skins[i] = result
        }
    }

    fun fillVertexSkins(value: Int) {
        vertexSkins = IntArray(vertexCount) { value }
    }

    fun translate(offsetX: Int, offsetY: Int, offsetZ: Int) {
        val vertexPositionsX = requireNotNull(this.vertexPositionsX)
        val vertexPositionsY = requireNotNull(this.vertexPositionsY)
        val vertexPositionsZ = requireNotNull(this.vertexPositionsZ)
        for (i in vertexPositionsX.indices) {
            vertexPositionsX[i] += offsetX
            vertexPositionsY[i] += offsetY
            vertexPositionsZ[i] += offsetZ
        }
    }

    fun resize(x: Int, y: Int, z: Int) {
        require(x >= 0)
        require(y >= 0)
        require(z >= 0)
        val vertexPositionsX = requireNotNull(this.vertexPositionsX)
        val vertexPositionsY = requireNotNull(this.vertexPositionsY)
        val vertexPositionsZ = requireNotNull(this.vertexPositionsZ)
        for (i in vertexPositionsX.indices) {
            vertexPositionsX[i] = vertexPositionsX[i] * x / 128
            vertexPositionsY[i] = vertexPositionsY[i] * y / 128
            vertexPositionsZ[i] = vertexPositionsZ[i] * z / 128
        }
    }

    /**
     * Rotates the model by the provided [xan], [yan] and [zan] values.
     * The values are expected to be in range of 0 to 2047(inclusive), and will be sanitized to be within them.
     * @param xan defines the rotation on the horizontal axis (think of it as a chathead looking left/right)
     * @param yan defines the rotation on the vertical axis (think of it as a chathead looking up or down)
     * @param zan defines the tilt of the model (think of it as a chathead tilting its head to the right or left)
     */
    fun rotate(xan: Int, yan: Int, zan: Int) {
        val vertexPositionsX = requireNotNull(this.vertexPositionsX)
        val vertexPositionsY = requireNotNull(this.vertexPositionsY)
        val vertexPositionsZ = requireNotNull(this.vertexPositionsZ)
        for (i in 0 until vertexCount) {
            if (xan != 0) {
                val sin = SINE[xan and 0x7FF]
                val cos = COSINE[xan and 0x7FF]
                val temp = sin * vertexPositionsZ[i] + cos * vertexPositionsX[i] shr 16
                vertexPositionsZ[i] = cos * vertexPositionsZ[i] - sin * vertexPositionsX[i] shr 16
                vertexPositionsX[i] = temp
            }

            if (yan != 0) {
                val sin = SINE[yan and 0x7FF]
                val cos = COSINE[yan and 0x7FF]
                val temp = cos * vertexPositionsY[i] - sin * vertexPositionsZ[i] shr 16
                vertexPositionsZ[i] = sin * vertexPositionsY[i] + cos * vertexPositionsZ[i] shr 16
                vertexPositionsY[i] = temp
            }

            if (zan != 0) {
                val sin = SINE[zan and 0x7FF]
                val cos = COSINE[zan and 0x7FF]
                val temp = sin * vertexPositionsY[i] + cos * vertexPositionsX[i] shr 16
                vertexPositionsY[i] = cos * vertexPositionsY[i] - sin * vertexPositionsX[i] shr 16
                vertexPositionsX[i] = temp
            }
        }
    }

    fun append(models: Collection<ModelType>) {
        vertexCount = 0
        triangleCount = 0
        renderPriority = 0
        var hasTriangleRenderTypes = false
        var hasTriangleRenderPriorities = false
        var hasTriangleAlphas = false
        var hasTriangleSkins = false
        var hasTriangleTextures = false
        var hasTextureCoordinates = false
        var hasSkeletalInfo = false
        vertexCount = 0
        triangleCount = 0
        textureTriangleCount = 0
        renderPriority = -1

        var emittersCount = 0
        var effectorsCount = 0
        var billboardsCount = 0
        for (model in models) {
            vertexCount += model.vertexCount
            triangleCount += model.triangleCount
            textureTriangleCount += model.textureTriangleCount
            if (model.effectors != null) {
                effectorsCount += model.effectors!!.size
            }
            if (model.faceBillboards != null) {
                billboardsCount += model.faceBillboards!!.size
            }
            if (model.emitters != null) {
                emittersCount += model.emitters!!.size
            }
            if (model.triangleRenderPriorities != null) {
                hasTriangleRenderPriorities = true
            } else {
                if (renderPriority == -1) {
                    renderPriority = model.renderPriority
                }
                if (renderPriority != model.renderPriority) {
                    hasTriangleRenderPriorities = true
                }
            }

            hasTriangleRenderTypes = hasTriangleRenderTypes or (model.triangleRenderTypes != null)
            hasTriangleAlphas = hasTriangleAlphas or (model.triangleAlphas != null)
            hasTriangleSkins = hasTriangleSkins or (model.triangleSkins != null)
            hasTriangleTextures = hasTriangleTextures or (model.triangleTextures != null)
            hasTextureCoordinates = hasTextureCoordinates or (model.textureCoordinates != null)
            hasSkeletalInfo = hasSkeletalInfo or (model.skeletalBones != null)
        }

        vertexPositionsX = IntArray(vertexCount)
        vertexPositionsY = IntArray(vertexCount)
        vertexPositionsZ = IntArray(vertexCount)
        vertexSkins = IntArray(vertexCount)
        triangleVertex1 = IntArray(triangleCount)
        triangleVertex2 = IntArray(triangleCount)
        triangleVertex3 = IntArray(triangleCount)
        var effectors: Array<EffectiveVertex?>? = null
        var emitters: Array<EmissiveTriangle?>? = null
        var billboards: Array<FaceBillboard?>? = null
        if (effectorsCount > 0) {
            effectors = arrayOfNulls(effectorsCount)
        }
        if (emittersCount > 0) {
            emitters = arrayOfNulls(emittersCount)
        }
        if (billboardsCount > 0) {
            billboards = arrayOfNulls(billboardsCount)
        }
        if (hasTriangleRenderTypes) {
            triangleRenderTypes = IntArray(triangleCount)
        }

        if (hasTriangleRenderPriorities) {
            triangleRenderPriorities = IntArray(triangleCount)
        }

        if (hasTriangleAlphas) {
            triangleAlphas = IntArray(triangleCount)
        }

        if (hasTriangleSkins) {
            triangleSkins = IntArray(triangleCount)
        }

        if (hasTriangleTextures) {
            triangleTextures = IntArray(triangleCount)
        }

        if (hasTextureCoordinates) {
            textureCoordinates = IntArray(triangleCount)
        }

        if (hasSkeletalInfo) {
            skeletalBones = arrayOfNulls(vertexCount)
            skeletalScales = arrayOfNulls(vertexCount)
        }

        triangleColors = ShortArray(triangleCount)
        if (textureTriangleCount > 0) {
            textureScaleZ = IntArray(textureTriangleCount)
            textureRotation = IntArray(textureTriangleCount)
            textureScaleY = IntArray(textureTriangleCount)
            textureTriangleVertex3 = IntArray(textureTriangleCount)
            textureTriangleVertex1 = IntArray(textureTriangleCount)
            textureScaleX = IntArray(textureTriangleCount)
            textureTriangleVertex2 = IntArray(textureTriangleCount)
            textureTransU = IntArray(textureTriangleCount)
            textureRenderTypes = IntArray(textureTriangleCount)
            textureDirection = IntArray(textureTriangleCount)
            textureSpeed = IntArray(textureTriangleCount)
            textureTransV = IntArray(textureTriangleCount)
        }

        vertexCount = 0
        triangleCount = 0
        textureTriangleCount = 0
        emittersCount = 0
        effectorsCount = 0
        billboardsCount = 0
        for (model in models) {
            for (index in 0 until model.triangleCount) {
                if (model.faceBillboards != null) {
                    for (i in 0 until model.faceBillboards!!.size) {
                        val billboard = model.faceBillboards!![i]
                        billboards!![billboardsCount++] = FaceBillboard(billboard.id, billboard.face + triangleCount, billboard.skin, billboard.distance)
                    }
                }
                if (hasTriangleRenderTypes && model.triangleRenderTypes != null) {
                    triangleRenderTypes!![triangleCount] = model.triangleRenderTypes!![index]
                }
                if (hasTriangleRenderPriorities) {
                    if (model.triangleRenderPriorities != null) {
                        triangleRenderPriorities!![triangleCount] = model.triangleRenderPriorities!![index]
                    } else {
                        triangleRenderPriorities!![triangleCount] = model.renderPriority
                    }
                }
                if (hasTriangleAlphas && model.triangleAlphas != null) {
                    triangleAlphas!![triangleCount] = model.triangleAlphas!![index]
                }
                if (hasTriangleSkins && model.triangleSkins != null) {
                    triangleSkins!![triangleCount] = model.triangleSkins!![index]
                }
                if (hasTriangleTextures) {
                    if (model.triangleTextures != null) {
                        triangleTextures!![triangleCount] = model.triangleTextures!![index]
                    } else {
                        triangleTextures!![triangleCount] = -1
                    }
                }
                if (hasTextureCoordinates) {
                    if (model.textureCoordinates != null && model.textureCoordinates!![index] != -1) {
                        textureCoordinates!![triangleCount] = (textureTriangleCount + model.textureCoordinates!![index]).toByte().toInt()
                    } else {
                        textureCoordinates!![triangleCount] = -1
                    }
                }
                triangleColors!![triangleCount] = model.triangleColors!![index]
                triangleVertex1!![triangleCount] = this.findVertex(model, model.triangleVertex1!![index])
                triangleVertex2!![triangleCount] = this.findVertex(model, model.triangleVertex2!![index])
                triangleVertex3!![triangleCount] = this.findVertex(model, model.triangleVertex3!![index])
                ++triangleCount
            }
            if (emitters != null) {
                for (index in emitters.indices) {
                    val emitter = model.emitters!![index]
                    val vertex1 = findVertex(model, emitter.s)
                    val vertex2 = findVertex(model, emitter.t)
                    val vertex3 = findVertex(model, emitter.u)
                    emitters[emittersCount] = EmissiveTriangle(emitter.emitter, emitter.face, vertex1, vertex2, vertex3, emitter.priority)
                    emittersCount++
                }
            }
            if (effectors != null) {
                for (index in effectors.indices) {
                    val effector = model.effectors!![index]
                    val vertex = findVertex(model, effector.vertex)
                    effectors[effectorsCount] = EffectiveVertex(effector.effector, vertex)
                    effectorsCount++
                }
            }
        }
        var count = 0
        for (model in models) {
            for (index in 0 until model.triangleCount) {
                if (hasTextureCoordinates) {
                    textureCoordinates!![count++] =
                        (
                                if (model.textureCoordinates == null || model.textureCoordinates!![index] == -1) {
                                    -1
                                } else {
                                    model.textureCoordinates!![index] + textureTriangleCount
                                }
                                ).toByte().toInt()
                }
            }
            for (index in 0 until model.textureTriangleCount) {
                textureRenderTypes!![textureTriangleCount] = model.textureRenderTypes!![index]
                val type = textureRenderTypes!![textureTriangleCount].toByte()
                if (type.toInt() == 0) {
                    textureTriangleVertex1!![textureTriangleCount] = findVertex(model, model.textureTriangleVertex1!![index]).toShort().toInt()
                    textureTriangleVertex2!![textureTriangleCount] = findVertex(model, model.textureTriangleVertex2!![index]).toShort().toInt()
                    textureTriangleVertex3!![textureTriangleCount] = findVertex(model, model.textureTriangleVertex3!![index]).toShort().toInt()
                }
                if (type in 1..3) {
                    textureTriangleVertex1!![textureTriangleCount] = model.textureTriangleVertex1!![index]
                    textureTriangleVertex2!![textureTriangleCount] = model.textureTriangleVertex2!![index]
                    textureTriangleVertex3!![textureTriangleCount] = model.textureTriangleVertex3!![index]
                    textureScaleZ!![textureTriangleCount] = model.textureScaleZ!![index]
                    textureSpeed!![textureTriangleCount] = model.textureSpeed!![index]
                    textureScaleX!![textureTriangleCount] = model.textureScaleX!![index]
                    textureRotation!![textureTriangleCount] = model.textureRotation!![index]
                    textureScaleY!![textureTriangleCount] = model.textureScaleY!![index]
                    textureDirection!![textureTriangleCount] = model.textureDirection!![index]
                }
                if (type.toInt() == 2) {
                    textureTransU!![textureTriangleCount] = model.textureTransU!![index]
                    textureTransV!![textureTriangleCount] = model.textureTransV!![index]
                }
                textureTriangleCount++
            }
        }
        sortTextures()
        if (emitters != null) {
            this.emitters = emitters.requireNoNulls()
        }
        if (effectors != null) {
            this.effectors = effectors.requireNoNulls()
        }
        if (billboards != null) {
            this.faceBillboards = billboards.requireNoNulls()
        }
    }

    private fun sortTextures() {
        if (textureTriangleCount <= 0) return
        var simpleTextureFaceCount = 0
        var complexTextureFaceCount = 0
        var cubeTextureFaceCount = 0
        for (index in 0 until textureTriangleCount) {
            val type = textureRenderTypes!![index]
            if (type == SIMPLE_TEXTURE) {
                simpleTextureFaceCount++
            }
            if (type == CUBE_TEXTURE) {
                cubeTextureFaceCount++
            }
            if (type in COMPLEX_TEXTURE_RANGE) {
                complexTextureFaceCount++
            }
        }

        val textureTriangleVertex1 = IntArray(textureTriangleCount)
        val textureTriangleVertex2 = IntArray(textureTriangleCount)
        val textureTriangleVertex3 = IntArray(textureTriangleCount)
        val textureRenderTypes = IntArray(textureTriangleCount)
        var count = 0
        if (complexTextureFaceCount > 0) {
            val textureScaleX = IntArray(complexTextureFaceCount)
            val textureScaleY = IntArray(complexTextureFaceCount)
            val textureScaleZ = IntArray(complexTextureFaceCount)
            val textureRotation = IntArray(complexTextureFaceCount)
            val textureDirection = IntArray(complexTextureFaceCount)
            val textureSpeed = IntArray(complexTextureFaceCount)
            if (cubeTextureFaceCount > 0) {
                val textureTransU = IntArray(cubeTextureFaceCount)
                val textureTransV = IntArray(cubeTextureFaceCount)
                for (index in 0 until textureTriangleCount) {
                    val type = this.textureRenderTypes!![index]
                    if (type == CUBE_TEXTURE) {
                        textureTriangleVertex1[count] = this.textureTriangleVertex1!![index]
                        textureTriangleVertex2[count] = this.textureTriangleVertex2!![index]
                        textureTriangleVertex3[count] = this.textureTriangleVertex3!![index]
                        textureScaleX[count] = this.textureScaleX!![index]
                        textureScaleY[count] = this.textureScaleY!![index]
                        textureScaleZ[count] = this.textureScaleZ!![index]
                        textureRotation[count] = this.textureRotation!![index]
                        textureDirection[count] = this.textureDirection!![index]
                        textureSpeed[count] = this.textureSpeed!![index]
                        textureTransU[count] = this.textureTransU!![index]
                        textureTransV[count] = this.textureTransV!![index]
                        textureRenderTypes[count] = this.textureRenderTypes!![index]
                        count++
                    }
                }
                this.textureTransU = textureTransU
                this.textureTransV = textureTransV
            }
            for (index in 0 until textureTriangleCount) {
                val type = this.textureRenderTypes!![index]
                if (type != CUBE_TEXTURE && type in COMPLEX_TEXTURE_RANGE) {
                    textureTriangleVertex1[count] = this.textureTriangleVertex1!![index]
                    textureTriangleVertex2[count] = this.textureTriangleVertex2!![index]
                    textureTriangleVertex3[count] = this.textureTriangleVertex3!![index]
                    textureScaleX[count] = this.textureScaleX!![index]
                    textureScaleY[count] = this.textureScaleY!![index]
                    textureScaleZ[count] = this.textureScaleZ!![index]
                    textureRotation[count] = this.textureRotation!![index]
                    textureDirection[count] = this.textureDirection!![index]
                    textureSpeed[count] = this.textureSpeed!![index]
                    textureRenderTypes[count] = this.textureRenderTypes!![index]
                    count++
                }
            }
            this.textureScaleX = textureScaleX
            this.textureScaleY = textureScaleY
            this.textureScaleZ = textureScaleZ
            this.textureRotation = textureRotation
            this.textureDirection = textureDirection
            this.textureSpeed = textureSpeed
        }
        for (index in 0 until textureTriangleCount) {
            val type = this.textureRenderTypes!![index]
            if (type == SIMPLE_TEXTURE) {
                textureTriangleVertex1[count] = this.textureTriangleVertex1!![index]
                textureTriangleVertex2[count] = this.textureTriangleVertex2!![index]
                textureTriangleVertex3[count] = this.textureTriangleVertex3!![index]
                textureRenderTypes[count] = this.textureRenderTypes!![index]
                count++
            }
        }
        this.textureTriangleVertex1 = textureTriangleVertex1
        this.textureTriangleVertex2 = textureTriangleVertex2
        this.textureTriangleVertex3 = textureTriangleVertex3
        this.textureRenderTypes = textureRenderTypes
    }

    private fun findVertex(var1: ModelType, var2: Int): Int {
        var var3 = -1
        val var4 = var1.vertexPositionsX!![var2]
        val var5 = var1.vertexPositionsY!![var2]
        val var6 = var1.vertexPositionsZ!![var2]

        for (var7 in 0 until vertexCount) {
            if (var4 == vertexPositionsX!![var7] && var5 == vertexPositionsY!![var7] && var6 == vertexPositionsZ!![var7]) {
                var3 = var7
                break
            }
        }

        if (var3 == -1) {
            vertexPositionsX!![vertexCount] = var4
            vertexPositionsY!![vertexCount] = var5
            vertexPositionsZ!![vertexCount] = var6
            if (var1.vertexSkins != null) {
                vertexSkins!![vertexCount] = var1.vertexSkins!![var2]
            } else if (this.vertexSkins != null) {
                this.vertexSkins!![vertexCount] = -1
            }
            if (var1.skeletalBones != null) {
                skeletalBones!![vertexCount] = var1.skeletalBones!![var2]
                skeletalScales!![vertexCount] = var1.skeletalScales!![var2]
            }
            var3 = vertexCount++
        }

        return var3
    }


    fun toModel(ambientLight: Int, contrastLevel: Int, xDirection: Int, yDirection: Int, zDirection: Int): Model {
        computeNormals()

        val directionMagnitude = sqrt((zDirection * zDirection + xDirection * xDirection + yDirection * yDirection).toDouble()).toInt()
        val contrastAdjusted = directionMagnitude * contrastLevel shr 8

        val illuminatedModel = Model()
        illuminatedModel.faceColors1 = IntArray(triangleCount)
        illuminatedModel.faceColors2 = IntArray(triangleCount)
        illuminatedModel.faceColors3 = IntArray(triangleCount)

        if (textureTriangleCount > 0 && textureCoordinates != null) {
            val textureTriangleCountt = IntArray(textureTriangleCount)
            var triangleIndex = 0

            while (triangleIndex < triangleCount) {
                if (textureCoordinates!![triangleIndex].toInt() != -1) {
                    ++textureTriangleCountt[textureCoordinates!![triangleIndex].toInt() and 255]
                }
                ++triangleIndex
            }

            illuminatedModel.numTextureFaces = 0
            triangleIndex = 0
            while (triangleIndex < textureTriangleCount) {
                if (textureTriangleCountt[triangleIndex] > 0 && textureRenderTypes!![triangleIndex].toInt() == 0) {
                    ++illuminatedModel.numTextureFaces
                }
                ++triangleIndex
            }

            illuminatedModel.texIndices1 = IntArray(illuminatedModel.numTextureFaces)
            illuminatedModel.texIndices2 = IntArray(illuminatedModel.numTextureFaces)
            illuminatedModel.texIndices3 = IntArray(illuminatedModel.numTextureFaces)

            triangleIndex = 0
            for (i in 0 until textureTriangleCount) {
                if (textureTriangleCountt[i] > 0 && textureRenderTypes!![i].toInt() == 0) {
                    illuminatedModel.texIndices1[triangleIndex] = textureTriangleVertex1!![i].toInt() and '\uffff'.code
                    illuminatedModel.texIndices2[triangleIndex] = textureTriangleVertex2!![i].toInt() and '\uffff'.code
                    illuminatedModel.texIndices3[triangleIndex] = textureTriangleVertex3!![i].toInt() and '\uffff'.code
                    textureTriangleCountt[i] = triangleIndex++
                } else {
                    textureTriangleCountt[i] = -1
                }
            }

            illuminatedModel.textureCoords = ByteArray(triangleCount)
            for (i in 0 until triangleCount) {
                if (textureCoordinates!![i].toInt() != -1) {
                    illuminatedModel.textureCoords!![i] = textureTriangleCountt[textureCoordinates!![i].toInt() and 255].toByte()
                } else {
                    illuminatedModel.textureCoords!![i] = -1
                }
            }
        }

        for (faceIdx in 0 until triangleCount) {
            var renderType: Int = if (triangleRenderTypes == null) 0 else triangleRenderTypes!![faceIdx]
            var alpha: Int = if (triangleAlphas == null) 0 else triangleAlphas!![faceIdx]
            var textureId: Int = if (triangleTextures == null) -1 else triangleTextures!![faceIdx]

            if (alpha == -2) {
                renderType = 3
            }

            if (alpha == -1) {
                renderType = 2
            }

            var vertexNormal: VertexNormal
            var temp: Int
            var faceNormal: FaceNormal

            if (textureId == -1) {
                if (renderType != 0) {
                    if (renderType == 1) {
                        faceNormal = faceNormals!![faceIdx]
                        temp = (yDirection * faceNormal.y + zDirection * faceNormal.z + xDirection * faceNormal.x) / (contrastAdjusted / 2 + contrastAdjusted) + ambientLight
                        illuminatedModel.faceColors1[faceIdx] = adjustColorBrightness(triangleColors!![faceIdx].toInt() and '\uffff'.code, temp)
                        illuminatedModel.faceColors3[faceIdx] = -1
                    } else if (renderType == 3) {
                        illuminatedModel.faceColors1[faceIdx] = 128
                        illuminatedModel.faceColors3[faceIdx] = -1
                    } else {
                        illuminatedModel.faceColors3[faceIdx] = -2
                    }
                } else {
                    val color = triangleColors!![faceIdx].toInt() and '\uffff'.code
                    vertexNormal = vertexNormals!![triangleVertex1!![faceIdx]]
                    temp = (yDirection * vertexNormal.y + zDirection * vertexNormal.z + xDirection * vertexNormal.x) / (contrastAdjusted * vertexNormal.magnitude) + ambientLight
                    illuminatedModel.faceColors1[faceIdx] = adjustColorBrightness(color, temp)

                    vertexNormal = vertexNormals!![triangleVertex2!![faceIdx]]
                    temp = (yDirection * vertexNormal.y + zDirection * vertexNormal.z + xDirection * vertexNormal.x) / (contrastAdjusted * vertexNormal.magnitude) + ambientLight
                    illuminatedModel.faceColors2[faceIdx] = adjustColorBrightness(color, temp)

                    vertexNormal = vertexNormals!![triangleVertex3!![faceIdx]]
                    temp = (yDirection * vertexNormal.y + zDirection * vertexNormal.z + xDirection * vertexNormal.x) / (contrastAdjusted * vertexNormal.magnitude) + ambientLight
                    illuminatedModel.faceColors3[faceIdx] = adjustColorBrightness(color, temp)
                }
            } else if (renderType != 0) {
                if (renderType == 1) {
                    faceNormal = faceNormals!![faceIdx]
                    temp = (yDirection * faceNormal.y + zDirection * faceNormal.z + xDirection * faceNormal.x) / (contrastAdjusted / 2 + contrastAdjusted) + ambientLight
                    illuminatedModel.faceColors1[faceIdx] = boundToRange(temp)
                    illuminatedModel.faceColors3[faceIdx] = -1
                } else {
                    illuminatedModel.faceColors3[faceIdx] = -2
                }
            } else {
                vertexNormal = vertexNormals!![triangleVertex1!![faceIdx]]
                temp = (yDirection * vertexNormal.y + zDirection * vertexNormal.z + xDirection * vertexNormal.x) / (contrastAdjusted * vertexNormal.magnitude) + ambientLight
                illuminatedModel.faceColors1[faceIdx] = boundToRange(temp)

                vertexNormal = vertexNormals!![triangleVertex2!![faceIdx]]
                temp = (yDirection * vertexNormal.y + zDirection * vertexNormal.z + xDirection * vertexNormal.x) / (contrastAdjusted * vertexNormal.magnitude) + ambientLight
                illuminatedModel.faceColors2[faceIdx] = boundToRange(temp)

                vertexNormal = vertexNormals!![triangleVertex3!![faceIdx]]
                temp = (yDirection * vertexNormal.y + zDirection * vertexNormal.z + xDirection * vertexNormal.x) / (contrastAdjusted * vertexNormal.magnitude) + ambientLight
                illuminatedModel.faceColors3[faceIdx] = boundToRange(temp)
            }
        }

        illuminatedModel.vertexCount = vertexCount
        illuminatedModel.verticesX = vertexPositionsX!!
        illuminatedModel.verticesY = vertexPositionsY!!
        illuminatedModel.verticesZ = vertexPositionsZ!!
        illuminatedModel.triangleCount = triangleCount
        illuminatedModel.triangleVertex1 = triangleVertex1!!
        illuminatedModel.triangleVertex2 = triangleVertex2!!
        illuminatedModel.triangleVertex3 = triangleVertex3!!
        illuminatedModel.facePriorities = triangleRenderPriorities
        illuminatedModel.triangleAlphas = triangleAlphas
        illuminatedModel.faceTextures = triangleTextures

        return illuminatedModel
    }

    fun adjustColorBrightness(colour: Int, var1: Int): Int {
        var var1 = var1
        var1 = ((colour and 127) * var1) shr 7
        var1 = boundToRange(var1)

        return (colour and 65408) + var1
    }

    fun boundToRange(var0: Int): Int {
        var var0 = var0
        if (var0 < 2) {
            var0 = 2
        } else if (var0 > 126) {
            var0 = 126
        }

        return var0
    }

    override fun toString(): String {
        return "Model(" +
                "id=$id, " +
                "vertexCount=$vertexCount, " +
                "triangleCount=$triangleCount, " +
                "textureTriangleCount=$textureTriangleCount, " +
                "version=$version, " +
                "type=$type, " +
                "renderPriority=$renderPriority, " +
                "triangleColors=${triangleColors?.contentToString()}, " +
                "triangleAlphas=${triangleAlphas?.contentToString()}, " +
                "triangleSkins=${triangleSkins?.contentToString()}, " +
                "triangleRenderTypes=${triangleRenderTypes?.contentToString()}, " +
                "triangleRenderPriorities=${triangleRenderPriorities?.contentToString()}, " +
                "triangleVertex1=${triangleVertex1?.contentToString()}, " +
                "triangleVertex2=${triangleVertex2?.contentToString()}, " +
                "triangleVertex3=${triangleVertex3?.contentToString()}, " +
                "vertexPositionsX=${vertexPositionsX?.contentToString()}, " +
                "vertexPositionsY=${vertexPositionsY?.contentToString()}, " +
                "vertexPositionsZ=${vertexPositionsZ?.contentToString()}, " +
                "vertexSkins=${vertexSkins?.contentToString()}, " +
                "triangleTextures=${triangleTextures?.contentToString()}, " +
                "textureRenderTypes=${textureRenderTypes?.contentToString()}, " +
                "textureTriangleVertex1=${textureTriangleVertex1?.contentToString()}, " +
                "textureTriangleVertex2=${textureTriangleVertex2?.contentToString()}, " +
                "textureTriangleVertex3=${textureTriangleVertex3?.contentToString()}, " +
                "textureScaleX=${textureScaleX?.contentToString()}, " +
                "textureScaleY=${textureScaleY?.contentToString()}, " +
                "textureScaleZ=${textureScaleZ?.contentToString()}, " +
                "textureRotation=${textureRotation?.contentToString()}, " +
                "textureDirection=${textureDirection?.contentToString()}, " +
                "textureSpeed=${textureSpeed?.contentToString()}, " +
                "textureTransU=${textureTransU?.contentToString()}, " +
                "textureTransV=${textureTransV?.contentToString()}, " +
                "textureCoordinates=${textureCoordinates?.contentToString()}, " +
                "skeletalBones=${skeletalBones?.contentToString()}, " +
                "skeletalScales=${skeletalScales?.contentToString()}, " +
                "emitters=${emitters?.contentToString()}, " +
                "effectors=${effectors?.contentToString()}, " +
                "faceBillboards=${faceBillboards?.contentToString()}" +
                ")"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModelType

        if (id != other.id) return false
        if (vertexCount != other.vertexCount) return false
        if (triangleCount != other.triangleCount) return false
        if (textureTriangleCount != other.textureTriangleCount) return false
        if (version != other.version) return false
        if (type != other.type) return false
        if (renderPriority != other.renderPriority) return false
        if (triangleColors != null) {
            if (other.triangleColors == null) return false
            if (!triangleColors.contentEquals(other.triangleColors)) return false
        } else if (other.triangleColors != null) return false
        if (triangleAlphas != null) {
            if (other.triangleAlphas == null) return false
            if (!triangleAlphas.contentEquals(other.triangleAlphas)) return false
        } else if (other.triangleAlphas != null) return false
        if (triangleSkins != null) {
            if (other.triangleSkins == null) return false
            if (!triangleSkins.contentEquals(other.triangleSkins)) return false
        } else if (other.triangleSkins != null) return false
        if (triangleRenderTypes != null) {
            if (other.triangleRenderTypes == null) return false
            if (!triangleRenderTypes.contentEquals(other.triangleRenderTypes)) return false
        } else if (other.triangleRenderTypes != null) return false
        if (triangleRenderPriorities != null) {
            if (other.triangleRenderPriorities == null) return false
            if (!triangleRenderPriorities.contentEquals(other.triangleRenderPriorities)) return false
        } else if (other.triangleRenderPriorities != null) return false
        if (triangleVertex1 != null) {
            if (other.triangleVertex1 == null) return false
            if (!triangleVertex1.contentEquals(other.triangleVertex1)) return false
        } else if (other.triangleVertex1 != null) return false
        if (triangleVertex2 != null) {
            if (other.triangleVertex2 == null) return false
            if (!triangleVertex2.contentEquals(other.triangleVertex2)) return false
        } else if (other.triangleVertex2 != null) return false
        if (triangleVertex3 != null) {
            if (other.triangleVertex3 == null) return false
            if (!triangleVertex3.contentEquals(other.triangleVertex3)) return false
        } else if (other.triangleVertex3 != null) return false
        if (vertexPositionsX != null) {
            if (other.vertexPositionsX == null) return false
            if (!vertexPositionsX.contentEquals(other.vertexPositionsX)) return false
        } else if (other.vertexPositionsX != null) return false
        if (vertexPositionsY != null) {
            if (other.vertexPositionsY == null) return false
            if (!vertexPositionsY.contentEquals(other.vertexPositionsY)) return false
        } else if (other.vertexPositionsY != null) return false
        if (vertexPositionsZ != null) {
            if (other.vertexPositionsZ == null) return false
            if (!vertexPositionsZ.contentEquals(other.vertexPositionsZ)) return false
        } else if (other.vertexPositionsZ != null) return false
        if (vertexSkins != null) {
            if (other.vertexSkins == null) return false
            if (!vertexSkins.contentEquals(other.vertexSkins)) return false
        } else if (other.vertexSkins != null) return false
        if (triangleTextures != null) {
            if (other.triangleTextures == null) return false
            if (!triangleTextures.contentEquals(other.triangleTextures)) return false
        } else if (other.triangleTextures != null) return false
        if (textureRenderTypes != null) {
            if (other.textureRenderTypes == null) return false
            if (!textureRenderTypes.contentEquals(other.textureRenderTypes)) return false
        } else if (other.textureRenderTypes != null) return false
        if (textureTriangleVertex1 != null) {
            if (other.textureTriangleVertex1 == null) return false
            if (!textureTriangleVertex1.contentEquals(other.textureTriangleVertex1)) return false
        } else if (other.textureTriangleVertex1 != null) return false
        if (textureTriangleVertex2 != null) {
            if (other.textureTriangleVertex2 == null) return false
            if (!textureTriangleVertex2.contentEquals(other.textureTriangleVertex2)) return false
        } else if (other.textureTriangleVertex2 != null) return false
        if (textureTriangleVertex3 != null) {
            if (other.textureTriangleVertex3 == null) return false
            if (!textureTriangleVertex3.contentEquals(other.textureTriangleVertex3)) return false
        } else if (other.textureTriangleVertex3 != null) return false
        if (textureScaleX != null) {
            if (other.textureScaleX == null) return false
            if (!textureScaleX.contentEquals(other.textureScaleX)) return false
        } else if (other.textureScaleX != null) return false
        if (textureScaleY != null) {
            if (other.textureScaleY == null) return false
            if (!textureScaleY.contentEquals(other.textureScaleY)) return false
        } else if (other.textureScaleY != null) return false
        if (textureScaleZ != null) {
            if (other.textureScaleZ == null) return false
            if (!textureScaleZ.contentEquals(other.textureScaleZ)) return false
        } else if (other.textureScaleZ != null) return false
        if (textureRotation != null) {
            if (other.textureRotation == null) return false
            if (!textureRotation.contentEquals(other.textureRotation)) return false
        } else if (other.textureRotation != null) return false
        if (textureDirection != null) {
            if (other.textureDirection == null) return false
            if (!textureDirection.contentEquals(other.textureDirection)) return false
        } else if (other.textureDirection != null) return false
        if (textureSpeed != null) {
            if (other.textureSpeed == null) return false
            if (!textureSpeed.contentEquals(other.textureSpeed)) return false
        } else if (other.textureSpeed != null) return false
        if (textureTransU != null) {
            if (other.textureTransU == null) return false
            if (!textureTransU.contentEquals(other.textureTransU)) return false
        } else if (other.textureTransU != null) return false
        if (textureTransV != null) {
            if (other.textureTransV == null) return false
            if (!textureTransV.contentEquals(other.textureTransV)) return false
        } else if (other.textureTransV != null) return false
        if (textureCoordinates != null) {
            if (other.textureCoordinates == null) return false
            if (!textureCoordinates.contentEquals(other.textureCoordinates)) return false
        } else if (other.textureCoordinates != null) return false
        if (skeletalBones != null) {
            if (other.skeletalBones == null) return false
            if (!skeletalBones.contentDeepEquals(other.skeletalBones)) return false
        } else if (other.skeletalBones != null) return false
        if (skeletalScales != null) {
            if (other.skeletalScales == null) return false
            if (!skeletalScales.contentDeepEquals(other.skeletalScales)) return false
        } else if (other.skeletalScales != null) return false
        if (emitters != null) {
            if (other.emitters == null) return false
            if (!emitters.contentEquals(other.emitters)) return false
        } else if (other.emitters != null) return false
        if (effectors != null) {
            if (other.effectors == null) return false
            if (!effectors.contentEquals(other.effectors)) return false
        } else if (other.effectors != null) return false
        if (faceBillboards != null) {
            if (other.faceBillboards == null) return false
            if (!faceBillboards.contentEquals(other.faceBillboards)) return false
        } else if (other.faceBillboards != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + vertexCount
        result = 31 * result + triangleCount
        result = 31 * result + textureTriangleCount
        result = 31 * result + version
        result = 31 * result + type.hashCode()
        result = 31 * result + renderPriority
        result = 31 * result + (triangleColors?.contentHashCode() ?: 0)
        result = 31 * result + (triangleAlphas?.contentHashCode() ?: 0)
        result = 31 * result + (triangleSkins?.contentHashCode() ?: 0)
        result = 31 * result + (triangleRenderTypes?.contentHashCode() ?: 0)
        result = 31 * result + (triangleRenderPriorities?.contentHashCode() ?: 0)
        result = 31 * result + (triangleVertex1?.contentHashCode() ?: 0)
        result = 31 * result + (triangleVertex2?.contentHashCode() ?: 0)
        result = 31 * result + (triangleVertex3?.contentHashCode() ?: 0)
        result = 31 * result + (vertexPositionsX?.contentHashCode() ?: 0)
        result = 31 * result + (vertexPositionsY?.contentHashCode() ?: 0)
        result = 31 * result + (vertexPositionsZ?.contentHashCode() ?: 0)
        result = 31 * result + (vertexSkins?.contentHashCode() ?: 0)
        result = 31 * result + (triangleTextures?.contentHashCode() ?: 0)
        result = 31 * result + (textureRenderTypes?.contentHashCode() ?: 0)
        result = 31 * result + (textureTriangleVertex1?.contentHashCode() ?: 0)
        result = 31 * result + (textureTriangleVertex2?.contentHashCode() ?: 0)
        result = 31 * result + (textureTriangleVertex3?.contentHashCode() ?: 0)
        result = 31 * result + (textureScaleX?.contentHashCode() ?: 0)
        result = 31 * result + (textureScaleY?.contentHashCode() ?: 0)
        result = 31 * result + (textureScaleZ?.contentHashCode() ?: 0)
        result = 31 * result + (textureRotation?.contentHashCode() ?: 0)
        result = 31 * result + (textureDirection?.contentHashCode() ?: 0)
        result = 31 * result + (textureSpeed?.contentHashCode() ?: 0)
        result = 31 * result + (textureTransU?.contentHashCode() ?: 0)
        result = 31 * result + (textureTransV?.contentHashCode() ?: 0)
        result = 31 * result + (textureCoordinates?.contentHashCode() ?: 0)
        result = 31 * result + (skeletalBones?.contentDeepHashCode() ?: 0)
        result = 31 * result + (skeletalScales?.contentDeepHashCode() ?: 0)
        result = 31 * result + (emitters?.contentHashCode() ?: 0)
        result = 31 * result + (effectors?.contentHashCode() ?: 0)
        result = 31 * result + (faceBillboards?.contentHashCode() ?: 0)
        return result
    }


    companion object {
        const val DOWNSCALE_FACTOR = 2
        const val UNVERSIONED = -1
        const val DEFAULT_VERSION = 12

        val FACE_TYPES_FLAG = 0x1
        val PARTICLES_FLAG = 0x2
        val BILLBOARDS_FLAG = 0x4
        val VERSION_FLAG = 0x8

        const val SIMPLE_TEXTURE = 0x0
        const val CYLINDRICAL_TEXTURE = 0x1
        const val CUBE_TEXTURE = 0x2
        const val SPHERICAL_TEXTURE = 0x3
        val COMPLEX_TEXTURE_RANGE = 1..3

        const val X_POS_FLAG = 0x1
        const val Y_POS_FLAG = 0x2
        const val Z_POS_FLAG = 0x4

        const val USES_FACE_TYPES_FLAG = 0x1
        const val USES_MATERIALS_FLAG = 0x2

        private const val UNIT = Math.PI / 1024.0

        val SINE = IntArray(2048) {
            (65536.0 * sin(it * UNIT)).toInt()
        }

        val COSINE = IntArray(2048) {
            (65536.0 * cos(it * UNIT)).toInt()
        }
    }

}