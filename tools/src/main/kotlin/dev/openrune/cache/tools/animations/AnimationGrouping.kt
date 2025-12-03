import com.github.michaelbull.logging.InlineLogger
import com.google.gson.GsonBuilder
import dev.openrune.*
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.lookup
import dev.openrune.cache.tools.animations.*
import dev.openrune.definition.*
import dev.openrune.definition.type.*
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import kotlinx.coroutines.*
import java.io.*
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

internal class AnimationGrouping {

    private val logger = InlineLogger()

    private lateinit var cache: Cache
    private lateinit var frames: FrameArchive
    private lateinit var frameMaps: FrameMapArchive

    private val graphics = HashMap<Int, SpotAnimType>(3000)
    private val objects = HashMap<Int, ObjectType>(10000)
    private val npcs = HashMap<Int, NpcType>(10000)
    private val sequences = HashMap<Int, SequenceType>(10000)

    suspend fun generate(cache: Cache) = withContext(Dispatchers.Default) {

        logger.info { "Initializing cache..." }
        init(cache)

        val animationGroups = computeAnimationGroups()

        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonGroups = ConcurrentHashMap<Int, AnimationGroup>()

        val spotNames = GameValHandler.readGameVal(GameValGroupTypes.SPOTTYPES,cache)
        val seqNames = GameValHandler.readGameVal(GameValGroupTypes.SEQTYPES,cache)
        val npcNames = GameValHandler.readGameVal(GameValGroupTypes.NPCTYPES,cache)
        val locNames = GameValHandler.readGameVal(GameValGroupTypes.LOCTYPES,cache)

        withContext(Dispatchers.IO) {
            animationGroups.toSortedMap().forEach { (baseId, anims) ->
                if (anims.isEmpty()) return@forEach

                val matchedNpcs = npcs.values.filter { it.sequenceIds().any(anims::contains) }
                val matchedGraphics = graphics.values.filter { it.animationId in anims }
                val matchedObjects = objects.values.filter { it.animationId in anims }

                jsonGroups[baseId] = AnimationGroup(
                    anims.map { NamedElement(seqNames.lookup(it)?.name?: "null", it) },
                    matchedGraphics.map { NamedElement(spotNames.lookup(it.id)?.name?: "null", it.id) },
                    matchedNpcs.map { NamedElement(npcNames.lookup(it.id)?.name?: "null", it.id) },
                    matchedObjects.map { NamedElement(locNames.lookup(it.id)?.name?: "null", it.id) }
                )
            }
        }

        File("anims.json").writeText(gson.toJson(jsonGroups))
    }

    private suspend fun init(cache: Cache, cacheRevision : Int = readCacheRevision(cache)) = withContext(Dispatchers.IO) {

        OsrsCacheProvider.SequenceDecoder(cacheRevision).load(cache, sequences)
        OsrsCacheProvider.NPCDecoder(cacheRevision).load(cache, npcs)
        OsrsCacheProvider.SpotAnimDecoder().load(cache, graphics)
        OsrsCacheProvider.ObjectDecoder(cacheRevision).load(cache, objects)

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
        val cache = Cache.load(Path.of("E:\\RSPS\\Hazy\\HazyGameServer\\data\\cache"))
        AnimationGrouping().generate(cache)
    }
    println("Time To Dump: ${time} ms")
}
