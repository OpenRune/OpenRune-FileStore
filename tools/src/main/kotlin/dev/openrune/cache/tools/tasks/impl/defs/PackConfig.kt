package dev.openrune.cache.tools.tasks.impl.defs

import cc.ekblad.toml.decode
import cc.ekblad.toml.tomlMapper
import com.displee.cache.CacheLibrary
import dev.openrune.cache.*
import dev.openrune.cache.filestore.buffer.BufferWriter
import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.definition.DefinitionEncoder
import dev.openrune.cache.filestore.definition.data.ItemDefinition
import dev.openrune.cache.filestore.definition.data.NPCDefinition
import dev.openrune.cache.filestore.definition.data.ObjectDefinition
import dev.openrune.cache.filestore.definition.decoder.ItemDecoder
import dev.openrune.cache.filestore.definition.decoder.NPCDecoder
import dev.openrune.cache.filestore.definition.decoder.ObjectDecoder
import dev.openrune.cache.filestore.definition.encoder.ItemEncoder
import dev.openrune.cache.filestore.definition.encoder.NpcEncoder
import dev.openrune.cache.filestore.definition.encoder.ObjectEncoder
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.capitalizeFirstLetter
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.lang.reflect.Modifier

enum class PackMode{
    NPCS,
    ITEMS,
    OBJECTS
}

class PackConfig(val type : PackMode, private val directory : File) : CacheTask() {

    val logger = KotlinLogging.logger {}
    val mapper = tomlMapper {}


    override fun init(library: CacheLibrary) {
        val size = getFiles(directory, "toml").size
        val progress = progress("Packing ${type.name.lowercase().capitalizeFirstLetter()}", size)
        if (size != 0) {
            getFiles(directory, "toml").forEach {
                progress.extraMessage = it.name
                when(type) {
                    PackMode.ITEMS -> packDefinitions<ItemDefinition>(it, ItemEncoder(),ItemDecoder(),library)
                    PackMode.NPCS -> packDefinitions<NPCDefinition>(it, NpcEncoder(),NPCDecoder(),library)
                    PackMode.OBJECTS -> packDefinitions<ObjectDefinition>(it, ObjectEncoder(),ObjectDecoder(),library)
                    else -> println("Not Supported")
                }
                progress.step()
            }
            progress.close()
        }
    }

    private inline fun <reified T: Definition> packDefinitions(file: File, encoder: DefinitionEncoder<T>, decoder: DefinitionDecoder<T>, library: CacheLibrary) {

        var def: T = mapper.decode<T>(file.toPath())

        if (def.id == -1) {
            logger.info { "Unable to pack as the ID is -1 or has not been defined" }
            return
        }

        val defId = def.id

        if (def.inherit != -1) {
            val data = library.data(CONFIGS, decoder.index, def.inherit)
            if (data != null) {

                val inheritedDef = decoder.loadSingle(def.inherit, data)!!
                def = mergeDefinitions<T>(inheritedDef, def)
            } else {
                logger.warn { "No inherited definition found for ID ${def.inherit}" }
                return
            }
        }

        val writer = BufferWriter(4096)
        with(encoder) { writer.encode(def) }

        library.index(CONFIGS).archive(decoder.index)?.add(defId, writer.toArray())
    }

    companion object {
        inline fun <reified T : Definition> mergeDefinitions(baseDef: T, inheritedDef: T): T {
            val ignoreFields = setOf("inherit")
            val defaultDef = T::class.java.getDeclaredConstructor().newInstance()

            T::class.java.declaredFields.forEach { field ->
                if (!Modifier.isStatic(field.modifiers) && !ignoreFields.contains(field.name)) {
                    field.isAccessible = true
                    val baseValue = field.get(baseDef)
                    val inheritedValue = field.get(inheritedDef)
                    val defaultValue = field.get(defaultDef)

                    // Only overwrite the base value if the inherited value is different from both the base and default values
                    if (inheritedValue != baseValue && inheritedValue != defaultValue) {
                        field.set(baseDef, inheritedValue)
                    }
                }
            }

            return baseDef
        }

    }

}


