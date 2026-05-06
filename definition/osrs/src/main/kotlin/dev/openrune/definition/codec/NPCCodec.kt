package dev.openrune.definition.codec

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.EntityOpsLoader
import dev.openrune.definition.revisionIsOrAfter
import dev.openrune.definition.revisionIsOrBefore
import dev.openrune.definition.type.BgSound
import dev.openrune.definition.type.BgSoundFade
import dev.openrune.definition.type.NpcType
import dev.openrune.definition.type.RandomSound
import dev.openrune.definition.util.*
import io.netty.buffer.ByteBuf

class NPCCodec(private val revision: Int) : DefinitionCodec<NpcType> {
    private val entityOpsLoader = EntityOpsLoader(revision)

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
            in 30..34 -> entityOpsLoader.decodeBaseOp(actions, buffer, opcode - 30)
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            60 -> {
                val length: Int = buffer.readUnsignedByte().toInt()
                chatheadModels = MutableList(length) { 0 }
                (0 until length).forEach {
                    chatheadModels!![it] = buffer.readUnsignedShort()
                }
            }
            61 -> {
                val length: Int = buffer.readUnsignedByte().toInt()
                models = MutableList(length) { 0 }
                (0 until length).forEach {
                    models!![it] = buffer.readInt()
                }
            }
            62 -> {
                val length: Int = buffer.readUnsignedByte().toInt()
                chatheadModels = MutableList(length) { 0 }
                (0 until length).forEach {
                    chatheadModels!![it] = buffer.readInt()
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
            99 -> renderPriority = 1
            100 -> ambient = buffer.readByte().toInt()
            101 -> contrast = buffer.readByte().toInt()
            102 -> {
                if (revisionIsOrBefore(revision, 210)) {
                    headIconGraphics = mutableListOf(0)
                    headIconIndexes = mutableListOf(buffer.readUnsignedShort())
                } else {

                    val bits = buffer.readUnsignedByte().toInt()
                    val length = 32 - Integer.numberOfLeadingZeros(bits)
                    val iconGroups = MutableList(length) { 0 }
                    val iconIndexes = MutableList(length) { 0 }

                    for (index in 0 until length) {
                        if ((bits and (1 shl index)) == 0) {
                            iconGroups[index] = -1
                            iconIndexes[index] = -1
                        } else {
                            iconGroups[index] = buffer.readNullableLargeSmart()
                            iconIndexes[index] = buffer.readShortSmartSub()
                        }
                    }

                    this.headIconGraphics = iconGroups
                    this.headIconIndexes = iconIndexes

                }
            }

            111 -> if (revisionIsOrBefore(revision,232)) {
                isFollower = true
            } else {
                renderPriority = 2
            }
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
            126 -> footprintSize = buffer.readUnsignedShort()
            130 -> readyAnimDuringAnim = true
            145 -> canHideForOverlap = true
            146 -> overlapTintHSL = buffer.readUnsignedShort()
            147 -> zbuf = false
            148 -> {
                bgSound = BgSound(
                    id = buffer.readUnsignedShort(),
                    range = buffer.readUnsignedByte().toInt(),
                    volume = buffer.readUnsignedByte().toInt()
                )
            }

            149 -> {
                val fade = bgSoundFade ?: BgSoundFade()
                fade.dropoffEasing = buffer.readUnsignedByte().toInt()
                bgSoundFade = fade
            }

            150 -> {
                val fade = bgSoundFade ?: BgSoundFade()
                fade.easeInType = buffer.readUnsignedByte().toInt()
                fade.easeInDuration = buffer.readUnsignedShort()
                fade.easeOutType = buffer.readUnsignedByte().toInt()
                fade.easeOutDuration = buffer.readUnsignedShort()
                bgSoundFade = fade
            }

            151 -> crossWorldSound = buffer.readUnsignedByte().toInt()

            152 -> {
                randomSound = RandomSound(
                    minDelay = buffer.readUnsignedShort(),
                    maxDelay = buffer.readUnsignedShort(),
                    minVolume = buffer.readUnsignedByte().toInt(),
                    maxVolume = buffer.readUnsignedByte().toInt(),
                    soundIds = MutableList(buffer.readUnsignedByte().toInt()) {
                        buffer.readUnsignedShort()
                    }
                )
            }
            249 -> readParameters(buffer)
            else -> logger.info { "Unable to decode Npcs [${opcode}]" }
        }
    }

