package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.Rs2AchievementDesc
import dev.openrune.definition.type.Rs2AchievementRef
import dev.openrune.definition.type.Rs2AchievementStatReq
import dev.openrune.definition.type.Rs2AchievementTally
import dev.openrune.definition.type.Rs2AchievementTestBit
import dev.openrune.definition.type.Rs2AchievementType
import dev.openrune.definition.type.Rs2AchievementVarReq
import dev.openrune.definition.util.readNullableLargeSmartCorrect
import dev.openrune.definition.util.readPrefixedStringCP
import dev.openrune.definition.util.readSmart2or4
import dev.openrune.definition.util.readUnsignedShortSmart
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import dev.openrune.definition.util.writePrefixedStringCP
import dev.openrune.definition.util.writeSmart2or4
import dev.openrune.definition.util.writeUnsignedShortSmart
import io.netty.buffer.ByteBuf

class Rs2AchievementCodec : DefinitionCodec<Rs2AchievementType> {

    override fun Rs2AchievementType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> name = buffer.readPrefixedStringCP()
            2 -> descriptions = List(buffer.readUnsignedByte().toInt()) {
                Rs2AchievementDesc(buffer.readUnsignedByte().toInt(), buffer.readPrefixedStringCP())
            }
            3 -> category = buffer.readUnsignedShort()
            4 -> sprite = buffer.readNullableLargeSmartCorrect()
            5 -> runescore = buffer.readUnsignedByte().toInt()
            6 -> graceDay = buffer.readUnsignedShort()
            7 -> reward = buffer.readPrefixedStringCP()

            8 -> statPrereqs = readList(buffer) { readStatReq(buffer) }
            9 -> varpPrereqs = readList(buffer) { readVarReq(buffer, wide = false) }
            10 -> { wideVarbits = false; varbitPrereqs = readList(buffer) { readVarReq(buffer, wide = false) } }
            11 -> achievementPrereqs = readList(buffer) { readRef(buffer) }
            12 -> statReqs = readList(buffer) { readStatReq(buffer) }
            13 -> varpReqs = readList(buffer) { readVarReq(buffer, wide = false) }
            14 -> { wideVarbits = false; varbitReqs = readList(buffer) { readVarReq(buffer, wide = false) } }
            15 -> achievementReqs = readList(buffer) { readRef(buffer) }

            16 -> subCategory = buffer.readUnsignedShort()
            17 -> locked = true
            18 -> hide = buffer.readUnsignedByte().toInt()
            19 -> members = false

            20 -> questPrereqs = readList(buffer) { readRef(buffer) }
            21 -> questReqs = readList(buffer) { readRef(buffer) }

            22 -> varpTestBitPrereqs = readList(buffer) { readTestBit(buffer, wide = false) }
            23 -> varpTestBitReqs = readList(buffer) { readTestBit(buffer, wide = false) }
            24 -> { wideVarbits = false; varbitTestBitPrereqs = readList(buffer) { readTestBit(buffer, wide = false) } }
            25 -> { wideVarbits = false; varbitTestBitReqs = readList(buffer) { readTestBit(buffer, wide = false) } }

            26 -> dbRow = buffer.readUnsignedShort()
            27 -> checklist = true

            28 -> prereqItemsToComplete = readList(buffer) { buffer.readUnsignedShortSmart() }
            29 -> numPrereqGroupsToComplete = buffer.readUnsignedByte().toInt()
            30 -> reqGroupItemsToComplete = readList(buffer) { buffer.readUnsignedShortSmart() }
            31 -> numReqGroupsToComplete = buffer.readUnsignedByte().toInt()
            32 -> tally = Rs2AchievementTally(
                buffer.readUnsignedByte().toInt(),
                buffer.readUnsignedByte().toInt(),
                buffer.readUnsignedByte().toInt()
            )

            33 -> { wideVarbits = true; varbitPrereqs = readList(buffer) { readVarReq(buffer, wide = true) } }
            34 -> { wideVarbits = true; varbitTestBitPrereqs = readList(buffer) { readTestBit(buffer, wide = true) } }
            35 -> { wideVarbits = true; varbitReqs = readList(buffer) { readVarReq(buffer, wide = true) } }
            36 -> { wideVarbits = true; varbitTestBitReqs = readList(buffer) { readTestBit(buffer, wide = true) } }

