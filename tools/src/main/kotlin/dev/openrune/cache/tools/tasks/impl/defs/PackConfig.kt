package dev.openrune.cache.tools.tasks.impl.defs

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import dev.openrune.OsrsCacheProvider.Companion.CACHE_REVISION
import dev.openrune.cache.*
import dev.openrune.definition.util.toArray
import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.*
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.capitalizeFirstLetter
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import dev.openrune.definition.codec.*
import dev.openrune.filesystem.Cache
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.Unpooled
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

    override fun init(cache: Cache) {
        val size = getFiles(directory, "toml").size
        val progress = progress("Packing ${type.name.lowercase().capitalizeFirstLetter()}", size)
        if (size != 0) {
            getFiles(directory, "toml").forEach {
                progress.extraMessage = it.name
                when(type) {
                    PackMode.ITEMS -> packDefinitions<ItemType>(it, ItemCodec(),cache, OBJECT)
                    PackMode.NPCS -> packDefinitions<NpcType>(it, NPCCodec(CACHE_REVISION),cache,NPC)
                    PackMode.OBJECTS -> packDefinitions<dev.openrune.definition.type.ObjectType>(it, ObjectCodec(CACHE_REVISION),cache, OBJECT)
                    PackMode.HITSPLATS -> packDefinitions<HitSplatType>(it,
                        dev.openrune.definition.codec.HitSplatCodec(),cache, HITSPLAT)
                    PackMode.HEALTBAR -> packDefinitions<HealthBarType>(it,
                        dev.openrune.definition.codec.HealthBarCodec(),cache, HEALTHBAR)
                    PackMode.SEQUENCE -> packDefinitions<SequenceType>(it, SequenceCodec(CACHE_REVISION),cache, SEQUENCE)
                    PackMode.AREA -> packDefinitions<AreaType>(it, AreaCodec(),cache, AREA)
                    else -> println("Not Supported")
                }
                progress.step()
            }
            progress.close()
        }
    }

    private inline fun <reified T : Definition> packDefinitions(
        file: File,
        codec: DefinitionCodec<T>,
        cache: Cache,
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
            val data = cache.data(CONFIGS, archive, def.inherit, null)
            if (data != null) {
                val inheritedDef = codec.loadData(def.inherit, data)
                def = mergeDefinitions(inheritedDef, def)
            } else {
                logger.warn { "No inherited definition found for ID ${def.inherit}" }
                return
            }
        }

        val writer = Unpooled.buffer(4096)
        with(codec) { writer.encode(def) }

        cache.write(CONFIGS, archive, defId, writer.toArray())
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


