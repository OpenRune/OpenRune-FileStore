package dev.openrune.definition.codec

import dev.openrune.buffer.*
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.ItemType
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf

class ItemCodec : DefinitionCodec<ItemType> {
    override fun ItemType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> inventoryModel = buffer.readUnsignedShort()
            2 -> name = buffer.readString()
            3 -> examine = buffer.readString()
            4 -> zoom2d = buffer.readUnsignedShort()
            5 -> xan2d = buffer.readUnsignedShort()
            6 -> yan2d = buffer.readUnsignedShort()
            7 -> {
                xOffset2d = buffer.readUnsignedShort()
                if (xOffset2d > 32767) {
                    xOffset2d -= 65536
                }
            }

            8 -> {
                yOffset2d = buffer.readUnsignedShort()
                if (yOffset2d > 32767) {
                    yOffset2d -= 65536;
                }
            }

            11 -> stacks = 1
            12 -> cost = buffer.readInt()
            13 -> equipSlot = buffer.readUnsignedByte().toInt()
            14 -> appearanceOverride1 = buffer.readUnsignedByte().toInt()
            16 -> members = true
            23 -> {
                maleModel0 = buffer.readUnsignedShort()
                maleOffset = buffer.readUnsignedByte().toInt()
            }

            24 -> maleModel1 = buffer.readUnsignedShort()
            25 -> {
                femaleModel0 = buffer.readUnsignedShort()
                femaleOffset = buffer.readUnsignedByte().toInt()
            }

            26 -> femaleModel1 = buffer.readUnsignedShort()
            27 -> appearanceOverride2 = buffer.readByte().toInt()
            in 30..34 -> options[opcode - 30] = buffer.readString()
            in 35..39 -> interfaceOptions[opcode - 35] = buffer.readString()
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            42 -> dropOptionIndex = buffer.readByte().toInt()
            43 -> {
                val opId = buffer.readUnsignedByte().toInt()
                if (subops == null) {
                    subops = arrayOfNulls(5)
                }

                val valid = opId in 0..4
                if (valid && subops!![opId] == null) {
                    subops!![opId] = arrayOfNulls(20)
                }

                while (true) {
                    val subopId = buffer.readUnsignedByte().toInt() - 1
                    if (subopId == -1) {
                        break
                    }

                    val op = buffer.readString()
                    if (valid && subopId in 0..19) {
                        subops!![opId]?.set(subopId, op)
                    }
                }
            }

            65 -> isTradeable = true
            75 -> weight = buffer.readUnsignedShort().toDouble()
            78 -> maleModel2 = buffer.readUnsignedShort()
            79 -> femaleModel2 = buffer.readUnsignedShort()
            90 -> maleHeadModel0 = buffer.readUnsignedShort()
            91 -> femaleHeadModel0 = buffer.readUnsignedShort()
            92 -> maleHeadModel1 = buffer.readUnsignedShort()
            93 -> femaleHeadModel1 = buffer.readUnsignedShort()
            94 -> category = buffer.readUnsignedShort()
            95 -> zan2d = buffer.readUnsignedShort()
            97 -> noteLinkId = buffer.readUnsignedShort()
            98 -> noteTemplateId = buffer.readUnsignedShort()
            in 100..109 -> {
                if (countCo == null) {
                    countObj = MutableList(10) { 0 }
                    countCo = MutableList(10) { 0 }
                }
                countObj!![opcode - 100] = buffer.readUnsignedShort()
                countCo!![opcode - 100] = buffer.readUnsignedShort()
            }

