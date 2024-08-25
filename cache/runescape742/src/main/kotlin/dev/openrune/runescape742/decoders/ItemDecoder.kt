package dev.openrune.runescape742.decoders

import dev.openrune.Index.ITEMS
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.definition.data.ItemType

class ItemDecoder : DefinitionDecoder<ItemType>(ITEMS) {

    override fun create(size: Int) = Array(size) { ItemType(it) }

    override fun getFile(id: Int) = id and 0xff

    override fun getArchive(id: Int) = id ushr 8

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
            18 -> buffer.readShort()
            23 -> maleModel0 = buffer.readBigSmart()
            24 -> maleModel1 = buffer.readBigSmart()
            25 -> femaleModel0 = buffer.readBigSmart()
            26 -> femaleModel1 = buffer.readBigSmart()
            in 30..34 -> options[opcode - 30] = buffer.readString()
            in 35..39 -> interfaceOptions[opcode - 35] = buffer.readString()
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            42 -> readColourPalette(buffer)
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
                countObj[opcode - 100] = buffer.readShort()
                countCo[opcode - 100] = buffer.readShort()
            }
            110 -> resizeX = buffer.readShort()
            111 -> resizeY = buffer.readShort()
            112 -> resizeZ = buffer.readShort()
            113 -> ambient = buffer.readByte()
            114 -> contrast = buffer.readByte() * 5
            115 -> teamCape = buffer.readUnsignedByte()
            121 -> {
                val lendId = buffer.readShort()
            }
            122 -> {
                val lendTemplateId = buffer.readShort()
            }
            125 -> {
                maleOffset = buffer.readByte() shl 2
                val maleWieldZ = buffer.readByte() shl 2
                val maleWieldY = buffer.readByte() shl 2
            }
            126 -> {
                femaleOffset = buffer.readByte() shl 2
                val femaleWieldZ = buffer.readByte() shl 2
                val femaleWieldY = buffer.readByte() shl 2
            }
            127 -> {
                val primaryCursorOpcode = buffer.readUnsignedByte()
                val primaryCursor = buffer.readShort()
            }
            128 -> {
                val secondaryCursorOpcode = buffer.readUnsignedByte()
                val secondaryCursor = buffer.readShort()
            }
            129 -> {
                val primaryInterfaceCursorOpcode = buffer.readUnsignedByte()
                val primaryInterfaceCursor = buffer.readShort()
            }
            130 -> {
                val secondaryInterfaceCursorOpcode = buffer.readUnsignedByte()
                val secondaryInterfaceCursor = buffer.readShort()
            }
            132 -> {
                val length = buffer.readUnsignedByte()
                val campaigns = IntArray(length) { buffer.readShort() }
            }
            134 -> {
                val pickSizeShift = buffer.readUnsignedByte()
            }
            139 -> unnotedId = buffer.readShort()
            140 -> notedId = buffer.readShort()
            249 -> readParameters(buffer)
        }
    }

    private fun readColourPalette(buffer: Reader) {
        val length = buffer.readUnsignedByte()
        ByteArray(length) { buffer.readByte().toByte() }
    }

}