            else -> error("Unknown achievement opcode $opcode")
        }
    }

    private inline fun <T> readList(buffer: ByteBuf, read: () -> T): List<T> =
        List(buffer.readUnsignedShortSmart()) { read() }

    private fun readStatReq(buffer: ByteBuf): Rs2AchievementStatReq {
        val group = buffer.readUnsignedByte().toInt()
        val level = buffer.readUnsignedByte().toInt()
        val text = buffer.readPrefixedStringCP()
        val stats = List(buffer.readUnsignedShortSmart()) { buffer.readUnsignedShort() }
        return Rs2AchievementStatReq(group, level, text, stats)
    }

    private fun readVarReq(buffer: ByteBuf, wide: Boolean): Rs2AchievementVarReq {
        val group = buffer.readUnsignedByte().toInt()
        val value = buffer.readSmart2or4()
        val text = buffer.readPrefixedStringCP()
        val variables = List(buffer.readUnsignedShortSmart()) {
            if (wide) buffer.readUnsignedMedium() else buffer.readUnsignedShort()
        }
        return Rs2AchievementVarReq(group, value, text, variables)
    }

    private fun readRef(buffer: ByteBuf) =
        Rs2AchievementRef(buffer.readUnsignedByte().toInt(), buffer.readUnsignedShort())

    private fun readTestBit(buffer: ByteBuf, wide: Boolean) = Rs2AchievementTestBit(
        group = buffer.readUnsignedByte().toInt(),
        variable = if (wide) buffer.readUnsignedMedium() else buffer.readUnsignedShort(),
        bit = buffer.readUnsignedByte().toInt(),
        text = buffer.readPrefixedStringCP(),
        expected = buffer.readUnsignedByte().toInt()
    )

    override fun ByteBuf.encode(definition: Rs2AchievementType) {
        val wide = definition.wideVarbits

        definition.name?.let {
            writeByte(1)
            writePrefixedStringCP(it)
        }
        if (definition.descriptions.isNotEmpty()) {
            writeByte(2)
            writeByte(definition.descriptions.size)
            definition.descriptions.forEach {
                writeByte(it.type)
                writePrefixedStringCP(it.text)
            }
        }
        if (definition.category != -1) {
            writeByte(3)
            writeShort(definition.category)
        }
        definition.sprite?.let {
            writeByte(4)
            writeNullableLargeSmartCorrect(it)
        }
        if (definition.runescore != 0) {
            writeByte(5)
            writeByte(definition.runescore)
        }
        if (definition.graceDay != 0) {
            writeByte(6)
            writeShort(definition.graceDay)
        }
        definition.reward?.let {
            writeByte(7)
            writePrefixedStringCP(it)
        }

        writeListIfNotEmpty(8, definition.statPrereqs) { writeStatReq(it) }
        writeListIfNotEmpty(9, definition.varpPrereqs) { writeVarReq(it, wide = false) }
        writeListIfNotEmpty(if (wide) 33 else 10, definition.varbitPrereqs) { writeVarReq(it, wide) }
        writeListIfNotEmpty(11, definition.achievementPrereqs) { writeRef(it) }
        writeListIfNotEmpty(12, definition.statReqs) { writeStatReq(it) }
        writeListIfNotEmpty(13, definition.varpReqs) { writeVarReq(it, wide = false) }
        writeListIfNotEmpty(if (wide) 35 else 14, definition.varbitReqs) { writeVarReq(it, wide) }
        writeListIfNotEmpty(15, definition.achievementReqs) { writeRef(it) }

        if (definition.subCategory != -1) {
            writeByte(16)
            writeShort(definition.subCategory)
        }
        if (definition.locked) writeByte(17)
        if (definition.hide != 0) {
            writeByte(18)
            writeByte(definition.hide)
        }
        if (!definition.members) writeByte(19)

        writeListIfNotEmpty(20, definition.questPrereqs) { writeRef(it) }
        writeListIfNotEmpty(21, definition.questReqs) { writeRef(it) }
        writeListIfNotEmpty(22, definition.varpTestBitPrereqs) { writeTestBit(it, wide = false) }
        writeListIfNotEmpty(23, definition.varpTestBitReqs) { writeTestBit(it, wide = false) }
        writeListIfNotEmpty(if (wide) 34 else 24, definition.varbitTestBitPrereqs) { writeTestBit(it, wide) }
        writeListIfNotEmpty(if (wide) 36 else 25, definition.varbitTestBitReqs) { writeTestBit(it, wide) }

        definition.dbRow?.let {
            writeByte(26)
            writeShort(it)
        }
        if (definition.checklist) writeByte(27)

        writeListIfNotEmpty(28, definition.prereqItemsToComplete) { writeUnsignedShortSmart(it) }
        if (definition.numPrereqGroupsToComplete != 0) {
            writeByte(29)
            writeByte(definition.numPrereqGroupsToComplete)
        }
        writeListIfNotEmpty(30, definition.reqGroupItemsToComplete) { writeUnsignedShortSmart(it) }
        if (definition.numReqGroupsToComplete != 0) {
            writeByte(31)
            writeByte(definition.numReqGroupsToComplete)
        }
        definition.tally?.let {
            writeByte(32)
            writeByte(it.a)
            writeByte(it.b)
            writeByte(it.c)
        }

        writeByte(0)
    }

    private inline fun <T> ByteBuf.writeListIfNotEmpty(opcode: Int, list: List<T>, write: ByteBuf.(T) -> Unit) {
        if (list.isEmpty()) return
        writeByte(opcode)
        writeUnsignedShortSmart(list.size)
        list.forEach { write(it) }
    }

    private fun ByteBuf.writeStatReq(req: Rs2AchievementStatReq) {
        writeByte(req.group)
        writeByte(req.level)
        writePrefixedStringCP(req.text)
        writeUnsignedShortSmart(req.stats.size)
        req.stats.forEach { writeShort(it) }
    }

    private fun ByteBuf.writeVarReq(req: Rs2AchievementVarReq, wide: Boolean) {
        writeByte(req.group)
        writeSmart2or4(req.value)
        writePrefixedStringCP(req.text)
        writeUnsignedShortSmart(req.variables.size)
        req.variables.forEach { if (wide) writeMedium(it) else writeShort(it) }
    }

    private fun ByteBuf.writeRef(ref: Rs2AchievementRef) {
        writeByte(ref.group)
        writeShort(ref.id)
    }

    private fun ByteBuf.writeTestBit(testBit: Rs2AchievementTestBit, wide: Boolean) {
        writeByte(testBit.group)
        if (wide) writeMedium(testBit.variable) else writeShort(testBit.variable)
        writeByte(testBit.bit)
        writePrefixedStringCP(testBit.text)
        writeByte(testBit.expected)
    }

    override fun createDefinition() = Rs2AchievementType(0)
}
