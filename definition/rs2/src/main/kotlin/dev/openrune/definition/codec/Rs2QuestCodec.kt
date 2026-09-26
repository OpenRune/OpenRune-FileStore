package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.Rs2QuestDifficulty
import dev.openrune.definition.type.Rs2QuestKind
import dev.openrune.definition.type.Rs2QuestStatReq
import dev.openrune.definition.type.Rs2QuestType
import dev.openrune.definition.type.Rs2QuestVarRange
import dev.openrune.definition.type.Rs2QuestVarReq
import dev.openrune.definition.util.readNullableLargeSmartCorrect
import dev.openrune.definition.util.readPrefixedStringCP
import dev.openrune.definition.util.readStringCP
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import dev.openrune.definition.util.writePrefixedStringCP
import dev.openrune.definition.util.writeStringCP
import io.netty.buffer.ByteBuf

class Rs2QuestCodec : DefinitionCodec<Rs2QuestType> {

    override fun Rs2QuestType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> name = buffer.readPrefixedStringCP()
            2 -> sortName = buffer.readPrefixedStringCP()

            3 -> masterQuestVarps = List(buffer.readUnsignedByte().toInt()) {
                Rs2QuestVarRange(buffer.readUnsignedShort(), buffer.readInt(), buffer.readInt())
            }
            4 -> {
                wideVarbits = false
                masterQuestVarbits = List(buffer.readUnsignedByte().toInt()) {
                    Rs2QuestVarRange(buffer.readUnsignedShort(), buffer.readInt(), buffer.readInt())
                }
            }
            22 -> {
                wideVarbits = true
                masterQuestVarbits = List(buffer.readUnsignedByte().toInt()) {
                    Rs2QuestVarRange(buffer.readUnsignedMedium(), buffer.readInt(), buffer.readInt())
                }
            }

            5 -> parent = buffer.readUnsignedShort()

            6 -> kind = when (val value = buffer.readUnsignedByte().toInt()) {
                0 -> Rs2QuestKind.NORMAL
                1 -> Rs2QuestKind.SEASONAL
                2 -> Rs2QuestKind.TUTORIAL
                else -> error("Invalid quest type $value")
            }

            7 -> difficulty = Rs2QuestDifficulty.fromValue(buffer.readUnsignedByte().toInt())
            8 -> members = true
            9 -> questPoints = buffer.readUnsignedByte().toInt()

            10 -> startCoords = List(buffer.readUnsignedByte().toInt()) { buffer.readInt() }
            12 -> viaCoord = buffer.readInt()

            13 -> questReqs = List(buffer.readUnsignedByte().toInt()) { buffer.readUnsignedShort() }
            14 -> statReqs = List(buffer.readUnsignedByte().toInt()) {
                Rs2QuestStatReq(buffer.readUnsignedByte().toInt(), buffer.readUnsignedByte().toInt())
            }
            15 -> questPointsReq = buffer.readUnsignedShort()
            17 -> icon = buffer.readNullableLargeSmartCorrect()

            18 -> varpReqs = List(buffer.readUnsignedByte().toInt()) { readVarReq(buffer) }
            19 -> varbitReqs = List(buffer.readUnsignedByte().toInt()) { readVarReq(buffer) }

            249 -> readParameters(buffer)

            else -> error("Unknown quest opcode $opcode")
        }
    }

    private fun readVarReq(buffer: ByteBuf) =
        Rs2QuestVarReq(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readStringCP())

    override fun ByteBuf.encode(definition: Rs2QuestType) {
        definition.name?.let {
            writeByte(1)
            writePrefixedStringCP(it)
        }
        definition.sortName?.let {
            writeByte(2)
            writePrefixedStringCP(it)
        }
        if (definition.masterQuestVarps.isNotEmpty()) {
            writeByte(3)
            writeByte(definition.masterQuestVarps.size)
            definition.masterQuestVarps.forEach {
                writeShort(it.variable)
                writeInt(it.min)
                writeInt(it.max)
            }
        }
        if (definition.masterQuestVarbits.isNotEmpty()) {
            writeByte(if (definition.wideVarbits) 22 else 4)
            writeByte(definition.masterQuestVarbits.size)
            definition.masterQuestVarbits.forEach {
                if (definition.wideVarbits) writeMedium(it.variable) else writeShort(it.variable)
                writeInt(it.min)
                writeInt(it.max)
            }
        }
        definition.parent?.let {
            writeByte(5)
            writeShort(it)
        }
        if (definition.kind != Rs2QuestKind.NORMAL) {
            writeByte(6)
            writeByte(definition.kind.ordinal)
        }
        if (definition.difficulty != Rs2QuestDifficulty.NOVICE) {
            writeByte(7)
            writeByte(definition.difficulty.value)
        }
        if (definition.members) writeByte(8)
        if (definition.questPoints != 0) {
            writeByte(9)
            writeByte(definition.questPoints)
        }
        if (definition.startCoords.isNotEmpty()) {
            writeByte(10)
            writeByte(definition.startCoords.size)
            definition.startCoords.forEach { writeInt(it) }
        }
        definition.viaCoord?.let {
            writeByte(12)
            writeInt(it)
        }
        if (definition.questReqs.isNotEmpty()) {
            writeByte(13)
            writeByte(definition.questReqs.size)
            definition.questReqs.forEach { writeShort(it) }
        }
        if (definition.statReqs.isNotEmpty()) {
            writeByte(14)
            writeByte(definition.statReqs.size)
            definition.statReqs.forEach {
                writeByte(it.stat)
                writeByte(it.level)
            }
        }
        if (definition.questPointsReq != 0) {
            writeByte(15)
            writeShort(definition.questPointsReq)
        }
        definition.icon?.let {
            writeByte(17)
            writeNullableLargeSmartCorrect(it)
        }
        if (definition.varpReqs.isNotEmpty()) {
            writeByte(18)
            writeVarReqs(definition.varpReqs)
        }
        if (definition.varbitReqs.isNotEmpty()) {
            writeByte(19)
            writeVarReqs(definition.varbitReqs)
        }

        definition.writeParameters(this)

        writeByte(0)
    }

    private fun ByteBuf.writeVarReqs(reqs: List<Rs2QuestVarReq>) {
        writeByte(reqs.size)
        reqs.forEach {
            writeInt(it.variable)
            writeInt(it.min)
            writeInt(it.max)
            writeStringCP(it.message)
        }
    }

    override fun createDefinition() = Rs2QuestType(0)
}
