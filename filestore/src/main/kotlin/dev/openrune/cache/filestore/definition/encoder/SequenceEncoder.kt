package dev.openrune.cache.filestore.definition.encoder

import dev.openrune.cache.CacheManager
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.ConfigEncoder
import dev.openrune.cache.filestore.definition.data.SequenceType

class SequenceEncoder: ConfigEncoder<SequenceType>() {

    override fun Writer.encode(definition: SequenceType) {

        if (definition.frameIDs != null) {
            writeByte(1)
            writeShort(definition.frameIDs!!.size)
            for (i in 0 until definition.frameDelays!!.size) {
                writeShort(definition.frameDelays!![i])
            }
            for (i in 0 until definition.frameIDs!!.size) {
                writeShort(definition.frameIDs!![i])
            }
            for (i in 0 until definition.frameIDs!!.size) {
                writeShort(definition.frameIDs!![i] shr 16)
            }
        }

        if (definition.getFrameStep() != -1) {
            writeByte(2)
            writeShort(definition.getFrameStep())
        }

        if (definition.interleaveLeave != null) {
            writeByte(3)
            writeByte(definition.interleaveLeave!!.size)
            for (i in 0 until definition.interleaveLeave!!.size) {
                writeByte(definition.interleaveLeave!![i])
            }
        }

        if (definition.stretches) {
            writeByte(4)
        }

        if (definition.getForcedPriority() != 5) {
            writeByte(5)
            writeByte(definition.getForcedPriority())
        }

        if (definition.getLeftHandItem() != -1) {
            writeByte(6)
            writeShort(definition.getLeftHandItem())
        }

        if (definition.getRightHandItem() != -1) {
            writeByte(7)
            writeShort(definition.getRightHandItem())
        }

        if (definition.getMaxLoops() != 99) {
            writeByte(8)
            writeByte(definition.getMaxLoops())
        }

        if (definition.getPrecedenceAnimating() != -1) {
            writeByte(9)
            writeByte(definition.getPrecedenceAnimating())
        }

        if (definition.getPriority() != -1) {
            writeByte(10)
            writeByte(definition.getPriority())
        }

        if (definition.getReplyMode() != 2) {
            writeByte(11)
            writeByte(definition.getReplyMode())
        }

        if (definition.chatFrameIds != null) {
            writeByte(12)
            writeByte(definition.chatFrameIds!!.size)

            for (i in 0 until definition.chatFrameIds!!.size) {
                writeShort(definition.chatFrameIds!![i])
            }
            for (i in 0 until definition.chatFrameIds!!.size) {
                writeShort(definition.chatFrameIds!![i] shr 16)
            }

        }

        if (definition.soundEffects.isNotEmpty()) {
            writeByte(13)
            writeByte(definition.soundEffects.size)
            definition.soundEffects.forEach {
                it!!.writeSound(this, CacheManager.revisionIsOrAfter(220))
            }
        }

        if (definition.skeletalId != -1) {
            writeByte(14)
            writeInt(definition.skeletalId)
        }

        if (definition.skeletalSounds.isNotEmpty()) {
            writeByte(15)
            writeShort(definition.skeletalSounds.size)
            definition.skeletalSounds.forEach { (index, sound) ->
                writeShort(index)
                sound.writeSound(this, CacheManager.revisionIsOrAfter(220))
            }
        }

        if (definition.getRangeBegin() != 0 || definition.getRangeEnd() != 0) {
            writeByte(16)
            writeShort(definition.getRangeBegin())
            writeShort(definition.getRangeEnd())
        }

        if (definition.mask != null) {
            writeByte(17)

            writeByte(definition.mask!!.filter { it }.size)
            definition.mask!!.forEachIndexed { index, state ->
                if (state) {
                    writeByte(index)
                }
            }

        }

        writeByte(0)
    }


}