package dev.openrune.definition.codec

import dev.openrune.definition.type.EnumType
import dev.openrune.definition.type.builders.EnumTypeBuilder
import dev.openrune.definition.util.BoxedInts
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.definition.util.readSmart
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeSmart
import dev.openrune.definition.util.writeString
import io.netty.buffer.ByteBuf

class Rs2EnumCodec : Rs2OrderedBuilderCodec<EnumType, EnumTypeBuilder> {

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
                @Suppress("UNCHECKED_CAST")
                val target = values as MutableMap<Any, Any>
                repeat(count) {
                    val key = BoxedInts.of(buffer.readInt())
                    target[key] = if (opcode == 5) buffer.readString() else BoxedInts.of(buffer.readInt())
                }
            }

            7, 8 -> {
                dense = true
                capacity = buffer.readUnsignedShort()
                val count = buffer.readUnsignedShort()
                @Suppress("UNCHECKED_CAST")
                val target = values as MutableMap<Any, Any>
                repeat(count) {
                    val key = BoxedInts.of(buffer.readUnsignedShort())
                    target[key] = if (opcode == 7) buffer.readString() else BoxedInts.of(buffer.readInt())
                }
            }

            101 -> {
                keyType = Rs2VarLiteral.byID(buffer.readSmart())
                typesFromId = true
            }

            102 -> {
                valueType = Rs2VarLiteral.byID(buffer.readSmart())
                typesFromId = true
            }

            103 -> pluginApi = false
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: EnumType) {
        when (opcode) {
            1 -> writeByte(definition.keyType.ch.code)
            2 -> writeByte(definition.valueType.ch.code)
            3 -> writeString(definition.defaultString)
            4 -> writeInt(definition.defaultInt)
            5, 6 -> {
                writeShort(definition.values.size)
                for ((key, value) in definition.values) {
                    writeInt(key)
                    if (opcode == 5) writeString(value.toString()) else writeInt(value.toString().toInt())
                }
            }

            7, 8 -> {
                writeShort(definition.rs2?.capacity ?: definition.values.size)
                writeShort(definition.values.size)
                for ((key, value) in definition.values) {
                    writeShort(key)
                    if (opcode == 7) writeString(value.toString()) else writeInt(value.toString().toInt())
                }
            }

            101 -> writeSmart(definition.keyType.id)
            102 -> writeSmart(definition.valueType.id)
            103 -> Unit
        }
    }

    override fun createDefinition() = EnumType()
}
