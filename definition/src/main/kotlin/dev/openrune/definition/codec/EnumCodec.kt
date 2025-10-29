package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.util.VarType
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeString
import io.netty.buffer.ByteBuf

class EnumCodec : DefinitionCodec<EnumType> {
    override fun EnumType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> keyType = VarType.byChar(buffer.readUnsignedByte().toInt().toChar())
            2 -> valueType = VarType.byChar(buffer.readUnsignedByte().toInt().toChar())
            3 -> defaultString = buffer.readString()
            4 -> defaultInt = buffer.readInt()
            5, 6 -> {
                val count = buffer.readUnsignedShort()
                for (i in 0 until count) {
                    val key = buffer.readInt()
                    if (opcode == 5) {
                        values[key.toString()] = buffer.readString()
                    } else {
                        values[key.toString()] = buffer.readInt()
                    }
                }
            }
        }
    }

    override fun ByteBuf.encode(definition: EnumType) {
        writeByte(1)
        writeByte(definition.keyType.ch.code)

        writeByte(2)
        writeByte(definition.valueType.ch.code)

        if (definition.valueType == VarType.STRING) {
            if (definition.defaultString.isNotEmpty()) {
                writeByte(3)
                writeString(definition.defaultString)
            }

            if (!definition.values.isEmpty()) {
                writeByte(5)
                writeShort(definition.values.size)
                for ((key, value) in definition.values) {
                    writeInt(key.toInt())
                    writeString(value.toString())
                }
            }
        } else {
            if (definition.defaultInt != 0) {
                writeByte(4)
                writeInt(definition.defaultInt)
            }

            if (!definition.values.isEmpty()) {
                writeByte(6)
                writeShort(definition.values.size)
                for ((key, value) in definition.values) {
                    writeInt(key.toInt())
                    writeInt(value.toString().toDouble().toInt())
                }
            }
        }
        writeByte(0)
    }

    override fun createDefinition() = EnumType()
}