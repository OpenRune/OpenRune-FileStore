package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Recolourable
import dev.openrune.definition.serialization.Rscm
import kotlinx.serialization.Serializable

@Serializable
data class SpotAnimType(
    override var id: Rscm = -1,
    override var originalColours: MutableList<Int>? = null,
    override var modifiedColours: MutableList<Int>? = null,
    override var originalTextureColours: MutableList<Int>? = null,
    override var modifiedTextureColours: MutableList<Int>? = null,
    var resizeY: Int = 128,
    var resizeX: Int = 128,
    var rotation: Int = 0,
    var animationId: Int = -1,
    var modelId: Int = 0,
    var ambient: Int = 0,
    var contrast: Int = 0,
    var debugName : String = ""
) : Definition, Recolourable