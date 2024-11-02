package dev.openrune.cache.filestore.definition.decoder

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
                values["models"] = models
            }
            2 -> values["name"] = buffer.readString()
            12 -> values["size"] = buffer.readUnsignedByte()
            13 -> values["standAnim"] = buffer.readUnsignedShort()
            14 -> values["walkAnim"] = buffer.readUnsignedShort()
            15 -> values["rotateLeftAnim"] = buffer.readUnsignedShort()
            16 -> values["rotateRightAnim"] = buffer.readUnsignedShort()
            17 -> {
                values["walkAnim"] = buffer.readUnsignedShort()
                values["rotateBackAnim"] = buffer.readUnsignedShort()
                values["walkLeftAnim"] = buffer.readUnsignedShort()
                values["walkRightAnim"] = buffer.readUnsignedShort()
            }
            18 -> values["category"] = buffer.readUnsignedShort()
            in 30..34 -> {
                val action = buffer.readString()
                (values["actions"] as MutableList<String?>)[opcode - 30] = if (action.equals("Hidden", true)) null else action
            }
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            60 -> {
                val length: Int = buffer.readUnsignedByte()
                values["chatheadModels"] = MutableList(length) { buffer.readUnsignedShort() }

            }
            in 74..79 -> (values["stats"] as IntArray)[opcode - 74] = buffer.readUnsignedShort()

            93 -> values["isMinimapVisible"] = false
            95 -> values["combatLevel"] = buffer.readUnsignedShort()
            97 -> values["widthScale"] = buffer.readUnsignedShort()
            98 -> values["heightScale"] = buffer.readUnsignedShort()
            99 -> values["hasRenderPriority"] = true
            100 -> values["ambient"] = buffer.readByte()
            101 -> values["contrast"] = buffer.readByte()
            102 -> {
                var headIconArchiveIds: MutableList<Int>? = null;
                var headIconSpriteIndex: MutableList<Int>? = null;
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
                    headIconArchiveIds =  MutableList(size) { 0 }
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
                values["headIconArchiveIds"] = headIconArchiveIds
                values["headIconSpriteIndex"] = headIconSpriteIndex
            }
            111 -> values["isFollower"] = true
            103 -> values["rotation"] = buffer.readUnsignedShort()
            106, 118 -> readTransforms(buffer, opcode == 118)
            107 -> values["isInteractable"] = false
            109 -> values["isClickable"] = false
            114 -> values["runSequence"] = buffer.readUnsignedShort()
            115 -> {
                values["runSequence"] = buffer.readUnsignedShort()
                values["runBackSequence"] = buffer.readUnsignedShort()
                values["runRightSequence"] = buffer.readUnsignedShort()
                values["runLeftSequence"] = buffer.readUnsignedShort()
            }
            116 -> values["crawlSequence"] = buffer.readUnsignedShort()
            117 -> {
                values["crawlSequence"] = buffer.readUnsignedShort()
                values["crawlBackSequence"] = buffer.readUnsignedShort()
                values["crawlRightSequence"] = buffer.readUnsignedShort()
                values["crawlLeftSequence"] = buffer.readUnsignedShort()
            }
            122 -> values["lowPriorityFollowerOps"] = true
            123 -> values["isFollower"] = true
            124 -> values["height"] = buffer.readUnsignedShort()
            249 -> readParameters(buffer)
            else -> logger.info { "Unable to decode Npcs [${opcode}]" }
        }
    }

}