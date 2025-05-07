package dev.openrune.cache.tools.tasks.impl

import dev.openrune.cache.GAMEVALS
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.definition.Js5GameValGroup
import dev.openrune.definition.util.toArray
import dev.openrune.definition.util.writeString
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled

internal class PackGameVals(private val rev : Int) : CacheTask() {

    override fun init(cache: Cache) {
        if (rev < 230) {
            return
        }

        CacheTool.gameValMappings.forEach { (group, values) ->
            val lastFileId = cache.lastFileId(GAMEVALS, group.id)
            packGameVal(group, lastFileId, values, cache)
        }
    }

    private fun packGameVal(type: Js5GameValGroup, lastFileId: Int, values: List<Pair<String, Int>>, cache: Cache) {
        val usedIds = values.map { it.second }.toSet()
        val maxUsedId = usedIds.maxOrNull() ?: lastFileId
        val missingIds = ((lastFileId + 1)..maxUsedId).filterNot(usedIds::contains)

        when (type) {
            Js5GameValGroup.TABLETYPES -> {

                val emptyWriter = Unpooled.buffer(4096).apply {
                    writeByte(1)
                    writeString("")
                    writeByte(1)
                    writeString("")
                    writeByte(0)
                }

                val emptyData = emptyWriter.toArray()

                for (id in missingIds) {
                    cache.write(GAMEVALS, type.id, id, emptyData)
                }

                for ((name, id) in values) {
                    val (tableName, columnsPart) = name.split(':')
                    val columns = columnsPart.removePrefix("[").removeSuffix("]").split(',')

                    val writer = Unpooled.buffer(4096).apply {
                        writeByte(1)
                        writeString(tableName)
                        columns.forEach {
                            writeByte(1)
                            writeString(it)
                        }
                        writeByte(0)
                    }

                    cache.write(GAMEVALS, type.id, id, writer.toArray())
                }
            }

            Js5GameValGroup.IFTYPES -> {
                missingIds.forEach {
                    val writer = Unpooled.buffer(64).apply {
                        writeString("")
                        writeByte(0xFF)
                        writeByte(0)
                    }
                    cache.write(GAMEVALS, type.id, it, writer.toArray())
                }

                for ((name, id) in values) {
                    val (infName, _) = name.substringBefore("[").split(":")
                    val components = name.substringAfter("[").replace(",]","").split(",")

                    val writer = Unpooled.buffer(4096).apply {
                        writeString(infName)
                        components.forEach {
                            val (componentMame,_) = it.split(":")
                            writeByte(1)
                            writeString(componentMame)
                        }
                        writeByte(0xFF)
                        writeByte(0)

                    }
                    cache.write(GAMEVALS, type.id, id, writer.toArray())
                }
            }

            else -> {
                missingIds.forEach { cache.write(GAMEVALS, type.id, it, byteArrayOf()) }

                for ((name, id) in values) {
                    val data = formatString(name).encodeToByteArray()
                    cache.write(GAMEVALS, type.id, id, data)
                }
            }
        }
    }

    private fun formatString(input: String): String {
        return input.lowercase().replace(" ", "_")
    }
}
