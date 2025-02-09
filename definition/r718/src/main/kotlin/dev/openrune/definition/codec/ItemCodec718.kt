package dev.openrune.definition.codec

import dev.openrune.buffer.*
import io.netty.buffer.ByteBuf
import dev.openrune.buffer.Writer
import dev.openrune.buffer.readBigSmartRD
import dev.openrune.buffer.readStringRD
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.ItemType

fun ItemType.getPrimaryCursorOpcode(): Int {
    return getIntProperty("primaryCursorOpcode")
}

fun ItemType.getPrimaryCursor(): Int {
    return getIntProperty("primaryCursor")
}

fun ItemType.getSecondaryCursorOpcode(): Int {
    return getIntProperty("Int")
}

fun ItemType.getSecondaryCursor(): Int {
    return getIntProperty("secondaryCursor")
}

fun ItemType.getCampaigns(): IntArray {
    return getIntArrayProperty("campaigns")
}

fun ItemType.getPickSizeShift(): Int {
    return getIntProperty("pickSizeShift")
}

fun ItemType.getLendId(): Int {
    return getIntProperty("lendId")
}

fun ItemType.getLendTemplateId(): Int {
    return getIntProperty("lendTemplateId")
}

fun ItemType.getMaleWieldZ(): Int {
    return getIntProperty("maleWieldZ")
}

fun ItemType.getMaleWieldY(): Int {
    return getIntProperty("maleWieldY")
}

fun ItemType.getFemaleWieldZ(): Int {
    return getIntProperty("femaleWieldZ")
}

fun ItemType.getFemaleWieldY(): Int {
    return getIntProperty("femaleWieldY")
}

fun ItemType.getUnknown18(): Int {
    return getIntProperty("unknown18")
}

fun ItemType.getAppearanceOverride1(): Int {
    return getIntProperty("appearanceOverride1")
}

fun ItemType.getNoteLinkId(): Int {
    return getIntProperty("noteLinkId")
}

fun ItemType.getNoteTemplateId(): Int {
    return getIntProperty("noteTemplateId")
}

class ItemCodec718 : DefinitionCodec<ItemType> {
    override fun ItemType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> inventoryModel = buffer.readBigSmartRD()
            2 -> name = buffer.readStringRD()
            4 -> zoom2d = buffer.readShortRD()
            5 -> xan2d = buffer.readShortRD()
            6 -> yan2d = buffer.readShortRD()
            7 -> xOffset2d = buffer.readShortRD()
            8 -> yOffset2d = buffer.readShortRD()
            11 -> stacks = 1
            12 -> cost = buffer.readIntRD()
            13 -> equipSlot = buffer.readUnsignedByteRD()
            14 -> appearanceOverride1 = buffer.readUnsignedByteRD()
            16 -> members = true
            18 -> setExtraProperty("multiStackSize", buffer.readShortRD())
            23 -> maleModel0 = buffer.readBigSmartRD()
            24 -> maleModel1 = buffer.readBigSmartRD()
            25 -> femaleModel0 = buffer.readBigSmartRD()
            26 -> femaleModel1 = buffer.readBigSmartRD()
            in 30..34 -> options[opcode - 30] = buffer.readStringRD()
            in 35..39 -> interfaceOptions[opcode - 35] = buffer.readStringRD()
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            42 -> setExtraProperty("colourPalette",readColourPalette(buffer))
            65 -> isTradeable = true
            78 -> maleModel2 = buffer.readBigSmartRD()
            79 -> femaleModel2 = buffer.readBigSmartRD()
            90 -> maleHeadModel0 = buffer.readBigSmartRD()
            91 -> femaleHeadModel0 = buffer.readBigSmartRD()
            92 -> maleHeadModel1 = buffer.readBigSmartRD()
            93 -> femaleHeadModel1 = buffer.readBigSmartRD()
            95 -> zan2d = buffer.readShortRD()
            96 -> category = buffer.readUnsignedByteRD()
            97 -> noteLinkId = buffer.readShortRD()
            98 -> noteTemplateId = buffer.readShortRD()
            in 100..109 -> {
                if (countCo == null) {
                    countObj = MutableList(10) { 0 }
                    countCo = MutableList(10) { 0 }
                }
                countObj!![opcode - 100] = buffer.readShortRD()
                countCo!![opcode - 100] = buffer.readShortRD()
            }
            110 -> resizeX = buffer.readShortRD()
            111 -> resizeY = buffer.readShortRD()
            112 -> resizeZ = buffer.readShortRD()
            113 -> ambient = buffer.readByteRD()
            114 -> contrast = buffer.readByteRD() * 5
            115 -> teamCape = buffer.readUnsignedByteRD()
            121 -> setExtraProperty("lendId", buffer.readShortRD())
            122 -> setExtraProperty("lendTemplateId", buffer.readShortRD())
            125 -> {
                maleOffset = buffer.readByteRD() shl 2
                setExtraProperty("maleWieldZ", buffer.readByteRD() shl 2)
                setExtraProperty("maleWieldY", buffer.readByteRD() shl 2)
            }
            126 -> {
                femaleOffset = buffer.readByteRD() shl 2
                setExtraProperty("femaleWieldZ", buffer.readByteRD() shl 2)
                setExtraProperty("femaleWieldY", buffer.readByteRD() shl 2)
            }
            127 -> {
                setExtraProperty("primaryCursorOpcode", buffer.readUnsignedByteRD())
                setExtraProperty("primaryCursor", buffer.readShortRD())
            }
            128 -> {
                setExtraProperty("secondaryCursorOpcode", buffer.readUnsignedByteRD())
                setExtraProperty("secondaryCursor", buffer.readShortRD())
            }
            129 -> {
                setExtraProperty("primaryInterfaceCursorOpcode", buffer.readUnsignedByteRD())
                setExtraProperty("primaryInterfaceCursor", buffer.readShortRD())
            }
            130 -> {
                setExtraProperty("secondaryInterfaceCursorOpcode", buffer.readUnsignedByteRD())
                setExtraProperty("secondaryInterfaceCursor", buffer.readShortRD())
            }
            132 -> {
                val length = buffer.readUnsignedByteRD()
                setExtraProperty("campaigns", IntArray(length) { buffer.readShortRD() })
            }
            134 -> setExtraProperty("pickSizeShift", buffer.readUnsignedByteRD())
            139 -> unnotedId = buffer.readShortRD()
            140 -> notedId = buffer.readShortRD()
            249 -> readParameters(buffer)
        }
    }

    override fun Writer.encode(definition: ItemType) {
        TODO("Not yet implemented")
    }

    private fun readColourPalette(buffer: ByteBuf) {
        val length = buffer.readUnsignedByteRD()
        ByteArray(length) { buffer.readByteRD().toByte() }
    }

    override fun createDefinition() = ItemType()
}