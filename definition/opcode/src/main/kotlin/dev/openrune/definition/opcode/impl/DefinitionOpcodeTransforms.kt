package dev.openrune.definition.opcode.impl

import dev.openrune.definition.util.readSmart
import dev.openrune.definition.util.readUnsignedShortOrNull
import io.netty.buffer.ByteBuf
import kotlin.reflect.KMutableProperty1
import dev.openrune.definition.opcode.DefinitionOpcode

fun <T> DefinitionOpcodeTransforms(
    opcodes: IntRange,
    transforms: KMutableProperty1<T, MutableList<Int>?>,
    multiVarBit: KMutableProperty1<T, Int>,
    multiVarp: KMutableProperty1<T, Int>,
    multiDefaultValue: KMutableProperty1<T, Int>? = null,
    extendedTransforms: Boolean = false
): DefinitionOpcode<T> = DefinitionOpcode(
    opcodes,
    decode = { buf, def, opcode ->
        val isLast = opcode == opcodes.last
        val vb = buf.readUnsignedShortOrNull() ?: -1
        val vp = buf.readUnsignedShortOrNull() ?: -1
        val multiDefault = if (isLast) buf.readUnsignedShortOrNull() ?: -1 else -1
        val count = if (extendedTransforms) buf.readSmart() else buf.readUnsignedByte().toInt()
        val transformsValue = MutableList(count + 2) { -1 }
        for (i in 0..count) {
            transformsValue[i] = buf.readUnsignedShortOrNull() ?: -1
        }
        transformsValue[count + 1] = multiDefault
        multiDefaultValue?.set(def, multiDefault)

        transforms.set(def, transformsValue)
        multiVarBit.set(def, vb)
        multiVarp.set(def, vp)
    },
    encode = { buf, def ->
        val ids = transforms.get(def)
        val vb = multiVarBit.get(def)
        val vp = multiVarp.get(def)
        if (ids == null || (vb == -1 && vp == -1)) {
            return@DefinitionOpcode
        }
        val last = multiDefaultValue?.get(def).takeUnless { it == -1 } ?: ids.last()
        val hasDefault = last != -1
        buf.writeByte(if (hasDefault) opcodes.last() else opcodes.first())
        buf.writeShort(vb)
        buf.writeShort(vp)
        if (hasDefault) {
            buf.writeShort(last)
        }
        buf.writeByte(ids.size - 2)
        for (i in 0 until ids.size - 1) {
            buf.writeShort(ids[i])
        }
    },
    skipByteEncode = true,
    shouldEncode = { true }
)
