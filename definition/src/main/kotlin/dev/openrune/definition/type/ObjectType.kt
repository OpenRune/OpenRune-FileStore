package dev.openrune.definition.type

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.toml.serialization.TomlField
import dev.openrune.definition.Definition
import dev.openrune.definition.EntityOpsDefinition
import dev.openrune.definition.Parameterized
import dev.openrune.definition.Recolourable
import dev.openrune.definition.Transforms
import dev.openrune.seralizer.ObjectTypeOptionsTableHook
import dev.openrune.seralizer.ParamSerializer
import kotlin.math.abs

@RsTableHeaders(
    "object",
    rowPostDecode = ObjectTypeOptionsTableHook::class,
)
data class ObjectType(
    override var id: Int = -1,
    var name: String = "null",
    var decorDisplacement: Int = 16,
    var isHollow: Boolean = false,
    var objectModels: MutableList<Int>? = null,
    var objectTypes: MutableList<Int>? = null,
    var mapAreaId: Int = -1,
    var sizeX: Int = 1,
    var sizeY: Int = 1,
    var soundDistance: Int = 0,
    var soundRetain: Int = 0,
    var ambientSoundIds: MutableList<Int>? = null,
    var offsetX: Int = 0,
    var nonFlatShading: Boolean = false,
    var interactive: Int = -1,
    var animationId: Int = -1,
    var ambient: Int = 0,
    var contrast: Int = 0,
    var actions: EntityOpsDefinition = EntityOpsDefinition(),
    var solid: Int = 2,
    var mapSceneID: Int = -1,
    var clipMask: Int = 0,
    var clipped: Boolean = true,
    var modelSizeX: Int = 128,
    var modelSizeZ: Int = 128,
    var modelSizeY: Int = 128,
    var offsetZ: Int = 0,
    var offsetY: Int = 0,
    var obstructive: Boolean = false,
    var randomizeAnimStart: Boolean = true,
    var clipType: Int = -1,
    var category: Int = -1,
    var supportsItems: Int = -1,
    var isRotated: Boolean = false,
    var ambientSoundId: Int = -1,
    var modelClipped: Boolean = false,
    var soundMin: Int = 0,
    var soundMax: Int = 0,
    var soundDistanceFadeCurve : Int = 0,
    var soundFadeInDuration : Int = 300,
    var soundFadeOutDuration : Int = 300,
    var soundFadeInCurve : Int = 0,
    var soundFadeOutCurve : Int = 0,
    var delayAnimationUpdate: Boolean = false,
    var impenetrable: Boolean = true,
    var soundVisibility : Int = 2,
    var rasie : Int = 0,
    override var originalColours: MutableList<Int>? = null,
    override var modifiedColours: MutableList<Int>? = null,
    override var originalTextureColours: MutableList<Int>? = null,
    override var modifiedTextureColours: MutableList<Int>? = null,
    override var multiVarBit: Int = -1,
    override var multiVarp: Int = -1,
    override var multiDefault: Int = -1,
    override var transforms: MutableList<Int>? = null,
    @param:TomlField(serializer = ParamSerializer::class)
    override var params: MutableMap<Int, Any>? = null,
) : Definition, Transforms, Recolourable, Parameterized {

    private fun actionAt(index: Int): String? = actions.ops.getOrNull(index)?.text

    fun hasActions() = actions.ops.any { it != null }

    fun hasOption(vararg searchOptions: String): Boolean {
        return searchOptions.any { option ->
            actions.ops.any { it?.text.equals(option, ignoreCase = true) }
        }
    }

    fun getOption(vararg searchOptions: String): Int {
        searchOptions.forEach {
            actions.ops.forEachIndexed { index, option ->
                if (it.equals(option?.text, ignoreCase = true)) return index + 1
            }
        }
        return -1
    }

    fun oppositeDoorId(values: Map<Int, ObjectType?>): Int {
        if (getOption("open", "close") == -1) return -1

        val ids = values.values
            .filter { def ->
                def != null &&
                        def.id != id &&
                        def.name == name &&
                        def.modelSizeZ == modelSizeZ &&
                        def.objectModels == objectModels &&
                        def.objectTypes == objectTypes &&
                        def.modifiedColours == modifiedColours &&
                        def.isRotated == isRotated &&
                        def.actions != actions &&
                        (0..4).all { i ->
                            val s1 = def.actionAt(i)
                            val s2 = actionAt(i)
                            (s1 == s2) || (
                                    ("open".equals(s1, ignoreCase = true) && "close".equals(s2, ignoreCase = true)) ||
                                            ("close".equals(s1, ignoreCase = true) && "open".equals(s2, ignoreCase = true))
                                    )
                        }
            }
            .map { it!!.id }
            .sortedBy { abs(it - id) }

        return ids.firstOrNull() ?: -1
    }

    override fun hashCode(): Int {
        return listOf(
            name.hashCode(),
            mapAreaId,
            actions.hashCode(),
            sizeX,
            sizeY,
            objectModels?.hashCode() ?: 0,
            modelSizeX,
            modelSizeY,
            modelSizeZ,
            animationId
        ).fold(0) { acc, hash -> 31 * acc + hash }
    }

    fun postDecode() {
        if (interactive == -1) {
            interactive = 0
            if (objectModels != null && (objectTypes == null || objectTypes!![0] == 10)) {
                interactive = 1
            }

            if (hasActions()) {
                interactive = 1
            }
        }

        if (supportsItems == -1) {
            supportsItems = if (solid != 0) 1 else 0
        }
    }

    // Optional: custom equals to match based on the same fields
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is dev.openrune.definition.type.ObjectType) return false

        return name == other.name &&
                mapAreaId == other.mapAreaId &&
                actions == other.actions &&
                sizeX == other.sizeX &&
                sizeY == other.sizeY &&
                objectModels == other.objectModels &&
                modelSizeX == other.modelSizeX &&
                modelSizeY == other.modelSizeY &&
                modelSizeZ == other.modelSizeZ &&
                animationId == other.animationId
    }

}