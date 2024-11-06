package dev.openrune.cache.filestore.definition.decoder

import dev.openrune.*
import dev.openrune.cache.CONFIGS
import dev.openrune.cache.CacheManager
import dev.openrune.cache.NPC
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.*
import dev.openrune.cache.filestore.definition.data.NpcType
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

class NPCDecoder : DefinitionDecoder<NpcType>(CONFIGS) {

    override fun getArchive(id: Int) = NPC

    override fun create(): Int2ObjectOpenHashMap<NpcType> = createMap { NpcType(it) }

    override fun getFile(id: Int) = id

    override fun NpcType.read(opcode: Int, buffer: Reader) {
        when (opcode) {
            1 -> {
                val length = buffer.readUnsignedByte()
                val models = MutableList(length) { 0 }
                for (count in 0 until length) {
                    models[count] = buffer.readUnsignedShort()
                    if (models[count] == 65535) {
                        models[count] = -1
                    }
                }
                values.withListInt("models", models)
            }
            2 -> values.withString("name", buffer.readString())
            12 -> values.withInt("size", buffer.readUnsignedByte())
            13 -> values.withInt("standAnim", buffer.readUnsignedShort())
            14 -> values.withInt("walkAnim", buffer.readUnsignedShort())
            15 -> values.withInt("rotateLeftAnim", buffer.readUnsignedShort())
            16 -> values.withInt("rotateRightAnim", buffer.readUnsignedShort())
            17 -> {
                values.withInt("walkAnim", buffer.readUnsignedShort())
                values.withInt("rotateBackAnim", buffer.readUnsignedShort())
                values.withInt("walkLeftAnim", buffer.readUnsignedShort())
                values.withInt("walkRightAnim", buffer.readUnsignedShort())
            }
            18 -> values.withInt("category", buffer.readUnsignedShort())
            in 30..34 -> {
                val action = buffer.readString()
                values.setListValue("actions", opcode - 30, if (action.equals("Hidden", true)) null else action, 5)
            }
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            60 -> {
                val length: Int = buffer.readUnsignedByte()
                values.withListInt("chatheadModels", MutableList(length) { buffer.readUnsignedShort() })
            }
            in 74..79 -> {
                values.setListValue("stats", opcode - 74, buffer.readUnsignedShort(), 6)
            }
            93 -> values.withBoolean("isMinimapVisible", false)
            95 -> values.withInt("combatLevel", buffer.readUnsignedShort())
            97 -> values.withInt("widthScale", buffer.readUnsignedShort())
            98 -> values.withInt("heightScale", buffer.readUnsignedShort())
            99 -> values.withBoolean("hasRenderPriority", true)
            100 -> values.withInt("ambient", buffer.readByte())
            101 -> values.withInt("contrast", buffer.readByte())
            102 -> {
                var headIconArchiveIds: MutableList<Int>? = null
                var headIconSpriteIndex: MutableList<Int>? = null
                if (CacheManager.revisionIsOrBefore(210)) {
                    headIconArchiveIds = MutableList(0) { 0 }
                    headIconSpriteIndex = MutableList(buffer.readUnsignedShort()) { 0 }
                } else {
                    val bitfield = buffer.readUnsignedByte()
                    var size = 0
                    var pos = bitfield
                    while (pos != 0) {
                        ++size
                        pos = pos shr 1
                    }
                    headIconArchiveIds = MutableList(size) { 0 }
                    headIconSpriteIndex = MutableList(size) { 0 }

                    for (i in 0 until size) {
                        if (bitfield and (1 shl i) == 0) {
                            headIconArchiveIds[i] = -1
                            headIconSpriteIndex[i] = -1
                        } else {
                            headIconArchiveIds[i] = buffer.readUnsignedShort()
                            headIconSpriteIndex[i] = buffer.readShortSmart() - 1
                        }
                    }
                }
                values.withListInt("headIconArchiveIds", headIconArchiveIds)
                values.withListInt("headIconSpriteIndex", headIconSpriteIndex)
            }
            111 -> values.withBoolean("isFollower", true)
            103 -> values.withInt("rotation", buffer.readUnsignedShort())
            106, 118 -> readTransforms(buffer, opcode == 118)
            107 -> values.withBoolean("isInteractable", false)
            109 -> values.withBoolean("isClickable", false)
            114 -> values.withInt("runSequence", buffer.readUnsignedShort())
            115 -> {
                values.withInt("runSequence", buffer.readUnsignedShort())
                values.withInt("runBackSequence", buffer.readUnsignedShort())
                values.withInt("runRightSequence", buffer.readUnsignedShort())
                values.withInt("runLeftSequence", buffer.readUnsignedShort())
            }
            116 -> values.withInt("crawlSequence", buffer.readUnsignedShort())
            117 -> {
                values.withInt("crawlSequence", buffer.readUnsignedShort())
                values.withInt("crawlBackSequence", buffer.readUnsignedShort())
                values.withInt("crawlRightSequence", buffer.readUnsignedShort())
                values.withInt("crawlLeftSequence", buffer.readUnsignedShort())
            }
            122 -> values.withBoolean("lowPriorityFollowerOps", true)
            123 -> values.withBoolean("isFollower", true)
            124 -> values.withInt("height", buffer.readUnsignedShort())
            249 -> readParameters(buffer)
            else -> logger.info { "Unable to decode Npcs [${opcode}]" }
        }
    }

}