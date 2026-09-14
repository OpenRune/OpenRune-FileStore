package dev.openrune.definition.codec.midi

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled

/**
 * Decodes the cache's compacted MIDI format (index 6 `MUSIC_TRACKS` and index 11 `MUSIC_JINGLES`, both read
 * through the client's `MusicTrack`) into a standard, playable Standard MIDI File.
 *
 * The cache never stores an interleaved MIDI byte stream. It splits a track into ~20 parallel single-purpose
 * streams — event types, delta-times, note numbers, per-controller-type CC values, and so on — each delta or
 * XOR encoded, so similar values sit next to each other for the compressor. `MusicTrack` walks these streams
 * back into interleaved MIDI events at load time; this is the same walk; it just writes real MThd/MTrk bytes
 * instead of feeding a synthesizer.
 *
 * Trailer (last 3 bytes): track count (u8), division (u16).
 * Body, in order: event-type stream (1 byte/event, low nibble = category, high nibble = channel XOR delta,
 * `EOT`/`TEMPO` are reserved full values), delta-time stream (MIDI VLQ, one per event), CC controller-number
 * stream (delta mod 128, one per control-change event), then one 1-byte-delta stream per value slot listed in
 * [Streams] below, in that exact order.
 */
object JagexMidiCodec {

    private const val END_OF_TRACK = 7
    private const val SET_TEMPO = 23

    private const val NOTE_ON = 0
    private const val NOTE_OFF = 1
    private const val CONTROL_CHANGE = 2
    private const val PITCH_BEND = 3
    private const val CHANNEL_AFTERTOUCH = 4
    private const val POLY_AFTERTOUCH = 5
    private const val PROGRAM_CHANGE = 6

    fun decode(data: ByteArray): ByteArray {
        val trackCount = data[data.size - 3].toInt() and 0xFF
        val division = ((data[data.size - 2].toInt() and 0xFF) shl 8) or (data[data.size - 1].toInt() and 0xFF)

        val counts = Counts(data, trackCount)
        val streams = Streams(data, counts)

        val out = Unpooled.buffer()
        out.writeBytes(MTHD)
        out.writeInt(6)
        out.writeShort(if (trackCount > 1) 1 else 0)
        out.writeShort(trackCount)
        out.writeShort(division)

        var eventPos = 0
        var deltaPos = counts.deltaStreamStart
        val controllerValue = IntArray(128)
        var noteNumber = 0
        var noteOnVelocity = 0
        var noteOffVelocity = 0
        var pitch = 0
        var channelPressure = 0
        var polyPressure = 0
        var channel = 0
        var controllerNumber = 0

        repeat(trackCount) {
            val track = Unpooled.buffer()
            var runningStatus = -1

            while (true) {
                val (delta, nextDeltaPos) = readVlqValue(data, deltaPos)
                deltaPos = nextDeltaPos
                writeVlq(track, delta)

                val eventByte = data[eventPos++].toInt() and 0xFF

                if (eventByte == END_OF_TRACK) {
                    track.writeByte(0xFF)
                    track.writeByte(0x2F)
                    track.writeByte(0x00)
                    break
                }

                if (eventByte == SET_TEMPO) {
                    track.writeByte(0xFF)
                    track.writeByte(0x51)
                    track.writeByte(0x03)
                    track.writeByte(data[streams.tempo++].toInt())
                    track.writeByte(data[streams.tempo++].toInt())
                    track.writeByte(data[streams.tempo++].toInt())
                    continue
                }

                channel = channel xor (eventByte ushr 4)
                val category = eventByte and 0x0F
                val status = statusByte(category, channel)
                if (status != runningStatus) {
                    track.writeByte(status)
                    runningStatus = status
                }

                when (category) {
                    NOTE_ON -> {
                        noteNumber += data[streams.noteNumber++].toInt()
                        noteOnVelocity += data[streams.noteOnVelocity++].toInt()
                        track.writeByte(noteNumber and 127)
                        track.writeByte(noteOnVelocity and 127)
                    }
                    NOTE_OFF -> {
                        noteNumber += data[streams.noteNumber++].toInt()
                        noteOffVelocity += data[streams.noteOffVelocity++].toInt()
                        track.writeByte(noteNumber and 127)
                        track.writeByte(noteOffVelocity and 127)
                    }
                    CONTROL_CHANGE -> {
                        controllerNumber = (controllerNumber + (data[streams.controllerNumber++].toInt() and 0xFF)) and 127
                        track.writeByte(controllerNumber)

                        val slot = streams.slotFor(controllerNumber)
                        val delta2 = data[slot.index].toInt()
                        slot.advance()
                        val value = (controllerValue[controllerNumber] + delta2) and 127
                        controllerValue[controllerNumber] = value
                        track.writeByte(value)
                    }
                    PITCH_BEND -> {
                        pitch += data[streams.pitchLsb++].toInt()
                        pitch += (data[streams.pitchMsb++].toInt()) shl 7
                        track.writeByte(pitch and 127)
                        track.writeByte((pitch ushr 7) and 127)
                    }
                    CHANNEL_AFTERTOUCH -> {
                        channelPressure += data[streams.channelPressure++].toInt()
                        track.writeByte(channelPressure and 127)
                    }
                    POLY_AFTERTOUCH -> {
                        noteNumber += data[streams.noteNumber++].toInt()
                        polyPressure += data[streams.polyPressure++].toInt()
                        track.writeByte(noteNumber and 127)
                        track.writeByte(polyPressure and 127)
                    }
                    PROGRAM_CHANGE -> {
                        track.writeByte(data[streams.programOrBankSelect++].toInt())
                    }
                    else -> error("Unsupported event category $category")
                }
            }

            out.writeBytes(MTRK)
            out.writeInt(track.readableBytes())
            out.writeBytes(track)
        }

        return ByteBufUtil.getBytes(out)
    }

