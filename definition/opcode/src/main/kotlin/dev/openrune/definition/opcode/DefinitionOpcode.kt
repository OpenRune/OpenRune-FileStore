package dev.openrune.definition.opcode

import io.netty.buffer.ByteBuf

interface DefinitionOpcode<T> {
    val attachedOpcodes: Set<Int>
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
    override val attachedOpcodes = opcode1.toSet()
    override fun decode(currentOpcode : Int,buffer: ByteBuf, definition: T) = decode(buffer, definition, currentOpcode)
    override fun encode(buffer: ByteBuf, definition: T) {
        if (!skipByteEncode) buffer.writeByte(opcode1.first)
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
    override val attachedOpcodes = setOf(opcode)
    override fun decode(currentOpcode : Int,buffer: ByteBuf, definition: T) = decode(buffer, definition, currentOpcode)
    override fun encode(buffer: ByteBuf, definition: T) {
        if (!skipByteEncode) buffer.writeByte(opcode)
        encode(buffer, definition)
    }
    override fun shouldEncode(definition: T) = shouldEncode(definition)
}

fun <T> DefinitionOpcode(
    opcodes: Set<Int>,
    decode: (ByteBuf, T, Int) -> Unit,
    encode: (ByteBuf, T) -> Unit,
    shouldEncode: (T) -> Boolean = { true },
    skipByteEncode : Boolean = false,
    primaryOpcode: Int? = null
): DefinitionOpcode<T> = object : DefinitionOpcode<T> {
    init {
        require(opcodes.isNotEmpty()) { "Opcode set must not be empty." }
        if (!skipByteEncode) {
            require(primaryOpcode != null) { "primaryOpcode is required when skipByteEncode is false." }
        }
        if (primaryOpcode != null) {
            require(opcodes.contains(primaryOpcode)) { "primaryOpcode must be included in opcodes." }
        }
    }

    override val attachedOpcodes = opcodes
    override fun decode(currentOpcode : Int,buffer: ByteBuf, definition: T) = decode(buffer, definition, currentOpcode)
    override fun encode(buffer: ByteBuf, definition: T) {
        if (!skipByteEncode) buffer.writeByte(primaryOpcode!!)
        encode(buffer, definition)
    }
    override fun shouldEncode(definition: T) = shouldEncode(definition)
}