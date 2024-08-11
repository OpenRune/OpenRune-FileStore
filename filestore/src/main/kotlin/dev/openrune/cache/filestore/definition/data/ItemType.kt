package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.Parameterized
import dev.openrune.cache.filestore.definition.Recolourable
import dev.openrune.cache.filestore.serialization.UShortList
import dev.openrune.cache.filestore.serialization.toIntWithMaxCheck
import it.unimi.dsi.fastutil.bytes.Byte2ByteOpenHashMap
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Serializable
data class ItemType(
    override var id: Int = -1,
    var name: String = "null",
    var examine: String = "null",
    override var originalColours: UShortList = emptyList(),
    override var modifiedColours: UShortList = emptyList(),
    override var originalTextureColours: UShortList = emptyList(),
    override var modifiedTextureColours: UShortList = emptyList(),
    @Contextual
    override var params: Map<Int, @Polymorphic DataValue?>? = null,
    var resizeX: UShort = 128u,
    var resizeY: UShort = 128u,
    var resizeZ: UShort = 128u,
    var xan2d: UShort = 0u,
    var category : UShort = UShort.MAX_VALUE,
    var yan2d: UShort = 0u,
    var zan2d: UShort = 0u,
    var equipSlot: UByte = UByte.MAX_VALUE,
    var appearanceOverride1: UByte = UByte.MAX_VALUE,
    var appearanceOverride2: UByte = UByte.MAX_VALUE,
    var weight: UShort = 0u,
    var cost: Int = 1,
    var isTradeable: Boolean = false,
    var stacks: UShort = 0u,
    var inventoryModel: Int = 0,
    var members: Boolean = false,
    var zoom2d: Int = 2000,
    var xOffset2d: UShort = 0u,
    var yOffset2d: UShort = 0u,
    var ambient: Byte = 0,
    var contrast: Byte = 0,
    var countCo: MutableList<UShort> = MutableList(10) { 0u },
    var countObj: MutableList<UShort> = MutableList(10) { 0u },
    var options : MutableList<String?> = mutableListOf(null, null, "Take", null, null),
    var interfaceOptions  : MutableList<String?> = mutableListOf(null, null, null, null, "Drop"),
    var maleModel0: UShort = UShort.MAX_VALUE,
    var maleModel1: UShort = UShort.MAX_VALUE,
    var maleModel2: UShort = UShort.MAX_VALUE,
    var maleOffset: Int = 0,
    var maleHeadModel0: UShort = UShort.MAX_VALUE,
    var maleHeadModel1: UShort = UShort.MAX_VALUE,
    var femaleModel0: UShort = UShort.MAX_VALUE,
    var femaleModel1: UShort = UShort.MAX_VALUE,
    var femaleModel2: UShort = UShort.MAX_VALUE,
    var femaleOffset: UByte = 0u,
    var femaleHeadModel0: UShort = UShort.MAX_VALUE,
    var femaleHeadModel1: UShort = UShort.MAX_VALUE,
    var noteLinkId: UShort = UShort.MAX_VALUE,
    var noteTemplateId: UShort = UShort.MAX_VALUE,
    var teamCape: Byte = 0,
    var dropOptionIndex: Byte = -2,
    var unnotedId: UShort = UShort.MAX_VALUE,
    var notedId: UShort = UShort.MAX_VALUE,
    var placeholderLink: UShort = UShort.MAX_VALUE,
    var placeholderTemplate: UShort = UShort.MAX_VALUE,
    //Custom
    override var inherit: Int = -1,
    var option1: String? = null,
    var option2: String? = null,
    var option3: String = "Take",
    var option4: String? = null,
    var option5: String? = null,
    var ioption1: String? = null,
    var ioption2: String? = null,
    var ioption3: String? = null,
    var ioption4: String? = null,
    var ioption5: String = "Drop",

    ) : Definition, Recolourable, Parameterized {

    init {
        options = listOf(option1,option2,option3,option4,option5).toMutableList()
        interfaceOptions = listOf(ioption1,ioption2,ioption3,ioption4,ioption5).toMutableList()

    }

    var bonuses: IntArray = IntArray(0)
    var attackSpeed = -1
    var equipType = 0
    var weaponType = -1
    var renderAnimations: IntArray? = null
    @Contextual
    var skillReqs: Byte2ByteOpenHashMap? = null

    val stackable: Boolean get() = getStacks() == 1 || getNoteTemplateId() > 0

    val noted: Boolean get() = getNoteTemplateId() > 0

    /**
     * Whether or not the object is a placeholder.
     */
    val isPlaceholder get() = getPlaceholderTemplate() > 0 && getPlaceholderLink() > 0


    fun getResizeX(): Int = resizeX.toIntWithMaxCheck()
    fun getResizeY(): Int = resizeY.toIntWithMaxCheck()
    fun getResizeZ(): Int = resizeZ.toIntWithMaxCheck()
    fun getXan2d(): Int = xan2d.toIntWithMaxCheck()
    fun getCategory(): Int = category.toIntWithMaxCheck()
    fun getYan2d(): Int = yan2d.toIntWithMaxCheck()
    fun getZan2d(): Int = zan2d.toIntWithMaxCheck()
    fun getWeight(): Int = weight.toIntWithMaxCheck()
    fun getStacks(): Int = stacks.toIntWithMaxCheck()
    fun getXOffset2d(): Int = xOffset2d.toIntWithMaxCheck()
    fun getYOffset2d(): Int = yOffset2d.toIntWithMaxCheck()
    fun getMaleModel0(): Int = maleModel0.toIntWithMaxCheck()
    fun getMaleModel1(): Int = maleModel1.toIntWithMaxCheck()
    fun getMaleModel2(): Int = maleModel2.toIntWithMaxCheck()
    fun getMaleHeadModel0(): Int = maleHeadModel0.toIntWithMaxCheck()
    fun getMaleHeadModel1(): Int = maleHeadModel1.toIntWithMaxCheck()
    fun getFemaleModel0(): Int = femaleModel0.toIntWithMaxCheck()
    fun getFemaleModel1(): Int = femaleModel1.toIntWithMaxCheck()
    fun getFemaleModel2(): Int = femaleModel2.toIntWithMaxCheck()
    fun getFemaleHeadModel0(): Int = femaleHeadModel0.toIntWithMaxCheck()
    fun getFemaleHeadModel1(): Int = femaleHeadModel1.toIntWithMaxCheck()
    fun getNoteLinkId(): Int = noteLinkId.toIntWithMaxCheck()
    fun getNoteTemplateId(): Int = noteTemplateId.toIntWithMaxCheck()
    fun getUnnotedId(): Int = unnotedId.toIntWithMaxCheck()
    fun getNotedId(): Int = notedId.toIntWithMaxCheck()
    fun getPlaceholderLink(): Int = placeholderLink.toIntWithMaxCheck()
    fun getPlaceholderTemplate(): Int = placeholderTemplate.toIntWithMaxCheck()

    // Getters for UByte properties returning Int
    fun getEquipSlot(): Int = equipSlot.toIntWithMaxCheck()
    fun getAppearanceOverride1(): Int = appearanceOverride1.toIntWithMaxCheck()
    fun getAppearanceOverride2(): Int = appearanceOverride2.toIntWithMaxCheck()
    fun getFemaleOffset(): Int = femaleOffset.toIntWithMaxCheck()

    // Getters for Byte properties returning Int (no max check since Byte does not have an unsigned max value)
    fun getAmbient(): Int = ambient.toInt()
    fun getContrast(): Int = contrast.toInt()
    fun getTeamCape(): Int = teamCape.toInt()
    fun getDropOptionIndex(): Int = dropOptionIndex.toInt()


}
