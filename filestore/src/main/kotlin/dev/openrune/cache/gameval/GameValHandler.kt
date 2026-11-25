package dev.openrune.cache.gameval

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.GAMEVALS
import dev.openrune.cache.gameval.impl.Interface.InterfaceComponent
import dev.openrune.cache.gameval.GameValHandler.lookup
import dev.openrune.cache.gameval.GameValHandler.lookupAs
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.gameval.impl.Sprite
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.definition.GameValGroup
import dev.openrune.definition.GameValGroupTypeRS3
import dev.openrune.definition.GameValGroupTypesOSRS
import dev.openrune.definition.GameValGroupTypesOSRS.*
import dev.openrune.definition.codec.Rs2AssetNames
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.toArray
import dev.openrune.definition.util.writeString
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import readCacheRevision

object GameValHandler {

    private val logger = InlineLogger()

    fun List<GameValElement>.lookup(id: Int): GameValElement? =
        if (this is GameValList) first(id) else firstOrNull { it.id == id }

    inline fun <reified T : GameValElement> GameValElement.elementAs(): T? = this as? T

    inline fun <reified T : GameValElement> List<GameValElement>.lookupAs(id: Int): T? {
        if (this is GameValList) {
            val typed = first(id) as? T
            if (typed != null) return typed
        }
        return filterIsInstance<T>().firstOrNull { it.id == id }
    }

    private fun assertNoDuplicateGameValKeys(
        type: GameValGroup,
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
    /**
     * Groups that were renumbered rather than redesigned, so the old id stays the way callers ask for
     * the group and this resolves it to the archive the cache in hand actually uses.
     *
     * - [IFTYPES] (13) -> [IFTYPES_V2] (14) at 232, a format change.
     * - [SOUNDTYPES] (11) -> [JINGLETYPES] (18) at 242, when the single sound group was split into
     *   sfx / midi / jingle / ambient. Archive 11 only ever held jingles, so it maps straight across.
     */
    private fun replacementGroup(type: GameValGroup): GameValGroup? = when (type) {
        IFTYPES -> IFTYPES_V2
        SOUNDTYPES -> JINGLETYPES
        else -> null
    }

    /**
     * Reads go by which archive holds data: that stays right for a cache whose revision disagrees with
     * its gameval layout, which happens whenever an older tool packed it.
     */
    private fun resolveGroupForRead(type: GameValGroup, cache: Cache): GameValGroup {
        val replacement = replacementGroup(type) ?: return type
        return if (cache.files(type.index, type.id).isEmpty()) replacement else type
    }

    /**
     * Writes also stay with whichever archive already holds data, so a write never lands in the
     * archive a read would ignore. Only when neither is populated — a cache being built from
     * scratch, where emptiness says nothing — does the revision pick the target.
     */
    private fun resolveGroupForWrite(type: GameValGroup, cache: Cache, cacheRevision: Int): GameValGroup {
        val replacement = replacementGroup(type) ?: return type
        if (cache.files(type.index, type.id).isNotEmpty()) return type
        if (cache.files(replacement.index, replacement.id).isNotEmpty()) return replacement

        val rev = if (cacheRevision == -1) {
            readCacheRevision(cache, "${type.groupName} is unsupported in this revision")
        } else {
            cacheRevision
        }
        return if (rev >= replacement.revision) replacement else type
    }

    fun readGameVal(
        type: GameValGroup,
        cache: Cache,
        cacheRevision: Int = -1,
        spriteNameHashes: Map<Int, Int>? = null,
    ): List<GameValElement> {

        if (type is GameValGroupTypeRS3) return readRs3GameVal(type, cache, spriteNameHashes)

        var type = type

        if (type.revision != -1) {
            val rev = if (cacheRevision == -1) readCacheRevision(cache,"${type.groupName} is unsupported in this revision") else cacheRevision
            if (rev < type.revision) {
                logger.debug {
                    "Skipping GameVal group '${type.groupName}' (id=${type.id}): " +
                            "requires cache revision ${type.revision}, but cache revision is $rev"
                }
            }
        }


        type = resolveGroupForRead(type, cache)

        val archive = type.id
        val files = cache.files(type.index, archive)
        val elements = ArrayList<GameValElement>(files.size)
        for (file in files) {
            val data = cache.data(type.index, archive, file)
            elements.addAll(unpackGameVal(type, file, data))
        }

        return GameValList(elements)
    }

    fun unpackGameVal(type: GameValGroup, id: Int, bytes: ByteArray?): List<GameValElement> {
        if (bytes == null) return emptyList()
        val data = Unpooled.wrappedBuffer(bytes)
        val elements = ArrayList<GameValElement>(1)

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
                    val nextByte = if (data.readerIndex() < bytes.size) bytes[data.readerIndex()] else null

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

                    components.add(InterfaceComponent(componentName, child, id))
                }

                elements.add(Interface(interfaceName, id, components))
            }

