package dev.openrune.cache.tools.midi

import dev.openrune.cache.MUSIC_JINGLES
import dev.openrune.cache.MUSIC_TRACKS
import dev.openrune.cache.util.progress
import dev.openrune.definition.codec.midi.JagexMidiCodec
import dev.openrune.filesystem.Cache
import java.io.File

data class MidiDumpResult(
    val written: Int,
    val skipped: Int,
    val failed: List<Int>
)

/**
 * Dumps [MUSIC_TRACKS] (index 6) or [MUSIC_JINGLES] (index 11) as standard, playable `.mid` files.
 *
 * Both indices are decoded by the client's `MusicTrack` — same format, same [JagexMidiCodec], just two
 * different id ranges (full songs vs the short jingles played on level-ups, quest completion, etc). The cache
 * never stores an interleaved MIDI stream; it splits events into ~20 parallel delta-encoded value streams for
 * better compression, and [JagexMidiCodec] walks them back into a real MThd/MTrk file — the exact reverse of
 * what `MusicTrack.method8495` does at runtime, just written out rather than fed to the synth.
 */
class MidiDumper(private val cache: Cache, private val output: File) {
    var ids: Collection<Int>? = null
    var index: Int = MUSIC_TRACKS
    var overwrite: Boolean = true
    var showProgress: Boolean = true

    private var onFailure: (Int, Throwable?) -> Unit = { _, _ -> }

    fun onFailure(block: (Int, Throwable?) -> Unit) {
        onFailure = block
    }

    fun dump(): MidiDumpResult {
        output.mkdirs()

        val targets = (ids ?: cache.archives(index).toList()).sorted()
        var written = 0
        var skipped = 0
        val failed = mutableListOf<Int>()

        val bar = if (showProgress) progress("Dumping MIDI", targets.size) else null
        try {
            for (id in targets) {
                bar?.step()

                val file = File(output, "$id.mid")
                if (!overwrite && file.exists()) {
                    skipped++
                    continue
                }

                val data = cache.data(index, id)
                if (data == null) {
                    skipped++
                    continue
                }

                try {
                    file.writeBytes(JagexMidiCodec.decode(data))
                    written++
                } catch (e: Exception) {
                    failed += id
                    onFailure(id, e)
                }
            }
        } finally {
            bar?.close()
        }

        return MidiDumpResult(written, skipped, failed)
    }
}

fun dumpTracks(
    cache: Cache,
    output: File,
    block: MidiDumper.() -> Unit = {}
): MidiDumpResult {
    val dumper = MidiDumper(cache, output)
    dumper.block()
    return dumper.dump()
}

fun dumpJingles(
    cache: Cache,
    output: File,
    block: MidiDumper.() -> Unit = {}
): MidiDumpResult = dumpTracks(cache, output) {
    index = MUSIC_JINGLES
    block()
}
