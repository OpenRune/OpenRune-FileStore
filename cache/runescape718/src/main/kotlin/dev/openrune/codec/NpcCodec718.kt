package dev.openrune.codec

import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.data.NpcType

class NpcCodec718 : DefinitionCodec<NpcType> {
    override fun NpcType.read(opcode: Int, buffer: Reader) {
        when (opcode) {
            1 -> {
                val length = buffer.readUnsignedByte()
                for (count in 0 until length) {
                    buffer.readUnsignedShort()
                }
            }
            2 -> name = buffer.readString()
            12 -> size = buffer.readUnsignedByte()
            in 30..34 -> buffer.readString()
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            42 -> readColourPalette(buffer)
            60 ->  IntArray(buffer.readUnsignedByte()) { buffer.readUnsignedShort() }
            93 ->  {}
            95 -> buffer.readShort()
            97 -> buffer.readShort()
            98 -> buffer.readShort()
            99 -> {}
            100 -> buffer.readByte()
            101 -> buffer.readByte()
            102 -> buffer.readShort()
            103 -> rotation = buffer.readShort()
            106, 118 -> readTransforms(buffer, opcode == 118)
            107 -> {}
            109 -> {}
            111 -> {}
            113 -> {
                buffer.readShort().toShort()
                buffer.readShort().toShort()
            }
            114 -> {
                buffer.readByte().toByte()
                buffer.readByte().toByte()
            }
            119 -> buffer.readByte().toByte()
            121 -> {
                val length = buffer.readUnsignedByte()
                for (count in 0 until length) {
                    val index = buffer.readUnsignedByte()
                    intArrayOf(
                        buffer.readByte(),
                        buffer.readByte(),
                        buffer.readByte()
                    )
                }
            }
            122 -> buffer.readShort()
            123 -> height = buffer.readShort()
            125 -> buffer.readByte().toByte()
            127 -> buffer.readShort()
            128 -> buffer.readUnsignedByte()
            134 -> {
                buffer.readShort()
                buffer.readShort()
                buffer.readShort()
                buffer.readShort()
                buffer.readUnsignedByte()
            }
            135 -> {
                buffer.readUnsignedByte()
                buffer.readShort()
            }
            136 -> {
                buffer.readUnsignedByte()
                buffer.readShort()
            }
            137 -> buffer.readShort()
            138 -> buffer.readShort()
            139 -> buffer.readShort()
            140 -> buffer.readUnsignedByte()
            141 -> {}
            142 -> buffer.readShort()
            143 -> {}
            in 150..154 -> {
                buffer.readString()
            }
            155 -> {
                buffer.readByte().toByte()
                buffer.readByte().toByte()
                buffer.readByte().toByte()
                buffer.readByte().toByte()
            }
            158 -> 1.toByte()
            159 ->  0.toByte()
            160 -> {
                val length = buffer.readUnsignedByte()
                IntArray(length) { buffer.readShort() }
            }
            162 -> {}
            163 -> buffer.readUnsignedByte()
            164 -> {
                buffer.readShort()
                buffer.readShort()
            }
            165 -> buffer.readUnsignedByte()
            168 -> buffer.readUnsignedByte()
            249 -> readParameters(buffer)
        }
    }

    private fun readColourPalette(buffer: Reader) {
        val length = buffer.readUnsignedByte()
        ByteArray(length) { buffer.readByte().toByte() }
    }

    override fun Writer.encode(definition: NpcType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = NpcType()
}