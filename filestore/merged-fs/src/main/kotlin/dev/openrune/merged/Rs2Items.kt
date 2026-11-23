package dev.openrune.merged

import dev.openrune.definition.EntityOpsBuilder
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.type.ObjStackability
import dev.openrune.definition.type.Rs2ObjType

fun Rs2ObjType.toItemType(id: Int): ItemType {
    val obj = this
    val item = ItemType(
        id = id,
        name = obj.name ?: "null",
        examine = obj.description ?: "null",
        cost = (obj.cost ?: 0L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
        members = obj.members,
        tradeable = obj.tradeable,
        stockMarket = obj.stockMarket,
        category = obj.category ?: -1,
        stacks = if (obj.stackable == null) ObjStackability.Sometimes else ObjStackability.Always,
        inventoryModel = obj.models[MODEL_SLOT] ?: obj.modelList?.firstOrNull() ?: 0,
        equipSlot = obj.wearPos ?: -1,
        teamCape = obj.team ?: 0,
        zoom2d = obj.zoom2d ?: 2000,
        xan2d = obj.xan2d ?: 0,
        yan2d = obj.yan2d ?: 0,
        zan2d = obj.zan2d ?: 0,
        xOffset2d = obj.xOffset2d ?: 0,
        yOffset2d = obj.yOffset2d ?: 0,
        resizeX = obj.resizeX ?: 128,
        resizeY = obj.resizeY ?: 128,
        resizeZ = obj.resizeZ ?: 128,
        ambient = obj.ambient ?: 0,
        contrast = obj.contrast ?: 0,
        originalColours = obj.originalColours,
        modifiedColours = obj.modifiedColours,
        originalTextureColours = obj.originalTextureColours,
        modifiedTextureColours = obj.modifiedTextureColours,
        params = obj.params,
        options = obj.ops.toOps(OP_FIRST),
        interfaceOptions = obj.interfaceOps.toInterfaceOptions(),
    )
    item.setExtraProperty(RS2_PROPERTY, obj)
    return item
}

private fun Map<Int, String>.toOps(first: Int) = EntityOpsBuilder().apply {
    forEach { (opcode, text) -> op(opcode - first, text) }
}.build()

private fun Map<Int, String>.toInterfaceOptions(): MutableList<String?> {
    val options = MutableList<String?>(INTERFACE_OP_SLOTS) { null }
    forEach { (opcode, text) ->
        val slot = opcode - INTERFACE_OP_FIRST
        if (slot in options.indices) options[slot] = text
    }
    return options
}

private const val MODEL_SLOT = 1
private const val OP_FIRST = 30
private const val INTERFACE_OP_FIRST = 35
private const val INTERFACE_OP_SLOTS = 5
