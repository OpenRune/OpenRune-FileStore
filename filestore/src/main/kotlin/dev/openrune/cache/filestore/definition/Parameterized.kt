package dev.openrune.cache.filestore.definition

import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.data.DataValue
import dev.openrune.cache.filestore.definition.data.IntValue
import dev.openrune.cache.filestore.definition.data.StringValue
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic

interface Parameterized {

    @Contextual
    var params: Map<Int, @Polymorphic DataValue?>?

    fun readParameters(buffer: Reader) {
        val length = buffer.readUnsignedByte()
        if (length == 0) {
            return
        }
        val params = HashMap<Int, DataValue?>()
        for (i in 0 until length) {
            val string = buffer.readUnsignedBoolean()
            val id = buffer.readUnsignedMedium()
            if (string) {
                params[id] = StringValue(buffer.readString())
            } else {
                params[id] = IntValue(buffer.readInt())
            }
        }
        this.params = params
    }

    fun writeParameters(writer: Writer) {
        params?.let { params ->
            writer.writeByte(249)
            writer.writeByte(params.size)
            params.forEach { (id, value) ->
                writer.writeByte(value is StringValue)
                writer.writeMedium(id)
                if (value is StringValue) {
                    writer.writeString(value.value)
                } else if (value is IntValue) {
                    writer.writeInt(value.value)
                }
            }
        }
    }

}