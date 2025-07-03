package dev.openrune.definition.opcode.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.opcode.toGetterSetter
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

fun <T, R> DefinitionOpcodeListActions(
    opcode: Int,
    type: OpcodeType<R>,
    getter: (T) -> List<R?>?,
    setter: (T, List<R?>) -> Unit,
    expectedSize: Int
): DefinitionOpcode<T> = DefinitionOpcode(
    opcode = opcode,
    decode = { buf, def, _ ->
        val count = buf.readUnsignedByte().toInt()
        val list = MutableList<R?>(expectedSize) { null }
        repeat(count) {
            val index = buf.readUnsignedByte().toInt()
            require(index in 0 until expectedSize) { "Index $index out of bounds for list of size $expectedSize" }
            list[index] = type.read(buf)
        }
        setter(def, list)
    },
    encode = { buf, def ->
        val list = getter(def) ?: return@DefinitionOpcode
        val nonNullItems = list.withIndex().filter { it.index in 0 until expectedSize && it.value != null }
        buf.writeByte(nonNullItems.size)
        for ((index, value) in nonNullItems) {
            buf.writeByte(index)
            type.write(buf, value!!)
        }
    },
    shouldEncode = { def ->
        getter(def)?.any { it != null } == true
    }
)

fun <T, R> DefinitionOpcodeListActions(
    opcode: Int,
    type: OpcodeType<R>,
    property: KProperty1<T, List<R?>?>,
    expectedSize: Int,
    customSetter: ((T, List<R?>) -> Unit)? = null
): DefinitionOpcode<T> {
    val getter = { obj: T -> property.get(obj) }
    val setter = when {
        property is KMutableProperty1<T, List<R?>?> -> { obj: T, value: List<R?> -> property.set(obj, value) }
        customSetter != null -> { obj: T, value: List<R?> -> customSetter(obj, value) }
        else -> error("Property ${property.name} is not mutable and no customSetter was provided.")
    }
    return DefinitionOpcodeListActions(opcode, type, getter, setter, expectedSize)
}