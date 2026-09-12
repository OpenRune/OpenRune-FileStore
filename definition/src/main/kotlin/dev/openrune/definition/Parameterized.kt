package dev.openrune.definition

import dev.openrune.definition.util.BoxedInts
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.readUnsignedBoolean
import dev.openrune.definition.util.writeByte
import dev.openrune.definition.util.writeString
import io.netty.buffer.ByteBuf

interface Parameterized {

    var params: MutableMap<Int, Any>?

    fun readParameters(buffer: ByteBuf) {
        val length = buffer.readUnsignedByte().toInt()
        if (length == 0) return

        val params = LinkedHashMap<Int, Any>(if (length < 3) 4 else (length / 0.75f).toInt() + 1)

        // Param ids repeat across thousands of definitions, so their key boxes are pooled. The
        // unchecked view only widens the key type; equality semantics are unchanged.
        @Suppress("UNCHECKED_CAST")
        val target = params as MutableMap<Any, Any>

        repeat(length) {
            val type = buffer.readUnsignedByte().toInt()
            val id = buffer.readUnsignedMedium()

            val value: Any = when (type) {
                1 -> buffer.readString()
                2 -> buffer.readLong()
                else -> BoxedInts.of(buffer.readInt())
            }

            target[BoxedInts.of(id)] = value
        }

        this.params = params
    }

    fun writeParameters(writer: ByteBuf) {
        val params = params ?: return

        writer.writeByte(249)
        writer.writeByte(params.size)

        for ((id, value) in params) {
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