package dev.openrune.definition.codec.sound

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled

/** Converts between cache sounds and standalone Ogg Vorbis streams. */
object OggVorbis {

    private val MAGIC = "vorbis".toByteArray(Charsets.US_ASCII)
    private val OGGS = "OggS".toByteArray(Charsets.US_ASCII)

    private const val BOS = 0x02
    private const val EOS = 0x04

    private const val PAGE_TARGET_BYTES = 4096
    private const val MAX_SEGMENTS_PER_PAGE = 255

    /**
     * Wraps a cache sound in an ogg stream by synthesising the identification and comment headers the cache
     * leaves out, then muxing the sound's own audio packets into pages.
     */
    fun write(setup: VorbisSetup, sound: VorbisSound, serial: Int): ByteArray {
        val out = Unpooled.buffer()
        val pages = PageWriter(out, serial)

        pages.write(listOf(identificationHeader(setup, sound.sampleRate)), granulePosition = 0, flags = BOS)
        pages.write(listOf(commentHeader(sound)), granulePosition = 0, flags = 0)
        pages.write(listOf(byteArrayOf(0x05) + MAGIC + setup.body), granulePosition = 0, flags = 0)

        val granules = granulePositions(setup, sound)

        var pending = mutableListOf<ByteArray>()
        var pendingBytes = 0
        var pendingSegments = 0

        sound.packets.forEachIndexed { index, packet ->
            val last = index == sound.packets.lastIndex
            val segments = packet.size / 255 + 1
            val full = pendingBytes >= PAGE_TARGET_BYTES || pendingSegments + segments > MAX_SEGMENTS_PER_PAGE

            if (pending.isNotEmpty() && (full || last)) {
                pages.write(pending, granulePosition = granules[index - 1], flags = 0)
                pending = mutableListOf()
                pendingBytes = 0
                pendingSegments = 0
            }

            pending += packet
            pendingBytes += packet.size
            pendingSegments += segments

            if (last) pages.write(pending, granulePosition = granules[index], flags = EOS)
        }

        return ByteBufUtil.getBytes(out)
    }

    /**
     * Reads a standalone ogg stream back into a cache sound and the setup it was encoded with.
     *
     * The identification and comment headers are dropped, since the cache stores neither; their sample rate and
     * loop tags are carried over into the sound's own fields.
     */
    fun read(data: ByteArray): Pair<VorbisSetup, VorbisSound> {
        val stream = readPackets(Unpooled.wrappedBuffer(data))
        val packets = stream.packets
        require(packets.size >= 3) { "not an ogg vorbis stream: found ${packets.size} packets" }

        val identification = Unpooled.wrappedBuffer(packets[0])
        require(identification.readableBytes() >= 30 && identification.readUnsignedByte().toInt() == 0x01 && hasMagic(packets[0])) {
            "first packet is not a vorbis identification header"
        }

        identification.readerIndex(11)
        val channels = identification.readUnsignedByte().toInt()
        require(channels == CHANNELS) {
            "only mono sounds can be packed, this stream has $channels channels"
        }

        val sampleRate = identification.readIntLE()
        identification.readerIndex(28)
        val blocksizes = identification.readUnsignedByte().toInt()

        val setupPacket = packets[2]
        require(setupPacket.size > 7 && setupPacket[0].toInt() == 0x05 && hasMagic(setupPacket)) {
            "third packet is not a vorbis setup header"
        }
        val setup = VorbisSetup.of(
            blocksize0 = blocksizes and 0x0F,
            blocksize1 = (blocksizes ushr 4) and 0x0F,
            body = setupPacket.copyOfRange(7, setupPacket.size)
        )

        val comments = readComments(packets[1])
        val sampleCount = stream.finalGranulePosition.toInt()

        val loopStart = comments["LOOPSTART"]?.toIntOrNull() ?: 0
        val loopEnd = comments["LOOPEND"]?.toIntOrNull() ?: sampleCount
        val looping = comments["LOOP"]?.equals("true", ignoreCase = true) ?: (loopStart > 0)

        val sound = VorbisSound(
            sampleRate = sampleRate,
            sampleCount = sampleCount,
            loopStart = loopStart,
            loopEnd = loopEnd,
            looping = looping,
            packets = packets.drop(3)
        )
        return setup to sound
    }

    private fun hasMagic(packet: ByteArray): Boolean =
        packet.size > MAGIC.size && MAGIC.indices.all { packet[it + 1] == MAGIC[it] }

    private fun identificationHeader(setup: VorbisSetup, sampleRate: Int): ByteArray {
        val buffer = Unpooled.buffer()
        buffer.writeByte(0x01)
        buffer.writeBytes(MAGIC)
        buffer.writeIntLE(0)
        buffer.writeByte(CHANNELS)
        buffer.writeIntLE(sampleRate)
        buffer.writeIntLE(0)
        buffer.writeIntLE(0)
        buffer.writeIntLE(0)
        buffer.writeByte((setup.blocksize0 and 0x0F) or ((setup.blocksize1 and 0x0F) shl 4))
        buffer.writeByte(0x01)
        return ByteBufUtil.getBytes(buffer)
    }

