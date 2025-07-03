package dev.openrune.definition.opcode

import io.netty.buffer.ByteBuf

interface DefinitionOpcode<T> {
    val attachedOpcodes: IntRange
    fun decode(currentOpcode : Int,buffer: ByteBuf, definition: T)
    fun encode(buffer: ByteBuf, definition: T)
    fun shouldEncode(definition: T): Boolean = true
}

fun <T> DefinitionOpcode(
    opcode1: IntRange,
    decode: (ByteBuf, T, Int) -> Unit,
    encode: (ByteBuf, T) -> Unit,
    shouldEncode: (T) -> Boolean = { true },
    skipByteEncode : Boolean = false
): DefinitionOpcode<T> = object : DefinitionOpcode<T> {
    override val attachedOpcodes = opcode1
    override fun decode(currentOpcode : Int,buffer: ByteBuf, definition: T) = decode(buffer, definition, currentOpcode)
    override fun encode(buffer: ByteBuf, definition: T) {
        if (!skipByteEncode) buffer.writeByte(attachedOpcodes.first)
        encode(buffer, definition)
    }
    override fun shouldEncode(definition: T) = shouldEncode(definition)
}

fun <T> DefinitionOpcode(
    opcode: Int,
    decode: (ByteBuf, T, Int) -> Unit,
    encode: (ByteBuf, T) -> Unit,
    shouldEncode: (T) -> Boolean = { true },
    skipByteEncode : Boolean = false
): DefinitionOpcode<T> = object : DefinitionOpcode<T> {
    override val attachedOpcodes = IntRange(opcode,opcode)
    override fun decode(currentOpcode : Int,buffer: ByteBuf, definition: T) = decode(buffer, definition, currentOpcode)
    override fun encode(buffer: ByteBuf, definition: T) {
        if (!skipByteEncode) buffer.writeByte(opcode)
        encode(buffer, definition)
    }
    override fun shouldEncode(definition: T) = shouldEncode(definition)
}