import com.google.gson.GsonBuilder
import dev.openrune.*
import dev.openrune.cache.*
import dev.openrune.cache.tools.animations.*
import dev.openrune.cache.tools.typeDumper.TypeDumper.Companion.unpackGameVal
import dev.openrune.definition.*
import dev.openrune.definition.type.*
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.io.*
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

internal class AnimationGrouping {

    private val logger = KotlinLogging.logger {}

    private lateinit var cache: Cache
    private lateinit var frames: FrameArchive
    private lateinit var frameMaps: FrameMapArchive

    private val graphics = HashMap<Int, SpotAnimType>(3000)
    private val objects = HashMap<Int, ObjectType>(10000)
    private val npcs = HashMap<Int, NpcType>(10000)
    private val sequences = HashMap<Int, SequenceType>(10000)

    suspend fun generate() = withContext(Dispatchers.Default) {

        logger.info { "Initializing cache..." }
        loadCache()

        val animationGroups = computeAnimationGroups()

        val outputFile = File("anims.txt").apply { createNewFile() }
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonGroups = ConcurrentHashMap<Int, AnimationGroup>()

        val spotNames = collectGameVals(Js5GameValGroup.SPOTTYPES)
        val seqNames = collectGameVals(Js5GameValGroup.SEQTYPES)
        val npcNames = collectGameVals(Js5GameValGroup.NPCTYPES)
        val locNames = collectGameVals(Js5GameValGroup.LOCTYPES)

        withContext(Dispatchers.IO) {
            BufferedWriter(FileWriter(outputFile)).use { writer ->
                animationGroups.toSortedMap().forEach { (baseId, anims) ->
                    writer.writeLine("Skeleton: $baseId")
                    if (anims.isEmpty()) return@forEach

                    writer.writeLine("seq: [${anims.joinToString { "${seqNames[it]} ($it)" }}]")

                    val matchedNpcs = npcs.values.filter { it.sequenceIds().any(anims::contains) }
                    val matchedGraphics = graphics.values.filter { it.animationId in anims }
                    val matchedObjects = objects.values.filter { it.animationId in anims }

                    if (matchedNpcs.isNotEmpty()) {
                        writer.writeLine("npc: [${matchedNpcs.joinToString { "${npcNames[it.id]} (${it.id})" }}]")
                    }
                    if (matchedObjects.isNotEmpty()) {
                        writer.writeLine("loc: [${matchedObjects.joinToString { "${locNames[it.id]} (${it.id})" }}]")
                    }
                    if (matchedGraphics.isNotEmpty()) {
                        writer.writeLine("spotanim: [${matchedGraphics.joinToString { "${spotNames[it.id]} (${it.id})" }}]")
                    }

                    writer.newLine()

                    jsonGroups[baseId] = AnimationGroup(
                        anims.map { NamedElement(seqNames[it] ?: "null", it) },
                        matchedGraphics.map { NamedElement(spotNames[it.id] ?: "null", it.id) },
                        matchedNpcs.map { NamedElement(npcNames[it.id] ?: "null", it.id) },
                        matchedObjects.map { NamedElement(locNames[it.id] ?: "null", it.id) }
                    )
                }
            }
        }

        File("anims.json").writeText(gson.toJson(jsonGroups))
    }

    private suspend fun loadCache() = withContext(Dispatchers.IO) {
        cache = Cache.load(Path.of("E:\\RSPS\\Hazy\\HazyGameServer\\data\\cache"), false)
        OsrsCacheProvider.CACHE_REVISION = 231

        OsrsCacheProvider.SequenceDecoder().load(cache, sequences)
        OsrsCacheProvider.NPCDecoder().load(cache, npcs)
        OsrsCacheProvider.SpotAnimDecoder().load(cache, graphics)
        OsrsCacheProvider.ObjectDecoder().load(cache, objects)

        frameMaps = FrameMapArchive.load(cache)

        val normalFrameIds = HashSet<Int>(50000)
        val skeletalIds = HashSet<Int>(1000)

        for (seq in sequences.values) {
            if (seq.skeletalId != -1) skeletalIds += seq.skeletalId
            seq.frameIDs?.let { normalFrameIds += it }
            seq.chatFrameIds?.let { normalFrameIds += it }
        }

        val framess = mutableMapOf<Int, Frame>()
        for (frameId in normalFrameIds) {
            val groupId = frameId shr 16
            val fileId = frameId and 0xFFFF
            val buf =
                try {
                    Unpooled.wrappedBuffer(cache.data(FrameArchive.id, groupId, fileId))
                } catch (e: Exception) {
                    continue
                }
            val frame = Frame.decode(frameMaps.frameMaps, buf)
            buf.release()
            framess[frameId] = frame
        }
        for (skeletalAnim in skeletalIds) {
            val groupId = skeletalAnim shr 16
            val fileId = skeletalAnim and 0xFFFF
            val buf = Unpooled.wrappedBuffer(cache.data(22, groupId, fileId))
            val frame = Frame.decodePartial(buf)
            buf.release()
            framess[skeletalAnim] = frame
        }
        frames = FrameArchive(framess)
    }

    private fun computeAnimationGroups(): Map<Int, List<Int>> {
        val groups = HashMap<Int, MutableList<Int>>(1000)
        val visitedBases = HashSet<Int>(1000)

        for ((id, seq) in sequences) {
            val baseIds = mutableSetOf<Int>()

            if (seq.skeletalId != -1) {
                val frame = frames.frames[seq.skeletalId]
                frame?.let { baseIds += it.frameMapId }
            }

            seq.frameIDs?.forEach { id -> frames.frames[id]?.let { baseIds += it.frameMapId } }
            seq.chatFrameIds?.forEach { id -> frames.frames[id]?.let { baseIds += it.frameMapId } }

            for (baseId in baseIds) {
                groups.computeIfAbsent(baseId) { ArrayList() }.add(id)
            }
        }

        return groups
    }

    private fun collectGameVals(type: Js5GameValGroup): Map<Int, String> {
        val result = HashMap<Int, String>(5000)
        val lines = buildString {
            cache.files(GAMEVALS, type.id).forEach { file ->
                val data = cache.data(GAMEVALS, type.id, file)
                unpackGameVal(type, file, data, this)
            }
        }
        lines.lineSequence().filter { it.isNotEmpty() }.forEach {
            val (name, id) = it.split(":")
            result[id.toInt()] = name
        }
        return result
    }

    private fun BufferedWriter.writeLine(string: String) {
        write(string)
        newLine()
    }

    private fun NpcType.sequenceIds(): IntArray = intArrayOfNotNull(
        standAnim, walkAnim, rotateBackAnim, walkLeftAnim, walkRightAnim,
        rotateLeftAnim, rotateRightAnim, runBackSequence, runLeftSequence, runRightSequence
    )

    private inline fun intArrayOfNotNull(vararg values: Int): IntArray {
        val result = IntArray(values.count { it != -1 })
        var idx = 0
        for (value in values) if (value != -1) result[idx++] = value
        return result
    }

    private data class AnimationGroup(
        val seq: List<NamedElement>,
        val spotanim: List<NamedElement>,
        val npc: List<NamedElement>,
        val loc: List<NamedElement>
    )

    private data class NamedElement(val name: String, val id: Int)
}

fun main() = runBlocking {
    val time = measureTimeMillis {
        AnimationGrouping().generate()
    }
    println("Time To Dump: ${time} ms")
}
