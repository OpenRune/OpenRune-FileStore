package dev.openrune.definition

import io.netty.buffer.ByteBuf

data class SoundData(
    var id: Int,
    var unknown : Int,
    var loops: Int,
    var location: Int,
    var retain: Int,
) {
    fun writeSound(writer: ByteBuf, revision : Int) {
        if (!revisionIsOrAfter(revision, 220)) {
            val payload: Int = (location and 15) or (id shl 8) or (loops shl 4 and 7)
            writer.writeMedium(payload)
        } else {
            writer.writeByte(id)
            if (revisionIsOrAfter(revision, 226)) {
                writer.writeByte(unknown)
            }
            writer.writeByte(location)
            writer.writeByte(retain)
        }
    }
}

interface Sound {


    fun readSounds(buffer: ByteBuf, revision : Int) : SoundData? {
        val id: Int
        val loops: Int
        val location: Int
        val retain: Int
        var unknown : Int = 0

        if (!revisionIsOrAfter(revision, 220)) {
            val payload: Int = buffer.readMedium()
            retain = 0
            location = payload and 15
            id = payload shr 8
            loops = payload shr 4 and 7
        } else {
            id = buffer.readUnsignedShort()
            if (revisionIsOrAfter(revision, 226)) {
                unknown = buffer.readUnsignedByte().toInt()
            }
            loops = buffer.readUnsignedByte().toInt()
            location = buffer.readUnsignedByte().toInt()
            retain = buffer.readUnsignedByte().toInt()
        }

        return if (id >= 1 && loops >= 1 && location >= 0 && retain >= 0) { SoundData(id, unknown, loops, location, retain) } else { null }
    }

}