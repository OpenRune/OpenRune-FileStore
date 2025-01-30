package dev.openrune.cache.tools.tasks.impl.defs

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.displee.cache.CacheLibrary
import dev.openrune.*
import dev.openrune.cache.*
import dev.openrune.cache.filestore.buffer.BufferWriter
import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.data.*
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.capitalizeFirstLetter
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.decodeFromString
import java.io.File
import java.lang.reflect.Modifier

enum class PackMode{
    NPCS,
    ITEMS,
    OBJECTS,
    HITSPLATS,
    HEALTBAR,
    SEQUENCE,
    AREA

}

class PackConfig(val type : PackMode, private val directory : File) : CacheTask() {

    val logger = KotlinLogging.logger {}

    override fun init(library: CacheLibrary) {
        val size = getFiles(directory, "toml").size
        val progress = progress("Packing ${type.name.lowercase().capitalizeFirstLetter()}", size)
        if (size != 0) {
            getFiles(directory, "toml").forEach {
                progress.extraMessage = it.name
                when(type) {
                    PackMode.ITEMS -> packDefinitions<ItemType>(it, ItemDecoder(),library, OBJECT)
                    PackMode.NPCS -> packDefinitions<NpcType>(it, NPCDecoder(),library,NPC)
                    PackMode.OBJECTS -> packDefinitions<ObjectType>(it, ObjectDecoder(),library, OBJECT)
                    PackMode.HITSPLATS -> packDefinitions<HitSplatType>(it, HitSplatDecoder(),library, HITSPLAT)
                    PackMode.HEALTBAR -> packDefinitions<HealthBarType>(it, HealthBarDecoder(),library, HEALTHBAR)
                    PackMode.SEQUENCE -> packDefinitions<SequenceType>(it, SequenceDecoder(),library, SEQUENCE)
                    PackMode.AREA -> packDefinitions<AreaType>(it, AreaDecoder(),library, AREA)
                    else -> println("Not Supported")
                }
                progress.step()
            }
            progress.close()
        }
    }

    private inline fun <reified T : Definition> packDefinitions(
        file: File,
        decoder: DefinitionDecoderOSRS<T>,
        library: CacheLibrary,
        archive: Int
    ) {
        val tomlContent = file.readText()
        val toml = Toml(TomlInputConfig(true))
        var def: T = toml.decodeFromString(tomlContent)


        if (def.id == -1) {
            logger.info { "Unable to pack as the ID is -1 or has not been defined" }
            return
        }

        val defId = def.id

        if (def.inherit != -1) {
            val data = library.data(CONFIGS, archive, def.inherit)
            if (data != null) {
                val inheritedDef = decoder.loadData(def.inherit, data)
                def = mergeDefinitions(inheritedDef, def)
            } else {
                logger.warn { "No inherited definition found for ID ${def.inherit}" }
                return
            }
        }

        val writer = BufferWriter(4096)
        with(decoder.codec) { writer.encode(def) }

        library.index(CONFIGS).archive(archive)?.add(defId, writer.toArray())
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


