package dev.openrune.cache.tools.npc

import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.tools.SpriteNaming
import dev.openrune.cache.util.progress
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.type.NpcType
import dev.openrune.filesystem.Cache
import readCacheRevision
import java.io.File
import javax.imageio.ImageIO

data class NpcSpriteDumpResult(
    val written: Int,
    val skipped: Int,
    val failed: List<Int>
)

class NpcSpriteDumper(private val factory: NpcSpriteFactory, private val output: File) {
    var ids: Collection<Int>? = null

    var naming: SpriteNaming = SpriteNaming.GAMEVAL

    var size: Int = 128
    var border: Int = 0
    var shadowColor: Int = 0

    var orientation: Int = 0

    var yan: Int = 0
    var xan: Int = 160
    var zan: Int = 0
    var chathead: Boolean = false
    var fitToCanvas: Boolean = true
    var fitMargin: Double = 0.05
    var cameraDistance: Double = 8.0

    var overwrite: Boolean = true

    var showProgress: Boolean = true

    private var gameValNames: Map<Int, String> = emptyMap()
    private var filter: (NpcType) -> Boolean = { true }
    private var namer: ((NpcType) -> String)? = null
    private var configure: (NpcSpriteBuilder, NpcType) -> Unit = { _, _ -> }
    private var onFailure: (NpcType, Throwable?) -> Unit = { _, _ -> }

    fun filter(block: (NpcType) -> Boolean) {
        filter = block
    }

    fun name(block: (NpcType) -> String) {
        namer = block
    }

    fun configure(block: (NpcSpriteBuilder, NpcType) -> Unit) {
        configure = block
    }

    fun onFailure(block: (NpcType, Throwable?) -> Unit) {
        onFailure = block
    }

    fun gameValNames(names: Map<Int, String>) {
        gameValNames = names
    }

    fun dump(): NpcSpriteDumpResult {
        output.mkdirs()

        val targets = (ids ?: factory.npcs.keys).sorted()
        var written = 0
        var skipped = 0
        val failed = mutableListOf<Int>()
        val usedNames = mutableSetOf<String>()

        val bar = if (showProgress) progress("Dumping NPC Sprites", targets.size) else null
        try {
            for (id in targets) {
                bar?.step()
                val npc = factory.npcs[id] ?: continue
                if (!filter(npc)) {
                    skipped++
                    continue
                }

                val file = File(output, "${fileName(npc, usedNames)}.png")
                if (!overwrite && file.exists()) {
                    skipped++
                    continue
                }

                var error: Throwable? = null
                val image = try {
                    factory.npc(id)
                        .size(size)
                        .border(border)
                        .shadowColor(shadowColor)
                        .orientation(orientation)
                        .yan(yan)
                        .xan(xan)
                        .zan(zan)
                        .chathead(chathead)
                        .fitToCanvas(fitToCanvas)
                        .fitMargin(fitMargin)
                        .cameraDistance(cameraDistance)
                        .also { configure(it, npc) }
                        .create()
                } catch (e: Exception) {
                    error = e
                    null
                }

                if (image == null) {
                    failed += id
                    onFailure(npc, error)
                    continue
                }

                ImageIO.write(image, "png", file)
                written++
            }
        } finally {
            bar?.close()
        }

        return NpcSpriteDumpResult(written, skipped, failed)
    }

    private fun fileName(npc: NpcType, used: MutableSet<String>): String {
        namer?.let { return it(npc) }

        if (naming == SpriteNaming.ID) return npc.id.toString()

        val raw = if (naming.usesGameVal) gameValNames[npc.id] ?: npc.name else npc.name
        val name = sanitize(raw)
        if (naming.prefixesId) return "${npc.id}_$name"

        return if (used.add(name)) name else "${name}_${npc.id}"
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

fun dumpNpcSprites(
    cache: Cache,
    output: File,
    revision: Int = readCacheRevision(cache),
    block: NpcSpriteDumper.() -> Unit = {}
): NpcSpriteDumpResult {
    val dumper = NpcSpriteDumper(NpcSpriteFactory.fromCache(cache, revision), output)
    dumper.gameValNames(
        GameValHandler.readGameVal(GameValGroupTypes.NPCTYPES, cache, revision)
            .associate { it.id to it.name }
    )
    dumper.block()
    return dumper.dump()
}

fun dumpNpcSprites(
    factory: NpcSpriteFactory,
    output: File,
    block: NpcSpriteDumper.() -> Unit = {}
): NpcSpriteDumpResult {
    val dumper = NpcSpriteDumper(factory, output)
    dumper.block()
    return dumper.dump()
}
