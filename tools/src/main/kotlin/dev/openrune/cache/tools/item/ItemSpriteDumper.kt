package dev.openrune.cache.tools.item

import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.tools.SpriteNaming
import dev.openrune.cache.util.progress
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.type.ItemType
import dev.openrune.filesystem.Cache
import readCacheRevision
import java.io.File
import javax.imageio.ImageIO

data class ItemSpriteDumpResult(
    val written: Int,
    val skipped: Int,
    val failed: List<Int>
)

class ItemSpriteDumper(private val factory: ItemSpriteFactory, private val output: File) {
    var ids: Collection<Int>? = null

    var naming: SpriteNaming = SpriteNaming.GAMEVAL

    var quantity: Int = 1
    var size: Int = 36
    var border: Int = 1
    var shadowColor: Int = 3153952
    var autoScaleZoom: Boolean = false

    var fitToCanvas: Boolean = true

    var fitMargin: Double = 0.0
    var xan2d: Int = -1
    var yan2d: Int = -1
    var zan2d: Int = -1
    var zoom2d: Int = -1
    var xOffset2d: Int = -1
    var yOffset2d: Int = -1

    var overwrite: Boolean = true

    var showProgress: Boolean = true

    private var gameValNames: Map<Int, String> = emptyMap()
    private var filter: (ItemType) -> Boolean = { true }
    private var namer: ((ItemType) -> String)? = null
    private var configure: (ItemSpriteBuilder, ItemType) -> Unit = { _, _ -> }
    private var onFailure: (ItemType, Throwable?) -> Unit = { _, _ -> }

    fun filter(block: (ItemType) -> Boolean) {
        filter = block
    }

    fun name(block: (ItemType) -> String) {
        namer = block
    }

    fun configure(block: (ItemSpriteBuilder, ItemType) -> Unit) {
        configure = block
    }

    fun onFailure(block: (ItemType, Throwable?) -> Unit) {
        onFailure = block
    }

    fun gameValNames(names: Map<Int, String>) {
        gameValNames = names
    }

    fun dump(): ItemSpriteDumpResult {
        output.mkdirs()

        val targets = (ids ?: factory.items.keys).sorted()
        var written = 0
        var skipped = 0
        val failed = mutableListOf<Int>()
        val usedNames = mutableSetOf<String>()

        val bar = if (showProgress) progress("Dumping Item Sprites", targets.size) else null
        try {
            for (id in targets) {
                bar?.step()
                val item = factory.items[id] ?: continue
                if (!filter(item)) {
                    skipped++
                    continue
                }

                val file = File(output, "${fileName(item, usedNames)}.png")
                if (!overwrite && file.exists()) {
                    skipped++
                    continue
                }

                var error: Throwable? = null
                val image = try {
                    factory.item(id)
                        .quantity(quantity)
                        .size(size)
                        .border(border)
                        .shadowColor(shadowColor)
                        .autoScaleZoom(autoScaleZoom)
                        .fitToCanvas(fitToCanvas)
                        .fitMargin(fitMargin)
                        .xan2d(xan2d)
                        .yan2d(yan2d)
                        .zan2d(zan2d)
                        .zoom2d(zoom2d)
                        .xOffset2d(xOffset2d)
                        .yOffset2d(yOffset2d)
                        .also { configure(it, item) }
                        .create()
                } catch (e: Exception) {
                    error = e
                    null
                }

                if (image == null) {
                    failed += id
                    onFailure(item, error)
                    continue
                }

                ImageIO.write(image, "png", file)
                written++
            }
        } finally {
            bar?.close()
        }

        return ItemSpriteDumpResult(written, skipped, failed)
    }

    private fun fileName(item: ItemType, used: MutableSet<String>): String {
        namer?.let { return it(item) }

        if (naming == SpriteNaming.ID) return item.id.toString()

        val raw = if (naming.usesGameVal) gameValNames[item.id] ?: displayName(item) else displayName(item)
        val name = sanitize(raw)
        if (naming.prefixesId) return "${item.id}_$name"

        return if (used.add(name)) name else "${name}_${item.id}"
    }

    private fun displayName(item: ItemType): String {
        if (item.name != "null" && item.name.isNotBlank()) return item.name
        val linked = when {
            item.noteTemplateId != -1 -> item.noteLinkId
            item.notedId != -1 -> item.unnotedId
            item.placeholderTemplate != -1 -> item.placeholderLink
            else -> -1
        }
        return factory.items[linked]?.name ?: item.name
    }

    private fun sanitize(name: String): String {
        val cleaned = name
            .replace(Regex("<[^>]*>"), "")
            .trim()
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
        return cleaned.ifEmpty { "unnamed" }
    }
}

fun dumpItemSprites(
    cache: Cache,
    output: File,
    revision: Int = readCacheRevision(cache),
    block: ItemSpriteDumper.() -> Unit = {}
): ItemSpriteDumpResult {
    val dumper = ItemSpriteDumper(ItemSpriteFactory.fromCache(cache, revision), output)
    dumper.gameValNames(
        GameValHandler.readGameVal(GameValGroupTypes.OBJTYPES, cache, revision)
            .associate { it.id to it.name }
    )
    dumper.block()
    return dumper.dump()
}

fun dumpItemSprites(
    factory: ItemSpriteFactory,
    output: File,
    block: ItemSpriteDumper.() -> Unit = {}
): ItemSpriteDumpResult {
    val dumper = ItemSpriteDumper(factory, output)
    dumper.block()
    return dumper.dump()
}
