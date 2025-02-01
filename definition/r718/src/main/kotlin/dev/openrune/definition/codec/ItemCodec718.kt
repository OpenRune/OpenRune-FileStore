package dev.openrune.definition.codec

import dev.openrune.buffer.Reader
import dev.openrune.buffer.Writer
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
    override fun ItemType.read(opcode: Int, buffer: Reader) {
        when (opcode) {
            1 -> inventoryModel = buffer.readBigSmart()
            2 -> name = buffer.readString()
            4 -> zoom2d = buffer.readShort()
            5 -> xan2d = buffer.readShort()
            6 -> yan2d = buffer.readShort()
            7 -> xOffset2d = buffer.readShort()
            8 -> yOffset2d = buffer.readShort()
            11 -> stacks = 1
            12 -> cost = buffer.readInt()
            13 -> equipSlot = buffer.readUnsignedByte()
            14 -> appearanceOverride1 = buffer.readUnsignedByte()
            16 -> members = true
            18 -> setExtraProperty("multiStackSize", buffer.readShort())
            23 -> maleModel0 = buffer.readBigSmart()
            24 -> maleModel1 = buffer.readBigSmart()
            25 -> femaleModel0 = buffer.readBigSmart()
            26 -> femaleModel1 = buffer.readBigSmart()
            in 30..34 -> options[opcode - 30] = buffer.readString()
            in 35..39 -> interfaceOptions[opcode - 35] = buffer.readString()
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            42 -> setExtraProperty("colourPalette",readColourPalette(buffer))
            65 -> isTradeable = true
            78 -> maleModel2 = buffer.readBigSmart()
            79 -> femaleModel2 = buffer.readBigSmart()
            90 -> maleHeadModel0 = buffer.readBigSmart()
            91 -> femaleHeadModel0 = buffer.readBigSmart()
            92 -> maleHeadModel1 = buffer.readBigSmart()
            93 -> femaleHeadModel1 = buffer.readBigSmart()
            95 -> zan2d = buffer.readShort()
            96 -> category = buffer.readUnsignedByte()
            97 -> noteLinkId = buffer.readShort()
            98 -> noteTemplateId = buffer.readShort()
            in 100..109 -> {
                if (countCo == null) {
                    countObj = MutableList(10) { 0 }
                    countCo = MutableList(10) { 0 }
                }
                countObj!![opcode - 100] = buffer.readShort()
                countCo!![opcode - 100] = buffer.readShort()
            }
            110 -> resizeX = buffer.readShort()
            111 -> resizeY = buffer.readShort()
            112 -> resizeZ = buffer.readShort()
            113 -> ambient = buffer.readByte()
            114 -> contrast = buffer.readByte() * 5
            115 -> teamCape = buffer.readUnsignedByte()
            121 -> setExtraProperty("lendId", buffer.readShort())
            122 -> setExtraProperty("lendTemplateId", buffer.readShort())
            125 -> {
                maleOffset = buffer.readByte() shl 2
                setExtraProperty("maleWieldZ", buffer.readByte() shl 2)
                setExtraProperty("maleWieldY", buffer.readByte() shl 2)
            }
            126 -> {
                femaleOffset = buffer.readByte() shl 2
                setExtraProperty("femaleWieldZ", buffer.readByte() shl 2)
                setExtraProperty("femaleWieldY", buffer.readByte() shl 2)
            }
            127 -> {
                setExtraProperty("primaryCursorOpcode", buffer.readUnsignedByte())
                setExtraProperty("primaryCursor", buffer.readShort())
            }
            128 -> {
                setExtraProperty("secondaryCursorOpcode", buffer.readUnsignedByte())
                setExtraProperty("secondaryCursor", buffer.readShort())
            }
            129 -> {
                setExtraProperty("primaryInterfaceCursorOpcode", buffer.readUnsignedByte())
                setExtraProperty("primaryInterfaceCursor", buffer.readShort())
            }
            130 -> {
                setExtraProperty("secondaryInterfaceCursorOpcode", buffer.readUnsignedByte())
                setExtraProperty("secondaryInterfaceCursor", buffer.readShort())
            }
            132 -> {
                val length = buffer.readUnsignedByte()
                setExtraProperty("campaigns", IntArray(length) { buffer.readShort() })
            }
            134 -> setExtraProperty("pickSizeShift", buffer.readUnsignedByte())
            139 -> unnotedId = buffer.readShort()
            140 -> notedId = buffer.readShort()
            249 -> readParameters(buffer)
        }
    }

    override fun Writer.encode(definition: ItemType) {
        TODO("Not yet implemented")
    }

    private fun readColourPalette(buffer: Reader) {
        val length = buffer.readUnsignedByte()
        ByteArray(length) { buffer.readByte().toByte() }
    }

    override fun createDefinition() = ItemType()
}