    /**
     * Encodes a Standard MIDI File into the cache's compacted layout — the exact inverse of [decode]: every
     * accumulator here mirrors the one in [decode] so the client's `MusicTrack` reads back the same events.
     *
     * Event types this format has no room for — sysex, and meta events other than tempo/end-of-track — are
     * dropped; their delta-time is folded into the following kept event so overall timing is unaffected.
     */
    fun encode(midi: ByteArray): ByteArray {
        require(matches(midi, 0, MTHD)) { "not a standard midi file: missing MThd header" }
        val headerLength = readInt(midi, 4)
        require(headerLength == 6) { "unexpected MThd length $headerLength" }

        val trackCount = readShort(midi, 10)
        val division = readShort(midi, 12)

        var pos = 14
        val tracks = ArrayList<List<ParsedEvent>>(trackCount)
        repeat(trackCount) {
            require(matches(midi, pos, MTRK)) { "missing MTrk header for track ${tracks.size}" }
            val length = readInt(midi, pos + 4)
            tracks += parseTrack(midi, pos + 8, pos + 8 + length)
            pos += 8 + length
        }

        val streams = EncodeStreams()
        var channel = 0
        var noteNumber = 0
        var noteOnVelocity = 0
        var noteOffVelocity = 0
        var channelPressure = 0
        var polyPressure = 0
        var pitch = 0
        var controllerNumber = 0
        val controllerValue = IntArray(128)

        for (track in tracks) {
            for (event in track) {
                when (event) {
                    is ParsedEvent.Tempo -> {
                        writeVlq(streams.deltaTime, event.delta)
                        streams.eventType.writeByte(SET_TEMPO)
                        streams.tempo.writeByte((event.microsecondsPerQuarter ushr 16) and 0xFF)
                        streams.tempo.writeByte((event.microsecondsPerQuarter ushr 8) and 0xFF)
                        streams.tempo.writeByte(event.microsecondsPerQuarter and 0xFF)
                    }

                    is ParsedEvent.EndOfTrack -> {
                        writeVlq(streams.deltaTime, event.delta)
                        streams.eventType.writeByte(END_OF_TRACK)
                    }

                    is ParsedEvent.Voice -> {
                        writeVlq(streams.deltaTime, event.delta)
                        val channelDelta = channel xor event.channel
                        channel = event.channel
                        streams.eventType.writeByte((event.category and 0x0F) or (channelDelta shl 4))

                        when (event.category) {
                            NOTE_ON -> {
                                streams.noteNumber.writeByte(event.data1 - noteNumber); noteNumber = event.data1
                                streams.noteOnVelocity.writeByte(event.data2 - noteOnVelocity); noteOnVelocity = event.data2
                            }
                            NOTE_OFF -> {
                                streams.noteNumber.writeByte(event.data1 - noteNumber); noteNumber = event.data1
                                streams.noteOffVelocity.writeByte(event.data2 - noteOffVelocity); noteOffVelocity = event.data2
                            }
                            POLY_AFTERTOUCH -> {
                                streams.noteNumber.writeByte(event.data1 - noteNumber); noteNumber = event.data1
                                streams.polyPressure.writeByte(event.data2 - polyPressure); polyPressure = event.data2
                            }
                            CHANNEL_AFTERTOUCH -> {
                                streams.channelPressure.writeByte(event.data1 - channelPressure); channelPressure = event.data1
                            }
                            PROGRAM_CHANGE -> {
                                streams.programOrBankSelect.writeByte(event.data1)
                            }
                            CONTROL_CHANGE -> {
                                val number = event.data1
                                streams.ccNumber.writeByte(number - controllerNumber)
                                controllerNumber = number

                                streams.valueStreamFor(number).writeByte(event.data2 - controllerValue[number])
                                controllerValue[number] = event.data2
                            }
                            PITCH_BEND -> {
                                val target = (event.data2 shl 7) or event.data1
                                val total = target - Math.floorMod(pitch, 16384)
                                var msbDelta = total / 128
                                var lsbDelta = total - msbDelta * 128
                                if (lsbDelta > 127) { lsbDelta -= 128; msbDelta++ }
                                if (lsbDelta < -128) { lsbDelta += 128; msbDelta-- }
                                streams.pitchLsb.writeByte(lsbDelta)
                                streams.pitchMsb.writeByte(msbDelta)
                                pitch += lsbDelta + (msbDelta shl 7)
                            }
                            else -> error("Unsupported event category ${event.category}")
                        }
                    }
                }
            }
        }

        val out = Unpooled.buffer()
        streams.writeTo(out)
        out.writeByte(trackCount)
        out.writeShort(division)
        return ByteBufUtil.getBytes(out)
    }

