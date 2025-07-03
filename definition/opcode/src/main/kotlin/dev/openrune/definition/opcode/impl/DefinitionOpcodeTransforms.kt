package dev.openrune.definition.opcode.impl

import dev.openrune.definition.util.readSmart
import io.netty.buffer.ByteBuf
import kotlin.reflect.KMutableProperty1
import dev.openrune.definition.util.readUnsignedBoolean
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeString
import dev.openrune.definition.opcode.DefinitionOpcode

fun <T> DefinitionOpcodeTransforms(
    opcodes: IntRange,
    transforms: KMutableProperty1<T, MutableList<Int>?>,
    varbitValue: KMutableProperty1<T, Int>,
    varpValue: KMutableProperty1<T, Int>,
    extendedTransforms: Boolean = false
): DefinitionOpcode<T> = DefinitionOpcode(
    opcodes,
    decode = { buf, def, opcode ->
        val isLast = opcode == opcodes.last
        var varbit = buf.readShort().toInt()
        if (varbit == 65535) {
            varbit = -1
        }
        var varp = buf.readShort().toInt()
        if (varp == 65535) {
            varp = -1
        }
        var last = -1
        if (isLast) {
            last = buf.readUnsignedShort()
            if (last == 65535) {
                last = -1
            }
        }
        val length = if(extendedTransforms) buf.readSmart() else buf.readUnsignedByte().toInt()
        val transformsValue = MutableList(length + 2) { -1 }
        for (count in 0..length) {
            transformsValue[count] = buf.readUnsignedShort()
            if (transformsValue[count] == 65535) {
                transformsValue[count] = -1
            }
        }
        transformsValue[length + 1] = last

        transforms.set(def, transformsValue)
        varbitValue.set(def, varbit)
        varpValue.set(def, varp)
    },
    encode = { buf, def ->
        val configIds = transforms.get(def)
        val varbit = varbitValue.get(def)
        val varp = varpValue.get(def)
        if (configIds != null && (varbit != -1 || varp != -1)) {
            val last = configIds.last()
            val extended = last != -1
            buf.writeByte(if (extended) opcodes.last() else opcodes.first())
            buf.writeShort(varbit)
            buf.writeShort(varp)

            if (extended) {
                buf.writeShort(last)
            }
            buf.writeByte(configIds.size - 2)
            for (i in 0 until configIds.size - 1) {
                buf.writeShort(configIds[i])
            }
        }
    },
    skipByteEncode = true,
    shouldEncode = { true }
)