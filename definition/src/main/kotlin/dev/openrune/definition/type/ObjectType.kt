package dev.openrune.definition.type

import dev.openrune.definition.type.builders.ObjectTypeBuilder

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
    val name: String = "null",
    val decorDisplacement: Int = 16,
    val isHollow: Boolean = false,
    val objectModels: List<Int>? = null,
    val objectTypes: List<Int>? = null,
    val mapAreaId: Int = -1,
    val sizeX: Int = 1,
    val sizeY: Int = 1,
    val soundDistance: Int = 0,
    val soundRetain: Int = 0,
    val ambientSoundIds: List<Int>? = null,
    val offsetX: Int = 0,
    val nonFlatShading: Boolean = false,
    val interactive: Int = -1,
    val animationId: Int = -1,
    val ambient: Int = 0,
    val contrast: Int = 0,
    var actions: EntityOpsDefinition = EntityOpsDefinition.EMPTY,
    val solid: Int = 2,
    val mapSceneID: Int = -1,
    val clipMask: Int = 0,
    val clipped: Boolean = true,
    val modelSizeX: Int = 128,
    val modelSizeZ: Int = 128,
    val modelSizeY: Int = 128,
    val offsetZ: Int = 0,
    val offsetY: Int = 0,
    val obstructive: Boolean = false,
    val randomizeAnimStart: Boolean = true,
    val clipType: Int = -1,
    val category: Int = -1,
    val supportsItems: Int = -1,
    val isRotated: Boolean = false,
    val ambientSoundId: Int = -1,
    val modelClipped: Boolean = false,
    val soundMin: Int = 0,
    val soundMax: Int = 0,
    val soundDistanceFadeCurve : Int = 0,
    val soundFadeInDuration : Int = 300,
    val soundFadeOutDuration : Int = 300,
    val soundFadeInCurve : Int = 0,
    val soundFadeOutCurve : Int = 0,
    val delayAnimationUpdate: Boolean = false,
    val impenetrable: Boolean = true,
    val soundVisibility : Int = 2,
    val rasie : Int = 0,
    override val originalColours: List<Int>? = null,
    override val modifiedColours: List<Int>? = null,
    override val originalTextureColours: List<Int>? = null,
    override val modifiedTextureColours: List<Int>? = null,
    override val multiVarBit: Int = -1,
    override val multiVarp: Int = -1,
    override val multiDefault: Int = -1,
    override val transforms: List<Int>? = null,
    @param:TomlField(serializer = ParamSerializer::class)
    override val params: MutableMap<Int, Any>? = null,
) : Definition, Transforms, Recolourable, Parameterized {

    fun toBuilder(): ObjectTypeBuilder = ObjectTypeBuilder.from(this)

    private fun actionAt(index: Int): String? = actions.getOpOrNull(index)

    fun hasActions() = actions.opsOrEmpty.any { it != null }

    fun hasOption(vararg searchOptions: String): Boolean {
        return searchOptions.any { option ->
            actions.opsOrEmpty.any { it?.text.equals(option, ignoreCase = true) }
        }
    }

    fun getOption(vararg searchOptions: String): Int {
        searchOptions.forEach {
            actions.opsOrEmpty.forEachIndexed { index, option ->
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
        var result = name.hashCode()
        result = 31 * result + mapAreaId
        result = 31 * result + actions.hashCode()
        result = 31 * result + sizeX
        result = 31 * result + sizeY
        result = 31 * result + (objectModels?.hashCode() ?: 0)
        result = 31 * result + modelSizeX
        result = 31 * result + modelSizeY
        result = 31 * result + modelSizeZ
        result = 31 * result + animationId
        return result
    }

    fun postDecode(): ObjectType {
        var interactive = interactive
        var supportsItems = supportsItems

        if (interactive == -1) {
            interactive = 0
            if (objectModels != null && (objectTypes == null || objectTypes[0] == 10)) {
                interactive = 1
            }

            if (hasActions()) {
                interactive = 1
            }
        }

        if (supportsItems == -1) {
            supportsItems = if (solid != 0) 1 else 0
        }

        if (interactive == this.interactive && supportsItems == this.supportsItems) return this
        return copy(interactive = interactive, supportsItems = supportsItems)
    }

    // Optional: custom equals to match based on the same fields
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ObjectType) return false

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
