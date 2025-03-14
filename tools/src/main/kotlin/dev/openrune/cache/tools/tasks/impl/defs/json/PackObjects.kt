package dev.openrune.cache.tools.tasks.impl.defs.json

import com.google.gson.Gson
import dev.openrune.OsrsCacheProvider.Companion.CACHE_REVISION
import dev.openrune.cache.CONFIGS
import dev.openrune.cache.OBJECT
import dev.openrune.definition.util.toArray
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import dev.openrune.definition.codec.ObjectCodec
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import java.io.File

@Deprecated(
    message = "Deprecated since 1.2.4 due to conversion to TOML configuration: Use PackConfig with ConfigType.OBJECTS instead",
    replaceWith = ReplaceWith("PackConfig(ConfigType.OBJECTS)"),
    level = DeprecationLevel.WARNING // This will generate a warning during compilation, prompting the developer to migrate
)
class PackObjects(private val objectDir : File) : CacheTask() {
    override fun init(cache: Cache) {
        val size = getFiles(objectDir,"json").size
        val progress = progress("Packing Objects", size)
        val errors : MutableMap<String, String> = emptyMap<String, String>().toMutableMap()
        if (size != 0) {
            getFiles(objectDir,"json").forEach {
                val def: dev.openrune.definition.type.ObjectType = Gson().fromJson(it.readText(), dev.openrune.definition.type.ObjectType::class.java)
                if (def.id == 0) {
                    errors[it.toString()] = "ID is 0 please set a id for the npc to pack"
                    return@forEach
                }

                val encoder = ObjectCodec(CACHE_REVISION)
                val writer = Unpooled.buffer(4096)
                with(encoder) { writer.encode(def) }
                cache.write(CONFIGS, OBJECT, def.id, writer.toArray())

                progress.step()
            }

            progress.close()

            errors.forEach {
                println("[ERROR] ${it.key} : ${it.value}")
            }

        }
    }

}