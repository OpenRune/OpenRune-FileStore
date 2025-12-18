package dev.openrune.definition.opcode.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeType
import kotlin.reflect.KMutableProperty1

/**
 * Creates an opcode for reading/writing an enum via its ID.
 * Format: opcode, ID value (byte/int/etc)
 * Example: enum with fromId() and id property
 */
fun <T, E> DefinitionOpcodeEnum(
    opcode: Int,
    idType: OpcodeType<Int>,
    property: KMutableProperty1<T, E>,
    fromId: (Int) -> E,
    getId: (E) -> Int,
    defaultEnum: E
): DefinitionOpcode<T> = DefinitionOpcode(
    opcode = opcode,
    decode = { buf, def, _ ->
        val id = idType.read(buf)
        property.set(def, fromId(id))
    },
    encode = { buf, def ->
        idType.write(buf, getId(property.get(def)))
    },
    shouldEncode = { property.get(it) != defaultEnum }
)

