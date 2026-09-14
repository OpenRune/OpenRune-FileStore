package dev.openrune.cache.tools.tasks.impl

import dev.openrune.cache.SOUNDEFFECTS
import dev.openrune.cache.VORBIS
import dev.openrune.cache.tools.incremental.Hashing
import dev.openrune.cache.tools.incremental.PackUnit
import dev.openrune.definition.codec.sound.OggVorbis
import dev.openrune.cache.tools.sound.SoundDumper.Companion.SHARED_SETUP_ARCHIVE
import dev.openrune.cache.tools.sound.SoundDumper.Companion.VORBIS_FILE
import dev.openrune.definition.codec.sound.VorbisCodec
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.getFiles
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.filesystem.Cache
import java.io.File

/**
 * Packs `.ogg` and `.vorbis` sounds into the cache. Covers the same two vorbis targets [SoundDumper] dumps
 * from — pick whichever matches what you're packing:
 *
 * - [packSoundEffects] — index 4 (`SOUNDEFFECTS`), file 1. Standalone named clips. Each carries its own
 *   codebooks, so any `.ogg` can be packed as is — this is the default `index`. File 0 of that index is a
 *   different, non-vorbis procedural synth format (`SynthSound` in the client) and isn't produced here.
 * - [packInstrumentSamples] — index 14 (`VORBIS`). MIDI instrument voices, all decoded against one setup
 *   header shared out of archive 0. An `.ogg` only belongs here if it was encoded with those same codebooks —
 *   anything else is rejected by name (pointing at `packSoundEffects` instead) rather than written as noise.
 */
class PackSounds(
    private val soundDirectory: File,
    private val index: Int = SOUNDEFFECTS,
    private val rscmMappingPrefix: String = "sounds."
) : CacheTask() {

    override fun init(cache: Cache) {
        val soundFiles = getFiles(soundDirectory, "ogg", "vorbis")
        if (soundFiles.isEmpty()) return

        val header = File(soundDirectory, "$SHARED_HEADER_NAME.vorbis").takeIf { it.exists() }
        if (index == VORBIS && header != null) packSharedHeader(cache, header)

        val root = soundDirectory.absoluteFile
        val units = soundFiles
            .filter { it.nameWithoutExtension != SHARED_HEADER_NAME }
            .map { file ->
                PackUnit(key = file.absoluteFile.relativeTo(root).path.replace('\\', '/'), source = file)
            }

        incremental.run(
            task = this,
            scope = soundDirectory.absolutePath,
            label = "Packing Sounds",
            cache = cache,
            units = units,
            extraFingerprints = header?.let { mapOf(it.absolutePath to Hashing.hashBytes(it.readBytes())) }.orEmpty(),
        ) { packCache, unit ->
            packSound(packCache, unit.sources.single(), header)
        }
    }

    private fun packSharedHeader(cache: Cache, header: File) {
        incremental.runOnce(
            task = this,
            scope = soundDirectory.absolutePath,
            label = "Packing Sound Header",
            cache = cache,
            fingerprint = Hashing.hashBytes(header.readBytes()),
            sources = listOf(header),
        ) { packCache ->
            packCache.write(VORBIS, SHARED_SETUP_ARCHIVE, 0, header.readBytes())
        }
    }

    private fun packSound(cache: Cache, file: File, header: File?) {
        val id = idOf(file)
        if (id == null) {
            println("Unable to pack sound ${file.name}")
            return
        }

        val data = file.readBytes()
        val isOgg = file.extension.equals("ogg", ignoreCase = true)

        if (index == VORBIS) {
            cache.write(VORBIS, id, 0, if (isOgg) toSharedSound(cache, file, data, header) else sampleBody(file, data))
        } else {
            cache.write(index, id, VORBIS_FILE, if (isOgg) toSelfContained(data) else selfContained(file, data))
        }
    }

    private fun toSelfContained(data: ByteArray): ByteArray {
        val (setup, sound) = OggVorbis.read(data)
        return VorbisCodec.encodeSelfContained(setup, sound)
    }

    private fun toSharedSound(cache: Cache, file: File, data: ByteArray, header: File?): ByteArray {
        val headerData = header?.readBytes()
            ?: cache.data(VORBIS, SHARED_SETUP_ARCHIVE)
            ?: error("index $VORBIS has no setup header in archive $SHARED_SETUP_ARCHIVE")

        val (setup, sound) = OggVorbis.read(data)
        require(setup.matches(VorbisCodec.decodeSharedSetup(headerData))) {
            "${file.name} was encoded with different codebooks than index $VORBIS shares. Pack it into the " +
                "sound effects index instead, or re-encode it against the cache's own setup header."
        }
        return VorbisCodec.encodeSound(sound)
    }

    private fun selfContained(file: File, data: ByteArray): ByteArray = data.also {
        require(VorbisCodec.isSelfContained(it)) {
            "${file.name} holds an index 14 sample body, which has no setup header of its own"
        }
    }

    private fun sampleBody(file: File, data: ByteArray): ByteArray = data.also {
        require(!VorbisCodec.isSelfContained(it)) {
            "${file.name} carries its own setup header, so it belongs in the sound effects index"
        }
    }

    private fun idOf(file: File): Int? {
        val name = file.nameWithoutExtension
        return if (name.matches(NUMERIC_NAME)) {
            name.toInt()
        } else {
            ConstantProvider.getMapping(rscmMappingPrefix + name.lowercase().replace(" ", "_"))
        }
    }

    private companion object {
        const val SHARED_HEADER_NAME = "header"
        val NUMERIC_NAME = Regex("-?\\d+")
    }
}

/** Packs standalone named sound effect clips — index 4 (`SOUNDEFFECTS`), file 1. See the class doc above. */
fun packSoundEffects(directory: File, rscmMappingPrefix: String = "sounds."): PackSounds =
    PackSounds(directory, index = SOUNDEFFECTS, rscmMappingPrefix = rscmMappingPrefix)

/** Packs the MIDI synth's shared-header instrument samples — index 14 (`VORBIS`). See the class doc above. */
fun packInstrumentSamples(directory: File, rscmMappingPrefix: String = "sounds."): PackSounds =
    PackSounds(directory, index = VORBIS, rscmMappingPrefix = rscmMappingPrefix)