    private fun commentHeader(sound: VorbisSound): ByteArray {
        val buffer = Unpooled.buffer()
        buffer.writeByte(0x03)
        buffer.writeBytes(MAGIC)

        val vendor = "OpenRune-FileStore".toByteArray(Charsets.UTF_8)
        buffer.writeIntLE(vendor.size)
        buffer.writeBytes(vendor)

        val comments = mutableListOf<String>()
        if (sound.loopStart != 0 || sound.loopEnd != 0) {
            comments += "LOOPSTART=${sound.loopStart}"
            comments += "LOOPEND=${sound.loopEnd}"
        }
        if (sound.looping) comments += "LOOP=true"

        buffer.writeIntLE(comments.size)
        for (comment in comments) {
            val bytes = comment.toByteArray(Charsets.UTF_8)
            buffer.writeIntLE(bytes.size)
            buffer.writeBytes(bytes)
        }

        buffer.writeByte(0x01)
        return ByteBufUtil.getBytes(buffer)
    }

    private fun readComments(packet: ByteArray): Map<String, String> {
        if (packet.size < 11 || packet[0].toInt() != 0x03) return emptyMap()

        val buffer = Unpooled.wrappedBuffer(packet)
        buffer.readerIndex(7)
        buffer.skipBytes(buffer.readIntLE())

        if (buffer.readableBytes() < 4) return emptyMap()
        val count = buffer.readIntLE()

        val comments = mutableMapOf<String, String>()
        repeat(count) {
            if (buffer.readableBytes() < 4) return comments
            val length = buffer.readIntLE()
            if (length < 0 || length > buffer.readableBytes()) return comments

            val comment = buffer.readCharSequence(length, Charsets.UTF_8).toString()
            val separator = comment.indexOf('=')
            if (separator > 0) {
                comments[comment.substring(0, separator).uppercase()] = comment.substring(separator + 1)
            }
        }
        return comments
    }

    private class OggStream(val packets: List<ByteArray>, val finalGranulePosition: Long)

    /** Walks the ogg pages, rejoining packets that were split across page boundaries by the lacing table. */
    private fun readPackets(buffer: ByteBuf): OggStream {
        val packets = mutableListOf<ByteArray>()
        val current = Unpooled.buffer()
        var finalGranulePosition = 0L

        while (buffer.readableBytes() >= 27) {
            val capture = ByteArray(4).also { buffer.readBytes(it) }
            require(capture.contentEquals(OGGS)) { "missing ogg page header at ${buffer.readerIndex() - 4}" }

            buffer.skipBytes(2)
            val granulePosition = buffer.readLongLE()
            buffer.skipBytes(12)
            val segmentCount = buffer.readUnsignedByte().toInt()

            require(buffer.readableBytes() >= segmentCount) { "truncated ogg page" }
            val lacing = ByteArray(segmentCount).also { buffer.readBytes(it) }

            if (granulePosition >= 0) finalGranulePosition = granulePosition

            for (segment in lacing) {
                val length = segment.toInt() and 0xFF
                require(buffer.readableBytes() >= length) { "truncated ogg packet" }

                current.writeBytes(buffer, length)
                if (length < 255) {
                    packets += ByteBufUtil.getBytes(current)
                    current.clear()
                }
            }
        }

        if (current.readableBytes() > 0) packets += ByteBufUtil.getBytes(current)
        return OggStream(packets, finalGranulePosition)
    }

    /** Running sample count after each packet; a vorbis decoder emits (previous + current) / 4 samples per packet. */
    private fun granulePositions(setup: VorbisSetup, sound: VorbisSound): LongArray {
        val modeBits = ilog(setup.modeBlockFlags.size - 1)
        val granules = LongArray(sound.packets.size)

        var granule = 0L
        var previousBlockSize = -1
        sound.packets.forEachIndexed { index, packet ->
            val bits = BitReader(packet, 0)
            bits.read(1)
            val blockSize = setup.blockSize(if (modeBits == 0) 0 else bits.read(modeBits))

            if (previousBlockSize >= 0) granule += (previousBlockSize + blockSize) / 4
            previousBlockSize = blockSize
            granules[index] = granule
        }

        val last = granules.lastIndex
        if (last >= 0 && sound.sampleCount in 0..granules[last]) granules[last] = sound.sampleCount.toLong()
        return granules
    }

    private class PageWriter(private val out: ByteBuf, private val serial: Int) {
        private var sequence = 0

        fun write(packets: List<ByteArray>, granulePosition: Long, flags: Int) {
            val lacing = Unpooled.buffer()
            val payload = Unpooled.buffer()

            for (packet in packets) {
                var remaining = packet.size
                while (remaining >= 255) {
                    lacing.writeByte(255)
                    remaining -= 255
                }
                lacing.writeByte(remaining)
                payload.writeBytes(packet)
            }

            val page = Unpooled.buffer()
            page.writeBytes(OGGS)
            page.writeByte(0)
            page.writeByte(flags)
            page.writeLongLE(granulePosition)
            page.writeIntLE(serial)
            page.writeIntLE(sequence)
            page.writeIntLE(0)
            page.writeByte(lacing.readableBytes())
            page.writeBytes(lacing)
            page.writeBytes(payload)

            val bytes = ByteBufUtil.getBytes(page)
            val checksum = OggCrc.compute(bytes)
            for (i in 0 until 4) {
                bytes[22 + i] = ((checksum ushr (i * 8)) and 0xFF).toByte()
            }

            out.writeBytes(bytes)
            sequence++
        }
    }
}

/** Ogg's own CRC: an unreflected CRC-32 with no final inversion. */
private object OggCrc {
    private val table = IntArray(256) { index ->
        var r = index shl 24
        repeat(8) {
            r = if ((r and Int.MIN_VALUE) != 0) (r shl 1) xor 0x04c11db7 else r shl 1
        }
        r
    }

    fun compute(data: ByteArray): Int {
        var crc = 0
        for (byte in data) {
            crc = (crc shl 8) xor table[((crc ushr 24) xor (byte.toInt() and 0xFF)) and 0xFF]
        }
        return crc
    }
}
