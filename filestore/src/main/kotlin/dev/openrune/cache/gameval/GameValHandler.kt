package dev.openrune.cache.gameval

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.GAMEVALS
import dev.openrune.cache.gameval.impl.Interface.InterfaceComponent
import dev.openrune.cache.gameval.GameValHandler.lookup
import dev.openrune.cache.gameval.GameValHandler.lookupAs
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.gameval.impl.Sprite
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.GameValGroupTypes.*
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.toArray
import dev.openrune.definition.util.writeString
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import readCacheRevision

object GameValHandler {

    private val logger = InlineLogger()

    fun List<GameValElement>.lookup(id: Int): GameValElement? = this.firstOrNull { it.id == id }

    inline fun <reified T : GameValElement> GameValElement.elementAs(): T? = this as? T

    inline fun <reified T : GameValElement> List<GameValElement>.lookupAs(id: Int): T? =
        filterIsInstance<T>().firstOrNull { it.id == id }

    private fun assertNoDuplicateGameValKeys(
        type: GameValGroupTypes,
        elements: List<GameValElement>
    ) {
        val seen = mutableMapOf<String, Int>()

        elements.forEach { element ->
            val previousId = seen.putIfAbsent(element.name, element.id)

            if (previousId != null && previousId != element.id) {
                error(
                    "Duplicate GameVal key detected for type '$type': " +
                            "'${element.name}' previously mapped to $previousId, now to ${element.id}"
                )
            }
        }
    }
    fun readGameVal(
        type: GameValGroupTypes,
        cache: Cache,
        cacheRevision: Int = -1
    ): List<GameValElement> {

        var type = type

        if (type.revision != -1) {
            val rev = if (cacheRevision == -1) readCacheRevision(cache,"${type.name} is unsupported in this revision") else cacheRevision
            if (rev < type.revision) {
                logger.info {
                    "Skipping GameVal group '${type.name}' (id=${type.id}): " +
                            "requires cache revision ${type.revision}, but cache revision is $rev"
                }
            }
        }


        if (type == IFTYPES) {
            if (cache.files(GAMEVALS, IFTYPES.id).isEmpty()) {
                type = IFTYPES_V2
            }
        }

        val elements = cache.files(GAMEVALS, type.id).flatMap { file ->
            val archive = type.id
            val data = cache.data(GAMEVALS, archive, file)
            unpackGameVal(type, file, data)
        }

        return elements
    }

    fun unpackGameVal(type: GameValGroupTypes, id: Int, bytes: ByteArray?): List<GameValElement> {
        if (bytes == null) return emptyList()
        val data = Unpooled.wrappedBuffer(bytes)
        val elements = mutableListOf<GameValElement>()

        when (type) {
            TABLETYPES -> {
                data.readUnsignedByte()
                val tableName = data.readString()
                val columns = mutableListOf<Table.Column>()
                var columnId = 0
                while (true) {
                    val flag = data.readUnsignedByte().toInt()
                    if (flag == 0) break
                    val columnName = data.readString()
                    columns.add(Table.Column(columnName, columnId))
                    columnId++
                }
                elements.add(Table(tableName, id, columns))
            }

            IFTYPES -> {
                val interfaceName = data.readString()

                val components = mutableListOf<InterfaceComponent>()
                while (true) {
                    val child = data.readUnsignedByte().toInt()
                    val nextByte = if (data.readerIndex() < data.array().size) data.array()[data.readerIndex()] else null

                    if (child == 0xFF && nextByte == 0.toByte()) break

                    val componentName = data.readString()
                    if (componentName.isEmpty()) break

                    components.add(InterfaceComponent(componentName, child, id))
                }

                elements.add(Interface(interfaceName, id, components))
            }

            IFTYPES_V2 -> {
                val interfaceName = data.readString()

                val components = mutableListOf<InterfaceComponent>()
                while (true) {
                    val child = data.readUnsignedShort()

                    if (child == 0xFFFF) break
                    
                    val componentName = data.readString()
                    if (componentName.isEmpty()) break

                    components.add(InterfaceComponent(componentName, child, id))
                }

                elements.add(Interface(interfaceName, id, components))
            }

            else -> {
                val remainingBytes = ByteArray(data.readableBytes())
                data.readBytes(remainingBytes)
                val name = remainingBytes.toString(Charsets.UTF_8)
                if (type == SPRITETYPES) {
                    val parts = name.split(',')
                    val spriteName = parts[0]
                    val index = parts.getOrNull(1)?.toIntOrNull() ?: -1
                    elements.add(Sprite(spriteName, index, id))
                } else {
                    elements.add(GameValElement(name, id))
                }
            }
        }

        return elements
    }

