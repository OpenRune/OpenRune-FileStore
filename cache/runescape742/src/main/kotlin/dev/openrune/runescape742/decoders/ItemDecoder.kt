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
            1 -> {
                inventoryModel = buffer.readBigSmart()
            }
            2 -> {
                name = buffer.readString()
            }
            // Other opcodes are recognized but no assignments are performed
            4 -> {
                buffer.readShort().toInt()
            }
            5 -> {
                buffer.readShort().toInt()
            }
            6 -> {
                buffer.readShort().toInt()
            }
            7 -> {
                var xOffset2d = buffer.readShort().toInt()
                if (xOffset2d > 32767) {
                    xOffset2d -= 65536
                }
            }
            8 -> {
                var yOffset2d = buffer.readShort().toInt()
                if (yOffset2d > 32767) {
                    yOffset2d -= 65536
                }
            }
            11 -> {
                buffer.readBoolean()  // stackable
            }
            12 -> {
                buffer.readInt()  // cost
            }
            13 -> {
                buffer.readUnsignedByte().toInt()  // equipSlot
            }
            14 -> {
                buffer.readUnsignedByte().toInt()  // appearanceOverride1
            }
            16 -> {
                buffer.readBoolean()  // members
            }
            18 -> {
                buffer.readShort().toInt()  // multiStackSize
            }
            23 -> {
                buffer.readBigSmart()  // primaryMaleModel
            }
            24 -> {
                buffer.readBigSmart()  // secondaryMaleModel
            }
            25 -> {
                buffer.readBigSmart()  // primaryFemaleModel
            }
            26 -> {
                buffer.readBigSmart()  // secondaryFemaleModel
            }
            in 30..34 -> {
                buffer.readString()  // groundActions
            }
            in 35..39 -> {
                buffer.readString()  // inventoryActions
            }
            40 -> {
                val size = buffer.readUnsignedByte().toInt()
                val recolorFrom = ShortArray(size)
                val recolorTo = ShortArray(size)
                for (i in 0 until size) {
                    recolorFrom[i] = buffer.readShort().toShort()
                    recolorTo[i] = buffer.readShort().toShort()
                }
            }
            41 -> {
                val size = buffer.readUnsignedByte().toInt()
                val retextureFrom = ShortArray(size)
                val retextureTo = ShortArray(size)
                for (i in 0 until size) {
                    buffer.readShort().toShort()
                    buffer.readShort().toShort()
                }
            }
            42 -> {
                val length = buffer.readUnsignedByte().toInt()
                val recolourPallete = ByteArray(length)
                for (index in 0 until length) {
                    recolourPallete[index] = buffer.readByte().toByte()
                }
            }
            65 -> {
                buffer.readBoolean()  // isTradable
            }
            78 -> {
                buffer.readBigSmart()  // tertiaryMaleModel
            }
            79 -> {
                buffer.readBigSmart()  // tertiaryFemaleModel
            }
            90 -> {
                buffer.readBigSmart()  // primaryMaleDialogueHead
            }
            91 -> {
                buffer.readBigSmart()  // primaryFemaleDialogueHead
            }
            92 -> {
                buffer.readBigSmart()  // secondaryMaleDialogueHead
            }
            93 -> {
                buffer.readBigSmart()  // secondaryFemaleDialogueHead
            }
            95 -> {
                buffer.readShort().toInt()  // zan2d
            }
            96 -> {
                buffer.readUnsignedByte().toInt()  // category
            }
            97 -> {
                buffer.readShort().toInt()  // notedID
            }
            98 -> {
                buffer.readShort().toInt()  // notedTemplate
            }
            in 100..109 -> {
                buffer.readShort().toInt()
                buffer.readShort().toInt()
            }
            110 -> {
                buffer.readShort().toInt()  // resizeX
            }
            111 -> {
                buffer.readShort().toInt()  // resizeY
            }
            112 -> {
                buffer.readShort().toInt()  // resizeZ
            }
            113 -> {
                buffer.readByte()  // ambient
            }
            114 -> {
                buffer.readByte() * 5  // contrast
            }
            115 -> {
                buffer.readUnsignedByte().toInt()  // team
            }
            121 -> {
                buffer.readShort().toInt()  // lendId
            }
            122 -> {
                buffer.readShort().toInt()  // lendTemplateId
            }
            125 -> {
                buffer.readByte().toInt() shl 2
                buffer.readByte().toInt() shl 2
                buffer.readByte().toInt() shl 2  // maleWieldX, maleWieldZ, maleWieldY
            }
            126 -> {
                buffer.readByte().toInt() shl 2
                buffer.readByte().toInt() shl 2
                buffer.readByte().toInt() shl 2  // femaleWieldX, femaleWieldZ, femaleWieldY
            }
            127 -> {
                buffer.readUnsignedByte().toInt()  // primaryCursorOpcode
                buffer.readShort().toInt()  // primaryCursor
            }
            128 -> {
                buffer.readUnsignedByte().toInt()  // secondaryCursorOpcode
                buffer.readShort().toInt()  // secondaryCursor
            }
            129 -> {
                buffer.readUnsignedByte().toInt()  // primaryInterfaceCursorOpcode
                buffer.readShort().toInt()  // primaryInterfaceCursor
            }
            130 -> {
                buffer.readUnsignedByte().toInt()  // secondaryInterfaceCursorOpcode
                buffer.readShort().toInt()  // secondaryInterfaceCursor
            }
            132 -> {
                val length = buffer.readUnsignedByte().toInt()
                val campaigns = IntArray(length)
                for (i in 0 until length) {
                    campaigns[i] = buffer.readShort().toInt()
                }
            }
            134 -> {
                buffer.readUnsignedByte().toInt()  // pickSizeShift
            }
            139 -> {
                buffer.readShort().toInt()  // singleNoteId
            }
            140 -> {
                buffer.readShort().toInt()  // singleNoteTemplateId
            }
            249 -> {
                 readParameters(buffer)
            }
        }
    }

    fun readColourPalette(buffer: Reader) {
        val length = buffer.readUnsignedByte()
         ByteArray(length) { buffer.readByte().toByte() }
    }

}