    override fun ByteBuf.encode(definition: NpcType) {
        if (definition.models != null && definition.models!!.isNotEmpty()) {
            if (entityOpsLoader.supportsExtendedEntityOps()) {
                writeByte(61)
                writeByte(definition.models!!.size)
                for (i in definition.models!!.indices) {
                    writeInt(definition.models!![i])
                }
            } else {
                writeByte(1)
                writeByte(definition.models!!.size)
                for (i in definition.models!!.indices) {
                    writeShort(definition.models!![i])
                }
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

        if (definition.walkAnim != -1 && definition.rotateBackAnim != -1 && definition.walkLeftAnim != -1 && definition.walkRightAnim != -1) {
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

        definition.actions.ops.forEachIndexed { index, op ->
            entityOpsLoader.encodeBaseOp(this, index, op)
        }

        definition.writeColoursTextures(this)

        if (definition.chatheadModels != null) {
            if (entityOpsLoader.supportsExtendedEntityOps()) {
                writeByte(62)
                writeByte(definition.chatheadModels!!.size)
                for (i in definition.chatheadModels!!.indices) {
                    writeInt(definition.chatheadModels!![i])
                }
            } else {
                writeByte(60)
                writeByte(definition.chatheadModels!!.size)
                for (i in definition.chatheadModels!!.indices) {
                    writeShort(definition.chatheadModels!![i])
                }
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


        if (definition.renderPriority != 0 && revision < 223) {
            writeByte(99)
        }

        writeByte(100)
        writeByte(definition.ambient)

        writeByte(101)
        writeByte(definition.contrast)

        if (definition.headIconIndexes != null) {
            writeByte(102)

            if (revisionIsOrBefore(revision, 210)) {
                writeShort(definition.headIconIndexes!!.first().toInt())
            } else {
                val iconGraphics = checkNotNull(definition.headIconGraphics)
                val iconIndexes = checkNotNull(definition.headIconIndexes)

                check(iconGraphics.size == iconIndexes.size)

                val bitsPlaceholderPos = writerIndex()
                writeByte(0)

                var bits = 0
                for (i in iconGraphics.indices) {
                    if (iconGraphics[i] >= 0 && iconIndexes[i] >= 0) {
                        writeNullableLargeSmartCorrect(iconGraphics[i])
                        writeSmart(iconIndexes[i] + 1)
                        bits = bits or (1 shl i)
                    }
                }

                // update bits value
                setByte(bitsPlaceholderPos, bits)
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
            if(revisionIsOrBefore(revision,232)) {
                if (definition.isFollower) {
                    writeByte(111)
                }
            } else {
                if (definition.renderPriority == 2) {
                    writeByte(111)
                }
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

        if (definition.footprintSize != -1) {
            writeByte(126)
            writeShort(definition.footprintSize)
        }

        if (definition.canHideForOverlap) {
            writeByte(145)
        }


        if (definition.overlapTintHSL != 39188) {
            writeByte(146)
            writeShort(definition.overlapTintHSL)
        }

        if (!definition.zbuf) {
            writeByte(147)
        }

        definition.bgSound?.let {
            if (it.id != -1) {
                writeByte(148)
                writeShort(it.id)
                writeByte(it.range)
                writeByte(it.volume)
            }
        }

        definition.bgSoundFade?.let {
            if (it.dropoffEasing != 0) {
                writeByte(149)
                writeByte(it.dropoffEasing)
            }

            if (it.easeInDuration != 0 || it.easeOutDuration != 0) {
                writeByte(150)
                writeByte(it.easeInType)
                writeShort(it.easeInDuration)
                writeByte(it.easeOutType)
                writeShort(it.easeOutDuration)
            }
        }

        if (definition.crossWorldSound != 2) {
            writeByte(151)
            writeByte(definition.crossWorldSound)
        }

        definition.randomSound?.let {
            if (it.soundIds.isNotEmpty()) {
                writeByte(152)
                writeShort(it.minDelay)
                writeShort(it.maxDelay)
                writeByte(it.minVolume)
                writeByte(it.maxVolume)

                writeByte(it.soundIds.size)
                it.soundIds.forEach { id -> writeShort(id) }
            }
        }

        definition.writeParameters(this)

        writeByte(0)
    }

    override fun createDefinition() = NpcType()

    companion object {
        internal val logger = InlineLogger()
    }
}