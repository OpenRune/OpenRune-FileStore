package dev.openrune.definition.opcode.impl

import dev.openrune.definition.Definition
import dev.openrune.definition.EntityOpsDefinition
import dev.openrune.definition.EntityOpsLoader
import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeString
import io.netty.buffer.Unpooled
import kotlin.reflect.KProperty1

fun <T> DefinitionOpcodeEntityOps(
    opcode: Int,
    property: KProperty1<T, EntityOpsDefinition>,
    revision: Int
): DefinitionOpcode<T> {
    val entityOpsLoader = EntityOpsLoader(revision)
    return DefinitionOpcode(
        opcode = opcode,
        decode = { buf, def, _ ->
            val ops = property.get(def)
            val decodeExtended = entityOpsLoader.supportsExtendedEntityOps()
            if (!decodeExtended) {
                val opCount = buf.readUnsignedByte().toInt()
                repeat(opCount) {
                    val index = buf.readUnsignedByte().toInt()
                    val text = buf.readString()
                    val payload = Unpooled.buffer()
                    payload.writeString(text)
                    entityOpsLoader.decodeBaseOp(ops, payload, index)
                }
                return@DefinitionOpcode
            }

            val flags = buf.readUnsignedByte().toInt()

            if ((flags and 0x1) != 0) {
                val opCount = buf.readUnsignedByte().toInt()
                repeat(opCount) {
                    val index = buf.readUnsignedByte().toInt()
                    val text = buf.readString()
                    val payload = Unpooled.buffer()
                    payload.writeString(text)
                    entityOpsLoader.decodeBaseOp(ops, payload, index)
                }
            }

            if ((flags and 0x2) != 0) {
                val slotCount = buf.readUnsignedByte().toInt()
                repeat(slotCount) {
                    val index = buf.readUnsignedByte().toInt()
                    val subCount = buf.readUnsignedShort()
                    repeat(subCount) {
                        val subID = buf.readUnsignedShort().toInt()
                        val text = buf.readString()
                        val payload = Unpooled.buffer()
                        payload.writeByte(index)
                        payload.writeByte(subID + 1)
                        payload.writeString(text)
                        payload.writeByte(0)
                        entityOpsLoader.decodeSubOp(ops, payload)
                    }
                }
            }

            if ((flags and 0x4) != 0) {
                val slotCount = buf.readUnsignedByte().toInt()
                repeat(slotCount) {
                    val index = buf.readUnsignedByte().toInt()
                    val conditionalCount = buf.readUnsignedShort()
                    repeat(conditionalCount) {
                        val varp = buf.readUnsignedShort().toInt()
                        val varbit = buf.readUnsignedShort().toInt()
                        val min = buf.readInt()
                        val max = buf.readInt()
                        val text = buf.readString()
                        val payload = Unpooled.buffer()
                        payload.writeByte(index)
                        payload.writeShort(varp)
                        payload.writeShort(varbit)
                        payload.writeInt(min)
                        payload.writeInt(max)
                        payload.writeString(text)
                        entityOpsLoader.decodeConditionalOp(ops, payload)
                    }
                }
            }

            if ((flags and 0x8) != 0) {
                val slotCount = buf.readUnsignedByte().toInt()
                repeat(slotCount) {
                    val index = buf.readUnsignedByte().toInt()
                    val conditionalSubCount = buf.readUnsignedShort()
                    repeat(conditionalSubCount) {
                        val subID = buf.readUnsignedShort().toInt()
                        val varp = buf.readUnsignedShort().toInt()
                        val varbit = buf.readUnsignedShort().toInt()
                        val min = buf.readInt()
                        val max = buf.readInt()
                        val text = buf.readString()
                        val payload = Unpooled.buffer()
                        payload.writeByte(index)
                        payload.writeShort(subID)
                        payload.writeShort(varp)
                        payload.writeShort(varbit)
                        payload.writeInt(min)
                        payload.writeInt(max)
                        payload.writeString(text)
                        entityOpsLoader.decodeConditionalSubOp(ops, payload)
                    }
                }
            }
        },
        encode = { buf, def ->
            val ops = property.get(def)
            val encodeExtended = entityOpsLoader.supportsExtendedEntityOps()
            val nonNullOps = ops.ops.withIndex().filter { it.value != null }
            if (!encodeExtended) {
                buf.writeByte(nonNullOps.size)
                nonNullOps.forEach { (index, op) ->
                    buf.writeByte(index)
                    buf.writeString(op!!.text)
                }
                return@DefinitionOpcode
            }

            val subSlots = ops.subOps.withIndex().filter { it.value.isNotEmpty() }
            val conditionalSlots = ops.conditionalOps.withIndex().filter { it.value.isNotEmpty() }
            val conditionalSubSlots = ops.conditionalSubOps.withIndex().filter { it.value.isNotEmpty() }

            var flags = 0
            if (nonNullOps.isNotEmpty()) flags = flags or 0x1
            if (subSlots.isNotEmpty()) flags = flags or 0x2
            if (conditionalSlots.isNotEmpty()) flags = flags or 0x4
            if (conditionalSubSlots.isNotEmpty()) flags = flags or 0x8
            buf.writeByte(flags)

            if ((flags and 0x1) != 0) {
                buf.writeByte(nonNullOps.size)
                nonNullOps.forEach { (index, op) ->
                    buf.writeByte(index)
                    buf.writeString(op!!.text)
                }
            }

            if ((flags and 0x2) != 0) {
                buf.writeByte(subSlots.size)
                subSlots.forEach { (index, list) ->
                    buf.writeByte(index)
                    val sorted = list.sortedBy { it.subID }
                    buf.writeShort(sorted.size)
                    sorted.forEach { sub ->
                        buf.writeShort(sub.subID)
                        buf.writeString(sub.text)
                    }
                }
            }

            if ((flags and 0x4) != 0) {
                buf.writeByte(conditionalSlots.size)
                conditionalSlots.forEach { (index, list) ->
                    buf.writeByte(index)
                    buf.writeShort(list.size)
                    list.forEach { op ->
                        buf.writeShort(op.varpID)
                        buf.writeShort(op.varbitID)
                        buf.writeInt(op.minValue)
                        buf.writeInt(op.maxValue)
                        buf.writeString(op.text)
                    }
                }
            }

            if ((flags and 0x8) != 0) {
                buf.writeByte(conditionalSubSlots.size)
                conditionalSubSlots.forEach { (index, map) ->
                    buf.writeByte(index)
                    val flattened = map.toSortedMap().values.flatten()
                    buf.writeShort(flattened.size)
                    flattened.forEach { op ->
                        buf.writeShort(op.subID)
                        buf.writeShort(op.varpID)
                        buf.writeShort(op.varbitID)
                        buf.writeInt(op.minValue)
                        buf.writeInt(op.maxValue)
                        buf.writeString(op.text)
                    }
                }
            }
        },
        shouldEncode = { def ->
            val ops = property.get(def)
            ops.ops.any { it != null } ||
                ops.subOps.any { it.isNotEmpty() } ||
                ops.conditionalOps.any { it.isNotEmpty() } ||
                ops.conditionalSubOps.any { it.isNotEmpty() }
        }
    )
}
