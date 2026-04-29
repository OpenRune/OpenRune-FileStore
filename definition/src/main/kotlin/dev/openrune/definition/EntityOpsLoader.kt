package dev.openrune.definition

import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeString
import io.netty.buffer.ByteBuf

class EntityOpsLoader(private val revision: Int) {

    fun supportsExtendedEntityOps(): Boolean = revisionIsOrAfter(revision, EXTENDED_ENTITY_OPS_REVISION)

    fun decodeBaseOp(ops: EntityOpsDefinition, buffer: ByteBuf, index: Int) {
        val text = buffer.readString()
        if (!text.equals("Hidden", ignoreCase = true)) {
            ops.setOp(index, text)
        }
    }

    fun encodeBaseOp(buffer: ByteBuf, index: Int, op: EntityOpsDefinition.Op?) {
        if (op == null) {
            return
        }
        buffer.writeByte(index + 30)
        buffer.writeString(op.text)
    }

    fun decodeSubOp(ops: EntityOpsDefinition, buffer: ByteBuf) {
        val index = buffer.readUnsignedByte().toInt()
        while (true) {
            val subID = buffer.readUnsignedByte().toInt() - 1
            if (subID == -1) {
                break
            }
            val text = buffer.readString()
            ops.setSubOp(index, subID, text)
        }
    }

    fun decodeConditionalOp(ops: EntityOpsDefinition, buffer: ByteBuf) {
        val index = buffer.readUnsignedByte().toInt()
        val varp = buffer.readUnsignedShort().toInt()
        val varb = buffer.readUnsignedShort().toInt()
        val min = buffer.readInt()
        val max = buffer.readInt()
        val text = buffer.readString()

        ops.setConditionalOp(index, text, varp, varb, min, max)
    }

    fun decodeConditionalSubOp(ops: EntityOpsDefinition, buffer: ByteBuf) {
        val index = buffer.readUnsignedByte().toInt()
        val subID = buffer.readUnsignedShort().toInt()
        val varp = buffer.readUnsignedShort().toInt()
        val varb = buffer.readUnsignedShort().toInt()
        val min = buffer.readInt()
        val max = buffer.readInt()
        val text = buffer.readString()

        ops.setConditionalSubOp(index, subID, text, varp, varb, min, max)
    }

    fun encodeSubOpsOpcode(
        buffer: ByteBuf,
        opcode: Int,
        index: Int,
        subOps: MutableList<EntityOpsDefinition.SubOp>?
    ) {
        if (subOps.isNullOrEmpty()) {
            return
        }

        buffer.writeByte(opcode)
        buffer.writeByte(index)
        subOps.sortedBy { it.subID }.forEach { subOp ->
            buffer.writeByte(subOp.subID + 1)
            buffer.writeString(subOp.text)
        }
        buffer.writeByte(0)
    }

    fun encodeConditionalOpsOpcode(
        buffer: ByteBuf,
        opcode: Int,
        index: Int,
        conditionalOps: MutableList<EntityOpsDefinition.ConditionalOp>?
    ) {
        if (conditionalOps.isNullOrEmpty()) {
            return
        }

        conditionalOps.forEach { op ->
            buffer.writeByte(opcode)
            buffer.writeByte(index)
            buffer.writeShort(op.varpID)
            buffer.writeShort(op.varbitID)
            buffer.writeInt(op.minValue)
            buffer.writeInt(op.maxValue)
            buffer.writeString(op.text)
        }
    }

    fun encodeConditionalSubOpsOpcode(
        buffer: ByteBuf,
        opcode: Int,
        index: Int,
        conditionalSubOps: MutableMap<Int, MutableList<EntityOpsDefinition.ConditionalSubOp>>?
    ) {
        if (conditionalSubOps.isNullOrEmpty()) {
            return
        }

        conditionalSubOps.toSortedMap().forEach { (_, entries) ->
            entries.forEach { op ->
                buffer.writeByte(opcode)
                buffer.writeByte(index)
                buffer.writeShort(op.subID)
                buffer.writeShort(op.varpID)
                buffer.writeShort(op.varbitID)
                buffer.writeInt(op.minValue)
                buffer.writeInt(op.maxValue)
                buffer.writeString(op.text)
            }
        }
    }

    fun encodeOpcodeSubOps(buffer: ByteBuf, index: Int, subOps: MutableList<EntityOpsDefinition.SubOp>?) =
        encodeSubOpsOpcode(buffer, 200, index, subOps)

    fun encodeOpcodeConditionalOps(buffer: ByteBuf, index: Int, conditionalOps: MutableList<EntityOpsDefinition.ConditionalOp>?) =
        encodeConditionalOpsOpcode(buffer, 201, index, conditionalOps)

    fun encodeOpcodeConditionalSubOps(
        buffer: ByteBuf,
        index: Int,
        conditionalSubOps: MutableMap<Int, MutableList<EntityOpsDefinition.ConditionalSubOp>>?
    ) = encodeConditionalSubOpsOpcode(buffer, 202, index, conditionalSubOps)

    companion object {
        private const val EXTENDED_ENTITY_OPS_REVISION = 237
    }
}