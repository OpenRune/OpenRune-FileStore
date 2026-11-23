package dev.openrune.definition.codec

import dev.openrune.definition.Rs2Opcode
import io.netty.buffer.ByteBuf

internal fun readOpcodeOrder(buffer: ByteBuf, read: (Int) -> Unit): List<Rs2Opcode> {
    val opcodes = mutableListOf<Int>()
    val payloads = mutableListOf<ByteArray>()
    while (true) {
        val opcode = buffer.readUnsignedByte().toInt()
        if (opcode == 0) break
        val start = buffer.readerIndex()
        read(opcode)
        opcodes += opcode
        payloads += ByteArray(buffer.readerIndex() - start).also { buffer.getBytes(start, it) }
    }

    val lastOf = HashMap<Int, Int>()
    opcodes.forEachIndexed { index, opcode -> lastOf[opcode] = index }
    return opcodes.mapIndexed { index, opcode ->
        Rs2Opcode(opcode, payloads[index].takeIf { lastOf[opcode] != index })
    }
}

internal inline fun ByteBuf.replayOpcodes(order: List<Rs2Opcode>, writePayload: (Int, Int) -> Unit) {
    val seen = HashMap<Int, Int>()
    for (entry in order) {
        writeByte(entry.opcode)
        val occurrence = seen.getOrDefault(entry.opcode, 0)
        seen[entry.opcode] = occurrence + 1
        val superseded = entry.superseded
        if (superseded != null) writeBytes(superseded) else writePayload(entry.opcode, occurrence)
    }
    writeByte(0)
}
