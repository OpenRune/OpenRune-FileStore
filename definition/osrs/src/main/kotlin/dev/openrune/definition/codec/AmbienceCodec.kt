package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.AmbienceType
import dev.openrune.definition.type.RandomSound
import dev.openrune.definition.type.VarpType
import io.netty.buffer.ByteBuf

class AmbienceCodec : DefinitionCodec<AmbienceType> {

    override fun AmbienceType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {

            1 -> {
                val count = buffer.readUnsignedByte().toInt()
                sequentialSounds = IntArray(count) { buffer.readUnsignedShort() }
            }

            2 -> {
                val delayMin = buffer.readUnsignedShort()
                val delayMax = buffer.readUnsignedShort()
                val count = buffer.readUnsignedByte().toInt()
                val sounds = IntArray(count) { buffer.readUnsignedShort() }.toList().toMutableList()

                randomSounds = RandomSound(delayMin, delayMax, soundIds = sounds)
            }

            3 -> {
                val f = fade ?: AmbienceType.SoundFade()
                f.inSpeed = buffer.readUnsignedByte().toInt()
                f.inDuration = buffer.readUnsignedShort()
                fade = f
            }

            4 -> {
                val f = fade ?: AmbienceType.SoundFade()
                f.outSpeed = buffer.readUnsignedByte().toInt()
                f.outDuration = buffer.readUnsignedShort()
                fade = f
            }
        }
    }

    override fun ByteBuf.encode(definition: AmbienceType) {

        definition.sequentialSounds?.let { sounds ->
            writeByte(1)
            writeByte(sounds.size)
            sounds.forEach { writeShort(it) }
        }

        definition.randomSounds?.let {
            if (it.soundIds.isNotEmpty()) {
                writeByte(2)
                writeShort(it.minDelay)
                writeShort(it.maxDelay)
                writeByte(it.soundIds.size)
                it.soundIds.forEach { s -> writeShort(s) }
            }
        }

        definition.fade?.let {
            if (it.inSpeed != null) {
                writeByte(3)
                writeByte(it.inSpeed!!)
                writeShort(it.inDuration!!)
            }

            if (it.outSpeed != null) {
                writeByte(4)
                writeByte(it.outSpeed!!)
                writeShort(it.outDuration!!)
            }
        }

        writeByte(0)
    }

    override fun createDefinition() = AmbienceType()
}