package dev.openrune.definition.game.render.model

import dev.openrune.definition.game.render.draw.Rasterizer3D
import kotlin.math.sqrt

class Model : Renderable() {

    private val isFaceClipped = BooleanArray(MAX_VERTICES)
    private val viewportYCoordinates = IntArray(MAX_VERTICES)
    private val viewportXCoordinates = IntArray(MAX_VERTICES)
    private val viewportZCoordinates = IntArray(MAX_VERTICES)
    private val localXCoordinates = IntArray(MAX_VERTICES)
    private val localYCoordinates = IntArray(MAX_VERTICES)
    private val localZCoordinates = IntArray(MAX_VERTICES)

    private val distanceFaceCounts = IntArray(MAX_FACES)
    private val facesByDistance = Array(MAX_FACES) { IntArray(512) }
    private val priorityCounts = IntArray(12)
    private val orderedFacesByPriority = Array(12) { IntArray(2000) }
    private val eq10Distances = IntArray(2000)
    private val eq11Distances = IntArray(2000)
    private val lt10Distances = IntArray(12)

    var vertexCount = 0
    lateinit var verticesX: IntArray
    lateinit var verticesY: IntArray
    lateinit var verticesZ: IntArray

    var triangleCount = 0
    lateinit var triangleVertex1: IntArray
    lateinit var triangleVertex2: IntArray
    lateinit var triangleVertex3: IntArray

    lateinit var faceColors1: IntArray
    lateinit var faceColors2: IntArray
    lateinit var faceColors3: IntArray

    var facePriorities: IntArray? = null
    var triangleAlphas: IntArray? = null
    var textureCoords: ByteArray? = null
    var faceTextures: IntArray? = null

    var numTextureFaces = 0
    lateinit var texIndices1: IntArray
    lateinit var texIndices2: IntArray
    lateinit var texIndices3: IntArray

    private var boundsType = 0
    private var bottomY = 0
    private var xyzMagnitude = 0
    private var diameter = 0
    private var radius = 0

    companion object {
        private val sineTable = Rasterizer3D.SINE
        private val cosineTable = Rasterizer3D.COSINE
        private const val MAX_VERTICES = 6500
        private const val MAX_FACES = 6000
        private const val BOUNDS_TYPE_CYLINDER = 1
        private const val BOUNDS_TYPE_PROJECTED = 2
    }

    fun calculateBoundsCylinder() {
        if (this.boundsType != BOUNDS_TYPE_CYLINDER) {
            this.boundsType = BOUNDS_TYPE_CYLINDER
            super.modelHeight = 0
            this.bottomY = 0
            this.xyzMagnitude = 0

            for (i in 0 until this.vertexCount) {
                val x = verticesX[i]
                val y = verticesY[i]
                val z = verticesZ[i]

                super.modelHeight = maxOf(super.modelHeight, -y)
                this.bottomY = maxOf(this.bottomY, y)

                val magnitudeSquared = x * x + z * z
                this.xyzMagnitude = maxOf(this.xyzMagnitude, magnitudeSquared)
            }

            this.xyzMagnitude = (sqrt(xyzMagnitude.toDouble()) + 0.99).toInt()
            this.radius = (sqrt((this.xyzMagnitude * this.xyzMagnitude + super.modelHeight * super.modelHeight).toDouble()) + 0.99).toInt()
            this.diameter = this.radius + (sqrt((this.xyzMagnitude * this.xyzMagnitude + this.bottomY * this.bottomY).toDouble()) + 0.99).toInt()
        }
    }

