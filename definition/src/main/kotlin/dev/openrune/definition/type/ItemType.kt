package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Parameterized
import dev.openrune.definition.Recolourable
import it.unimi.dsi.fastutil.bytes.Byte2ByteOpenHashMap
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class ItemType(
    override var id: Int = -1,
    var name: String = "null",
    var examine: String = "null",
    override var originalColours: MutableList<Int>? = null,
    override var modifiedColours: MutableList<Int>? = null,
    override var originalTextureColours: MutableList<Int>? = null,
    override var modifiedTextureColours: MutableList<Int>? = null,
    override var params: Map<Int, @Contextual Any>? = null,
    var resizeX: Int = 128,
    var resizeY: Int = 128,
    var resizeZ: Int = 128,
    var xan2d: Int = 0,
    var category: Int = -1,
    var yan2d: Int = 0,
    var zan2d: Int = 0,
    var equipSlot: Int = -1,
    var appearanceOverride1: Int = -1,
    var appearanceOverride2: Int = -1,
    var weight: Double = 0.0,
    var cost: Int = 1,
    var isTradeable: Boolean = false,
    var stacks: Int = 0,
    var inventoryModel: Int = 0,
    var members: Boolean = false,
    var zoom2d: Int = 2000,
    var xOffset2d: Int = 0,
    var yOffset2d: Int = 0,
    var ambient: Int = 0,
    var contrast: Int = 0,
    var countCo: MutableList<Int>? = null,
    var countObj: MutableList<Int>? = null,
    var options: MutableList<String?> = mutableListOf(null, null, "Take", null, null),
    var interfaceOptions: MutableList<String?> = mutableListOf(null, null, null, null, "Drop"),
    var maleModel0: Int = -1,
    var maleModel1: Int = -1,
    var maleModel2: Int = -1,
    var maleOffset: Int = 0,
    var maleHeadModel0: Int = -1,
    var maleHeadModel1: Int = -1,
    var femaleModel0: Int = -1,
    var femaleModel1: Int = -1,
    var femaleModel2: Int = -1,
    var femaleOffset: Int = 0,
    var femaleHeadModel0: Int = -1,
    var femaleHeadModel1: Int = -1,
    var noteLinkId: Int = -1,
    var noteTemplateId: Int = -1,
    var teamCape: Int = 0,
    var dropOptionIndex: Int = -2,
    var unnotedId: Int = -1,
    var notedId: Int = -1,
    var placeholderLink: Int = -1,
    var placeholderTemplate: Int = -1,
    var subops: Array<Array<String?>?>? = null,
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

    val stackable: Boolean
        get() = stacks == 1 || noteTemplateId > 0

    val noted: Boolean
        get() = noteTemplateId > 0

    /**
     * Whether or not the object is a placeholder.
     */
    val isPlaceholder
        get() = placeholderTemplate > 0 && placeholderLink > 0


    fun linkNote(notedItem: ItemType, unnotedItem: ItemType) {
        this.inventoryModel = notedItem!!.inventoryModel
        this.zoom2d = notedItem.zoom2d
        this.xan2d = notedItem.xan2d
        this.yan2d = notedItem.yan2d
        this.zan2d = notedItem.zan2d
        this.xOffset2d = notedItem.xOffset2d
        this.yOffset2d = notedItem.yOffset2d
        this.originalTextureColours = notedItem.originalTextureColours
        this.modifiedColours = notedItem.modifiedColours
        this.originalTextureColours = notedItem.originalTextureColours
        this.modifiedTextureColours = notedItem.modifiedTextureColours
        this.name = unnotedItem.name
        this.members = unnotedItem.members
        this.cost = unnotedItem.cost
        this.stacks = 1
    }

    fun linkBought(var1: ItemType, var2: ItemType) {
        this.inventoryModel = var1.inventoryModel
        this.zoom2d = var1.zoom2d
        this.xan2d = var1.xan2d
        this.yan2d = var1.yan2d
        this.zan2d = var1.zan2d
        this.xOffset2d = var1.xOffset2d
        this.yOffset2d = var1.yOffset2d
        this.originalTextureColours = var2.originalTextureColours
        this.modifiedColours = var2.modifiedColours
        this.originalTextureColours = var2.originalTextureColours
        this.modifiedTextureColours = var2.modifiedTextureColours
        this.name = var2.name
        this.members = var2.members
        this.stacks = var2.stacks
        this.maleModel0 = var2.maleModel0
        this.maleModel1 = var2.maleModel1
        this.maleModel2 = var2.maleModel2
        this.femaleModel0 = var2.femaleModel0
        this.femaleModel1 = var2.femaleModel1
        this.femaleModel2 = var2.femaleModel2
        this.maleHeadModel0 = var2.maleHeadModel0
        this.maleHeadModel1 = var2.maleHeadModel1
        this.femaleHeadModel0 = var2.femaleHeadModel0
        this.femaleHeadModel1 = var2.femaleHeadModel1
        this.teamCape = var2.teamCape
        this.options = var2.options
        this.interfaceOptions = arrayOfNulls<String>(5).toMutableList()
        for (var3 in 0..3) {
            interfaceOptions[var3] = var2.interfaceOptions[var3]
        }

        interfaceOptions[4] = "Discard"
        this.cost = 0
    }

    fun linkPlaceholder(var1: ItemType, var2: ItemType) {
        this.inventoryModel = var1.inventoryModel
        this.zoom2d = var1.zoom2d
        this.xan2d = var1.xan2d
        this.yan2d = var1.yan2d
        this.zan2d = var1.zan2d
        this.xOffset2d = var1.xOffset2d
        this.yOffset2d = var1.yOffset2d
        this.originalTextureColours = var1.originalTextureColours
        this.modifiedTextureColours = var1.modifiedTextureColours
        this.originalTextureColours = var1.originalTextureColours
        this.modifiedTextureColours = var1.modifiedTextureColours
        this.stacks = var1.stacks
        this.name = var2.name
        this.cost = 0
        this.members = false
        this.isTradeable = false
    }

}