package dev.openrune.definition

import dev.openrune.definition.util.readString
import dev.openrune.definition.util.readUnsignedBoolean
import dev.openrune.definition.util.writeByte
import dev.openrune.definition.util.writeString
import io.netty.buffer.ByteBuf

interface Parameterized {

    var params: MutableMap<String, Any>?

    fun readParameters(buffer: ByteBuf) {
        val length = buffer.readUnsignedByte().toInt()
        if (length == 0) return

        val params = mutableMapOf<String, Any>()

        repeat(length) {
            val type = buffer.readUnsignedByte().toInt()
            val id = buffer.readUnsignedMedium()

            val value: Any = when (type) {
                1 -> buffer.readString()
                2 -> buffer.readLong()
                else -> buffer.readInt()
            }

            params[id.toString()] = value
        }

        this.params = params
    }

    fun writeParameters(writer: ByteBuf) {
        val params = params ?: return

        writer.writeByte(249)
        writer.writeByte(params.size)

        for ((idStr, value) in params) {
            val id = idStr.toIntOrNull() ?: error("Invalid param id (not an Int): '$idStr'")


            when (value) {
                is Int -> {
                    writer.writeByte(0)
                    writer.writeMedium(id)
                    writer.writeInt(value)
                }

                is String -> {
                    writer.writeByte(1)
                    writer.writeMedium(id)
                    writer.writeString(value)
                }

                is Long -> {
                    writer.writeByte(2)
                    writer.writeMedium(id)
                    writer.writeLong(value)
                }

                else -> error("Unsupported parameter type for id $id: ${value::class}")
            }
        }
    }

}