    private sealed class ParsedEvent {
        abstract val delta: Int
        class Voice(override val delta: Int, val channel: Int, val category: Int, val data1: Int, val data2: Int) : ParsedEvent()
        class Tempo(override val delta: Int, val microsecondsPerQuarter: Int) : ParsedEvent()
        class EndOfTrack(override val delta: Int) : ParsedEvent()
    }

    /** Parses one MTrk's event data, applying MIDI running status. Unsupported events fold their delta forward. */
    private fun parseTrack(data: ByteArray, start: Int, end: Int): List<ParsedEvent> {
        var pos = start
        var runningStatus = 0
        var pendingDelta = 0
        val events = mutableListOf<ParsedEvent>()

        while (pos < end) {
            val (delta, afterDelta) = readVlqValue(data, pos)
            pos = afterDelta
            pendingDelta += delta

            var status = data[pos].toInt() and 0xFF
            if (status >= 0x80) {
                pos++
                runningStatus = status
            } else {
                status = runningStatus
            }

            when {
                status == 0xFF -> {
                    val type = data[pos++].toInt() and 0xFF
                    val (length, afterLength) = readVlqValue(data, pos)
                    pos = afterLength
                    when (type) {
                        0x51 -> {
                            val microseconds = ((data[pos].toInt() and 0xFF) shl 16) or
                                ((data[pos + 1].toInt() and 0xFF) shl 8) or (data[pos + 2].toInt() and 0xFF)
                            events += ParsedEvent.Tempo(pendingDelta, microseconds)
                            pendingDelta = 0
                        }
                        0x2F -> {
                            events += ParsedEvent.EndOfTrack(pendingDelta)
                            return events
                        }
                    }
                    pos += length
                }
                status == 0xF0 || status == 0xF7 -> {
                    val (length, afterLength) = readVlqValue(data, pos)
                    pos = afterLength + length
                }
                status in 0x80..0xEF -> {
                    val category = when (status and 0xF0) {
                        0x80 -> NOTE_OFF
                        0x90 -> NOTE_ON
                        0xA0 -> POLY_AFTERTOUCH
                        0xB0 -> CONTROL_CHANGE
                        0xC0 -> PROGRAM_CHANGE
                        0xD0 -> CHANNEL_AFTERTOUCH
                        0xE0 -> PITCH_BEND
                        else -> error("unreachable")
                    }
                    val channel = status and 0x0F
                    val data1 = data[pos++].toInt() and 0xFF
                    val data2 = if (category == PROGRAM_CHANGE || category == CHANNEL_AFTERTOUCH) {
                        -1
                    } else {
                        data[pos++].toInt() and 0xFF
                    }
                    events += ParsedEvent.Voice(pendingDelta, channel, category, data1, data2)
                    pendingDelta = 0
                }
            }
        }

        events += ParsedEvent.EndOfTrack(pendingDelta)
        return events
    }

