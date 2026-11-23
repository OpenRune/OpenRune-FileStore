package dev.openrune.definition.codec

import dev.openrune.definition.revisionIsOrAfter
import dev.openrune.definition.writeParameterBody
import dev.openrune.definition.writeRecolourPairs
import dev.openrune.definition.type.Rs2ObjType
import dev.openrune.definition.util.readNullableLargeSmartCorrect
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.readUnsignedShortOrNull
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import dev.openrune.definition.util.writeString
import io.netty.buffer.ByteBuf

class Rs2ObjCodec(private val revision: Int) : Rs2OrderedCodec<Rs2ObjType> {

    private val packedRecolours get() = revisionIsOrAfter(revision, 465)
    private val wideModels get() = revisionIsOrAfter(revision, 681)
    private val wearWithoutOffset get() = revisionIsOrAfter(revision, 501)
    private val modelListAtNine get() = revisionIsOrAfter(revision, 949)

    private fun ByteBuf.readModel(): Int =
        if (wideModels) readNullableLargeSmartCorrect() ?: -1 else readUnsignedShortOrNull() ?: -1

    private fun ByteBuf.writeModel(value: Int) {
        if (wideModels) writeNullableLargeSmartCorrect(value) else writeShort(value)
    }

    override fun Rs2ObjType.read(opcode: Int, buffer: ByteBuf) {
        check(packedRecolours) { "RS3 objs before revision 465 are not supported (got $revision)" }
        when {
            opcode == 1 -> models[1] = buffer.readModel()
            opcode == 2 -> name = buffer.readString()
            opcode == 3 -> description = buffer.readString()
            opcode == 4 -> zoom2d = buffer.readUnsignedShort()
            opcode == 5 -> xan2d = buffer.readUnsignedShort()
            opcode == 6 -> yan2d = buffer.readUnsignedShort()
            opcode == 7 -> xOffset2d = buffer.readShort().toInt()
            opcode == 8 -> yOffset2d = buffer.readShort().toInt()
            opcode == 9 -> when {
                !revisionIsOrAfter(revision, 300) -> unknown9Flag = true
                !modelListAtNine -> unknown9Text = buffer.readString()
                else -> {
                    val count = buffer.readUnsignedByte().toInt()
                    modelList = MutableList(count) { buffer.readNullableLargeSmartCorrect() ?: -1 }
                }
            }

            opcode == 10 -> anim = buffer.readUnsignedShort()
            opcode == 11 -> stackable = 11
            opcode == 12 -> {
                cost = buffer.readInt().toLong()
                costLong = false
            }

            opcode == 13 -> wearPos = buffer.readUnsignedByte().toInt()
            opcode == 14 -> wearPos2 = buffer.readUnsignedByte().toInt()
            opcode == 15 -> tradeable = false
            opcode == 16 -> members = true
            opcode == 23 -> if (wearWithoutOffset) {
                manWear = buffer.readModel()
            } else {
                manWear = buffer.readUnsignedShort()
                manWearOffsetLegacy = buffer.readUnsignedByte().toInt()
            }

            opcode == 24 -> manWear2 = buffer.readModel()
            opcode == 25 -> if (wearWithoutOffset) {
                womanWear = buffer.readModel()
            } else {
                womanWear = buffer.readUnsignedShort()
                womanWearOffsetLegacy = buffer.readUnsignedByte().toInt()
            }

            opcode == 26 -> womanWear2 = buffer.readModel()
            opcode == 27 -> wearPos3 = buffer.readUnsignedByte().toInt()
            opcode in 30..34 -> ops[opcode] = buffer.readString()
            opcode in 35..39 -> interfaceOps[opcode] = buffer.readString()
            opcode == 40 -> readColours(buffer)
            opcode == 41 -> readTextures(buffer)
            opcode == 42 -> {
                val count = buffer.readUnsignedByte().toInt()
                recolourPalette = MutableList(count) { buffer.readByte().toInt() }
            }

            opcode == 43 -> miniMenuColour = buffer.readInt()
            opcode == 44 -> recolourMask = buffer.readUnsignedShort()
            opcode == 45 -> textureMask = buffer.readUnsignedShort()
            opcode in 46..56 -> models[opcode] = buffer.readNullableLargeSmartCorrect() ?: -1
            opcode == 65 -> stockMarket = true
            opcode == 69 -> stockMarketLimit = buffer.readInt()
            opcode == 78 -> manWear3 = buffer.readModel()
            opcode == 79 -> womanWear3 = buffer.readModel()
            opcode == 90 -> manHead = buffer.readModel()
            opcode == 91 -> womanHead = buffer.readModel()
            opcode == 92 -> manHead2 = buffer.readModel()
            opcode == 93 -> womanHead2 = buffer.readModel()
            opcode == 94 -> category = buffer.readUnsignedShort()
            opcode == 95 -> zan2d = buffer.readUnsignedShort()
            opcode == 96 -> dummyItem = buffer.readUnsignedByte().toInt()
            opcode in 100..109 ->
                counts[opcode] = mutableListOf(buffer.readUnsignedShort(), buffer.readUnsignedShort())

            opcode in 190..199 ->
                counts[opcode] = mutableListOf(buffer.readUnsignedMedium(), buffer.readUnsignedShort())

            opcode == 110 -> resizeX = buffer.readUnsignedShort()
            opcode == 111 -> resizeY = buffer.readUnsignedShort()
            opcode == 112 -> resizeZ = buffer.readUnsignedShort()
            opcode == 113 -> ambient = buffer.readByte().toInt()
            opcode == 114 -> contrast = buffer.readByte().toInt()
            opcode == 115 -> team = buffer.readUnsignedByte().toInt()
            opcode == 125 -> manWearOffset = MutableList(3) { buffer.readByte().toInt() }
            opcode == 126 -> womanWearOffset = MutableList(3) { buffer.readByte().toInt() }
            opcode in 127..130 ->
                legacyCursors[opcode] =
                    mutableListOf(buffer.readUnsignedByte().toInt(), buffer.readUnsignedShort())

            opcode == 131 -> secondaryDescription = buffer.readString()
            opcode == 132 -> {
                val count = buffer.readUnsignedByte().toInt()
                quests = MutableList(count) { buffer.readUnsignedShort() }
            }

            opcode == 134 -> pickSizeShift = buffer.readUnsignedByte().toInt()
            opcode in 142..146 || opcode in 150..154 -> cursors[opcode] = buffer.readUnsignedShort()
            opcode == 156 -> shadow = false
            opcode == 157 -> randomPosition = true
            opcode == 163 -> shardCount = buffer.readUnsignedShort()
            opcode == 164 -> shardName = buffer.readString()
            opcode == 165 -> stackable = 165
            opcode == 167 -> certTradeable = true
            opcode == 168 -> placeholder = false
            opcode == 178 -> stackable = 178
            opcode == 181 -> {
                cost = buffer.readLong()
                costLong = true
            }

            opcode == 182 -> planTemplate = buffer.readUnsignedMedium()
            opcode == 97 || opcode == 98 || opcode == 121 || opcode == 122 ||
                opcode == 139 || opcode == 140 || opcode == 148 || opcode == 149 ||
                opcode == 161 || opcode == 162 -> links[opcode] = buffer.readUnsignedShort()

            opcode in 201..208 -> links[opcode] = buffer.readUnsignedMedium()
            opcode == 249 -> readParameters(buffer)
            isLegacyColourSlot(opcode) -> readLegacyColourSlot(opcode, buffer)
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: Rs2ObjType) {
        when {
            opcode == 1 -> writeModel(definition.models.getValue(1))
            opcode == 2 -> writeString(definition.name!!)
            opcode == 3 -> writeString(definition.description!!)
            opcode == 4 -> writeShort(definition.zoom2d!!)
            opcode == 5 -> writeShort(definition.xan2d!!)
            opcode == 6 -> writeShort(definition.yan2d!!)
            opcode == 7 -> writeShort(definition.xOffset2d!!)
            opcode == 8 -> writeShort(definition.yOffset2d!!)
            opcode == 9 -> when {
                !revisionIsOrAfter(revision, 300) -> Unit
                !modelListAtNine -> writeString(definition.unknown9Text!!)
                else -> {
                    val list = definition.modelList!!
                    writeByte(list.size)
                    list.forEach { writeNullableLargeSmartCorrect(it) }
                }
            }

            opcode == 10 -> writeShort(definition.anim!!)
            opcode == 11 -> Unit
            opcode == 12 -> writeInt(definition.cost!!.toInt())
            opcode == 13 -> writeByte(definition.wearPos!!)
            opcode == 14 -> writeByte(definition.wearPos2!!)
            opcode == 15 -> Unit
            opcode == 16 -> Unit
            opcode == 23 -> if (wearWithoutOffset) {
                writeModel(definition.manWear!!)
            } else {
                writeShort(definition.manWear!!)
                writeByte(definition.manWearOffsetLegacy ?: 0)
            }

            opcode == 24 -> writeModel(definition.manWear2!!)
            opcode == 25 -> if (wearWithoutOffset) {
                writeModel(definition.womanWear!!)
            } else {
                writeShort(definition.womanWear!!)
                writeByte(definition.womanWearOffsetLegacy ?: 0)
            }

            opcode == 26 -> writeModel(definition.womanWear2!!)
            opcode == 27 -> writeByte(definition.wearPos3!!)
            opcode in 30..34 -> writeString(definition.ops.getValue(opcode))
            opcode in 35..39 -> writeString(definition.interfaceOps.getValue(opcode))
            opcode == 40 -> writeRecolourPairs(definition.originalColours, definition.modifiedColours)
            opcode == 41 -> writeRecolourPairs(definition.originalTextureColours, definition.modifiedTextureColours)
            opcode == 42 -> {
                val palette = definition.recolourPalette!!
                writeByte(palette.size)
                palette.forEach { writeByte(it) }
            }

            opcode == 43 -> writeInt(definition.miniMenuColour!!)
            opcode == 44 -> writeShort(definition.recolourMask!!)
            opcode == 45 -> writeShort(definition.textureMask!!)
            opcode in 46..56 -> writeNullableLargeSmartCorrect(definition.models.getValue(opcode))
            opcode == 65 -> Unit
            opcode == 69 -> writeInt(definition.stockMarketLimit!!)
            opcode == 78 -> writeModel(definition.manWear3!!)
            opcode == 79 -> writeModel(definition.womanWear3!!)
            opcode == 90 -> writeModel(definition.manHead!!)
            opcode == 91 -> writeModel(definition.womanHead!!)
            opcode == 92 -> writeModel(definition.manHead2!!)
            opcode == 93 -> writeModel(definition.womanHead2!!)
            opcode == 94 -> writeShort(definition.category!!)
            opcode == 95 -> writeShort(definition.zan2d!!)
            opcode == 96 -> writeByte(definition.dummyItem!!)
            opcode in 100..109 -> {
                val pair = definition.counts.getValue(opcode)
                writeShort(pair[0])
                writeShort(pair[1])
            }

            opcode in 190..199 -> {
                val pair = definition.counts.getValue(opcode)
                writeMedium(pair[0])
                writeShort(pair[1])
            }

            opcode == 110 -> writeShort(definition.resizeX!!)
            opcode == 111 -> writeShort(definition.resizeY!!)
            opcode == 112 -> writeShort(definition.resizeZ!!)
            opcode == 113 -> writeByte(definition.ambient!!)
            opcode == 114 -> writeByte(definition.contrast!!)
            opcode == 115 -> writeByte(definition.team!!)
            opcode == 125 -> definition.manWearOffset!!.forEach { writeByte(it) }
            opcode == 126 -> definition.womanWearOffset!!.forEach { writeByte(it) }
            opcode in 127..130 -> {
                val pair = definition.legacyCursors.getValue(opcode)
                writeByte(pair[0])
                writeShort(pair[1])
            }

            opcode == 131 -> writeString(definition.secondaryDescription!!)
            opcode == 132 -> {
                val quests = definition.quests!!
                writeByte(quests.size)
                quests.forEach { writeShort(it) }
            }

            opcode == 134 -> writeByte(definition.pickSizeShift!!)
            opcode in 142..146 || opcode in 150..154 -> writeShort(definition.cursors.getValue(opcode))
            opcode == 156 -> Unit
            opcode == 157 -> Unit
            opcode == 163 -> writeShort(definition.shardCount!!)
            opcode == 164 -> writeString(definition.shardName!!)
            opcode == 165 -> Unit
            opcode == 167 -> Unit
            opcode == 168 -> Unit
            opcode == 178 -> Unit
            opcode == 181 -> writeLong(definition.cost!!)
            opcode == 182 -> writeMedium(definition.planTemplate!!)
            opcode == 97 || opcode == 98 || opcode == 121 || opcode == 122 ||
                opcode == 139 || opcode == 140 || opcode == 148 || opcode == 149 ||
                opcode == 161 || opcode == 162 -> writeShort(definition.links.getValue(opcode))

            opcode in 201..208 -> writeMedium(definition.links.getValue(opcode))
            opcode == 249 -> writeParameterBody(this, definition.params)
            definition.isLegacyColourSlot(opcode) -> writeShort(definition.legacyColourSlots.getValue(opcode))
        }
    }

    override fun createDefinition() = Rs2ObjType()
}
