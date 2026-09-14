package dev.openrune.cache.tools.tasks.impl

import dev.openrune.cache.MUSIC_JINGLES
import dev.openrune.cache.MUSIC_TRACKS
import dev.openrune.cache.tools.incremental.PackUnit
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.getFiles
import dev.openrune.definition.codec.midi.JagexMidiCodec
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.filesystem.Cache
import java.io.File

/**
 * Packs `.mid` files into [MUSIC_TRACKS] (index 6, full songs) or [MUSIC_JINGLES] (index 11, short jingles) —
 * see [MidiDumper][dev.openrune.cache.tools.midi.MidiDumper] for the matching dumper and a fuller explanation
 * of the format. Both indices share the same layout and the same [JagexMidiCodec], so any standard MIDI file
 * can be packed into either — pick the index by what the file actually is.
 */
class PackMidi(
    private val midiDirectory: File,
    private val index: Int = MUSIC_TRACKS,
    private val rscmMappingPrefix: String = if (index == MUSIC_JINGLES) "jingle." else "midi."
) : CacheTask() {

    override fun init(cache: Cache) {
        val midiFiles = getFiles(midiDirectory, "mid", "midi")
        if (midiFiles.isEmpty()) return

        val root = midiDirectory.absoluteFile
        val units = midiFiles.map { file ->
            PackUnit(key = file.absoluteFile.relativeTo(root).path.replace('\\', '/'), source = file)
        }

        incremental.run(
            task = this,
            scope = midiDirectory.absolutePath,
            label = if (index == MUSIC_JINGLES) "Packing Jingles" else "Packing Tracks",
            cache = cache,
            units = units,
        ) { packCache, unit ->
            packMidi(packCache, unit.sources.single())
        }
    }

    private fun packMidi(cache: Cache, file: File) {
        val id = idOf(file)
        if (id == null) {
            println("Unable to pack midi ${file.name}")
            return
        }

        val encoded = JagexMidiCodec.encode(file.readBytes())
        cache.write(index, id, 0, encoded)
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
        val NUMERIC_NAME = Regex("-?\\d+")
    }
}

fun packTracks(directory: File, rscmMappingPrefix: String = "midi."): PackMidi =
    PackMidi(directory, index = MUSIC_TRACKS, rscmMappingPrefix = rscmMappingPrefix)

fun packJingles(directory: File, rscmMappingPrefix: String = "jingle."): PackMidi =
    PackMidi(directory, index = MUSIC_JINGLES, rscmMappingPrefix = rscmMappingPrefix)
