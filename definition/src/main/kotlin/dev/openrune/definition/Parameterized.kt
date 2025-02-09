package dev.openrune.definition

import dev.openrune.buffer.*
import io.netty.buffer.ByteBuf
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap

interface Parameterized {

    var params: Map<Int, Any>?

    fun readParameters(buffer: ByteBuf) {
        val length = buffer.readUnsignedByteRD()
        if (length == 0) {
            return
        }
        val params = Int2ObjectArrayMap<Any>()
        for (i in 0 until length) {
            val string = buffer.readUnsignedBooleanRD()
            val id = buffer.readUnsignedMediumRD()
            params[id] = if (string) buffer.readStringRD() else buffer.readIntRD()
        }
        this.params = params
    }

    fun writeParameters(writer: Writer) {
        params?.let { params ->
            writer.writeByte(249)
            writer.writeByte(params.size)
            params.forEach { (id, value) ->
                writer.writeByte(value is String)
                writer.writeMedium(id)
                if (value is String) {
                    writer.writeString(value)
                } else if (value is Int) {
                    writer.writeInt(value)
                }
            }
        }
    }

}