package dev.openrune.cache.filestore.definition

import dev.openrune.cache.filestore.buffer.Reader

data class SoundData(
    var id: Int,
    var loops: Int,
    var location: Int,
    var retain: Int,
)

interface Sound {


    fun readSounds(buffer: Reader, after220 : Boolean) : SoundData? {
        val id: Int
        val loops: Int
        val location: Int
        val retain: Int

        if (!after220) {
            val payload: Int = buffer.readMedium()
            retain = 0
            location = payload and 15
            id  = payload shr 8
            loops = payload shr 4 and 7
        } else {
            id = buffer.readUnsignedShort()
            loops = buffer.readUnsignedByte()
            location = buffer.readUnsignedByte()
            retain = buffer.readUnsignedByte()
        }

        return if (id >= 1 && loops >= 1 && location >= 0 && retain >= 0) { SoundData(id, loops, location, retain) } else { null }
    }

}