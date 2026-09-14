package dev.openrune.cache.tools.sound

import dev.openrune.cache.SOUNDEFFECTS
import dev.openrune.cache.VORBIS
import dev.openrune.cache.util.progress
import dev.openrune.definition.codec.sound.OggVorbis
import dev.openrune.definition.codec.sound.VorbisCodec
import dev.openrune.definition.codec.sound.VorbisSetup
import dev.openrune.definition.codec.sound.VorbisSound
import dev.openrune.filesystem.Cache
import java.io.File

enum class SoundFormat {
    OGG,

    VORBIS
}

data class SoundDumpResult(
    val written: Int,
    val skipped: Int,
    val failed: List<Int>
)

/**
 * Dumps vorbis audio out of the cache. The client stores it in two unrelated places, both vorbis, used for
 * different jobs — pick the one that matches what you're after:
 *
 * - [dumpSoundEffects] — index 4 (`SOUNDEFFECTS`), file 1. Standalone named clips (item pickup, interface
 *   clicks, money pouch, etc.), each carrying its own codebooks. This is what `SoundEffect.method501` in the
 *   client loads through `VorbisSample`. File 0 of the same index is a *different*, non-vorbis format — a small
 *   procedural synth (`SynthSound`: oscillator/envelope/filter parameters, no audio stream) — and isn't handled
 *   here, since there's no vorbis data to convert.
 * - [dumpInstrumentSamples] — index 14 (`VORBIS`). Instrument voices for the MIDI music synthesizer, all
 *   decoded against one setup header shared out of archive 0 (`VorbisSampleLoader` in the client). Dumping
 *   from here also writes that header out as `header.vorbis`, since it's needed to pack samples back.
 */
class SoundDumper(private val cache: Cache, private val output: File) {
    var ids: Collection<Int>? = null
    var index: Int = VORBIS
    var formats: Set<SoundFormat> = setOf(SoundFormat.OGG)
    var overwrite: Boolean = true
    var showProgress: Boolean = true

    private var onFailure: (Int, Throwable?) -> Unit = { _, _ -> }

    fun onFailure(block: (Int, Throwable?) -> Unit) {
        onFailure = block
    }

    fun dump(): SoundDumpResult {
        output.mkdirs()
        return if (index == VORBIS) dumpShared() else dumpSelfContained()
    }

    private fun dumpShared(): SoundDumpResult {
        val headerData = requireNotNull(cache.data(VORBIS, SHARED_SETUP_ARCHIVE)) {
            "archive $SHARED_SETUP_ARCHIVE of index $VORBIS holds no vorbis setup header"
        }
        val setup = VorbisCodec.decodeSharedSetup(headerData)

        if (SoundFormat.VORBIS in formats) {
            File(output, "header.vorbis").writeBytes(headerData)
        }

        val targets = (ids ?: cache.archives(VORBIS).toList())
            .filter { it != SHARED_SETUP_ARCHIVE }
            .sorted()

        return dumpEach(targets) { id ->
            val data = cache.data(VORBIS, id) ?: return@dumpEach null
            Loaded(data, VorbisCodec.decodeSound(data), setup)
        }
    }

    private fun dumpSelfContained(): SoundDumpResult {
        val targets = (ids ?: cache.archives(index).filter { cache.files(index, it).contains(VORBIS_FILE) })
            .sorted()

        return dumpEach(targets) { id ->
            val data = cache.data(index, id, VORBIS_FILE) ?: return@dumpEach null
            val (setup, sound) = VorbisCodec.decodeSelfContained(data)
            Loaded(data, sound, setup)
        }
    }

    private class Loaded(val data: ByteArray, val sound: VorbisSound, val setup: VorbisSetup)

    private fun dumpEach(targets: Collection<Int>, load: (Int) -> Loaded?): SoundDumpResult {
        var written = 0
        var skipped = 0
        val failed = mutableListOf<Int>()

        val bar = if (showProgress) progress("Dumping Sounds", targets.size) else null
        try {
            for (id in targets) {
                bar?.step()

                val ogg = File(output, "$id.ogg")
                val raw = File(output, "$id.vorbis")
                val present = (SoundFormat.OGG !in formats || ogg.exists()) &&
                    (SoundFormat.VORBIS !in formats || raw.exists())
                if (!overwrite && present) {
                    skipped++
                    continue
                }

                try {
                    val loaded = load(id)
                    if (loaded == null) {
                        skipped++
                        continue
                    }

                    if (SoundFormat.VORBIS in formats) raw.writeBytes(loaded.data)
                    if (SoundFormat.OGG in formats) {
                        ogg.writeBytes(OggVorbis.write(loaded.setup, loaded.sound, serial = id))
                    }
                    written++
                } catch (e: Exception) {
                    failed += id
                    onFailure(id, e)
                }
            }
        } finally {
            bar?.close()
        }

        return SoundDumpResult(written, skipped, failed)
    }

    companion object {
        const val SHARED_SETUP_ARCHIVE = 0
        const val VORBIS_FILE = 1
    }
}

/** Dumps the MIDI synth's shared-header instrument samples — index 14 (`VORBIS`). See the class doc above. */
fun dumpInstrumentSamples(
    cache: Cache,
    output: File,
    block: SoundDumper.() -> Unit = {}
): SoundDumpResult {
    val dumper = SoundDumper(cache, output)
    dumper.block()
    return dumper.dump()
}

/** Dumps standalone named sound effect clips — index 4 (`SOUNDEFFECTS`), file 1. See the class doc above. */
fun dumpSoundEffects(
    cache: Cache,
    output: File,
    block: SoundDumper.() -> Unit = {}
): SoundDumpResult = dumpInstrumentSamples(cache, output) {
    index = SOUNDEFFECTS
    block()
}
