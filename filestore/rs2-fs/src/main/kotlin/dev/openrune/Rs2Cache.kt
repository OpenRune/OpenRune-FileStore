package dev.openrune

import dev.openrune.definition.codec.MapSceneIconCodec
import dev.openrune.definition.codec.QuickChatCategoryCodec
import dev.openrune.definition.codec.QuickChatPhraseCodec
import dev.openrune.definition.codec.Rs2AchievementCodec
import dev.openrune.definition.codec.Rs2MapElementCodec
import dev.openrune.definition.codec.Rs2MaterialDecoder
import dev.openrune.definition.codec.Rs2ModelDecoder
import dev.openrune.definition.codec.Rs2QuestCodec
import dev.openrune.definition.type.Rs2MaterialType
import dev.openrune.definition.type.Rs2ModelType
import dev.openrune.definition.type.MapSceneIconType
import dev.openrune.definition.type.Rs2AchievementType
import dev.openrune.definition.type.Rs2MapElementType
import dev.openrune.definition.type.Rs2QuestType
import dev.openrune.definition.type.QuickChatCategoryType
import dev.openrune.definition.type.QuickChatPhraseType
import dev.openrune.definition.type.SpriteType
import dev.openrune.rs2.OpenRs2ArchiveStore
import dev.openrune.rs2.OpenRs2CacheArchive
import dev.openrune.rs2.Rs2ConfigGroup
import dev.openrune.rs2.Rs2ConfigLocation
import dev.openrune.rs2.Rs2Index
import dev.openrune.rs2.Rs2Manifest
import dev.openrune.rs2.Rs2Sprite
import org.openrs2.cache.Cache
import java.awt.image.BufferedImage
import java.nio.file.Path

/** Wraps an openrs2 [Cache], which transparently supports both the legacy flat-file and JS5 container RS2 cache formats. */
class Rs2Cache private constructor(val cache: Cache, val build: Int? = null) : AutoCloseable {

    companion object {
        private const val QUICKCHAT_CATEGORY_GROUP = 0
        private const val QUICKCHAT_PHRASE_GROUP = 1
        private const val ACHIEVEMENT_FILE_BITS = 7

        fun load(root: Path, build: Int? = null): Rs2Cache = Rs2Cache(Cache.open(root), build)

        /** @param id the opaque archive.openrs2.org cache id - see [loadRemoteFromRev] for a build number instead. */
        fun loadRemote(id: Int, scope: String = "runescape"): Rs2Cache {
            val build = OpenRs2CacheArchive.buildFor(scope, id)
            return Rs2Cache(Cache.open(OpenRs2ArchiveStore(scope, id)), build)
        }

        /** @param build the client build/revision number, e.g. `822`. Defaults to the English cache unless [language] is given. */
        fun loadRemoteFromRev(build: Int, game: String = "runescape", scope: String = "runescape", environment: String = "live", language: String = "en"): Rs2Cache {
            val entry = OpenRs2CacheArchive.findByBuild(build, game, scope, environment, language)
                ?: throw java.io.FileNotFoundException("No $scope/$game/$environment/$language cache found for build $build")
            return Rs2Cache(Cache.open(OpenRs2ArchiveStore(scope, entry.id)), build)
        }
    }

    fun readSprite(archive: Int = Rs2Index.SPRITES, group: Int): List<BufferedImage> {
        val buf = cache.read(archive, group, 0)
        try {
            return Rs2Sprite.decode(buf)
        } finally {
            buf.release()
        }
    }

    /** Same as [readSprite] but as a [SpriteType], matching OSRS's [dev.openrune.definition.codec.SpriteCodec] output. */
    fun readSpriteType(archive: Int = Rs2Index.SPRITES, group: Int): SpriteType {
        val buf = cache.read(archive, group, 0)
        try {
            return Rs2Sprite.decodeAsType(group, buf)
        } finally {
            buf.release()
        }
    }

    fun hasIndex(index: Int): Boolean = cache.exists(index)

    fun groups(index: Int): List<Int> =
        cache.list(index).asSequence().map { it.id }.sorted().toList()

