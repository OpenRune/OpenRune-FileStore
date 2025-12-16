package dev.openrune.definition.opcode.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.opcode.toGetterSetter
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

/**
 * Creates multiple opcodes where each opcode directly maps to a list index.
 * Format: opcode, string value (where opcode - range.first = index)
 * Decode: list[opcode - range.first] = buf.readString()
 * Encode: for each non-null item, write (range.first + index), then string value
 * Example: range 30..34 maps opcodes 30-34 to indices 0-4
 */
fun <T> DefinitionOpcodeListActions(
    opcodeRange: IntRange,
    getter: (T) -> List<String?>?,
    setter: (T, List<String?>) -> Unit
): List<DefinitionOpcode<T>> {
    val listSize = opcodeRange.last - opcodeRange.first + 1
    return opcodeRange.map { opcode ->
        val index = opcode - opcodeRange.first
        DefinitionOpcode(
            opcode = opcode,
            decode = { buf, def, _ ->
                val value = OpcodeType.STRING.read(buf)
                val existing = getter(def) ?: emptyList()
                val currentList = ArrayList<String?>(listSize).apply {
                    addAll(existing)
                    repeat(listSize - existing.size) { add(null) }
                }
                currentList[index] = value
                setter(def, currentList)
            },
            encode = { buf, def ->
                getter(def)?.getOrNull(index)?.let { item ->
                    OpcodeType.STRING.write(buf, item)
                }
            },
            shouldEncode = { def ->
                getter(def)?.getOrNull(index) != null
            }
        )
    }
}

/**
 * Creates multiple opcodes where each opcode directly maps to a list index.
 * Format: opcode, string value (where opcode - range.first = index)
 * Example: range 30..34 maps opcodes 30-34 to indices 0-4
 */
fun <T> DefinitionOpcodeListActions(
    opcodeRange: IntRange,
    property: KProperty1<T, List<String?>?>,
    customSetter: ((T, List<String?>) -> Unit)? = null
): List<DefinitionOpcode<T>> {
    val getter = { obj: T -> property.get(obj) }
    val setter = when {
        property is KMutableProperty1<T, List<String?>?> -> { obj: T, value: List<String?> -> property.set(obj, value) }
        customSetter != null -> customSetter
        else -> error("Property ${property.name} is not mutable and no customSetter was provided.")
    }
    return DefinitionOpcodeListActions(opcodeRange, getter, setter)
}