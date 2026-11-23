package dev.openrune.definition.codec

import dev.openrune.definition.VarDomain
import dev.openrune.definition.type.Rs2VarBitType
import dev.openrune.definition.util.readNullableLargeSmartCorrect
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import io.netty.buffer.ByteBuf

class Rs2VarBitCodec : Rs2OrderedCodec<Rs2VarBitType> {

    override fun Rs2VarBitType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> {
                domain = VarDomain.byID(buffer.readUnsignedByte().toInt())
                baseVar = buffer.readNullableLargeSmartCorrect() ?: -1
            }

            2 -> {
                startBit = buffer.readUnsignedByte().toInt()
                endBit = buffer.readUnsignedByte().toInt()
            }

            16 -> wikiSync = true
            17 -> pluginApi = false
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: Rs2VarBitType) {
        when (opcode) {
            1 -> {
                writeByte(definition.domain!!.id)
                writeNullableLargeSmartCorrect(definition.baseVar)
            }

            2 -> {
                writeByte(definition.startBit!!)
                writeByte(definition.endBit!!)
            }

            16, 17 -> Unit
        }
    }

    override fun createDefinition() = Rs2VarBitType()
}
