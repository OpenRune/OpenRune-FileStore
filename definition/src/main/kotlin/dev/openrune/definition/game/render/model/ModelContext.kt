package dev.openrune.definition.game.render.model

import dev.openrune.definition.game.render.model.Model.Companion.MAX_FACES
import dev.openrune.definition.game.render.model.Model.Companion.MAX_VERTICES

class ModelContext {
    val isFaceClipped = BooleanArray(MAX_VERTICES)
    val viewportYCoordinates = IntArray(MAX_VERTICES)
    val viewportXCoordinates = IntArray(MAX_VERTICES)
    val viewportZCoordinates = IntArray(MAX_VERTICES)
    val localXCoordinates = IntArray(MAX_VERTICES)
    val localYCoordinates = IntArray(MAX_VERTICES)
    val localZCoordinates = IntArray(MAX_VERTICES)

    val distanceFaceCounts = IntArray(MAX_FACES)
    val facesByDistance = Array(MAX_FACES) { IntArray(512) }
    val priorityCounts = IntArray(12)
    val orderedFacesByPriority = Array(12) { IntArray(2000) }
    val eq10Distances = IntArray(2000)
    val eq11Distances = IntArray(2000)
    val lt10Distances = IntArray(12)
}