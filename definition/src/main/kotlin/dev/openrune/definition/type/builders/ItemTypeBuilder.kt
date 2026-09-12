package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

import dev.openrune.definition.EntityOpsDefinition
import dev.openrune.definition.MutableParameterized
import dev.openrune.definition.MutableRecolourable
import dev.openrune.definition.util.IntListPool

class ItemTypeBuilder(var id: Int = -1) : MutableRecolourable, MutableParameterized {

    var name: String = "null"
    var examine: String = "null"
    var resizeX: Int = 128
    var resizeY: Int = 128
    var resizeZ: Int = 128
    var xan2d: Int = 0
    var category: Int = -1
    var yan2d: Int = 0
    var zan2d: Int = 0
    var equipSlot: Int = -1
    var appearanceOverride1: Int = -1
    var appearanceOverride2: Int = -1
    var weight: Double = 0.0
    var cost: Int = 1
    var stockMarket: Boolean = false
    var tradeable: Boolean = true
    var stacks: ObjStackability = ObjStackability.Sometimes
    var inventoryModel: Int = 0
    var members: Boolean = false
    var zoom2d: Int = 2000
    var xOffset2d: Int = 0
    var yOffset2d: Int = 0
    var ambient: Int = 0
    var contrast: Int = 0
    var countCo: MutableList<Int>? = null
    var countObj: MutableList<Int>? = null
    var options: EntityOpsDefinition = DEFAULT_ITEM_OPTIONS
    var interfaceOptions: MutableList<String?> = mutableListOf(null, null, null, null, "Drop")
    var maleModel0: Int = -1
    var maleModel1: Int = -1
    var maleModel2: Int = -1
    var maleOffset: Int = 0
    var maleHeadModel0: Int = -1
    var maleHeadModel1: Int = -1
    var femaleModel0: Int = -1
    var femaleModel1: Int = -1
    var femaleModel2: Int = -1
    var femaleOffset: Int = 0
    var femaleHeadModel0: Int = -1
    var femaleHeadModel1: Int = -1
    var noteLinkId: Int = -1
    var noteTemplateId: Int = -1
    var teamCape: Int = 0
    var dropOptionIndex: Int = -2
    var unnotedId: Int = -1
    var notedId: Int = -1
    var placeholderLink: Int = -1
    var placeholderTemplate: Int = -1
    var subops: Array<Array<String?>?>? = null

    override var originalColours: MutableList<Int>? = null
    override var modifiedColours: MutableList<Int>? = null
    override var originalTextureColours: MutableList<Int>? = null
    override var modifiedTextureColours: MutableList<Int>? = null
    override var params: MutableMap<Int, Any>? = null

    private val extras = LinkedHashMap<String, Any?>()

    fun setExtraProperty(key: String, value: Any?) {
        extras[key] = value
    }

    fun linkNote(notedItem: ItemType, unnotedItem: ItemType) = apply {
        inventoryModel = notedItem.inventoryModel
        zoom2d = notedItem.zoom2d
        xan2d = notedItem.xan2d
        yan2d = notedItem.yan2d
        zan2d = notedItem.zan2d
        xOffset2d = notedItem.xOffset2d
        yOffset2d = notedItem.yOffset2d
        originalTextureColours = notedItem.originalTextureColours?.toMutableList()
        modifiedColours = notedItem.modifiedColours?.toMutableList()
        modifiedTextureColours = notedItem.modifiedTextureColours?.toMutableList()
        name = unnotedItem.name
        members = unnotedItem.members
        cost = unnotedItem.cost
        stacks = ObjStackability.Always
    }

    fun linkBought(var1: ItemType, var2: ItemType) = apply {
        inventoryModel = var1.inventoryModel
        zoom2d = var1.zoom2d
        xan2d = var1.xan2d
        yan2d = var1.yan2d
        zan2d = var1.zan2d
        xOffset2d = var1.xOffset2d
        yOffset2d = var1.yOffset2d
        originalTextureColours = var2.originalTextureColours?.toMutableList()
        modifiedColours = var2.modifiedColours?.toMutableList()
        modifiedTextureColours = var2.modifiedTextureColours?.toMutableList()
        name = var2.name
        members = var2.members
        stacks = var2.stacks
        maleModel0 = var2.maleModel0
        maleModel1 = var2.maleModel1
        maleModel2 = var2.maleModel2
        femaleModel0 = var2.femaleModel0
        femaleModel1 = var2.femaleModel1
        femaleModel2 = var2.femaleModel2
        maleHeadModel0 = var2.maleHeadModel0
        maleHeadModel1 = var2.maleHeadModel1
        femaleHeadModel0 = var2.femaleHeadModel0
        femaleHeadModel1 = var2.femaleHeadModel1
        teamCape = var2.teamCape
        options = var2.options
        interfaceOptions = arrayOfNulls<String>(5).toMutableList()
        for (var3 in 0..3) {
            interfaceOptions[var3] = var2.interfaceOptions[var3]
        }
        interfaceOptions[4] = "Discard"
        cost = 0
    }

