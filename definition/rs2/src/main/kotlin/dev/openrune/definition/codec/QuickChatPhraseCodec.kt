package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.QuickChatCommand
import dev.openrune.definition.type.QuickChatPhraseType
import dev.openrune.definition.util.readStringCP
import dev.openrune.definition.util.readVarInt
import dev.openrune.definition.util.writeStringCP
import dev.openrune.definition.util.writeVarInt
import io.netty.buffer.ByteBuf

/** Ported from [zwyz/rs3-cache](https://github.com/zwyz/rs3-cache)'s `QuickChatPhraseUnpacker.java`. */
class QuickChatPhraseCodec : DefinitionCodec<QuickChatPhraseType> {

    override fun QuickChatPhraseType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> template = buffer.readStringCP()
            2 -> autoResponses = List(buffer.readUnsignedByte().toInt()) { buffer.readUnsignedShort() }
            3 -> {
                wide = false
                commands = readCommands(buffer, wide = false)
            }
            4 -> searchable = false
            5 -> {
                wide = true
                commands = readCommands(buffer, wide = true)
            }
        }
    }

    private fun readCommands(buffer: ByteBuf, wide: Boolean): List<QuickChatCommand> =
        List(buffer.readUnsignedByte().toInt()) {
            val type = buffer.readUnsignedShort()
            val paramCount = COMMAND_PARAM_COUNTS[type] ?: error("Unknown quickchat command $type")
            val parameters = IntArray(paramCount) { if (wide) buffer.readVarInt() else buffer.readUnsignedShort() }
            QuickChatCommand(type, parameters)
        }

    override fun ByteBuf.encode(definition: QuickChatPhraseType) {
        definition.template?.let {
            writeByte(1)
            writeStringCP(it)
        }
        if (definition.autoResponses.isNotEmpty()) {
            writeByte(2)
            writeByte(definition.autoResponses.size)
            definition.autoResponses.forEach { writeShort(it) }
        }
        if (definition.commands.isNotEmpty()) {
            writeByte(if (definition.wide) 5 else 3)
            writeByte(definition.commands.size)
            definition.commands.forEach { command ->
                writeShort(command.type)
                command.parameters.forEach { if (definition.wide) writeVarInt(it) else writeShort(it) }
            }
        }
        if (!definition.searchable) writeByte(4)
        writeByte(0)
    }

    override fun createDefinition() = QuickChatPhraseType(0)

    companion object {
        // command type -> parameter count, from QuickChatPhraseUnpacker.unpackCommand
        private val COMMAND_PARAM_COUNTS = mapOf(
            0 to 1, 1 to 0, 2 to 0, 4 to 1, 6 to 2, 7 to 1, 8 to 1, 9 to 1,
            10 to 0, 11 to 2, 12 to 0, 13 to 0, 14 to 1, 15 to 0, 16 to 2
        )
    }
}
