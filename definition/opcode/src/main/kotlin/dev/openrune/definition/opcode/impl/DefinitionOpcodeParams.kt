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
        val length = buf.readUnsignedByte().toInt()
        if (length == 0) {
            setter(def, emptyMap())
            return@DefinitionOpcode
        }

        val params = mutableMapOf<String, Any?>()

        repeat(length) {
            val type = buf.readUnsignedByte().toInt()
            val id = buf.readUnsignedMedium()

            val value: Any = when (type) {
                1 -> buf.readString()
                2 -> buf.readLong()
                else -> buf.readInt()
            }

            params[id.toString()] = value
        }

        setter(def, params)
    },

    encode = { buf, def ->
        val params = getter(def) ?: return@DefinitionOpcode
        if (params.isEmpty()) return@DefinitionOpcode

        buf.writeByte(opcode)
        buf.writeByte(params.size)

        for ((idStr, value) in params) {
            val id = idStr.toIntOrNull()
                ?: error("Invalid param id (not an Int): '$idStr'")

            require(id in 0..0xFFFFFF) {
                "Param id out of range (0..16777215): $id"
            }

            when (value) {
                is Int -> {
                    buf.writeByte(0)
                    buf.writeMedium(id)
                    buf.writeInt(value)
                }

                is String -> {
                    buf.writeByte(1)
                    buf.writeMedium(id)
                    buf.writeString(value)
                }

                is Long -> {
                    buf.writeByte(2)
                    buf.writeMedium(id)
                    buf.writeLong(value)
                }

                null -> {
                    error("Null parameter value for id $id")
                }

                else -> {
                    error("Unsupported parameter type for id $id: ${value::class}")
                }
            }
        }
    },

    skipByteEncode = true,
    shouldEncode = { getter(it)?.isNotEmpty() == true }
)
