package dev.openrune.definition

class Rs2Opcode(val opcode: Int, val superseded: ByteArray? = null) {

    override fun toString() = if (superseded == null) "$opcode" else "$opcode(superseded)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Rs2Opcode) return false
        if (opcode != other.opcode) return false
        val mine = superseded
        val theirs = other.superseded
        return if (mine == null || theirs == null) mine === theirs else mine.contentEquals(theirs)
    }

    override fun hashCode(): Int = 31 * opcode + (superseded?.contentHashCode() ?: 0)
}

interface Rs2OpcodeOrdered {
    val opcodeOrder: MutableList<Rs2Opcode>
}