    /** Group id to name hash for [index], for the groups that carry a name. */
    fun groupNameHashes(index: Int): Map<Int, Int> {
        if (!cache.exists(index)) return emptyMap()
        val hashes = LinkedHashMap<Int, Int>()
        cache.list(index).forEach { group ->
            val hash = group.nameHash
            if (hash != -1) hashes[group.id] = hash
        }
        return hashes
    }

    fun files(index: Int, group: Int): List<Int> =
        cache.list(index, group).asSequence().map { it.id }.sorted().toList()

    fun groupBytes(index: Int, group: Int, file: Int = 0): ByteArray {
        val buf = cache.read(index, group, file)
        try {
            return ByteArray(buf.readableBytes()).also { buf.readBytes(it) }
        } finally {
            buf.release()
        }
    }

    fun spriteGroups(archive: Int = Rs2Index.SPRITES): List<Int> =
        cache.list(archive).asSequence().map { it.id }.sorted().toList()

    private val materialGroups: Map<Int, Int> by lazy {
        if (!cache.exists(Rs2Index.MATERIALS)) {
            emptyMap()
        } else {
            val groups = LinkedHashMap<Int, Int>()
            cache.list(Rs2Index.MATERIALS).forEach { group ->
                cache.list(Rs2Index.MATERIALS, group.id).forEach { file -> groups[file.id] = group.id }
            }
            groups
        }
    }

    fun materialIds(): List<Int> = materialGroups.keys.sorted()

    fun readMaterial(id: Int): Rs2MaterialType {
        val group = materialGroups[id] ?: throw java.io.FileNotFoundException("No material $id")
        val buf = cache.read(Rs2Index.MATERIALS, group, id)
        try {
            return Rs2MaterialDecoder.decode(id, buf)
        } finally {
            buf.release()
        }
    }

    fun modelIds(): List<Int> =
        if (!cache.exists(Rs2Index.MODELS_RT7)) emptyList() else groups(Rs2Index.MODELS_RT7)

    fun readModel(id: Int): Rs2ModelType {
        val buf = cache.read(Rs2Index.MODELS_RT7, id, 0)
        try {
            return Rs2ModelDecoder.decode(id, buf, build ?: Int.MAX_VALUE)
        } finally {
            buf.release()
        }
    }

    fun quickChatCategoryIds(): List<Int> =
        cache.list(Rs2Index.QUICKCHAT, QUICKCHAT_CATEGORY_GROUP).asSequence().map { it.id }.sorted().toList()

    fun quickChatPhraseIds(): List<Int> =
        cache.list(Rs2Index.QUICKCHAT, QUICKCHAT_PHRASE_GROUP).asSequence().map { it.id }.sorted().toList()

    fun readQuickChatCategory(id: Int): QuickChatCategoryType {
        val buf = cache.read(Rs2Index.QUICKCHAT, QUICKCHAT_CATEGORY_GROUP, id)
        try {
            return QuickChatCategoryCodec().loadData(id, buf)
        } finally {
            buf.release()
        }
    }

    fun readQuickChatPhrase(id: Int): QuickChatPhraseType {
        val buf = cache.read(Rs2Index.QUICKCHAT, QUICKCHAT_PHRASE_GROUP, id)
        try {
            return QuickChatPhraseCodec().loadData(id, buf)
        } finally {
            buf.release()
        }
    }

    fun mapSceneIconIds(): List<Int> =
        cache.list(Rs2Index.CONFIG, Rs2ConfigGroup.MSITYPE).asSequence().map { it.id }.sorted().toList()

    fun readMapSceneIcon(id: Int): MapSceneIconType {
        val buf = cache.read(Rs2Index.CONFIG, Rs2ConfigGroup.MSITYPE, id)
        try {
            return MapSceneIconCodec().loadData(id, buf)
        } finally {
            buf.release()
        }
    }

    fun mapElementIds(): List<Int> =
        cache.list(Rs2Index.CONFIG, Rs2ConfigGroup.MELTYPE).asSequence().map { it.id }.sorted().toList()

    fun readMapElement(id: Int): Rs2MapElementType {
        val buf = cache.read(Rs2Index.CONFIG, Rs2ConfigGroup.MELTYPE, id)
        try {
            return Rs2MapElementCodec(build ?: Int.MAX_VALUE).loadData(id, buf)
        } finally {
            buf.release()
        }
    }

