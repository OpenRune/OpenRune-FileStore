package dev.openrune.definition.codec

import dev.openrune.buffer.Reader
import dev.openrune.buffer.Writer
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.IdentityKitType

class IdentityKitCodec : DefinitionCodec<IdentityKitType> {
    override fun IdentityKitType.read(opcode: Int, buffer: Reader) {
        when (opcode) {
            1 -> bodyPartId = buffer.readUnsignedByte()
            2 -> {
                val length = buffer.readUnsignedByte()
                models = MutableList(length) { 0 }
                for (count in 0 until length) {
                    models!![count] = buffer.readUnsignedShort()
                    if (models!![count] == 65535) {
                        models!![count] = -1
                    }
                }
            }
            3 -> nonSelectable = true
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            in 60..70 -> chatheadModels[opcode - 60] = buffer.readUnsignedShort()
        }
    }

    override fun Writer.encode(definition: IdentityKitType) {
        if(definition.bodyPartId != -1) {
            writeByte(1)
            writeByte(definition.bodyPartId)
        }

        if (definition.models != null && definition.models!!.isNotEmpty()) {
            writeByte(1)
            writeByte(definition.models!!.size)
            for (i in definition.models!!.indices) {
                writeShort(definition.models!![i])
            }
        }

        if (definition.nonSelectable) {
            writeByte(3)
        }

        definition.writeColoursTextures(this)

        if (definition.chatheadModels.any { it != -1 }) {
            for (i in 0 until definition.chatheadModels.size) {
                writeByte(60 + i)
                writeShort(definition.chatheadModels[i])
            }
        }
    }

    override fun createDefinition() = IdentityKitType()
}