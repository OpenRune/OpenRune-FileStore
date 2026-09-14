package dev.openrune.definition.codec.sound

/**
 * The codec setup a sample is decoded with: the two block sizes plus the raw vorbis setup header body
 * (spec 4.2.4, without the `0x05 "vorbis"` magic the ogg encapsulation adds).
 */
class VorbisSetup(
    val blocksize0: Int,
    val blocksize1: Int,
    val body: ByteArray,
    val modeBlockFlags: BooleanArray
) {
    fun blockSize(mode: Int): Int = 1 shl (if (modeBlockFlags[mode]) blocksize1 else blocksize0)

    fun matches(other: VorbisSetup): Boolean =
        blocksize0 == other.blocksize0 && blocksize1 == other.blocksize1 && body.contentEquals(other.body)

    companion object {

        /** Reads a setup from cache bytes: one packed blocksize byte followed by the setup header body. */
        fun read(data: ByteArray, offset: Int = 0): VorbisSetup {
            val blocksizes = data[offset].toInt() and 0xFF
            val scan = scanSetupHeader(data, offset + 1)
            return VorbisSetup(
                blocksize0 = blocksizes and 0x0F,
                blocksize1 = (blocksizes ushr 4) and 0x0F,
                body = data.copyOfRange(offset + 1, scan.end),
                modeBlockFlags = scan.modeBlockFlags
            )
        }

        /** Wraps a setup header body lifted out of an ogg stream, whose block sizes are carried separately. */
        fun of(blocksize0: Int, blocksize1: Int, body: ByteArray): VorbisSetup =
            VorbisSetup(blocksize0, blocksize1, body, scanSetupHeader(body, 0).modeBlockFlags)
    }
}

class VorbisSound(
    val sampleRate: Int,
    val sampleCount: Int,
    val loopStart: Int,
    val loopEnd: Int,
    val looping: Boolean,
    val packets: List<ByteArray>
)

internal class SetupScan(val end: Int, val modeBlockFlags: BooleanArray)

/**
 * Walks a vorbis setup header (spec 4.2.4) far enough to find where it ends and which modes use the long
 * block. The header carries no length, so its end is only knowable by decoding its bitstream.
 */
internal fun scanSetupHeader(data: ByteArray, start: Int): SetupScan {
    val bits = BitReader(data, start)

    val codebookCount = bits.read(8) + 1
    repeat(codebookCount) { readCodebook(bits) }

    val timeCount = bits.read(6) + 1
    repeat(timeCount) { bits.read(16) }

    val floorCount = bits.read(6) + 1
    repeat(floorCount) { readFloor(bits) }

    val residueCount = bits.read(6) + 1
    repeat(residueCount) { readResidue(bits) }

    val mappingCount = bits.read(6) + 1
    repeat(mappingCount) { readMapping(bits) }

    val modeCount = bits.read(6) + 1
    val modeBlockFlags = BooleanArray(modeCount) { readMode(bits) }

    bits.read(1)

    return SetupScan(bits.byteAlignedPosition(), modeBlockFlags)
}

private fun readCodebook(bits: BitReader) {
    bits.read(24)
    val dimensions = bits.read(16)
    val entries = bits.read(24)

    if (bits.read(1) == 0) {
        val sparse = bits.read(1) == 1
        repeat(entries) {
            if (!sparse || bits.read(1) == 1) bits.read(5)
        }
    } else {
        var entry = 0
        bits.read(5)
        while (entry < entries) {
            val width = ilog(entries - entry)
            entry += if (width == 0) 0 else bits.read(width)
        }
    }

    when (val lookupType = bits.read(4)) {
        0 -> Unit
        1, 2 -> {
            bits.read(32)
            bits.read(32)
            val valueBits = bits.read(4) + 1
            bits.read(1)
            val lookupValues = if (lookupType == 1) quantVals(entries, dimensions) else entries * dimensions
            repeat(lookupValues) { bits.read(valueBits) }
        }
        else -> throw IllegalStateException("Unsupported codebook lookup type $lookupType")
    }
}

