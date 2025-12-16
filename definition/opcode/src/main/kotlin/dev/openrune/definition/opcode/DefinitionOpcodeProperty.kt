package dev.openrune.definition.opcode

import io.netty.buffer.ByteBuf
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

fun <T, R> KMutableProperty1<T, R?>.toGetterSetter(): Pair<(T) -> R?, (T, R) -> Unit> =
    ({ receiver: T -> this.get(receiver) }) to ({ receiver: T, value: R -> this.set(receiver, value) })

fun <T, R> KProperty1<T, R?>.toGetterSetter(customSetter: ((T, R) -> Unit)? = null): Pair<(T) -> R?, (T, R) -> Unit> {
    if (this is KMutableProperty1<T, R?>) {
        return this.toGetterSetter()
    }
    requireNotNull(customSetter) { "Cannot decode into read-only property '${this.name}'. Provide a setter." }
    return ({ receiver: T -> this.get(receiver) }) to customSetter
}

fun <T, R> DefinitionOpcode(
    opcode: Int,
    type: OpcodeType<R>,
    propertyChain: PropertyChain<T, R>
): DefinitionOpcode<T> {
    val (getter, setter) = propertyChain.toGetterSetter()
    return DefinitionOpcode(opcode, type, getter, setter)
}

fun <T, R> DefinitionOpcode(
    opcode: Int,
    type: OpcodeType<R>,
    getterSetter: Pair<(T) -> R?, (T, R) -> Unit>
): DefinitionOpcode<T> {
    val (getter, setter) = getterSetter
    return DefinitionOpcode(opcode, type, getter, setter)
}

fun <T, R> DefinitionOpcode(
    opcode: Int,
    type: OpcodeType<R>,
    getter: (T) -> R?,
    setter: (T, R) -> Unit
): DefinitionOpcode<T> = DefinitionOpcode(
    opcode,
    decode = { buf, def, _ ->
        val value = type.read(buf)
        setter(def, value)
    },
    encode = { buf, def ->
        getter(def)?.let { type.write(buf, it) }
    },
    shouldEncode = { getter(it) != null }
)

fun <T, R> IgnoreOpcode(
    opcode: Int,
    type: OpcodeType<R>
): DefinitionOpcode<T> = DefinitionOpcode(
    opcode = opcode,
    decode = { buf, _, _ ->
        type.read(buf)
    },
    encode = { _, _ -> },
    shouldEncode = { false }
)

fun <T> IgnoreOpcode(
    opcode: Int,
    decode: (ByteBuf) -> Unit
): DefinitionOpcode<T> = DefinitionOpcode(
    opcode = opcode,
    decode = { buf, _, _ -> decode(buf) },
    encode = { _, _ -> },
    shouldEncode = { false }
)

fun <T, R> DefinitionOpcode(
    opcode: Int,
    type: OpcodeType<R>,
    property: KMutableProperty1<T, R?>
): DefinitionOpcode<T> {
    val (getter, setter) = property.toGetterSetter()
    return DefinitionOpcode(opcode, type, getter, setter)
}

fun <T, R> DefinitionOpcode(
    opcode: Int,
    type: OpcodeType<R>,
    property: KProperty1<T, R?>,
    customSetter: ((T, R) -> Unit)? = null
): DefinitionOpcode<T> {
    val (getter, setter) = property.toGetterSetter(customSetter)
    return DefinitionOpcode(opcode, type, getter, setter)
}