package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.Parameterized
import dev.openrune.cache.filestore.definition.Recolourable
import dev.openrune.cache.filestore.definition.Transforms
import dev.openrune.cache.filestore.serialization.UShortList
import dev.openrune.cache.filestore.serialization.toIntWithMaxCheck
import kotlinx.serialization.Polymorphic

data class ObjectType(
    override var id: Int = -1,
    var name: String = "null",
    var decorDisplacement : UByte = 16u,
    var isHollow : Boolean = false,
    var objectModels: MutableList<Int>? = null,
    var objectTypes: MutableList<Int>? = null,
    var recolorToFind: MutableList<Int>? = null,
    var mapAreaId: UShort = UShort.MAX_VALUE,
    var retextureToReplace: MutableList<Int>? = null,
    var sizeX: UByte = 1u,
    var sizeY: UByte = 1u,
    var soundDistance: UByte = 0u,
    var soundRetain: UByte = 0u,
    var ambientSoundIds: MutableList<Int>? = null,
    var offsetX: UShort = 0u,
    var nonFlatShading: Boolean = false,
    var interactive: UByte = UByte.MAX_VALUE,
    var animationId: UShort = UShort.MAX_VALUE,
    var varbitId: Int = -1,
    var ambient: Int = 0,
    var contrast: Int = 0,
    var actions : MutableList<String?> = mutableListOf(null, null, null, null, null),
    var solid: Int = 2,
    var mapSceneID: UShort = UShort.MAX_VALUE,
    var clipMask: Int = 0,
    var recolorToReplace: List<Short>? = null,
    var clipped: Boolean = true,
    var modelSizeX: UShort = 128u,
    var modelSizeZ: UShort = 128u,
    var modelSizeY: UShort = 128u,
    var offsetZ: UShort = 0u,
    var offsetY: UShort = 0u,
    var obstructive: Boolean = false,
    var randomizeAnimStart: Boolean = true,
    var clipType: UByte = UByte.MAX_VALUE,
    var category : UShort = UShort.MAX_VALUE,
    var supportsItems: UByte = UByte.MAX_VALUE,
    var configs: IntArray? = null,
    var isRotated: Boolean = false,
    var varpId: Int = -1,
    var ambientSoundId: UShort = UShort.MAX_VALUE,
    var modelClipped: Boolean = false,
    var soundMin: UShort = 0u,
    var soundMax: UShort = 0u,
    var delayAnimationUpdate : Boolean = false,
    var impenetrable: Boolean = true,
    override var originalColours: UShortList = emptyList(),
    override var modifiedColours: UShortList = emptyList(),
    override var originalTextureColours: UShortList = emptyList(),
    override var modifiedTextureColours: UShortList = emptyList(),
    override var varbit: Int = -1,
    override var varp: Int = -1,
    override var transforms: MutableList<Int>? = null,
    override var params: Map<Int, @Polymorphic DataValue?>? = null,

    //Custom
    var option1: String? = null,
    var option2: String? = null,
    var option3: String? = null,
    var option4: String? = null,
    var option5: String? = null,
    override var inherit : Int = -1

) : Definition, Transforms, Recolourable, Parameterized {
    init {
        actions = listOf(option1,option2,option3,option4,option5).toMutableList()
    }

    fun getDecorDisplacement(): Int = decorDisplacement.toIntWithMaxCheck()
    fun getSizeX(): Int = sizeX.toIntWithMaxCheck()
    fun getSizeY(): Int = sizeY.toIntWithMaxCheck()
    fun getSoundDistance(): Int = soundDistance.toIntWithMaxCheck()
    fun getSoundRetain(): Int = soundRetain.toIntWithMaxCheck()
    fun getInteractive(): Int = interactive.toIntWithMaxCheck()
    fun getClipType(): Int = clipType.toIntWithMaxCheck()
    fun getSupportsItems(): Int = supportsItems.toIntWithMaxCheck()

    fun getMapAreaId(): Int = mapAreaId.toIntWithMaxCheck()
    fun getOffsetX(): Int = offsetX.toIntWithMaxCheck()
    fun getAnimationId(): Int = animationId.toIntWithMaxCheck()
    fun getMapSceneID(): Int = mapSceneID.toIntWithMaxCheck()
    fun getModelSizeX(): Int = modelSizeX.toIntWithMaxCheck()
    fun getModelSizeZ(): Int = modelSizeZ.toIntWithMaxCheck()
    fun getModelSizeY(): Int = modelSizeY.toIntWithMaxCheck()
    fun getOffsetZ(): Int = offsetZ.toIntWithMaxCheck()
    fun getOffsetY(): Int = offsetY.toIntWithMaxCheck()
    fun getCategory(): Int = category.toIntWithMaxCheck()
    fun getAmbientSoundId(): Int = ambientSoundId.toIntWithMaxCheck()
    fun getSoundMin(): Int = soundMin.toIntWithMaxCheck()
    fun getSoundMax(): Int = soundMax.toIntWithMaxCheck()

}