package dev.openrune.cache.gameval

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

object GameValHandler {

    fun List<GameValElement>.lookup(id: Int): GameValElement? = this.firstOrNull { it.id == id }

    inline fun <reified T : GameValElement> GameValElement.elementAs(): T? = this as? T

    inline fun <reified T : GameValElement> List<GameValElement>.lookupAs(id: Int): T? =
        filterIsInstance<T>().firstOrNull { it.id == id }

    fun readGameVal(type: GameValGroupTypes, cache: Cache): List<GameValElement> =
        cache.files(GAMEVALS, type.id).flatMap { file ->
            val data = cache.data(GAMEVALS, type.id, file)
            unpackGameVal(type, file, data)
        }

    private fun unpackGameVal(type: GameValGroupTypes, id: Int, bytes: ByteArray?): List<GameValElement> {
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
                    val byte = data.readUnsignedByte().toInt()
                    val nextByte = if (data.readerIndex() < data.array().size) data.array()[data.readerIndex()] else null

                    if (byte == 0xFF && nextByte == 0.toByte()) break

                    val componentName = data.readString()
                    if (componentName.isEmpty()) break

                    components.add(InterfaceComponent(componentName, components.size, id))
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

    fun encodeGameVals(type: GameValGroupTypes, values: List<GameValElement>, cache: Cache) {
        when (type) {
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
                                writeByte(1)
                                writeString(standardizeGamevalName(it.name))
                            }
                            writeByte(0xFF)
                            writeByte(0)
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