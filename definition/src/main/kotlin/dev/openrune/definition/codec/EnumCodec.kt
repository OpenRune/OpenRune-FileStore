package dev.openrune.definition.codec

import dev.openrune.definition.BuilderDefinitionCodec
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.type.builders.EnumTypeBuilder
import dev.openrune.definition.util.BoxedInts
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeString
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class EnumCodec : BuilderDefinitionCodec<EnumType, EnumTypeBuilder> {

    override fun builder(id: Int) = EnumTypeBuilder(id)

    override fun build(builder: EnumTypeBuilder) = builder.build()

    override fun EnumTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> keyType = CacheVarLiteral.byChar(buffer.readUnsignedByte().toInt().toChar())
            2 -> valueType = CacheVarLiteral.byChar(buffer.readUnsignedByte().toInt().toChar())
            3 -> defaultString = buffer.readString()
            4 -> defaultInt = buffer.readInt()
            5, 6 -> {
                val count = buffer.readUnsignedShort()
                // The same ids appear as keys across many enums, so their boxes are pooled too.
                // The unchecked view only widens the key type; equality semantics are unchanged.
                @Suppress("UNCHECKED_CAST")
                val target = values as MutableMap<Any, Any>
                for (i in 0 until count) {
                    val key = BoxedInts.of(buffer.readInt())
                    if (opcode == 5) {
                        target[key] = buffer.readString()
                    } else {
                        target[key] = BoxedInts.of(buffer.readInt())
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

        if (definition.valueType == CacheVarLiteral.STRING) {
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
