package dev.openrune.definition.opcode.impl

import io.netty.buffer.ByteBuf
import kotlin.reflect.KMutableProperty1
import dev.openrune.definition.util.readUnsignedBoolean
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeString
import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.PropertyChain

fun <T, R> DefinitionOpcodeParams(
    opcode: Int,
    propertyChain: PropertyChain<T, R>
): DefinitionOpcode<T> where R : Map<String, Any?>? {
    val (getter, setter) = propertyChain.toGetterSetter()
    @Suppress("UNCHECKED_CAST")
    return definitionOpcodeParams(
        opcode,
        getter as (T) -> Map<String, Any?>?,
        setter as (T, Map<String, Any?>?) -> Unit
    )
}

fun <T> DefinitionOpcodeParams(
    opcode: Int,
    property: KMutableProperty1<T, MutableMap<String, Any>?>
): DefinitionOpcode<T> = definitionOpcodeParams(
    opcode,
    { def -> property.get(def) },
    { def, map ->
        val nonNullMap = map?.filterValues { it != null }?.mapValues { it.value as Any }?.toMutableMap()
        property.set(def, nonNullMap)
    }
)

// Core function unifying encoding and decoding logic
private fun <T> definitionOpcodeParams(
    opcode: Int,
    getter: (T) -> Map<String, Any?>?,
    setter: (T, Map<String, Any?>?) -> Unit
): DefinitionOpcode<T> = DefinitionOpcode(
    opcode,
    decode = { buf, def, _ ->
        val count = buf.readUnsignedByte().toInt()
        val map = mutableMapOf<String, Any?>()
        repeat(count) {
            val isString = buf.readUnsignedBoolean()
            val id = buf.readUnsignedMedium()
            val value: Any? = if (isString) buf.readString() else buf.readInt()
            map[id.toString()] = value
        }
        setter(def, map)
    },
    encode = { buf, def ->
        getter(def)?.let { params ->
            buf.writeByte(opcode)
            buf.writeByte(params.size)
            for ((id, value) in params) {
                val isString = value is String
                buf.writeByte(if (isString) 1 else 0)
                val keyInt = id.toIntOrNull() ?: error("Key $id is not a valid integer string")
                buf.writeMedium(keyInt)
                when (value) {
                    is String -> buf.writeString(value)
                    is Int -> buf.writeInt(value)
                    is Long -> {
                        require(value in Int.MIN_VALUE..Int.MAX_VALUE) {
                            "Long value $value is out of Int range for id $id"
                        }
                        buf.writeInt(value.toInt())
                    }
                    null -> buf.writeInt(0)
                    else -> error("Unsupported parameter type for id $id: ${value::class}")
                }
            }
        }
    },
    skipByteEncode = true,
    shouldEncode = { getter(it)?.isNotEmpty() == true }
)
