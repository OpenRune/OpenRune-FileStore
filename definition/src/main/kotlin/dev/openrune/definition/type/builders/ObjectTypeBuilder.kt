package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

import dev.openrune.definition.EntityOpsBuilder
import dev.openrune.definition.MutableParameterized
import dev.openrune.definition.MutableRecolourable
import dev.openrune.definition.MutableTransforms
import dev.openrune.definition.util.IntListPool

/**
 * The mutable side of [ObjectType]. Codecs decode into one of these and the packing tools edit
 * one — either fresh or via [ObjectType.toBuilder] — then [build] produces the immutable
 * definition everything else reads.
 */
class ObjectTypeBuilder(var id: Int = -1) : MutableTransforms, MutableRecolourable, MutableParameterized {

    var name: String = "null"
    var decorDisplacement: Int = 16
    var isHollow: Boolean = false
    var objectModels: MutableList<Int>? = null
    var objectTypes: MutableList<Int>? = null
    var mapAreaId: Int = -1
    var sizeX: Int = 1
    var sizeY: Int = 1
    var soundDistance: Int = 0
    var soundRetain: Int = 0
    var ambientSoundIds: MutableList<Int>? = null
    var offsetX: Int = 0
    var nonFlatShading: Boolean = false
    var interactive: Int = -1
    var animationId: Int = -1
    var ambient: Int = 0
    var contrast: Int = 0
    val actions: EntityOpsBuilder = EntityOpsBuilder()
    var solid: Int = 2
    var mapSceneID: Int = -1
    var clipMask: Int = 0
    var clipped: Boolean = true
    var modelSizeX: Int = 128
    var modelSizeZ: Int = 128
    var modelSizeY: Int = 128
    var offsetZ: Int = 0
    var offsetY: Int = 0
    var obstructive: Boolean = false
    var randomizeAnimStart: Boolean = true
    var clipType: Int = -1
    var category: Int = -1
    var supportsItems: Int = -1
    var isRotated: Boolean = false
    var ambientSoundId: Int = -1
    var modelClipped: Boolean = false
    var soundMin: Int = 0
    var soundMax: Int = 0
    var soundDistanceFadeCurve: Int = 0
    var soundFadeInDuration: Int = 300
    var soundFadeOutDuration: Int = 300
    var soundFadeInCurve: Int = 0
    var soundFadeOutCurve: Int = 0
    var delayAnimationUpdate: Boolean = false
    var impenetrable: Boolean = true
    var soundVisibility: Int = 2
    var rasie: Int = 0

    override var originalColours: MutableList<Int>? = null
    override var modifiedColours: MutableList<Int>? = null
    override var originalTextureColours: MutableList<Int>? = null
    override var modifiedTextureColours: MutableList<Int>? = null
    override var multiVarBit: Int = -1
    override var multiVarp: Int = -1
    override var multiDefault: Int = -1
    override var transforms: MutableList<Int>? = null
    override var params: MutableMap<Int, Any>? = null