    fun encodeGameVals(type: GameValGroupTypes, values: List<GameValElement>, cache: Cache, cacheRevision : Int = -1) {

        assertNoDuplicateGameValKeys(type,values)

        var resolvedType = type

        if (resolvedType == IFTYPES) {
            if (cache.files(GAMEVALS, resolvedType.id).isEmpty()) {
                resolvedType = IFTYPES_V2
            }
        }

        if (resolvedType.revision != -1) {

            val rev = if (cacheRevision == -1) readCacheRevision(cache, "${resolvedType.name} is unsupported in this revision")
            else cacheRevision

            if (rev < resolvedType.revision) {
                logger.info {
                    "Skipping encoding of GameVal group '${resolvedType.name}' " +
                     "(requires rev ${resolvedType.revision}, cache rev=$rev)"
                }
                return
            }
        }


        when (resolvedType) {
            TABLETYPES -> {
                values.forEach { element ->
                    element.elementAs<Table>()?.let { table ->
                        val writer = Unpooled.buffer(4096).apply {
                            writeByte(1)
                            writeString(standardizeGamevalName(table.name))
                            table.columns.sortedBy { it.id }.forEach { column ->
                                writeByte(1)
                                writeString(standardizeGamevalName(column.name))
                            }
                            writeByte(0)
                        }

                        cache.write(GAMEVALS, type.id, table.id, writer.toArray())
                    }
                }
            }

            IFTYPES -> {
                values.forEach { element ->
                    val writer = Unpooled.buffer(4096).apply {
                        element.elementAs<Interface>()?.let { inf ->
                            writeString(standardizeGamevalName(inf.name))
                            inf.components.sortedBy { it.id }.forEach {
                                writeByte(it.id and 0xFFFF)
                                writeString(standardizeGamevalName(it.name))
                            }
                            writeByte(0xFF)
                            writeByte(0)
                        }
                    }
                    cache.write(GAMEVALS, type.id, element.id, writer.toArray())
                }
            }


            IFTYPES_V2 -> {
                values.forEach { element ->
                    val writer = Unpooled.buffer(4096).apply {
                        element.elementAs<Interface>()?.let { inf ->
                            writeString(standardizeGamevalName(inf.name))
                            inf.components.sortedBy { it.id }.forEach {
                                writeShort(it.id)
                                writeString(standardizeGamevalName(it.name))
                            }
                            writeShort(0xFFFF)
                        }
                    }
                    cache.write(GAMEVALS, type.id, element.id, writer.toArray())
                }
            }

            else -> {
                values.forEach { element ->
                    val data = when (type) {
                        SPRITETYPES -> element.elementAs<Sprite>()?.let { sprite ->
                            val base = if (sprite.index == -1) sprite.name else "${sprite.name},${sprite.index}"
                            standardizeGamevalName(base)
                        }
                        else -> element.elementAs<GameValElement>()?.name?.let(GameValHandler::standardizeGamevalName)
                    }
                    data?.encodeToByteArray()?.let { cache.write(GAMEVALS, type.id, element.id, it) }
                }
            }
        }
    }

    fun standardizeGamevalName(name: String): String {
        return name
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("@[^@\\s]+@?"), "")
            .replace(Regex("[^a-zA-Z0-9_+\\-\"']"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        .lowercase()
    }

}

fun main() {
    val cache = Cache.load(java.nio.file.Path.of("E:\\RSPS\\Hazy\\HazyGameServer\\data\\cache"))
    val sprites = GameValHandler.readGameVal(SPRITETYPES, cache)
    val infTypes = GameValHandler.readGameVal(IFTYPES, cache)
    val table = GameValHandler.readGameVal(TABLETYPES, cache)

    println(sprites.lookup(34)?.toFullString())
    println(sprites.lookup(1)?.toFullString())
    println(sprites.lookup(318)?.toFullString())
    println(infTypes.lookupAs<Interface>(1)?.toFullString())

    infTypes.lookupAs<Interface>(1)?.components?.forEach {
        println(it.toFullString(InterfaceComponent.FormatMode.PACKED))
    }

    println(table.lookup(2))

}