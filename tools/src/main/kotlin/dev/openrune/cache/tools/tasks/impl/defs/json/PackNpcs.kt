package dev.openrune.cache.tools.tasks.impl.defs.json

import com.google.gson.Gson
import dev.openrune.OsrsCacheProvider.Companion.CACHE_REVISION
import dev.openrune.cache.CONFIGS
import dev.openrune.cache.NPC
import dev.openrune.definition.util.toArray
import dev.openrune.definition.type.NpcType
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import dev.openrune.definition.codec.NPCCodec
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import java.io.File

@Deprecated(
    message = "Deprecated since 1.2.4 due to conversion to TOML configuration: Use PackConfig with ConfigType.NPCS instead",
    replaceWith = ReplaceWith("PackConfig(ConfigType.NPCS)"),
    level = DeprecationLevel.WARNING // This will generate a warning during compilation, prompting the developer to migrate
)
class PackNpcs(private val npcDir : File) : CacheTask() {
    override fun init(cache: Cache) {
        val size = getFiles(npcDir,"json").size
        val progress = progress("Packing Npcs", size)
        val errors : MutableMap<String, String> = emptyMap<String, String>().toMutableMap()
        if (size != 0) {
            getFiles(npcDir,"json").forEach {
                val def: NpcType = Gson().fromJson(it.readText(), NpcType::class.java)
                if (def.id == 0) {
                    errors[it.toString()] = "ID is 0 please set a id for the npc to pack"
                    return@forEach
                }

                val encoder = NPCCodec(CACHE_REVISION)
                val writer = Unpooled.buffer(4096)
                with(encoder) { writer.encode(def) }
                cache.write(CONFIGS, NPC, def.id, writer.toArray())

                progress.step()
            }

            progress.close()

            errors.forEach {
                println("[ERROR] ${it.key} : ${it.value}")
            }

        }
    }

}