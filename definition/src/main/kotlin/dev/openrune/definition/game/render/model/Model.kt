package dev.openrune.definition.game.render.model

import dev.openrune.definition.game.render.draw.Rasterizer3D
import kotlin.math.sqrt

class Model : Renderable() {

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
        const val MAX_VERTICES = 6500
        const val MAX_FACES = 6000
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
        context: ModelContext,
        yzRotation: Int,
        xzRotation: Int,
        xyRotation: Int,
        orientation: Int,
        xOffset: Int,
        yOffset: Int,
        zOffset: Int
    ) {
        context.distanceFaceCounts[0] = -1

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

            context.viewportZCoordinates[i] = z - zRelatedVariable
            context.viewportYCoordinates[i] = x * graphics.zoom / z + graphics.centerX
            context.viewportXCoordinates[i] = tmp * graphics.zoom / z + graphics.centerY

            if (faceTextures != null) {
                context.localXCoordinates[i] = x
                context.localYCoordinates[i] = tmp
                context.localZCoordinates[i] = z
            }
        }

        draw(graphics, context)
    }

    private fun draw(graphics: Rasterizer3D, context: ModelContext) {
        if (this.diameter < 6000) {
            for (i in 0 until this.diameter) {
                context.distanceFaceCounts[i] = 0
            }

            for (i in 0 until this.triangleCount) {
                if (faceColors3[i] != -2) {
                    val idx1 = triangleVertex1[i]
                    val idx2 = triangleVertex2[i]
                    val idx3 = triangleVertex3[i]
                    val y1 = context.viewportYCoordinates[idx1]
                    val y2 = context.viewportYCoordinates[idx2]
                    val y3 = context.viewportYCoordinates[idx3]

                    if ((y1 - y2) * (context.viewportXCoordinates[idx3] - context.viewportXCoordinates[idx2]) - (y3 - y2) * (context.viewportXCoordinates[idx1] - context.viewportXCoordinates[idx2]) > 0) {
                        if (y1 in 0..graphics.clippingOffsetX && y2 in 0..graphics.clippingOffsetX && y3 in 0..graphics.clippingOffsetX) {
                            context.isFaceClipped[i] = false
                        } else {
                            context.isFaceClipped[i] = true
                        }

                        val avgZ = (context.viewportZCoordinates[idx1] + context.viewportZCoordinates[idx2] + context.viewportZCoordinates[idx3]) / 3 + this.radius
                        context.facesByDistance[avgZ][context.distanceFaceCounts[avgZ]++] = i
                    }
                }
            }

            if (facePriorities == null) {
                var distanceIndex = this.diameter - 1
                while (distanceIndex >= 0) {
                    val faceCount = context.distanceFaceCounts[distanceIndex]
                    if (faceCount > 0) {
                        val facePriority = context.facesByDistance[distanceIndex]

                        var j = 0
                        while (j < faceCount) {
                            renderFace(graphics, context, facePriority[j])
                            ++j
                        }
                    }
                    --distanceIndex
                }
            } else {
                var distanceIndex = 0
                while (distanceIndex < 12) {
                    context.priorityCounts[distanceIndex] = 0
                    context.lt10Distances[distanceIndex] = 0
                    ++distanceIndex
                }

                distanceIndex = this.diameter - 1
                while (distanceIndex >= 0) {
                    val faceCount = context.distanceFaceCounts[distanceIndex]
                    if (faceCount > 0) {
                        val facePriority = context.facesByDistance[distanceIndex]

                        var faceIndexInPriority = 0
                        while (faceIndexInPriority < faceCount) {
                            val faceIndex = facePriority[faceIndexInPriority]
                            val priority = facePriorities!![faceIndex]
                            val priorityIndex = context.priorityCounts[priority]++
                            context.orderedFacesByPriority[priority][priorityIndex] = faceIndex
                            if (priority < 10) {
                                context.lt10Distances[priority] += distanceIndex
                            } else if (priority == 10) {
                                context.eq10Distances[priorityIndex] = distanceIndex
                            } else {
                                context.eq11Distances[priorityIndex] = distanceIndex
                            }
                            ++faceIndexInPriority
                        }
                    }
                    --distanceIndex
                }
                processFacePriorities(graphics, context)
            }
        }
    }

    private fun processFacePriorities(graphics: Rasterizer3D, context: ModelContext) {
        for (priority in 0 until 10) {
            var priorityIndex = 0
            while (priorityIndex < context.priorityCounts[priority]) {
                renderFace(graphics, context, context.orderedFacesByPriority[priority][priorityIndex])
                ++priorityIndex
            }
        }
    }

    private fun renderFace(graphics: Rasterizer3D, context: ModelContext, face: Int) {
        val v1 = triangleVertex1[face]
        val v2 = triangleVertex2[face]
        val v3 = triangleVertex3[face]

        graphics.isRasterClippingEnabled = context.isFaceClipped[face]
        graphics.alpha = triangleAlphas?.get(face)?.and(255) ?: 0

        if (faceTextures != null && faceTextures!![face] != -1) {
            val textureIndices = textureCoords?.let {
                val idx = textureCoords!![face].toInt() and 255
                arrayOf(texIndices1[idx], texIndices2[idx], texIndices3[idx])
            } ?: arrayOf(v1, v2, v3)

            if (faceColors3[face] == -1) {
                graphics.rasterTextureAffine(
                    context.viewportXCoordinates[v1], context.viewportXCoordinates[v2], context.viewportXCoordinates[v3],
                    context.viewportYCoordinates[v1], context.viewportYCoordinates[v2], context.viewportYCoordinates[v3],
                    faceColors1[face], faceColors1[face], faceColors1[face],
                    context.localXCoordinates[textureIndices[0]], context.localXCoordinates[textureIndices[1]], context.localXCoordinates[textureIndices[2]],
                    context.localYCoordinates[textureIndices[0]], context.localYCoordinates[textureIndices[1]], context.localYCoordinates[textureIndices[2]],
                    context.localZCoordinates[textureIndices[0]], context.localZCoordinates[textureIndices[1]], context.localZCoordinates[textureIndices[2]],
                    faceTextures!![face]
                )
            } else {
                graphics.rasterTextureAffine(
                    context.viewportXCoordinates[v1], context.viewportXCoordinates[v2], context.viewportXCoordinates[v3],
                    context.viewportYCoordinates[v1], context.viewportYCoordinates[v2], context.viewportYCoordinates[v3],
                    faceColors1[face], faceColors2[face], faceColors3[face],
                    context.localXCoordinates[textureIndices[0]], context.localXCoordinates[textureIndices[1]], context.localXCoordinates[textureIndices[2]],
                    context.localYCoordinates[textureIndices[0]], context.localYCoordinates[textureIndices[1]], context.localYCoordinates[textureIndices[2]],
                    context.localZCoordinates[textureIndices[0]], context.localZCoordinates[textureIndices[1]], context.localZCoordinates[textureIndices[2]],
                    faceTextures!![face]
                )
            }
        } else if (faceColors3[face] == -1) {
            graphics.rasterFlat(
                context.viewportXCoordinates[v1], context.viewportXCoordinates[v2], context.viewportXCoordinates[v3],
                context.viewportYCoordinates[v1], context.viewportYCoordinates[v2], context.viewportYCoordinates[v3],
                graphics.colorPalette[faceColors1[face]]
            )
        } else {
            graphics.rasterGouraud(
                context.viewportXCoordinates[v1], context.viewportXCoordinates[v2], context.viewportXCoordinates[v3],
                context.viewportYCoordinates[v1], context.viewportYCoordinates[v2], context.viewportYCoordinates[v3],
                faceColors1[face], faceColors2[face], faceColors3[face]
            )
        }
    }
}