    fun projectAndDraw(
        graphics: Rasterizer3D,
        yzRotation: Int,
        xzRotation: Int,
        xyRotation: Int,
        orientation: Int,
        xOffset: Int,
        yOffset: Int,
        zOffset: Int
    ) {
        distanceFaceCounts[0] = -1

        if (this.boundsType != BOUNDS_TYPE_PROJECTED && this.boundsType != BOUNDS_TYPE_CYLINDER) {
            this.boundsType = BOUNDS_TYPE_PROJECTED
            this.xyzMagnitude = 0

            for (i in 0 until this.vertexCount) {
                val x = verticesX[i]
                val y = verticesY[i]
                val z = verticesZ[i]
                val magnitudeSquared = x * x + z * z + y * y
                this.xyzMagnitude = maxOf(this.xyzMagnitude, magnitudeSquared)
            }

            this.xyzMagnitude = (sqrt(xyzMagnitude.toDouble()) + 0.99).toInt()
            this.radius = this.xyzMagnitude
            this.diameter = this.xyzMagnitude * 2
        }

        val sinX = sineTable[orientation]
        val cosX = cosineTable[orientation]
        val zRelatedVariable = (sinX * yOffset + cosX * zOffset) shr 16

        for (i in 0 until this.vertexCount) {
            var x = verticesX[i]
            var y = verticesY[i]
            var z = verticesZ[i]

            if (xyRotation != 0) {
                val sinZ = sineTable[xyRotation]
                val cosZ = cosineTable[xyRotation]
                val tmpX = y * sinZ + x * cosZ shr 16
                y = y * cosZ - x * sinZ shr 16
                x = tmpX
            }

            if (yzRotation != 0) {
                val sinR1 = sineTable[yzRotation]
                val cosR1 = cosineTable[yzRotation]
                val tmpY = y * cosR1 - z * sinR1 shr 16
                z = y * sinR1 + z * cosR1 shr 16
                y = tmpY
            }

            if (xzRotation != 0) {
                val sinY = sineTable[xzRotation]
                val cosY = cosineTable[xzRotation]
                val tmpZ = z * sinY + x * cosY shr 16
                z = z * cosY - x * sinY shr 16
                x = tmpZ
            }

            x += xOffset
            y += yOffset
            z += zOffset

            val tmp = y * cosX - z * sinX shr 16
            z = y * sinX + z * cosX shr 16

            viewportZCoordinates[i] = z - zRelatedVariable
            viewportYCoordinates[i] = x * graphics.zoom / z + graphics.centerX
            viewportXCoordinates[i] = tmp * graphics.zoom / z + graphics.centerY

            if (faceTextures != null) {
                localXCoordinates[i] = x
                localYCoordinates[i] = tmp
                localZCoordinates[i] = z
            }
        }

        draw(graphics)
    }

    private fun draw(graphics: Rasterizer3D) {
        if (this.diameter < 6000) {
            for (i in 0 until this.diameter) {
                distanceFaceCounts[i] = 0
            }

            for (i in 0 until this.triangleCount) {
                if (faceColors3[i] != -2) {
                    val idx1 = triangleVertex1[i]
                    val idx2 = triangleVertex2[i]
                    val idx3 = triangleVertex3[i]
                    val y1 = viewportYCoordinates[idx1]
                    val y2 = viewportYCoordinates[idx2]
                    val y3 = viewportYCoordinates[idx3]

                    if ((y1 - y2) * (viewportXCoordinates[idx3] - viewportXCoordinates[idx2]) - (y3 - y2) * (viewportXCoordinates[idx1] - viewportXCoordinates[idx2]) > 0) {
                        if (y1 in 0..graphics.clippingOffsetX && y2 in 0..graphics.clippingOffsetX && y3 in 0..graphics.clippingOffsetX) {
                            isFaceClipped[i] = false
                        } else {
                            isFaceClipped[i] = true
                        }

                        val avgZ = (viewportZCoordinates[idx1] + viewportZCoordinates[idx2] + viewportZCoordinates[idx3]) / 3 + this.radius
                        facesByDistance[avgZ][distanceFaceCounts[avgZ]++] = i
                    }
                }
            }

            if (facePriorities == null) {
                var distanceIndex = this.diameter - 1
                while (distanceIndex >= 0) {
                    val faceCount = distanceFaceCounts[distanceIndex]
                    if (faceCount > 0) {
                        val facePriority = facesByDistance[distanceIndex]

                        var j = 0
                        while (j < faceCount) {
                            renderFace(graphics, facePriority[j])
                            ++j
                        }
                    }
                    --distanceIndex
                }
            } else {
                var distanceIndex = 0
                while (distanceIndex < 12) {
                    priorityCounts[distanceIndex] = 0
                    lt10Distances[distanceIndex] = 0
                    ++distanceIndex
                }

                distanceIndex = this.diameter - 1
                while (distanceIndex >= 0) {
                    val faceCount = distanceFaceCounts[distanceIndex]
                    if (faceCount > 0) {
                        val facePriority = facesByDistance[distanceIndex]

                        var faceIndexInPriority = 0
                        while (faceIndexInPriority < faceCount) {
                            val faceIndex = facePriority[faceIndexInPriority]
                            val priority = facePriorities!![faceIndex]
                            val priorityIndex = priorityCounts[priority]++
                            orderedFacesByPriority[priority][priorityIndex] = faceIndex
                            if (priority < 10) {
                                lt10Distances[priority] += distanceIndex
                            } else if (priority == 10) {
                                eq10Distances[priorityIndex] = distanceIndex
                            } else {
                                eq11Distances[priorityIndex] = distanceIndex
                            }
                            ++faceIndexInPriority
                        }
                    }
                    --distanceIndex
                }
                processFacePriorities(graphics)
            }
        }
    }