    /** The write-side counterpart of [Streams]: one growable buffer per slot, concatenated in the same order. */
    private class EncodeStreams {
        val eventType: ByteBuf = Unpooled.buffer()
        val deltaTime: ByteBuf = Unpooled.buffer()
        val ccNumber: ByteBuf = Unpooled.buffer()
        val misc: ByteBuf = Unpooled.buffer()
        val polyPressure: ByteBuf = Unpooled.buffer()
        val channelPressure: ByteBuf = Unpooled.buffer()
        val pitchMsb: ByteBuf = Unpooled.buffer()
        val modMsb: ByteBuf = Unpooled.buffer()
        val volumeMsb: ByteBuf = Unpooled.buffer()
        val panMsb: ByteBuf = Unpooled.buffer()
        val noteNumber: ByteBuf = Unpooled.buffer()
        val noteOnVelocity: ByteBuf = Unpooled.buffer()
        val other: ByteBuf = Unpooled.buffer()
        val noteOffVelocity: ByteBuf = Unpooled.buffer()
        val modLsb: ByteBuf = Unpooled.buffer()
        val volumeLsb: ByteBuf = Unpooled.buffer()
        val panLsb: ByteBuf = Unpooled.buffer()
        val programOrBankSelect: ByteBuf = Unpooled.buffer()
        val pitchLsb: ByteBuf = Unpooled.buffer()
        val nrpnMsb: ByteBuf = Unpooled.buffer()
        val nrpnLsb: ByteBuf = Unpooled.buffer()
        val rpnMsb: ByteBuf = Unpooled.buffer()
        val rpnLsb: ByteBuf = Unpooled.buffer()
        val tempo: ByteBuf = Unpooled.buffer()

        fun valueStreamFor(controllerNumber: Int): ByteBuf = when (controllerNumber) {
            1 -> modMsb
            33 -> modLsb
            7 -> volumeMsb
            39 -> volumeLsb
            10 -> panMsb
            42 -> panLsb
            99 -> nrpnMsb
            98 -> nrpnLsb
            101 -> rpnMsb
            100 -> rpnLsb
            64, 65, 120, 121, 123 -> misc
            0, 32 -> programOrBankSelect
            else -> other
        }

