package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class QuickChatCommand(val type: Int, val parameters: IntArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QuickChatCommand) return false
        return type == other.type && parameters.contentEquals(other.parameters)
    }

    override fun hashCode(): Int = 31 * type + parameters.contentHashCode()
}

data class QuickChatPhraseType(
    override var id: Int = -1,
    var template: String? = null,
    var autoResponses: List<Int> = emptyList(),
    var commands: List<QuickChatCommand> = emptyList(),
    /** Whether [commands]' parameters are var-length ints (opcode 5) rather than plain shorts (opcode 3). */
    var wide: Boolean = false,
    var searchable: Boolean = true
) : Definition
