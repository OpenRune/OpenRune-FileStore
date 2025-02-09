package dev.openrune.definition.codec

import io.netty.buffer.ByteBuf
import dev.openrune.buffer.Writer
import dev.openrune.buffer.readBigSmart
import dev.openrune.buffer.readString
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
            1 -> inventoryModel = buffer.readBigSmart()
            2 -> name = buffer.readString()
            4 -> zoom2d = buffer.readShort().toInt()
            5 -> xan2d = buffer.readShort().toInt()
            6 -> yan2d = buffer.readShort().toInt()
            7 -> xOffset2d = buffer.readShort().toInt()
            8 -> yOffset2d = buffer.readShort().toInt()
            11 -> stacks = 1
            12 -> cost = buffer.readInt()
            13 -> equipSlot = buffer.readUnsignedByte().toInt()
            14 -> appearanceOverride1 = buffer.readUnsignedByte().toInt()
            16 -> members = true
            18 -> setExtraProperty("multiStackSize", buffer.readShort().toInt())
            23 -> maleModel0 = buffer.readBigSmart()
            24 -> maleModel1 = buffer.readBigSmart()
            25 -> femaleModel0 = buffer.readBigSmart()
            26 -> femaleModel1 = buffer.readBigSmart()
            in 30..34 -> options[opcode - 30] = buffer.readString()
            in 35..39 -> interfaceOptions[opcode - 35] = buffer.readString()
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            42 -> setExtraProperty("colourPalette", readColourPalette(buffer))
            65 -> isTradeable = true
            78 -> maleModel2 = buffer.readBigSmart()
            79 -> femaleModel2 = buffer.readBigSmart()
            90 -> maleHeadModel0 = buffer.readBigSmart()
            91 -> femaleHeadModel0 = buffer.readBigSmart()
            92 -> maleHeadModel1 = buffer.readBigSmart()
            93 -> femaleHeadModel1 = buffer.readBigSmart()
            95 -> zan2d = buffer.readShort().toInt()
            96 -> category = buffer.readUnsignedByte().toInt()
            97 -> noteLinkId = buffer.readShort().toInt()
            98 -> noteTemplateId = buffer.readShort().toInt()
            in 100..109 -> {
                if (countCo == null) {
                    countObj = MutableList(10) { 0 }
                    countCo = MutableList(10) { 0 }
                }
                countObj!![opcode - 100] = buffer.readShort().toInt()
                countCo!![opcode - 100] = buffer.readShort().toInt()
            }

            110 -> resizeX = buffer.readShort().toInt()
            111 -> resizeY = buffer.readShort().toInt()
            112 -> resizeZ = buffer.readShort().toInt()
            113 -> ambient = buffer.readByte().toInt()
            114 -> contrast = buffer.readByte().toInt() * 5
            115 -> teamCape = buffer.readUnsignedByte().toInt()
            121 -> setExtraProperty("lendId", buffer.readShort().toInt())
            122 -> setExtraProperty("lendTemplateId", buffer.readShort().toInt())
            125 -> {
                maleOffset = buffer.readByte().toInt() shl 2
                setExtraProperty("maleWieldZ", buffer.readByte().toInt() shl 2)
                setExtraProperty("maleWieldY", buffer.readByte().toInt() shl 2)
            }

            126 -> {
                femaleOffset = buffer.readByte().toInt() shl 2
                setExtraProperty("femaleWieldZ", buffer.readByte().toInt() shl 2)
                setExtraProperty("femaleWieldY", buffer.readByte().toInt() shl 2)
            }

            127 -> {
                setExtraProperty("primaryCursorOpcode", buffer.readUnsignedByte().toInt())
                setExtraProperty("primaryCursor", buffer.readShort().toInt())
            }

            128 -> {
                setExtraProperty("secondaryCursorOpcode", buffer.readUnsignedByte().toInt())
                setExtraProperty("secondaryCursor", buffer.readShort().toInt())
            }

            129 -> {
                setExtraProperty("primaryInterfaceCursorOpcode", buffer.readUnsignedByte().toInt())
                setExtraProperty("primaryInterfaceCursor", buffer.readShort().toInt())
            }

            130 -> {
                setExtraProperty("secondaryInterfaceCursorOpcode", buffer.readUnsignedByte().toInt())
                setExtraProperty("secondaryInterfaceCursor", buffer.readShort().toInt())
            }

            132 -> {
                val length = buffer.readUnsignedByte().toInt()
                setExtraProperty("campaigns", IntArray(length) { buffer.readShort().toInt() })
            }

            134 -> setExtraProperty("pickSizeShift", buffer.readUnsignedByte().toInt())
            139 -> unnotedId = buffer.readShort().toInt()
            140 -> notedId = buffer.readShort().toInt()
            249 -> readParameters(buffer)
        }
    }

    override fun Writer.encode(definition: ItemType) {
        TODO("Not yet implemented")
    }

    private fun readColourPalette(buffer: ByteBuf) {
        val length = buffer.readUnsignedByte().toInt()
        ByteArray(length) { buffer.readByte().toInt().toByte() }
    }

    override fun createDefinition() = ItemType()
}