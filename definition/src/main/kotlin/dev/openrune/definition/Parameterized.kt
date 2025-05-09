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
        params?.let { params ->
            writer.writeByte(249)
            writer.writeByte(params.size)
            params.forEach { (id, value) ->
                writer.writeByte(value is String)
                writer.writeMedium(id.toInt())
                if (value is String) {
                    writer.writeString(value)
                } else if (value is Int) {
                    writer.writeInt(value)
                }
            }
        }
    }

}