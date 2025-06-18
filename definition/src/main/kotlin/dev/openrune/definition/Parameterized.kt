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
            val string = buffer.readUnsignedBoolean()
            val id = buffer.readUnsignedMedium()
            params[id.toString()] = if (string) buffer.readString() else buffer.readInt()
        }
        this.params = params
    }

    fun writeParameters(writer: ByteBuf) {
        val params = params ?: return

        writer.writeByte(249)
        writer.writeByte(params.size)
        for ((id, value) in params) {
            val isString = value is String
            writer.writeByte(if (isString) 1 else 0)
            writer.writeMedium(id.toInt())

            when (value) {
                is String -> writer.writeString(value)
                is Int -> writer.writeInt(value)
                is Long -> {
                    require(value in Int.MIN_VALUE..Int.MAX_VALUE) {
                        "Long value $value is out of Int range for id $id"
                    }
                    writer.writeInt(value.toInt())
                }
                else -> error("Unsupported parameter type for id $id: ${value::class}")
            }
        }
    }

}