    fun build(): ObjectType = ObjectType(
        id = id,
        name = name,
        decorDisplacement = decorDisplacement,
        isHollow = isHollow,
        objectModels = IntListPool.of(objectModels),
        objectTypes = IntListPool.of(objectTypes),
        mapAreaId = mapAreaId,
        sizeX = sizeX,
        sizeY = sizeY,
        soundDistance = soundDistance,
        soundRetain = soundRetain,
        ambientSoundIds = IntListPool.of(ambientSoundIds),
        offsetX = offsetX,
        nonFlatShading = nonFlatShading,
        interactive = interactive,
        animationId = animationId,
        ambient = ambient,
        contrast = contrast,
        actions = actions.build(),
        solid = solid,
        mapSceneID = mapSceneID,
        clipMask = clipMask,
        clipped = clipped,
        modelSizeX = modelSizeX,
        modelSizeZ = modelSizeZ,
        modelSizeY = modelSizeY,
        offsetZ = offsetZ,
        offsetY = offsetY,
        obstructive = obstructive,
        randomizeAnimStart = randomizeAnimStart,
        clipType = clipType,
        category = category,
        supportsItems = supportsItems,
        isRotated = isRotated,
        ambientSoundId = ambientSoundId,
        modelClipped = modelClipped,
        soundMin = soundMin,
        soundMax = soundMax,
        soundDistanceFadeCurve = soundDistanceFadeCurve,
        soundFadeInDuration = soundFadeInDuration,
        soundFadeOutDuration = soundFadeOutDuration,
        soundFadeInCurve = soundFadeInCurve,
        soundFadeOutCurve = soundFadeOutCurve,
        delayAnimationUpdate = delayAnimationUpdate,
        impenetrable = impenetrable,
        soundVisibility = soundVisibility,
        rasie = rasie,
        originalColours = IntListPool.of(originalColours),
        modifiedColours = IntListPool.of(modifiedColours),
        originalTextureColours = IntListPool.of(originalTextureColours),
        modifiedTextureColours = IntListPool.of(modifiedTextureColours),
        multiVarBit = multiVarBit,
        multiVarp = multiVarp,
        multiDefault = multiDefault,
        transforms = IntListPool.of(transforms),
        params = params,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. List contents are copied too. */
        fun from(type: ObjectType): ObjectTypeBuilder {
            val builder = ObjectTypeBuilder(type.id)
            builder.name = type.name
            builder.decorDisplacement = type.decorDisplacement
            builder.isHollow = type.isHollow
            builder.objectModels = type.objectModels?.toMutableList()
            builder.objectTypes = type.objectTypes?.toMutableList()
            builder.mapAreaId = type.mapAreaId
            builder.sizeX = type.sizeX
            builder.sizeY = type.sizeY
            builder.soundDistance = type.soundDistance
            builder.soundRetain = type.soundRetain
            builder.ambientSoundIds = type.ambientSoundIds?.toMutableList()
            builder.offsetX = type.offsetX
            builder.nonFlatShading = type.nonFlatShading
            builder.interactive = type.interactive
            builder.animationId = type.animationId
            builder.ambient = type.ambient
            builder.contrast = type.contrast
            builder.actions.include(type.actions)
            builder.solid = type.solid
            builder.mapSceneID = type.mapSceneID
            builder.clipMask = type.clipMask
            builder.clipped = type.clipped
            builder.modelSizeX = type.modelSizeX
            builder.modelSizeZ = type.modelSizeZ
            builder.modelSizeY = type.modelSizeY
            builder.offsetZ = type.offsetZ
            builder.offsetY = type.offsetY
            builder.obstructive = type.obstructive
            builder.randomizeAnimStart = type.randomizeAnimStart
            builder.clipType = type.clipType
            builder.category = type.category
            builder.supportsItems = type.supportsItems
            builder.isRotated = type.isRotated
            builder.ambientSoundId = type.ambientSoundId
            builder.modelClipped = type.modelClipped
            builder.soundMin = type.soundMin
            builder.soundMax = type.soundMax
            builder.soundDistanceFadeCurve = type.soundDistanceFadeCurve
            builder.soundFadeInDuration = type.soundFadeInDuration
            builder.soundFadeOutDuration = type.soundFadeOutDuration
            builder.soundFadeInCurve = type.soundFadeInCurve
            builder.soundFadeOutCurve = type.soundFadeOutCurve
            builder.delayAnimationUpdate = type.delayAnimationUpdate
            builder.impenetrable = type.impenetrable
            builder.soundVisibility = type.soundVisibility
            builder.rasie = type.rasie
            builder.originalColours = type.originalColours?.toMutableList()
            builder.modifiedColours = type.modifiedColours?.toMutableList()
            builder.originalTextureColours = type.originalTextureColours?.toMutableList()
            builder.modifiedTextureColours = type.modifiedTextureColours?.toMutableList()
            builder.multiVarBit = type.multiVarBit
            builder.multiVarp = type.multiVarp
            builder.multiDefault = type.multiDefault
            builder.transforms = type.transforms?.toMutableList()
            builder.params = type.params?.toMutableMap()
            return builder
        }
    }
}
