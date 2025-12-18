package dev.openrune.definition.opcode.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.opcode.toGetterSetter
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

fun <T, R> DefinitionOpcodeList(
    opcode: Int,
    type: OpcodeType<R>,
    getter: (T) -> List<R>?,
    setter: (T, List<R>) -> Unit
): DefinitionOpcode<T> = DefinitionOpcode(
    opcode,
    decode = { buf, def, _ ->
        val count = buf.readUnsignedByte().toInt()
        val list = MutableList<R>(count) {
            type.read(buf)
        }
        setter(def, list)
    },
    encode = { buf, def ->
        val list = getter(def)
        buf.writeByte(list?.size ?: 0)
        list?.forEach { type.write(buf, it) }
    },
    shouldEncode = { getter(it)?.isNotEmpty() == true }
)

fun <T, R> DefinitionOpcodeList(
    opcode: Int,
    type: OpcodeType<R>,
    property: KProperty1<T, List<R>?>,
    customSetter: ((T, List<R>) -> Unit)? = null
): DefinitionOpcode<T> {
    val (getter, setter) = property.toGetterSetter(customSetter)
    return DefinitionOpcodeList(opcode, type, getter, setter)
}
