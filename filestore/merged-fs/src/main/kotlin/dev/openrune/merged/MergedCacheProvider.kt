package dev.openrune.merged

import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.CacheStore
import dev.openrune.cache.DenseIntMap
import dev.openrune.definition.Definition
import dev.openrune.definition.codec.HeadbarCodec
import dev.openrune.definition.codec.HitmarkCodec
import dev.openrune.definition.codec.Rs2EnumCodec
import dev.openrune.definition.codec.Rs2NpcCodec
import dev.openrune.definition.codec.Rs2ObjCodec
import dev.openrune.definition.codec.Rs2SequenceCodec
import dev.openrune.definition.codec.Rs2StructCodec
import dev.openrune.definition.codec.Rs2VarBitCodec
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.type.HeadbarType
import dev.openrune.definition.type.HitmarkType
import dev.openrune.definition.type.Rs2NpcType
import dev.openrune.definition.type.Rs2ObjType
import dev.openrune.definition.type.Rs2SequenceType
import dev.openrune.definition.type.Rs2VarBitType
import dev.openrune.definition.type.StructType
import dev.openrune.filesystem.Cache

class MergedCacheProvider(
    private val cache: Cache,
    private val rs2Build: Int,
    osrsRevision: Int = UNKNOWN_REVISION,
    private val indices: Map<String, Int> = emptyMap(),
    val rs2Offset: Int = Rs2MergeLayout.ID_OFFSET,
) : CacheStore() {

    private val osrs =
        if (osrsRevision == UNKNOWN_REVISION) OsrsCacheProvider(cache) else OsrsCacheProvider(cache, osrsRevision)

    override var cacheRevision: Int = osrs.cacheRevision

    override val npcs = osrs.npcs
    override val objects = osrs.objects
    override val items = osrs.items
    override val varbits = osrs.varbits
    override val varps = osrs.varps
    override val anims = osrs.anims
    override val enums = osrs.enums
    override val healthBars = osrs.healthBars
    override val hitsplats = osrs.hitsplats
    override val structs = osrs.structs
    override val dbrows = osrs.dbrows
    override val dbtables = osrs.dbtables

    private val counts = LinkedHashMap<String, Int>()

    override fun init() {
        osrs.init()
        cacheRevision = osrs.cacheRevision

        promoted(Rs2MergeLayout.Promoted.OBJ, Rs2ObjCodec(rs2Build)) { id, def: Rs2ObjType ->
            items[id] = def.toItemType(id)
        }
        promoted(Rs2MergeLayout.Promoted.NPC, Rs2NpcCodec(rs2Build)) { id, def: Rs2NpcType ->
            npcs[id] = def.toNpcType(id)
        }
        promoted(Rs2MergeLayout.Promoted.SEQ, Rs2SequenceCodec(rs2Build)) { id, def: Rs2SequenceType ->
            anims[id] = def.toSequenceType(id)
        }
        promoted(Rs2MergeLayout.Promoted.ENUM, Rs2EnumCodec()) { id, def: EnumType ->
            enums[id] = def.also { it.id = id }
        }
        promoted(Rs2MergeLayout.Promoted.STRUCT, Rs2StructCodec()) { id, def: StructType ->
            structs[id] = def.also { it.id = id }
        }

        grouped(Rs2MergeLayout.Grouped.VAR_BIT, Rs2VarBitCodec()) { id, def: Rs2VarBitType ->
            varbits[id] = def.toVarBitType(id)
        }
        grouped(Rs2MergeLayout.Grouped.HITMARK, HitmarkCodec()) { id, def: HitmarkType ->
            hitsplats[id] = def.toHitSplatType(id)
        }
        grouped(Rs2MergeLayout.Grouped.HEADBAR, HeadbarCodec()) { id, def: HeadbarType ->
            healthBars[id] = def.toHealthBarType(id)
        }
    }

    fun rs2Count(type: Rs2MergeLayout.Promoted): Int = counts[type.name] ?: 0

    fun rs2Count(type: Rs2MergeLayout.Grouped): Int = counts[type.name] ?: 0

    fun rs2Total(): Int = counts.values.sum()

    private inline fun <T : Definition> promoted(
        type: Rs2MergeLayout.Promoted,
        codec: dev.openrune.definition.DefinitionCodec<T>,
        put: (Int, T) -> Unit,
    ) {
        val index = indices[type.name] ?: return
        if (!cache.exists(index)) return
        val decoded: MutableMap<Int, T> = DenseIntMap()
        Rs2PromotedDecoder(index, codec, type.bits).load(cache, decoded)
        decoded.forEach { (rs2Id, def) -> put(rs2Id + rs2Offset, def) }
        counts[type.name] = decoded.size
    }

    private inline fun <T : Definition> grouped(
        type: Rs2MergeLayout.Grouped,
        codec: dev.openrune.definition.DefinitionCodec<T>,
        put: (Int, T) -> Unit,
    ) {
        val index = indices[type.name] ?: indices[GROUPED_KEY] ?: return
        if (!cache.exists(index)) return
        val decoded: MutableMap<Int, T> = DenseIntMap()
        Rs2GroupDecoder(index, codec, type.group).load(cache, decoded)
        decoded.forEach { (rs2Id, def) -> put(rs2Id + rs2Offset, def) }
        counts[type.name] = decoded.size
    }

    companion object {
        const val UNKNOWN_REVISION = -1

        const val GROUPED_KEY = "CONFIG"
    }
}
