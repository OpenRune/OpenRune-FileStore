package dev.openrune.cache.filestore.definition.encoder

import dev.openrune.cache.CacheManager
import dev.openrune.cache.CacheManager.revisionIsOrAfter
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.ConfigEncoder
import dev.openrune.cache.filestore.definition.data.NpcType
import dev.openrune.cache.filestore.definition.writeColoursTextures
import dev.openrune.cache.filestore.definition.writeParameters
import dev.openrune.cache.filestore.definition.writeTransforms

class NpcEncoder : ConfigEncoder<NpcType>() {

    override fun Writer.encode(definition: NpcType) {
        val models = definition.getModels()
        if (!models.isNullOrEmpty()) {
            writeByte(1)
            writeByte(models.size)
            models.forEach { writeShort(it) }
        }

        val name = definition.getName()
        if (name != "null") {
            writeByte(2)
            writeString(name)
        }

        val size = definition.getSize()
        if (size != -1) {
            writeByte(12)
            writeByte(size)
        }

        val standAnim = definition.getStandAnim()
        if (standAnim != -1) {
            writeByte(13)
            writeShort(standAnim)
        }

        val walkAnim = definition.getWalkAnim()
        if (walkAnim != -1) {
            writeByte(14)
            writeShort(walkAnim)
        }

        val rotateLeftAnim = definition.getRotateLeftAnim()
        if (rotateLeftAnim != -1) {
            writeByte(15)
            writeShort(rotateLeftAnim)
        }

        val rotateRightAnim = definition.getRotateRightAnim()
        if (rotateRightAnim != -1) {
            writeByte(16)
            writeShort(rotateRightAnim)
        }

        if (walkAnim != -1 || definition.getRotateBackAnim() != -1 || definition.getWalkLeftAnim() != -1 || definition.getWalkRightAnim() != -1) {
            writeByte(17)
            writeShort(walkAnim)
            writeShort(definition.getRotateBackAnim())
            writeShort(definition.getWalkLeftAnim())
            writeShort(definition.getWalkRightAnim())
        }

        val category = definition.getCategory()
        if (category != -1) {
            writeByte(18)
            writeShort(category)
        }

        val actions = definition.getActions()
        actions.forEachIndexed { i, action ->
            if (action != null) {
                writeByte(30 + i)
                writeString(action)
            }
        }


        definition.writeColoursTextures(this)

        val chatheadModels = definition.getChatheadModels()
        if (chatheadModels != null) {
            writeByte(60)
            writeByte(chatheadModels.size)
            chatheadModels.forEach { writeShort(it) }
        }

        val stats = definition.getStats()
        stats.forEachIndexed { i, stat ->
            if (stat != 1) {
                writeByte(74 + i)
                writeShort(stat)
            }
        }

        if (!definition.isMinimapVisible()) {
            writeByte(93)
        }

        val combatLevel = definition.getCombatLevel()
        if (combatLevel != -1) {
            writeByte(95)
            writeShort(combatLevel)
        }

        writeByte(97)
        writeShort(definition.getWidthScale())

        writeByte(98)
        writeShort(definition.getHeightScale())

        if (definition.hasRenderPriority()) {
            writeByte(99)
        }

        writeByte(100)
        writeByte(definition.getAmbient())

        writeByte(101)
        writeByte(definition.getContrast())

        val headIconSpriteIndex = definition.getHeadIconSpriteIndex()
        if (headIconSpriteIndex != null) {
            writeByte(102)
            if (CacheManager.revisionIsOrBefore(210)) {
                writeShort(headIconSpriteIndex.first())
            } else {
                val headIconArchiveIds = definition.getHeadIconArchiveIds()
                writeShort(headIconArchiveIds!!.size)
                repeat(headIconArchiveIds.size) {
                    writeShort(headIconArchiveIds[it])
                    writeShort(headIconSpriteIndex[it])
                }
            }
        }

        writeByte(103)
        writeShort(definition.getRotation())

        definition.writeTransforms(this, 106, 118)

        if (!definition.isInteractable()) {
            writeByte(107)
        }

        if (!definition.isClickable()) {
            writeByte(109)
        }

        if (revisionIsOrAfter(220)) {
            if (definition.hasLowPriorityFollowerOps()) {
                writeByte(122)
            }
            if (definition.isFollower()) {
                writeByte(123)
            }
        } else {
            if (definition.isFollower()) {
                writeByte(111)
            }
        }

        val runSequence = definition.getRunSequence()
        if (runSequence != -1) {
            writeByte(114)
            writeShort(runSequence)
        }

        if (runSequence != -1) {
            writeByte(115)
            writeShort(runSequence)
            writeShort(definition.getRunBackSequence())
            writeShort(definition.getRunRightSequence())
            writeShort(definition.getRunLeftSequence())
        }

        val crawlSequence = definition.getCrawlSequence()
        if (crawlSequence != -1) {
            writeByte(116)
            writeShort(crawlSequence)
        }

        if (crawlSequence != -1) {
            writeByte(117)
            writeShort(crawlSequence)
            writeShort(definition.getCrawlBackSequence())
            writeShort(definition.getCrawlRightSequence())
            writeShort(definition.getCrawlLeftSequence())
        }

        val height = definition.getHeight()
        if(height != -1) {
            writeByte(124)
            writeShort(height)
        }

        definition.writeParameters(this)

        writeByte(0)
    }

}