    private fun processFacePriorities(graphics: Rasterizer3D) {
        for (priority in 0 until 10) {
            var priorityIndex = 0
            while (priorityIndex < priorityCounts[priority]) {
                renderFace(graphics, orderedFacesByPriority[priority][priorityIndex])
                ++priorityIndex
            }
        }
    }

    private fun renderFace(graphics: Rasterizer3D, face: Int) {
        val v1 = triangleVertex1[face]
        val v2 = triangleVertex2[face]
        val v3 = triangleVertex3[face]

        graphics.isRasterClippingEnabled = isFaceClipped[face]
        graphics.alpha = triangleAlphas?.get(face)?.and(255) ?: 0

        if (faceTextures != null && faceTextures!![face] != -1) {
            val textureIndices = textureCoords?.let {
                val idx = textureCoords!![face].toInt() and 255
                arrayOf(texIndices1[idx], texIndices2[idx], texIndices3[idx])
            } ?: arrayOf(v1, v2, v3)

            if (faceColors3[face] == -1) {
                graphics.rasterTextureAffine(
                    viewportXCoordinates[v1], viewportXCoordinates[v2], viewportXCoordinates[v3],
                    viewportYCoordinates[v1], viewportYCoordinates[v2], viewportYCoordinates[v3],
                    faceColors1[face], faceColors1[face], faceColors1[face],
                    localXCoordinates[textureIndices[0]], localXCoordinates[textureIndices[1]], localXCoordinates[textureIndices[2]],
                    localYCoordinates[textureIndices[0]], localYCoordinates[textureIndices[1]], localYCoordinates[textureIndices[2]],
                    localZCoordinates[textureIndices[0]], localZCoordinates[textureIndices[1]], localZCoordinates[textureIndices[2]],
                    faceTextures!![face]
                )
            } else {
                graphics.rasterTextureAffine(
                    viewportXCoordinates[v1], viewportXCoordinates[v2], viewportXCoordinates[v3],
                    viewportYCoordinates[v1], viewportYCoordinates[v2], viewportYCoordinates[v3],
                    faceColors1[face], faceColors2[face], faceColors3[face],
                    localXCoordinates[textureIndices[0]], localXCoordinates[textureIndices[1]], localXCoordinates[textureIndices[2]],
                    localYCoordinates[textureIndices[0]], localYCoordinates[textureIndices[1]], localYCoordinates[textureIndices[2]],
                    localZCoordinates[textureIndices[0]], localZCoordinates[textureIndices[1]], localZCoordinates[textureIndices[2]],
                    faceTextures!![face]
                )
            }
        } else if (faceColors3[face] == -1) {
            graphics.rasterFlat(
                viewportXCoordinates[v1], viewportXCoordinates[v2], viewportXCoordinates[v3],
                viewportYCoordinates[v1], viewportYCoordinates[v2], viewportYCoordinates[v3],
                graphics.colorPalette[faceColors1[face]]
            )
        } else {
            graphics.rasterGouraud(
                viewportXCoordinates[v1], viewportXCoordinates[v2], viewportXCoordinates[v3],
                viewportYCoordinates[v1], viewportYCoordinates[v2], viewportYCoordinates[v3],
                faceColors1[face], faceColors2[face], faceColors3[face]
            )
        }
    }
}