    fun linkPlaceholder(var1: ItemType, var2: ItemType) = apply {
        inventoryModel = var1.inventoryModel
        zoom2d = var1.zoom2d
        xan2d = var1.xan2d
        yan2d = var1.yan2d
        zan2d = var1.zan2d
        xOffset2d = var1.xOffset2d
        yOffset2d = var1.yOffset2d
        originalTextureColours = var1.originalTextureColours?.toMutableList()
        modifiedTextureColours = var1.modifiedTextureColours?.toMutableList()
        stacks = var1.stacks
        name = var2.name
        cost = 0
        members = false
        stockMarket = false
    }

    fun build(): ItemType {
        val built = ItemType(
            id = id,
            name = name,
            examine = examine,
            originalColours = IntListPool.of(originalColours),
            modifiedColours = IntListPool.of(modifiedColours),
            originalTextureColours = IntListPool.of(originalTextureColours),
            modifiedTextureColours = IntListPool.of(modifiedTextureColours),
            params = params,
            resizeX = resizeX,
            resizeY = resizeY,
            resizeZ = resizeZ,
            xan2d = xan2d,
            category = category,
            yan2d = yan2d,
            zan2d = zan2d,
            equipSlot = equipSlot,
            appearanceOverride1 = appearanceOverride1,
            appearanceOverride2 = appearanceOverride2,
            weight = weight,
            cost = cost,
            stockMarket = stockMarket,
            tradeable = tradeable,
            stacks = stacks,
            inventoryModel = inventoryModel,
            members = members,
            zoom2d = zoom2d,
            xOffset2d = xOffset2d,
            yOffset2d = yOffset2d,
            ambient = ambient,
            contrast = contrast,
            countCo = IntListPool.of(countCo),
            countObj = IntListPool.of(countObj),
            options = options,
            interfaceOptions = interfaceOptions,
            maleModel0 = maleModel0,
            maleModel1 = maleModel1,
            maleModel2 = maleModel2,
            maleOffset = maleOffset,
            maleHeadModel0 = maleHeadModel0,
            maleHeadModel1 = maleHeadModel1,
            femaleModel0 = femaleModel0,
            femaleModel1 = femaleModel1,
            femaleModel2 = femaleModel2,
            femaleOffset = femaleOffset,
            femaleHeadModel0 = femaleHeadModel0,
            femaleHeadModel1 = femaleHeadModel1,
            noteLinkId = noteLinkId,
            noteTemplateId = noteTemplateId,
            teamCape = teamCape,
            dropOptionIndex = dropOptionIndex,
            unnotedId = unnotedId,
            notedId = notedId,
            placeholderLink = placeholderLink,
            placeholderTemplate = placeholderTemplate,
            subops = subops,
        )
        if (extras.isNotEmpty()) {
            built.extra.putAll(extras)
        }
        return built
    }

    companion object {
        fun from(type: ItemType): ItemTypeBuilder {
            val builder = ItemTypeBuilder(type.id)
            builder.name = type.name
            builder.examine = type.examine
            builder.originalColours = type.originalColours?.toMutableList()
            builder.modifiedColours = type.modifiedColours?.toMutableList()
            builder.originalTextureColours = type.originalTextureColours?.toMutableList()
            builder.modifiedTextureColours = type.modifiedTextureColours?.toMutableList()
            builder.params = type.params?.toMutableMap()
            builder.resizeX = type.resizeX
            builder.resizeY = type.resizeY
            builder.resizeZ = type.resizeZ
            builder.xan2d = type.xan2d
            builder.category = type.category
            builder.yan2d = type.yan2d
            builder.zan2d = type.zan2d
            builder.equipSlot = type.equipSlot
            builder.appearanceOverride1 = type.appearanceOverride1
            builder.appearanceOverride2 = type.appearanceOverride2
            builder.weight = type.weight
            builder.cost = type.cost
            builder.stockMarket = type.stockMarket
            builder.tradeable = type.tradeable
            builder.stacks = type.stacks
            builder.inventoryModel = type.inventoryModel
            builder.members = type.members
            builder.zoom2d = type.zoom2d
            builder.xOffset2d = type.xOffset2d
            builder.yOffset2d = type.yOffset2d
            builder.ambient = type.ambient
            builder.contrast = type.contrast
            builder.countCo = type.countCo?.toMutableList()
            builder.countObj = type.countObj?.toMutableList()
            builder.options = type.options
            builder.interfaceOptions = type.interfaceOptions.toMutableList()
            builder.maleModel0 = type.maleModel0
            builder.maleModel1 = type.maleModel1
            builder.maleModel2 = type.maleModel2
            builder.maleOffset = type.maleOffset
            builder.maleHeadModel0 = type.maleHeadModel0
            builder.maleHeadModel1 = type.maleHeadModel1
            builder.femaleModel0 = type.femaleModel0
            builder.femaleModel1 = type.femaleModel1
            builder.femaleModel2 = type.femaleModel2
            builder.femaleOffset = type.femaleOffset
            builder.femaleHeadModel0 = type.femaleHeadModel0
            builder.femaleHeadModel1 = type.femaleHeadModel1
            builder.noteLinkId = type.noteLinkId
            builder.noteTemplateId = type.noteTemplateId
            builder.teamCape = type.teamCape
            builder.dropOptionIndex = type.dropOptionIndex
            builder.unnotedId = type.unnotedId
            builder.notedId = type.notedId
            builder.placeholderLink = type.placeholderLink
            builder.placeholderTemplate = type.placeholderTemplate
            builder.subops = type.subops
            return builder
        }
    }
}
