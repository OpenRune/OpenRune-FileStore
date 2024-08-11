package dev.openrune.cache.filestore.definition.encoder

import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.ConfigEncoder
import dev.openrune.cache.filestore.definition.data.ItemType

class ItemEncoder : ConfigEncoder<ItemType>() {

    override fun Writer.encode(definition: ItemType) {
        if (definition.inventoryModel != 0) {
            writeByte(1)
            writeShort(definition.inventoryModel)
        }

        if (!definition.name.equals("null", ignoreCase = true)) {
            writeByte(2)
            writeString(definition.name)
        }

        if (!definition.name.equals("null", ignoreCase = true)) {
            writeByte(3)
            writeString(definition.examine)
        }

        if (definition.zoom2d != 2000) {
            writeByte(4)
            writeShort(definition.zoom2d)
        }

        if (definition.getXan2d() != 0) {
            writeByte(5)
            writeShort(definition.getXan2d())
        }

        if (definition.getYan2d() != 0) {
            writeByte(6)
            writeShort(definition.getYan2d())
        }

        if (definition.getXOffset2d() != 0) {
            writeByte(7)
            writeShort(definition.getXOffset2d())
        }

        if (definition.getYOffset2d() != 0) {
            writeByte(8)
            writeShort(definition.getYOffset2d())
        }

        if (definition.stackable) {
            writeByte(11)
        }

        if (definition.cost != 1) {
            writeByte(12)
            writeInt(definition.cost)
        }

        if (definition.getEquipSlot() != -1) {
            writeByte(13)
            writeByte(definition.getEquipSlot())
        }

        if (definition.getAppearanceOverride1() != -1) {
            writeByte(14)
            writeByte(definition.getAppearanceOverride1())
        }

        if (definition.members) {
            writeByte(16)
        }

        if (definition.getMaleModel0() != -1 || definition.maleOffset != 0) {
            writeByte(23)
            writeShort(definition.getMaleModel0())
            writeByte(definition.maleOffset)
        }

        if (definition.getMaleModel1() != -1) {
            writeByte(24)
            writeShort(definition.getMaleModel1())
        }

        if (definition.getFemaleModel0() != -1 || definition.getFemaleOffset() != 0) {
            writeByte(25)
            writeShort(definition.getFemaleModel0())
            writeByte(definition.getFemaleOffset())
        }

        if (definition.getMaleModel1() != -1) {
            writeByte(26)
            writeShort(definition.getMaleModel1())
        }

        if (definition.getAppearanceOverride2() != 0) {
            writeByte(27)
            writeByte(definition.getAppearanceOverride2())
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

        if (definition.getDropOptionIndex() != -2) {
            writeByte(42)
            writeByte(definition.getDropOptionIndex())
        }

        if (definition.isTradeable) {
            writeByte(65)
        }

        if (definition.getWeight() != 0) {
            writeByte(75)
            writeShort(definition.weight.toInt())
        }

        if (definition.getMaleModel2() != -1) {
            writeByte(78)
            writeShort(definition.getMaleModel2())
        }

        if (definition.getFemaleModel2() != -1) {
            writeByte(79)
            writeShort(definition.getFemaleModel2())
        }

        if (definition.getMaleHeadModel0() != -1) {
            writeByte(90)
            writeShort(definition.getMaleHeadModel0())
        }

        if (definition.getFemaleHeadModel0() != -1) {
            writeByte(91)
            writeShort(definition.getFemaleHeadModel0())
        }

        if (definition.getMaleHeadModel1() != -1) {
            writeByte(92)
            writeShort(definition.getMaleHeadModel1())
        }

        if (definition.getFemaleHeadModel1() != -1) {
            writeByte(93)
            writeShort(definition.getFemaleHeadModel1())
        }

        if (definition.getCategory() != -1) {
            writeByte(94)
            writeShort(definition.getCategory())
        }

        if (definition.getZan2d() != 0) {
            writeByte(95)
            writeShort(definition.getZan2d())
        }

        if (definition.getNoteLinkId() != -1) {
            writeByte(97)
            writeShort(definition.getNoteLinkId())
        }

        if (definition.getNoteTemplateId() != -1) {
            writeByte(98)
            writeShort(definition.getNoteTemplateId())
        }

        for (i in definition.countObj.indices) {
            writeByte(100 + i)
            writeShort(definition.countObj[i].toInt())
            writeShort(definition.countCo[i].toInt())
        }

        if (definition.getResizeX() != 128) {
            writeByte(110)
            writeShort(definition.getResizeX())
        }

        if (definition.getResizeY() != 128) {
            writeByte(111)
            writeShort(definition.getResizeY())
        }

        if (definition.getResizeZ() != 128) {
            writeByte(112)
            writeShort(definition.getResizeZ())
        }

        if (definition.getAmbient() != 0) {
            writeByte(113)
            writeByte(definition.getAmbient())
        }

        if (definition.getContrast() != 0) {
            writeByte(114)
            writeByte(definition.getContrast())
        }

        if (definition.getTeamCape() != 0) {
            writeByte(115)
            writeByte(definition.getTeamCape())
        }

        if (definition.getUnnotedId() != -1) {
            writeByte(139)
            writeShort(definition.getUnnotedId())
        }

        if (definition.getNotedId() != -1) {
            writeByte(140)
            writeShort(definition.getNotedId())
        }

        if (definition.getPlaceholderLink() != -1) {
            writeByte(148)
            writeShort(definition.getPlaceholderLink() )
        }

        if (definition.getPlaceholderTemplate() != -1) {
            writeByte(149)
            writeShort(definition.getPlaceholderTemplate())
        }

        definition.writeParameters(this)

        writeByte(0)
    }

}