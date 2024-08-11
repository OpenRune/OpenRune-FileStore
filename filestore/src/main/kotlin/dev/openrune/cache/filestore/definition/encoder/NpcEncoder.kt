package dev.openrune.cache.filestore.definition.encoder

import dev.openrune.cache.CacheManager
import dev.openrune.cache.CacheManager.revisionIsOrAfter
import dev.openrune.cache.filestore.buffer.BufferWriter
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.ConfigEncoder
import dev.openrune.cache.filestore.definition.data.NpcType

class NpcEncoder : ConfigEncoder<NpcType>() {

    override fun Writer.encode(definition: NpcType) {
        if (definition.models.isNotEmpty()) {
            writeByte(1)
            writeByte(definition.models.size)
            for (i in definition.models.indices) {
                writeShort(definition.models[i].toInt())
            }
        }

        if (definition.name != "null") {
            writeByte(2)
            writeString(definition.name)
        }

        if (definition.getSize() != -1) {
            writeByte(12)
            writeByte(definition.getSize())
        }

        if (definition.getStandAnim() != -1) {
            writeByte(13)
            writeShort(definition.getStandAnim())
        }

        if (definition.getWalkAnim() != -1) {
            writeByte(14)
            writeShort(definition.getWalkAnim())
        }

        if (definition.getRotateLeftAnim() != -1) {
            writeByte(15)
            writeShort(definition.getRotateLeftAnim())
        }

        if (definition.getRotateRightAnim() != -1) {
            writeByte(16)
            writeShort(definition.getRotateRightAnim())
        }

        if (definition.getWalkAnim() != -1 || definition.getRotateBackAnim() != -1 || definition.getWalkLeftAnim() != -1 || definition.getWalkRightAnim() != -1) {
            writeByte(17)
            writeShort(definition.getWalkAnim())
            writeShort(definition.getRotateBackAnim())
            writeShort(definition.getWalkLeftAnim())
            writeShort(definition.getWalkRightAnim())
        }

        if (definition.getCategory() != -1) {
            writeByte(18)
            writeShort(definition.getCategory())
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

        if (definition.chatheadModels.isNotEmpty()) {
            writeByte(60)
            writeByte(definition.chatheadModels.size)
            for (i in definition.chatheadModels.indices) {
                writeShort(definition.chatheadModels[i].toInt())
            }
        }

        for (i in 0 .. 5) {
            if (definition.stats[i].toInt() != 1) {
                writeByte(74 + i)
                writeShort(definition.stats[i].toInt())
            }
        }

        if (!definition.isMinimapVisible) {
            writeByte(93)
        }
        if (definition.getContrast() != -1) {
            writeByte(95)
            writeShort(definition.getContrast())
        }


        writeByte(97)
        writeShort(definition.getWidthScale())

        writeByte(98)
        writeShort(definition.getHeightScale())


        if (definition.hasRenderPriority) {
            writeByte(99)
        }


        writeByte(100)
        writeByte(definition.getAmbient())

        writeByte(101)
        writeByte(definition.getContrast())

        if (definition.headIconSpriteIndex != null) {
            writeByte(102)
            if (CacheManager.revisionIsOrBefore(210)) {
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
        writeShort(definition.getRotation())

        definition.writeTransforms(this, 106, 118)

        if (!definition.isInteractable) {
            writeByte(107)
        }

        if (!definition.isClickable) {
            writeByte(109)
        }

        if (revisionIsOrAfter(220)) {
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

        if (definition.getRunSequence() != -1) {
            writeByte(114)
            writeShort(definition.getRunSequence())
        }

        if (definition.getRunSequence() != -1) {
            writeByte(115)
            writeShort(definition.getRunSequence())
            writeShort(definition.getRunBackSequence())
            writeShort(definition.getRunRightSequence()  )
            writeShort(definition.getRunLeftSequence())
        }

        if (definition.getCrawlSequence() != -1) {
            writeByte(116)
            writeShort(definition.getCrawlSequence())
        }

        if (definition.getCrawlSequence() != -1) {
            writeByte(117)
            writeShort(definition.getCrawlSequence())
            writeShort(definition.getCrawlBackSequence())
            writeShort(definition.getCrawlRightSequence())
            writeShort(definition.getCrawlLeftSequence())
        }

        if(definition.getHeight() != -1) {
            writeByte(124)
            writeShort(definition.getHeight())
        }

        definition.writeParameters(this)

        writeByte(0)
    }

}