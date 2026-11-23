package dev.openrune.definition

import io.netty.buffer.ByteBuf

interface Rs2Recolourable : MutableRecolourable {
    var recolourMask: Int?
    var textureMask: Int?
    val legacyColourSlots: MutableMap<Int, Int>

    fun recolourSlots(): List<Int> {
        val mask = recolourMask ?: return originalColours?.indices?.toList() ?: emptyList()
        return (0 until 16).filter { (mask and (1 shl it)) != 0 }
    }

    fun readLegacyColourSlot(opcode: Int, buffer: ByteBuf) {
        legacyColourSlots[opcode] = buffer.readUnsignedShort()
    }

    fun writeLegacyColourSlots(buffer: ByteBuf) {
        for (opcode in legacyColourSlots.keys.sorted()) {
            buffer.writeByte(opcode)
            buffer.writeShort(legacyColourSlots.getValue(opcode))
        }
    }

    fun isLegacyColourSlot(opcode: Int): Boolean = opcode == 42 || opcode == 43 || opcode in 46..59
}

fun ByteBuf.writeRecolourPairs(original: List<Int>?, modified: List<Int>?) {
    val source = original.orEmpty()
    val destination = modified.orEmpty()
    writeByte(source.size)
    for (i in source.indices) {
        writeShort(source[i])
        writeShort(destination[i])
    }
}