        fun writeTo(out: ByteBuf) {
            for (stream in listOf(
                eventType, deltaTime, ccNumber, misc, polyPressure, channelPressure, pitchMsb, modMsb, volumeMsb,
                panMsb, noteNumber, noteOnVelocity, other, noteOffVelocity, modLsb, volumeLsb, panLsb,
                programOrBankSelect, pitchLsb, nrpnMsb, nrpnLsb, rpnMsb, rpnLsb, tempo
            )) {
                out.writeBytes(stream)
            }
        }
    }

    private fun statusByte(category: Int, channel: Int): Int = channel or when (category) {
        NOTE_OFF -> 0x80
        NOTE_ON -> 0x90
        POLY_AFTERTOUCH -> 0xA0
        CONTROL_CHANGE -> 0xB0
        PROGRAM_CHANGE -> 0xC0
        CHANNEL_AFTERTOUCH -> 0xD0
        PITCH_BEND -> 0xE0
        else -> error("Unsupported event category $category")
    }

    /** Tallies how many events fall into each category, needed to size and place every sub-stream. */
    private class Counts(data: ByteArray, private val trackCount: Int) {
        var noteOn = 0
        var noteOff = 0
        var controlChange = 0
        var pitchBend = 0
        var channelAftertouch = 0
        var polyAftertouch = 0
        var programChange = 0
        var tempo = 0
        val deltaStreamStart: Int

        init {
            var pos = 0
            repeat(trackCount) {
                while (true) {
                    val eventByte = data[pos++].toInt() and 0xFF
                    if (eventByte == END_OF_TRACK) break
                    if (eventByte == SET_TEMPO) {
                        tempo++
                        continue
                    }
                    when (eventByte and 0x0F) {
                        NOTE_OFF -> noteOff++
                        NOTE_ON -> noteOn++
                        CONTROL_CHANGE -> controlChange++
                        PITCH_BEND -> pitchBend++
                        CHANNEL_AFTERTOUCH -> channelAftertouch++
                        POLY_AFTERTOUCH -> polyAftertouch++
                        PROGRAM_CHANGE -> programChange++
                        else -> error("Unsupported event category ${eventByte and 0x0F}")
                    }
                }
            }
            deltaStreamStart = pos
        }

        // Every End-of-Track marker (one per track) also carries its own delta-time entry in stream B.
        val totalEvents get() = trackCount + noteOn + noteOff + controlChange + pitchBend + channelAftertouch + polyAftertouch + programChange + tempo
    }

    /** Computes where each of the ~20 delta streams begins, in the fixed order the cache lays them out. */
    private class Streams(data: ByteArray, counts: Counts) {
        var noteNumber: Int
        var noteOnVelocity: Int
        var noteOffVelocity: Int
        var pitchMsb: Int
        var pitchLsb: Int
        var channelPressure: Int
        var polyPressure: Int
        var programOrBankSelect: Int
        var tempo: Int

        private var modMsb: Int
        private var modLsb: Int
        private var volumeMsb: Int
        private var volumeLsb: Int
        private var panMsb: Int
        private var panLsb: Int
        private var nrpnMsb: Int
        private var nrpnLsb: Int
        private var rpnMsb: Int
        private var rpnLsb: Int
        private var misc: Int
        private var other: Int
        var controllerNumber: Int

