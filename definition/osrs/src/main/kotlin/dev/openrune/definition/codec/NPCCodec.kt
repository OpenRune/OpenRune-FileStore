package dev.openrune.definition.codec

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.definition.util.readShortSmart
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeString
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.revisionIsOrAfter
import dev.openrune.definition.revisionIsOrBefore
import dev.openrune.definition.type.NpcType
import io.netty.buffer.ByteBuf

class NPCCodec(private val revision: Int) : DefinitionCodec<NpcType> {
    override fun NpcType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> {
                val length = buffer.readUnsignedByte().toInt()
                models = MutableList(length) { 0 }
                for (count in 0 until length) {
                    models!![count] = buffer.readUnsignedShort()
                    if (models!![count] == 65535) {
                        models!![count] = -1
                    }
                }
            }

            2 -> name = buffer.readString()
            12 -> size = buffer.readUnsignedByte().toInt()
            13 -> standAnim = buffer.readUnsignedShort()
            14 -> walkAnim = buffer.readUnsignedShort()
            15 -> rotateLeftAnim = buffer.readUnsignedShort()
            16 -> rotateRightAnim = buffer.readUnsignedShort()
            17 -> {
                walkAnim = buffer.readUnsignedShort()
                rotateBackAnim = buffer.readUnsignedShort()
                walkLeftAnim = buffer.readUnsignedShort()
                walkRightAnim = buffer.readUnsignedShort()
            }

            18 -> category = buffer.readUnsignedShort()
            in 30..34 -> {
                actions[opcode - 30] = buffer.readString()
                if (actions[opcode - 30].equals("Hidden", true)) {
                    actions[opcode - 30] = null
                }
            }

            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            60 -> {
                val length: Int = buffer.readUnsignedByte().toInt()
                chatheadModels = MutableList(length) { 0 }
                (0 until length).forEach {
                    chatheadModels!![it] = buffer.readUnsignedShort()
                }
            }
            74 -> attack = buffer.readUnsignedShort()
            75 -> defence = buffer.readUnsignedShort()
            76 -> strength = buffer.readUnsignedShort()
            77 -> hitpoints = buffer.readUnsignedShort()
            78 -> ranged = buffer.readUnsignedShort()
            79 -> magic = buffer.readUnsignedShort()
            93 -> isMinimapVisible = false
            95 -> combatLevel = buffer.readUnsignedShort()
            97 -> widthScale = buffer.readUnsignedShort()
            98 -> heightScale = buffer.readUnsignedShort()
            99 -> hasRenderPriority = true
            100 -> ambient = buffer.readByte().toInt()
            101 -> contrast = buffer.readByte().toInt()
            102 -> {
                if (revisionIsOrBefore(revision, 210)) {
                    headIconArchiveIds = MutableList(0) { 0 }
                    headIconSpriteIndex = MutableList(buffer.readUnsignedShort()) { 0 }
                } else {
                    val bitfield = buffer.readUnsignedByte().toInt()
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
                            headIconArchiveIds!![i] = -1
                            headIconSpriteIndex!![i] = -1
                        } else {
                            headIconArchiveIds!![i] = buffer.readUnsignedShort()
                            headIconSpriteIndex!![i] = buffer.readShortSmart() - 1
                        }
                    }
                }
            }

            111 -> isFollower = true
            103 -> rotation = buffer.readUnsignedShort()
            106, 118 -> readTransforms(buffer, opcode == 118)
            107 -> isInteractable = false
            109 -> isClickable = false
            114 -> runSequence = buffer.readUnsignedShort()
            115 -> {
                runSequence = buffer.readUnsignedShort()
                runBackSequence = buffer.readUnsignedShort()
                runRightSequence = buffer.readUnsignedShort()
                runLeftSequence = buffer.readUnsignedShort()
            }

            116 -> crawlSequence = buffer.readUnsignedShort()
            117 -> {
                crawlSequence = buffer.readUnsignedShort()
                crawlBackSequence = buffer.readUnsignedShort()
                crawlRightSequence = buffer.readUnsignedShort()
                crawlLeftSequence = buffer.readUnsignedShort()
            }

            122 -> lowPriorityFollowerOps = true
            123 -> isFollower = true
            124 -> height = buffer.readUnsignedShort()
            249 -> readParameters(buffer)
            else -> dev.openrune.definition.codec.NPCCodec.logger.info { "Unable to decode Npcs [${opcode}]" }
        }
    }

    override fun ByteBuf.encode(definition: NpcType) {
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
        if(definition.attack != 1) {
            writeByte(74)
            writeShort(definition.attack)
        }
        if(definition.defence != 1) {
            writeByte(75)
            writeShort(definition.defence)
        }
        if(definition.strength != 1) {
            writeByte(76)
            writeShort(definition.strength)
        }
        if(definition.hitpoints != 1) {
            writeByte(77)
            writeShort(definition.hitpoints)
        }
        if(definition.ranged != 1) {
            writeByte(78)
            writeShort(definition.ranged)
        }
        if(definition.magic != 1) {
            writeByte(79)
            writeShort(definition.magic)
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
        internal val logger = InlineLogger()
    }
}