            110 -> resizeX = buffer.readUnsignedShort()
            111 -> resizeY = buffer.readUnsignedShort()
            112 -> resizeZ = buffer.readUnsignedShort()
            113 -> ambient = buffer.readByte().toInt()
            114 -> contrast = buffer.readByte().toInt()
            115 -> teamCape = buffer.readByte().toInt()
            139 -> unnotedId = buffer.readUnsignedShort()
            140 -> notedId = buffer.readUnsignedShort()
            148 -> placeholderLink = buffer.readUnsignedShort()
            149 -> placeholderTemplate = buffer.readUnsignedShort()
            249 -> readParameters(buffer)
            else -> dev.openrune.definition.codec.ItemCodec.logger.info { "Unable to decode Items [${opcode}]" }
        }
    }

    override fun Writer.encode(definition: ItemType) {
        if (definition.inventoryModel != 0) {
            writeByte(1)
            writeShort(definition.inventoryModel)
        }

        if (!definition.name.equals("null", ignoreCase = true)) {
            writeByte(2)
            writeString(definition.name)
        }

        if (!definition.examine.equals("null", ignoreCase = true)) {
            writeByte(3)
            writeString(definition.examine)
        }

        if (definition.zoom2d != 2000) {
            writeByte(4)
            writeShort(definition.zoom2d)
        }

        if (definition.xan2d != 0) {
            writeByte(5)
            writeShort(definition.xan2d)
        }

        if (definition.yan2d != 0) {
            writeByte(6)
            writeShort(definition.yan2d)
        }

        if (definition.xOffset2d != 0) {
            writeByte(7)
            writeShort(definition.xOffset2d)
        }

        if (definition.yOffset2d != 0) {
            writeByte(8)
            writeShort(definition.yOffset2d)
        }

        if (definition.stacks == 1) {
            writeByte(11)
        }

        if (definition.cost != 1) {
            writeByte(12)
            writeInt(definition.cost)
        }

        if (definition.equipSlot != -1) {
            writeByte(13)
            writeByte(definition.equipSlot)
        }

        if (definition.appearanceOverride1 != -1) {
            writeByte(14)
            writeByte(definition.appearanceOverride1)
        }

        if (definition.members) {
            writeByte(16)
        }

        if (definition.maleModel0 != -1 || definition.maleOffset != 0) {
            writeByte(23)
            writeShort(definition.maleModel0)
            writeByte(definition.maleOffset)
        }

        if (definition.maleModel1 != -1) {
            writeByte(24)
            writeShort(definition.maleModel1)
        }

        if (definition.femaleModel0 != -1 || definition.femaleOffset != 0) {
            writeByte(25)
            writeShort(definition.femaleModel0)
            writeByte(definition.femaleOffset)
        }

        if (definition.femaleModel1 != -1) {
            writeByte(26)
            writeShort(definition.femaleModel1)
        }

        if (definition.appearanceOverride2 != 0) {
            writeByte(27)
            writeByte(definition.appearanceOverride2)
        }

        if (definition.options != mutableListOf(null, null, "Take", null, null)) {
            for (i in 0 until definition.options.size) {
                if (definition.options[i] == null) {
                    continue
                }
                writeByte(i + 30)
                writeString(definition.options[i]!!)
            }
        }

        if (definition.interfaceOptions != mutableListOf(null, null, null, null, "Drop")) {
            for (i in 0 until definition.interfaceOptions.size) {
                if (definition.interfaceOptions[i] == null) {
                    continue
                }
                writeByte(i + 35)
                writeString(definition.interfaceOptions[i]!!)
            }
        }

        definition.writeColoursTextures(this)

        if (definition.dropOptionIndex != -2) {
            writeByte(42)
            writeByte(definition.dropOptionIndex)
        }

        definition.subops?.forEachIndexed { opId, subopArray ->
            writeByte(43)
            if (subopArray != null) {
                writeByte(opId)
                subopArray.forEachIndexed { subopId, op ->
                    if (op != null) {
                        writeByte(subopId + 1)
                        writeString(op)
                    }
                }
                writeByte(0)
            }
        }

        if (definition.isTradeable) {
            writeByte(65)
        }

        if (definition.weight != 0.0) {
            writeByte(75)
            writeShort(definition.weight.toInt())
        }

        if (definition.maleModel2 != -1) {
            writeByte(78)
            writeShort(definition.maleModel2)
        }

        if (definition.femaleModel2 != -1) {
            writeByte(79)
            writeShort(definition.femaleModel2)
        }

        if (definition.maleHeadModel0 != -1) {
            writeByte(90)
            writeShort(definition.maleHeadModel0)
        }

        if (definition.femaleHeadModel0 != -1) {
            writeByte(91)
            writeShort(definition.femaleHeadModel0)
        }

        if (definition.maleHeadModel1 != -1) {
            writeByte(92)
            writeShort(definition.maleHeadModel1)
        }

        if (definition.femaleHeadModel1 != -1) {
            writeByte(93)
            writeShort(definition.femaleHeadModel1)
        }

        if (definition.category != -1) {
            writeByte(94)
            writeShort(definition.category)
        }

        if (definition.zan2d != 0) {
            writeByte(95)
            writeShort(definition.zan2d)
        }

        if (definition.noteLinkId != -1) {
            writeByte(97)
            writeShort(definition.noteLinkId)
        }

        if (definition.noteTemplateId != -1) {
            writeByte(98)
            writeShort(definition.noteTemplateId)
        }

        if (definition.countObj != null) {
            for (i in definition.countObj!!.indices) {
                writeByte(100 + i)
                writeShort(definition.countObj!![i])
                writeShort(definition.countCo!![i])
            }
        }

        if (definition.resizeX != 128) {
            writeByte(110)
            writeShort(definition.resizeX)
        }

        if (definition.resizeY != 128) {
            writeByte(111)
            writeShort(definition.resizeY)
        }

        if (definition.resizeZ != 128) {
            writeByte(112)
            writeShort(definition.resizeZ)
        }

        if (definition.ambient != 0) {
            writeByte(113)
            writeByte(definition.ambient)
        }

        if (definition.contrast != 0) {
            writeByte(114)
            writeByte(definition.contrast)
        }

        if (definition.teamCape != 0) {
            writeByte(115)
            writeByte(definition.teamCape)
        }

        if (definition.unnotedId != -1) {
            writeByte(139)
            writeShort(definition.unnotedId)
        }

        if (definition.notedId != -1) {
            writeByte(140)
            writeShort(definition.notedId)
        }

        if (definition.placeholderLink != -1) {
            writeByte(148)
            writeShort(definition.placeholderLink )
        }

        if (definition.placeholderTemplate != -1) {
            writeByte(149)
            writeShort(definition.placeholderTemplate)
        }

        definition.writeParameters(this)

        writeByte(0)
    }

    override fun createDefinition() = ItemType()

    companion object {
        internal val logger = KotlinLogging.logger {}
    }
}