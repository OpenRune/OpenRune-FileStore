package dev.openrune.cache.tools.tasks.impl.defs

import com.displee.cache.CacheLibrary
import com.google.gson.Gson
import dev.openrune.cache.CONFIGS
import dev.openrune.cache.NPC
import dev.openrune.cache.filestore.buffer.BufferWriter
import dev.openrune.cache.filestore.definition.data.NPCDefinition
import dev.openrune.cache.filestore.definition.encoder.NpcEncoder
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import java.io.File

class PackNpcs(private val npcDir : File) : CacheTask() {
    override fun init(library: CacheLibrary) {
        val size = getFiles(npcDir,"json").size
        val progress = progress("Packing Npcs", size)
        val errors : MutableMap<String, String> = emptyMap<String, String>().toMutableMap()
        if (size != 0) {
            getFiles(npcDir,"json").forEach {
                val def: NPCDefinition = Gson().fromJson(it.readText(), NPCDefinition::class.java)
                if (def.id == 0) {
                    errors[it.toString()] = "ID is 0 please set a id for the npc to pack"
                    return@forEach
                }

                val encoder = NpcEncoder()
                val writer = BufferWriter(4096)
                with(encoder) { writer.encode(def) }
                library.index(CONFIGS).archive(NPC)!!.add(def.id, writer.toArray())

                progress.step()
            }

            progress.close()

            errors.forEach {
                println("[ERROR] ${it.key} : ${it.value}")
            }

        }
    }

}