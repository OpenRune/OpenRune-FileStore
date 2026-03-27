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
        if (length == 0) {
            return
        }
        val params: MutableMap<String, Any> = mutableMapOf()
        for (i in 0 until length) {
            val type = buffer.readUnsignedByte().toInt()
            val id = buffer.readUnsignedMedium()
            params[id.toString()] = when(type) {
                0 -> buffer.readString()
                1 -> buffer.readLong()
                2 -> buffer.readInt()
                else -> error("Unsupported Type: ${type}")
            }
        }
        this.params = params
    }

    fun writeParameters(writer: ByteBuf) {
        val params = params ?: return

        writer.writeByte(249)
        writer.writeByte(params.size)

        for ((id, rawValue) in params) {
            val value = when (rawValue) {
                is Number -> if (rawValue.toLong() in Int.MIN_VALUE..Int.MAX_VALUE) rawValue.toInt() else rawValue.toLong()
                else -> rawValue
            }

            when (value) {
                is String -> {
                    writer.writeByte(0)
                    writer.writeMedium(id.toInt())
                    writer.writeString(value)
                }

                is Int -> {
                    writer.writeByte(2)
                    writer.writeMedium(id.toInt())
                    writer.writeInt(value)
                }

                is Long -> {
                    writer.writeByte(1)
                    writer.writeMedium(id.toInt())
                    writer.writeLong(value)
                }

                else -> error("Unsupported parameter type for id $id: ${value::class}")
            }
        }
    }

}