package dev.openrune.definition.codec

import dev.openrune.buffer.*
import io.netty.buffer.ByteBuf
import dev.openrune.buffer.Writer
import dev.openrune.buffer.readStringRD
import dev.openrune.buffer.readUnsignedShortRD
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.revisionIsOrAfter
import dev.openrune.definition.revisionIsOrBefore
import dev.openrune.definition.type.NpcType
import io.github.oshai.kotlinlogging.KotlinLogging

class NPCCodec(private val revision: Int) : DefinitionCodec<NpcType> {
    override fun NpcType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> {
                val length = buffer.readUnsignedByteRD()
                models = MutableList(length) { 0 }
                for (count in 0 until length) {
                    models!![count] = buffer.readUnsignedShortRD()
                    if (models!![count] == 65535) {
                        models!![count] = -1
                    }
                }
            }
            2 -> name = buffer.readStringRD()
            12 -> size = buffer.readUnsignedByteRD()
            13 -> standAnim = buffer.readUnsignedShortRD()
            14 -> walkAnim = buffer.readUnsignedShortRD()
            15 -> rotateLeftAnim = buffer.readUnsignedShortRD()
            16 -> rotateRightAnim = buffer.readUnsignedShortRD()
            17 -> {
                walkAnim = buffer.readUnsignedShortRD()
                rotateBackAnim = buffer.readUnsignedShortRD()
                walkLeftAnim = buffer.readUnsignedShortRD()
                walkRightAnim = buffer.readUnsignedShortRD()
            }
            18 -> category = buffer.readUnsignedShortRD()
            in 30..34 -> {
                actions[opcode - 30] = buffer.readStringRD()
                if (actions[opcode - 30].equals("Hidden", true)) {
                    actions[opcode - 30] = null
                }
            }
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            60 -> {
                val length: Int = buffer.readUnsignedByteRD()
                chatheadModels = MutableList(length) { 0 }
                (0 until length).forEach {
                    chatheadModels!![it] = buffer.readUnsignedShortRD()
                }
            }
            in 74..79 -> stats[opcode - 74] = buffer.readUnsignedShortRD()
            93 -> isMinimapVisible = false
            95 -> combatLevel = buffer.readUnsignedShortRD()
            97 -> widthScale = buffer.readUnsignedShortRD()
            98 -> heightScale = buffer.readUnsignedShortRD()
            99 -> hasRenderPriority = true
            100 -> ambient = buffer.readByteRD()
            101 -> contrast = buffer.readByteRD()
            102 -> {
                if (revisionIsOrBefore(revision, 210)) {
                    headIconArchiveIds = MutableList(0) { 0 }
                    headIconSpriteIndex = MutableList(buffer.readUnsignedShortRD()) { 0 }
                } else {
                    val bitfield = buffer.readUnsignedByteRD()
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
                            headIconArchiveIds!![i] = -1
                            headIconSpriteIndex!![i] = -1
                        } else {
                            headIconArchiveIds!![i] = buffer.readUnsignedShortRD()
                            headIconSpriteIndex!![i] = buffer.readShortSmartRD() - 1
                        }
                    }
                }
            }
            111 -> isFollower = true
            103 -> rotation = buffer.readUnsignedShortRD()
            106, 118 -> readTransforms(buffer, opcode == 118)
            107 -> isInteractable = false
            109 -> isClickable = false
            114 -> runSequence = buffer.readUnsignedShortRD()
            115 -> {
                runSequence = buffer.readUnsignedShortRD()
                runBackSequence = buffer.readUnsignedShortRD()
                runRightSequence = buffer.readUnsignedShortRD()
                runLeftSequence = buffer.readUnsignedShortRD()
            }
            116 -> crawlSequence = buffer.readUnsignedShortRD()
            117 -> {
                crawlSequence = buffer.readUnsignedShortRD()
                crawlBackSequence = buffer.readUnsignedShortRD()
                crawlRightSequence = buffer.readUnsignedShortRD()
                crawlLeftSequence = buffer.readUnsignedShortRD()
            }
            122 -> lowPriorityFollowerOps = true
            123 -> isFollower = true
            124 -> height = buffer.readUnsignedShortRD()
            249 -> readParameters(buffer)
            else -> dev.openrune.definition.codec.NPCCodec.logger.info { "Unable to decode Npcs [${opcode}]" }
        }
    }

    override fun Writer.encode(definition: NpcType) {
        if (definition.models != null && definition.models!!.isNotEmpty()) {
            writeByte(1)
            writeByte(definition.models!!.size)
            for (i in definition.models!!.indices) {
                writeShort(definition.models!![i])
            }
        }

        if (definition.name != "null") {
            writeByte(2)
            writeString(definition.name)
        }

        if (definition.size != -1) {
            writeByte(12)
            writeByte(definition.size)
        }

        if (definition.standAnim != -1) {
            writeByte(13)
            writeShort(definition.standAnim)
        }

        if (definition.walkAnim != -1) {
            writeByte(14)
            writeShort(definition.walkAnim)
        }

        if (definition.rotateLeftAnim != -1) {
            writeByte(15)
            writeShort(definition.rotateLeftAnim)
        }

        if (definition.rotateRightAnim != -1) {
            writeByte(16)
            writeShort(definition.rotateRightAnim)
        }

        if (definition.walkAnim != -1 || definition.rotateBackAnim != -1 || definition.walkLeftAnim != -1 || definition.walkRightAnim != -1) {
            writeByte(17)
            writeShort(definition.walkAnim)
            writeShort(definition.rotateBackAnim)
            writeShort(definition.walkLeftAnim)
            writeShort(definition.walkRightAnim)
        }

        if (definition.category != -1) {
            writeByte(18)
            writeShort(definition.category)
        }

        if (definition.actions.any { it != null }) {
            for (i in 0 until definition.actions.size) {
                if (definition.actions[i] == null) {
                    continue
                }
                writeByte(30 + i)
                writeString(definition.actions[i]!!)
            }
        }

        definition.writeColoursTextures(this)

        if (definition.chatheadModels != null) {
            writeByte(60)
            writeByte(definition.chatheadModels!!.size)
            for (i in definition.chatheadModels!!.indices) {
                writeShort(definition.chatheadModels!![i])
            }
        }

        for (i in 0 .. 5) {
            if (definition.stats[i] != 1) {
                writeByte(74 + i)
                writeShort(definition.stats[i])
            }
        }

        if (!definition.isMinimapVisible) {
            writeByte(93)
        }
        if (definition.combatLevel != -1) {
            writeByte(95)
            writeShort(definition.combatLevel)
        }


        writeByte(97)
        writeShort(definition.widthScale)

        writeByte(98)
        writeShort(definition.heightScale)


        if (definition.hasRenderPriority) {
            writeByte(99)
        }


        writeByte(100)
        writeByte(definition.ambient)

        writeByte(101)
        writeByte(definition.contrast)

        if (definition.headIconSpriteIndex != null) {
            writeByte(102)
            if (revisionIsOrBefore(revision, 210)) {
                writeShort(definition.headIconSpriteIndex!!.first())
            } else {
                writeShort(definition.headIconArchiveIds!!.size)
                repeat(definition.headIconArchiveIds!!.size) {
                    writeShort(definition.headIconArchiveIds!![it])
                    writeShort(definition.headIconSpriteIndex!![it])
                }
            }
        }

        writeByte(103)
        writeShort(definition.rotation)

        definition.writeTransforms(this, 106, 118)

        if (!definition.isInteractable) {
            writeByte(107)
        }

        if (!definition.isClickable) {
            writeByte(109)
        }

        if (revisionIsOrAfter(revision, 220)) {
            if (definition.lowPriorityFollowerOps) {
                writeByte(122)
            }
            if (definition.isFollower) {
                writeByte(123)
            }
        } else {
            if (definition.isFollower) {
                writeByte(111)
            }
        }

        if (definition.runSequence != -1) {
            writeByte(114)
            writeShort(definition.runSequence)
        }

        if (definition.runSequence != -1) {
            writeByte(115)
            writeShort(definition.runSequence)
            writeShort(definition.runBackSequence)
            writeShort(definition.runRightSequence)
            writeShort(definition.runLeftSequence)
        }

        if (definition.crawlSequence != -1) {
            writeByte(116)
            writeShort(definition.crawlSequence)
        }

        if (definition.crawlSequence != -1) {
            writeByte(117)
            writeShort(definition.crawlSequence)
            writeShort(definition.crawlBackSequence)
            writeShort(definition.crawlRightSequence)
            writeShort(definition.crawlLeftSequence)
        }

        if(definition.height != -1) {
            writeByte(124)
            writeShort(definition.height)
        }

        definition.writeParameters(this)

        writeByte(0)
    }

    override fun createDefinition() = NpcType()

    companion object {
        internal val logger = KotlinLogging.logger {}
    }
}