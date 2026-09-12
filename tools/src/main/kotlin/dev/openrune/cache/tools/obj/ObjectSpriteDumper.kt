package dev.openrune.cache.tools.obj

import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.tools.SpriteNaming
import dev.openrune.cache.util.progress
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.type.ObjectType
import dev.openrune.filesystem.Cache
import readCacheRevision
import java.io.File
import javax.imageio.ImageIO

data class ObjectSpriteDumpResult(
    val written: Int,
    val skipped: Int,
    val failed: List<Int>
)

class ObjectSpriteDumper(private val factory: ObjectSpriteFactory, private val output: File) {
    var ids: Collection<Int>? = null

    var naming: SpriteNaming = SpriteNaming.GAMEVAL

    var size: Int = 128
    var border: Int = 0
    var shadowColor: Int = 0

    var orientation: Int = 0

    var yan: Int = 0
    var xan: Int = 160
    var zan: Int = 0

    var shape: Int? = null

    var fitToCanvas: Boolean = true
    var fitMargin: Double = 0.05
    var cameraDistance: Double = 8.0

    var overwrite: Boolean = true

    var showProgress: Boolean = true

    private var gameValNames: Map<Int, String> = emptyMap()
    private var filter: (ObjectType) -> Boolean = { true }
    private var namer: ((ObjectType) -> String)? = null
    private var configure: (ObjectSpriteBuilder, ObjectType) -> Unit = { _, _ -> }
    private var onFailure: (ObjectType, Throwable?) -> Unit = { _, _ -> }

    fun filter(block: (ObjectType) -> Boolean) {
        filter = block
    }

    fun name(block: (ObjectType) -> String) {
        namer = block
    }

    fun configure(block: (ObjectSpriteBuilder, ObjectType) -> Unit) {
        configure = block
    }

    fun onFailure(block: (ObjectType, Throwable?) -> Unit) {
        onFailure = block
    }

    fun gameValNames(names: Map<Int, String>) {
        gameValNames = names
    }

    fun dump(): ObjectSpriteDumpResult {
        output.mkdirs()

        val targets = (ids ?: factory.objects.keys).sorted()
        var written = 0
        var skipped = 0
        val failed = mutableListOf<Int>()
        val usedNames = mutableSetOf<String>()

        val bar = if (showProgress) progress("Dumping Object Sprites", targets.size) else null
        try {
            for (id in targets) {
                bar?.step()
                val obj = factory.objects[id] ?: continue
                if (!filter(obj)) {
                    skipped++
                    continue
                }

                val file = File(output, "${fileName(obj, usedNames)}.png")
                if (!overwrite && file.exists()) {
                    skipped++
                    continue
                }

                var error: Throwable? = null
                val image = try {
                    factory.obj(id)
                        .size(size)
                        .border(border)
                        .shadowColor(shadowColor)
                        .orientation(orientation)
                        .yan(yan)
                        .xan(xan)
                        .zan(zan)
                        .shape(shape)
                        .fitToCanvas(fitToCanvas)
                        .fitMargin(fitMargin)
                        .cameraDistance(cameraDistance)
                        .also { configure(it, obj) }
                        .create()
                } catch (e: Exception) {
                    error = e
                    null
                }

                if (image == null) {
                    failed += id
                    onFailure(obj, error)
                    continue
                }

                ImageIO.write(image, "png", file)
                written++
            }
        } finally {
            bar?.close()
        }

        return ObjectSpriteDumpResult(written, skipped, failed)
    }

    private fun fileName(obj: ObjectType, used: MutableSet<String>): String {
        namer?.let { return it(obj) }

        if (naming == SpriteNaming.ID) return obj.id.toString()

        val raw = if (naming.usesGameVal) gameValNames[obj.id] ?: obj.name else obj.name
        val name = sanitize(raw)
        if (naming.prefixesId) return "${obj.id}_$name"

        return if (used.add(name)) name else "${name}_${obj.id}"
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

fun dumpObjectSprites(
    cache: Cache,
    output: File,
    revision: Int = readCacheRevision(cache),
    block: ObjectSpriteDumper.() -> Unit = {}
): ObjectSpriteDumpResult {
    val dumper = ObjectSpriteDumper(ObjectSpriteFactory.fromCache(cache, revision), output)
    dumper.gameValNames(
        GameValHandler.readGameVal(GameValGroupTypes.LOCTYPES, cache, revision)
            .associate { it.id to it.name }
    )
    dumper.block()
    return dumper.dump()
}

fun dumpObjectSprites(
    factory: ObjectSpriteFactory,
    output: File,
    block: ObjectSpriteDumper.() -> Unit = {}
): ObjectSpriteDumpResult {
    val dumper = ObjectSpriteDumper(factory, output)
    dumper.block()
    return dumper.dump()
}