    fun questIds(): List<Int> =
        cache.list(Rs2Index.CONFIG, Rs2ConfigGroup.QUESTTYPE).asSequence().map { it.id }.sorted().toList()

    fun readQuest(id: Int): Rs2QuestType {
        val buf = cache.read(Rs2Index.CONFIG, Rs2ConfigGroup.QUESTTYPE, id)
        try {
            return Rs2QuestCodec().loadData(id, buf)
        } finally {
            buf.release()
        }
    }

    private fun achievementsPromoted(): Boolean = runCatching {
        cache.exists(Rs2Index.CONFIG_ACHIEVEMENT) && cache.list(Rs2Index.CONFIG_ACHIEVEMENT).hasNext()
    }.getOrDefault(false)

    fun achievementIds(): List<Int> = if (achievementsPromoted()) {
        cache.list(Rs2Index.CONFIG_ACHIEVEMENT).asSequence().flatMap { group ->
            cache.list(Rs2Index.CONFIG_ACHIEVEMENT, group.id).asSequence().map { (group.id shl ACHIEVEMENT_FILE_BITS) or it.id }
        }.sorted().toList()
    } else {
        cache.list(Rs2Index.CONFIG, Rs2ConfigGroup.ACHIEVEMENTTYPE).asSequence().map { it.id }.sorted().toList()
    }

    fun readAchievement(id: Int): Rs2AchievementType {
        val buf = if (achievementsPromoted()) {
            cache.read(Rs2Index.CONFIG_ACHIEVEMENT, id ushr ACHIEVEMENT_FILE_BITS, id and ((1 shl ACHIEVEMENT_FILE_BITS) - 1))
        } else {
            cache.read(Rs2Index.CONFIG, Rs2ConfigGroup.ACHIEVEMENTTYPE, id)
        }
        try {
            return Rs2AchievementCodec().loadData(id, buf)
        } finally {
            buf.release()
        }
    }

    fun configIds(location: Rs2ConfigLocation): List<Int> {
        val archive = location.archive
        if (archive != null && archivePopulated(archive.index)) {
            return cache.list(archive.index).asSequence().flatMap { group ->
                cache.list(archive.index, group.id).asSequence()
                    .map { (group.id shl archive.bits) + it.id }
            }.sorted().toList()
        }
        val group = location.group ?: return emptyList()
        return runCatching {
            cache.list(group.index, group.group).asSequence().map { it.id }.sorted().toList()
        }.getOrDefault(emptyList())
    }

    fun <T : dev.openrune.definition.Definition> readConfig(
        location: Rs2ConfigLocation,
        id: Int,
        codec: dev.openrune.definition.DefinitionCodec<T>,
    ): T {
        val archive = location.archive
        val buf = if (archive != null && archivePopulated(archive.index)) {
            cache.read(archive.index, id ushr archive.bits, id and ((1 shl archive.bits) - 1))
        } else {
            val group = location.group ?: throw java.io.FileNotFoundException("No location for config $id")
            cache.read(group.index, group.group, id)
        }
        try {
            return codec.loadData(id, buf)
        } finally {
            buf.release()
        }
    }

    fun configBytes(location: Rs2ConfigLocation, id: Int): ByteArray {
        val archive = location.archive
        val buf = if (archive != null && archivePopulated(archive.index)) {
            cache.read(archive.index, id ushr archive.bits, id and ((1 shl archive.bits) - 1))
        } else {
            val group = location.group ?: throw java.io.FileNotFoundException("No location for config $id")
            cache.read(group.index, group.group, id)
        }
        try {
            return ByteArray(buf.readableBytes()).also { buf.getBytes(buf.readerIndex(), it) }
        } finally {
            buf.release()
        }
    }

    private fun archivePopulated(index: Int): Boolean = runCatching {
        cache.exists(index) && cache.list(index).hasNext()
    }.getOrDefault(false)

    fun warnIfArchiveTooEarly(archiveName: String) {
        build?.let { Rs2Manifest.warnIfArchiveTooEarly(archiveName, it) }
    }

    fun warnIfConfigTooEarly(configName: String) {
        build?.let { Rs2Manifest.warnIfConfigTooEarly(configName, it) }
    }

    override fun close() = cache.close()

}