        init {
            var pos = deltaSkip(data, counts.deltaStreamStart, counts.totalEvents)

            controllerNumber = pos
            var bankSelect = 0
            var countModMsb = 0
            var countModLsb = 0
            var countVolMsb = 0
            var countVolLsb = 0
            var countPanMsb = 0
            var countPanLsb = 0
            var countNrpnMsb = 0
            var countNrpnLsb = 0
            var countRpnMsb = 0
            var countRpnLsb = 0
            var countMisc = 0
            var countOther = 0

            var acc = 0
            repeat(counts.controlChange) { i ->
                acc = (acc + (data[pos + i].toInt() and 0xFF)) and 127
                when (acc) {
                    0, 32 -> bankSelect++
                    1 -> countModMsb++
                    33 -> countModLsb++
                    7 -> countVolMsb++
                    39 -> countVolLsb++
                    10 -> countPanMsb++
                    42 -> countPanLsb++
                    99 -> countNrpnMsb++
                    98 -> countNrpnLsb++
                    101 -> countRpnMsb++
                    100 -> countRpnLsb++
                    64, 65, 120, 121, 123 -> countMisc++
                    else -> countOther++
                }
            }
            pos += counts.controlChange

            misc = pos; pos += countMisc
            polyPressure = pos; pos += counts.polyAftertouch
            channelPressure = pos; pos += counts.channelAftertouch
            pitchMsb = pos; pos += counts.pitchBend
            modMsb = pos; pos += countModMsb
            volumeMsb = pos; pos += countVolMsb
            panMsb = pos; pos += countPanMsb
            noteNumber = pos; pos += counts.noteOn + counts.noteOff + counts.polyAftertouch
            noteOnVelocity = pos; pos += counts.noteOn
            other = pos; pos += countOther
            noteOffVelocity = pos; pos += counts.noteOff
            modLsb = pos; pos += countModLsb
            volumeLsb = pos; pos += countVolLsb
            panLsb = pos; pos += countPanLsb
            programOrBankSelect = pos; pos += counts.programChange + bankSelect
            pitchLsb = pos; pos += counts.pitchBend
            nrpnMsb = pos; pos += countNrpnMsb
            nrpnLsb = pos; pos += countNrpnLsb
            rpnMsb = pos; pos += countRpnMsb
            rpnLsb = pos; pos += countRpnLsb
            tempo = pos
        }

        fun slotFor(controllerNumber: Int): Slot = when (controllerNumber) {
            1 -> Slot(::modMsb)
            33 -> Slot(::modLsb)
            7 -> Slot(::volumeMsb)
            39 -> Slot(::volumeLsb)
            10 -> Slot(::panMsb)
            42 -> Slot(::panLsb)
            99 -> Slot(::nrpnMsb)
            98 -> Slot(::nrpnLsb)
            101 -> Slot(::rpnMsb)
            100 -> Slot(::rpnLsb)
            64, 65, 120, 121, 123 -> Slot(::misc)
            0, 32 -> Slot(::programOrBankSelect)
            else -> Slot(::other)
        }

        inner class Slot(private val property: kotlin.reflect.KMutableProperty0<Int>) {
            val index: Int get() = property.get()
            fun advance() = property.set(property.get() + 1)
        }
    }

    private fun deltaSkip(data: ByteArray, start: Int, count: Int): Int {
        var pos = start
        repeat(count) { pos = skipVlq(data, pos) }
        return pos
    }

    private fun skipVlq(data: ByteArray, start: Int): Int {
        var pos = start
        while (data[pos].toInt() < 0) pos++
        return pos + 1
    }

    private fun readVlqValue(data: ByteArray, start: Int): Pair<Int, Int> {
        var pos = start
        var value = 0
        var byte = data[pos++].toInt()
        while (byte < 0) {
            value = (value or (byte and 127)) shl 7
            byte = data[pos++].toInt()
        }
        return (value or byte) to pos
    }

    private fun writeVlq(out: ByteBuf, value: Int) {
        var shift = 21
        var started = false
        while (shift >= 0) {
            val part = (value ushr shift) and 127
            if (part != 0 || started || shift == 0) {
                out.writeByte(if (shift == 0) part else part or 128)
                started = true
            }
            shift -= 7
        }
    }

    private fun matches(data: ByteArray, offset: Int, magic: ByteArray): Boolean =
        offset + magic.size <= data.size && magic.indices.all { data[offset + it] == magic[it] }

    private fun readInt(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 24) or ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or (data[offset + 3].toInt() and 0xFF)

    private fun readShort(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    private val MTHD = "MThd".toByteArray(Charsets.US_ASCII)
    private val MTRK = "MTrk".toByteArray(Charsets.US_ASCII)
}