            else -> {
                val name = data.toString(Charsets.UTF_8)
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

    fun encodeGameVals(type: GameValGroup, values: List<GameValElement>, cache: Cache, cacheRevision : Int = -1) {

        if (type is GameValGroupTypeRS3) {
            encodeRs3GameVals(type, values, cache)
            return
        }


        val resolvedType = resolveGroupForWrite(type, cache, cacheRevision)

        assertNoDuplicateGameValKeys(resolvedType,values)
        if (resolvedType.revision != -1) {

            val rev = if (cacheRevision == -1) readCacheRevision(cache, "${resolvedType.groupName} is unsupported in this revision")
            else cacheRevision

            if (rev < resolvedType.revision) {
                logger.debug {
                    "Skipping encoding of GameVal group '${resolvedType.groupName}' " +
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

                        cache.write(resolvedType.index, resolvedType.id, table.id, writer.toArray())
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
                    cache.write(resolvedType.index, resolvedType.id, element.id, writer.toArray())
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
                    cache.write(resolvedType.index, resolvedType.id, element.id, writer.toArray())
                }
            }

            else -> {
                values.forEach { element ->
                    val data = when (resolvedType) {
                        SPRITETYPES -> element.elementAs<Sprite>()?.let { sprite ->
                            val base = if (sprite.index == -1) sprite.name else "${sprite.name},${sprite.index}"
                            standardizeGamevalName(base)
                        }
                        else -> element.elementAs<GameValElement>()?.name?.let(GameValHandler::standardizeGamevalName)
                    }
                    data?.encodeToByteArray()?.let { cache.write(resolvedType.index, resolvedType.id, element.id, it) }
                }
            }
        }
    }

    /**
     * RS3 keeps a whole group's names in one packed table rather than a file per id, so the group is
     * read and decoded in one go. Var groups carry their varbit names inline behind an `_` prefix -
     * [readGameValVarBits] returns those, [readGameVal] the vars themselves.
     */
    private fun readRs3GameVal(
        type: GameValGroupTypeRS3,
        cache: Cache,
        spriteNameHashes: Map<Int, Int>?,
    ): List<GameValElement> {
        val names = readRs3Names(type, cache) ?: return GameValList(emptyList())
        if (type.hasVarBitNames) return GameValList(Rs2AssetNames.splitVarNames(type, names).first)
        return GameValList(
            names.entries.mapNotNull { entry ->
                val name = entry.name ?: return@mapNotNull null
                when (type) {
                    GameValGroupTypeRS3.GRAPHIC -> rs3Sprite(name, entry.id, cache, spriteNameHashes)
                    GameValGroupTypeRS3.COMPONENT -> GameValElement(name.replace("__", ":"), entry.id)
                    else -> GameValElement(name, entry.id)
                }
            }
        )
    }

    fun readGameValVarBits(type: GameValGroupTypeRS3, cache: Cache): List<GameValElement> {
        if (!type.hasVarBitNames) return GameValList(emptyList())
        val names = readRs3Names(type, cache) ?: return GameValList(emptyList())
        return GameValList(Rs2AssetNames.splitVarNames(type, names).second)
    }

    private fun readRs3Names(type: GameValGroupTypeRS3, cache: Cache): Rs2AssetNames.Names? {
        val bytes = cache.data(type.index, type.id, 0) ?: return null
        return runCatching { Rs2AssetNames.decode(Unpooled.wrappedBuffer(bytes)) }.getOrElse { error ->
            logger.warn { "Failed to decode RS3 gameval group '${type.groupName}' (id=${type.id}): $error" }
            null
        }
    }

    /**
     * Sprite names lose their comma on the way into the table (`some_name,1` -> `some_name_1`), so the
     * comma form is put back only when its hash matches the sprite group the name belongs to.
     */
    private fun rs3Sprite(
        name: String,
        id: Int,
        cache: Cache,
        spriteNameHashes: Map<Int, Int>?,
    ): GameValElement {
        val underscore = name.lastIndexOf('_')
        if (underscore != -1) {
            val candidate = name.substring(0, underscore) + "," + name.substring(underscore + 1)
            if (spriteNameMatches(candidate.hashCode(), id, cache, spriteNameHashes)) {
                return Sprite(name.substring(0, underscore), name.substring(underscore + 1).toIntOrNull() ?: -1, id)
            }
        }
        return Sprite(name, -1, id)
    }

    /**
     * [spriteNameHashes] is the sprite index's group-id to name-hash table, needed because the comma
     * can only be recovered by matching the candidate against the name the sprite group actually has.
     * Without it the only source is the cache being read, which is the right answer for a whole RS3
     * cache and no answer at all for one holding gamevals alone.
     */
    private fun spriteNameMatches(
        hash: Int,
        id: Int,
        cache: Cache,
        spriteNameHashes: Map<Int, Int>?,
    ): Boolean {
        if (spriteNameHashes != null) return spriteNameHashes[id] == hash
        return runCatching { cache.archiveId(RS3_SPRITE_INDEX, hash) }.getOrDefault(-1) == id
    }

    private fun encodeRs3GameVals(type: GameValGroupTypeRS3, values: List<GameValElement>, cache: Cache) {
        assertNoDuplicateGameValKeys(type, values)
        val entries = values.sortedBy { it.id }.map { element ->
            val name = when {
                element is Sprite && element.index != -1 -> "${element.name}_${element.index}"
                type == GameValGroupTypeRS3.COMPONENT -> element.name.replace(":", "__")
                else -> element.name
            }
            Rs2AssetNames.Entry(element.id, standardizeGamevalName(name))
        }
        val dense = entries.withIndex().all { (index, entry) -> entry.id == index }
        val packType = if (dense) 1 else 2
        cache.write(type.index, type.id, 0, Rs2AssetNames.encode(Rs2AssetNames.Names(packType, entries)))
    }

    private const val RS3_SPRITE_INDEX = 8

    private val TAGS = Regex("<[^>]*>")
    private val AT_CODES = Regex("@[^@\\s]+@?")
    private val NON_NAME_CHARS = Regex("[^a-zA-Z0-9_+\\-\"']")
    private val REPEATED_UNDERSCORES = Regex("_+")

    fun standardizeGamevalName(name: String): String {
        return name
            .replace(TAGS, "")
            .replace(AT_CODES, "")
            .replace(NON_NAME_CHARS, "_")
            .replace(REPEATED_UNDERSCORES, "_")
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