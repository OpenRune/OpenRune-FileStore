package dev.openrune.definition.opcode.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.opcode.PropertyChain

fun <T, K, V> DefinitionOpcodeMap(
    opcode: Int,
    keyType: OpcodeType<K>,
    valueType: OpcodeType<V>,
    propertyChain: PropertyChain<T, Map<K, V>>
): DefinitionOpcode<T> {
    val (getter, setter) = propertyChain.toGetterSetter()
    val safeSetter: (T, Map<K, V>?) -> Unit = { t, m -> setter(t, m ?: emptyMap()) }

    return DefinitionOpcodeMap(opcode, keyType, valueType, getter, safeSetter)
}

fun <T, K, V> DefinitionOpcodeMap(
    opcode: Int,
    keyType: OpcodeType<K>,
    valueType: OpcodeType<V>,
    getter: (T) -> Map<K, V>?,
    setter: (T, Map<K, V>?) -> Unit
): DefinitionOpcode<T> = DefinitionOpcode(
    opcode,
    decode = { buf, def, _ ->
        val size = buf.readUnsignedShort()
        val map = mutableMapOf<K, V>()
        repeat(size) {
            val key = keyType.read(buf)
            val value = valueType.read(buf)
            map[key] = value
        }
        setter(def, map)
    },
    encode = { buf, def ->
        val map = getter(def) ?: return@DefinitionOpcode
        buf.writeShort(map.size)
        for ((k, v) in map) {
            keyType.write(buf, k)
            valueType.write(buf, v)
        }
    },
    shouldEncode = { getter(it)?.isNotEmpty() == true }
)