/** Largest value whose [dimensions]th power still fits within [entries], per the lookup type 1 packing. */
private fun quantVals(entries: Int, dimensions: Int): Int {
    if (dimensions == 0) return 0
    var vals = Math.floor(Math.pow(entries.toDouble(), 1.0 / dimensions)).toInt().coerceAtLeast(1)
    while (true) {
        val acc = ipow(vals, dimensions)
        val next = ipow(vals + 1, dimensions)
        when {
            acc <= entries && next > entries -> return vals
            next <= entries -> vals++
            else -> vals--
        }
        if (vals < 1) return 1
    }
}

private fun ipow(base: Int, exponent: Int): Long {
    var result = 1L
    repeat(exponent) { result *= base }
    return result
}

private fun readFloor(bits: BitReader) {
    when (val type = bits.read(16)) {
        0 -> {
            bits.read(8)
            bits.read(16)
            bits.read(16)
            bits.read(6)
            bits.read(8)
            repeat(bits.read(4) + 1) { bits.read(8) }
        }
        1 -> {
            val partitions = bits.read(5)
            val classList = IntArray(partitions) { bits.read(4) }
            val maximumClass = classList.maxOrNull() ?: -1

            val classDimensions = IntArray(maximumClass + 1)
            for (i in 0..maximumClass) {
                classDimensions[i] = bits.read(3) + 1
                val subclasses = bits.read(2)
                if (subclasses != 0) bits.read(8)
                repeat(1 shl subclasses) { bits.read(8) }
            }

            bits.read(2)
            val rangeBits = bits.read(4)
            for (partition in classList) {
                repeat(classDimensions[partition]) { bits.read(rangeBits) }
            }
        }
        else -> throw IllegalStateException("Unsupported floor type $type")
    }
}

private fun readResidue(bits: BitReader) {
    val type = bits.read(16)
    if (type > 2) throw IllegalStateException("Unsupported residue type $type")

    bits.read(24)
    bits.read(24)
    bits.read(24)
    val classifications = bits.read(6) + 1
    bits.read(8)

    val cascade = IntArray(classifications) {
        val low = bits.read(3)
        val high = if (bits.read(1) == 1) bits.read(5) else 0
        high * 8 + low
    }

    for (classification in cascade) {
        for (bit in 0 until 8) {
            if ((classification and (1 shl bit)) != 0) bits.read(8)
        }
    }
}

private fun readMapping(bits: BitReader) {
    val type = bits.read(16)
    if (type != 0) throw IllegalStateException("Unsupported mapping type $type")

    val submaps = if (bits.read(1) == 1) bits.read(4) + 1 else 1

    if (bits.read(1) == 1) {
        val couplingSteps = bits.read(8) + 1
        val channelBits = ilog(CHANNELS - 1)
        repeat(couplingSteps) {
            if (channelBits > 0) {
                bits.read(channelBits)
                bits.read(channelBits)
            }
        }
    }

    bits.read(2)
    if (submaps > 1) repeat(CHANNELS) { bits.read(4) }

    repeat(submaps) {
        bits.read(8)
        bits.read(8)
        bits.read(8)
    }
}

private fun readMode(bits: BitReader): Boolean {
    val blockFlag = bits.read(1) == 1
    bits.read(16)
    bits.read(16)
    bits.read(8)
    return blockFlag
}

/** The client decodes these samples to a mono buffer, so every stream is single channel. */
internal const val CHANNELS = 1

internal fun ilog(value: Int): Int {
    var remaining = value
    var bits = 0
    while (remaining > 0) {
        bits++
        remaining = remaining ushr 1
    }
    return bits
}

/** Reads vorbis bitstreams, which pack fields low bit first within each byte. */
internal class BitReader(private val data: ByteArray, startByte: Int) {
    private var bitIndex: Long = startByte.toLong() * 8

    fun read(n: Int): Int {
        var value = 0
        for (i in 0 until n) {
            val bit = (data[(bitIndex ushr 3).toInt()].toInt() ushr (bitIndex and 7).toInt()) and 1
            value = value or (bit shl i)
            bitIndex++
        }
        return value
    }

    fun byteAlignedPosition(): Int = ((bitIndex + 7) / 8).toInt()
}
