package dev.openrune.definition.codec.sound

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled

/**
 * Reads and writes the two vorbis layouts the cache uses.
 *
 * Index 14 splits them: archive 0 holds one setup header shared by every sample, and each later archive holds
 * only a sample body. Index 4 file 1 is self contained, prefixing its own setup header with that header's length,
 * so those sounds carry their own codebooks.
 *
 * Sample body:
 *   sampling frequency (int), sample count (int), loop start (int),
 *   loop end (int, stored inverted when the sound loops), packet count (int),
 *   then per packet a length (unsigned bytes summed while each is 255) and that many bytes.
 */
object VorbisCodec {

    fun decodeSharedSetup(data: ByteArray): VorbisSetup = VorbisSetup.read(data)

    fun decodeSound(data: ByteArray): VorbisSound = readSound(Unpooled.wrappedBuffer(data))

    fun decodeSelfContained(data: ByteArray): Pair<VorbisSetup, VorbisSound> {
        val buffer = Unpooled.wrappedBuffer(data)
        val headerLength = buffer.readInt()
        val setup = VorbisSetup.read(data, buffer.readerIndex())

        buffer.readerIndex(buffer.readerIndex() + headerLength)
        return setup to readSound(buffer)
    }

    fun encodeSound(sound: VorbisSound): ByteArray {
        val buffer = Unpooled.buffer()
        buffer.writeSound(sound)
        return ByteBufUtil.getBytes(buffer)
    }

    fun encodeSelfContained(setup: VorbisSetup, sound: VorbisSound): ByteArray {
        val header = encodeSetup(setup)
        val buffer = Unpooled.buffer()
        buffer.writeInt(header.size)
        buffer.writeBytes(header)
        buffer.writeSound(sound)
        return ByteBufUtil.getBytes(buffer)
    }

    fun encodeSetup(setup: VorbisSetup): ByteArray {
        val buffer = Unpooled.buffer()
        buffer.writeByte((setup.blocksize0 and 0x0F) or ((setup.blocksize1 and 0x0F) shl 4))
        buffer.writeBytes(setup.body)
        return ByteBufUtil.getBytes(buffer)
    }

    /** True when the bytes carry a length prefixed setup header rather than a bare sample body. */
    fun isSelfContained(data: ByteArray): Boolean {
        if (data.size < 24) return false
        val headerLength = Unpooled.wrappedBuffer(data).readInt()
        return headerLength in 1..(data.size - 24)
    }

    private fun readSound(buffer: ByteBuf): VorbisSound {
        val sampleRate = buffer.readInt()
        val sampleCount = buffer.readInt()
        val loopStart = buffer.readInt()

        val rawLoopEnd = buffer.readInt()
        val looping = rawLoopEnd < 0

        val packetCount = buffer.readInt()
        val packets = ArrayList<ByteArray>(packetCount)
        repeat(packetCount) {
            var length = 0
            var part: Int
            do {
                part = buffer.readUnsignedByte().toInt()
                length += part
            } while (part >= 255)

            packets += ByteArray(length).also { buffer.readBytes(it) }
        }

        return VorbisSound(
            sampleRate = sampleRate,
            sampleCount = sampleCount,
            loopStart = loopStart,
            loopEnd = if (looping) rawLoopEnd.inv() else rawLoopEnd,
            looping = looping,
            packets = packets
        )
    }

    private fun ByteBuf.writeSound(sound: VorbisSound) {
        writeInt(sound.sampleRate)
        writeInt(sound.sampleCount)
        writeInt(sound.loopStart)
        writeInt(if (sound.looping) sound.loopEnd.inv() else sound.loopEnd)
        writeInt(sound.packets.size)

        for (packet in sound.packets) {
            var remaining = packet.size
            while (remaining >= 255) {
                writeByte(255)
                remaining -= 255
            }
            writeByte(remaining)
            writeBytes(packet)
        }
    }
}
