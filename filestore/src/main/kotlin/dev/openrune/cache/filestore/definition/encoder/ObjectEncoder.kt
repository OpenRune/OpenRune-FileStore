package dev.openrune.cache.filestore.definition.encoder

import dev.openrune.cache.CacheManager
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.ConfigEncoder
import dev.openrune.cache.filestore.definition.data.ObjectType

class ObjectEncoder : ConfigEncoder<ObjectType>() {

    override fun Writer.encode(definition: ObjectType) {
        if (definition.objectModels != null) {
            if (definition.objectTypes != null) {
                writeByte(1)
                writeByte(definition.objectModels!!.size)
                if (definition.objectModels!!.isNotEmpty()) {
                    for (i in 0 until definition.objectModels!!.size) {
                        writeShort(definition.objectModels!![i])
                        writeByte(definition.objectTypes!![i])
                    }
                }
            } else {
                writeByte(5)
                writeByte(definition.objectModels!!.size)
                if (definition.objectModels!!.isNotEmpty()) {
                    for (i in 0 until definition.objectModels!!.size) {
                        writeShort(definition.objectModels!![i])
                    }
                }
            }
        }

        if (definition.name != "null") {
            writeByte(2)
            writeString(definition.name)
        }


        writeByte(14)
        writeByte(definition.getSizeX())

        writeByte(15)
        writeByte(definition.getSizeY())


        if (definition.solid == 0 && !definition.impenetrable) {
            writeByte(17)
        }

        if (!definition.impenetrable) {
            writeByte(18)
        }

        if (definition.getInteractive() != -1) {
            writeByte(19)
            writeByte(definition.getInteractive())
        }

        if (definition.getClipType() == 0) {
            writeByte(21)
        }

        if (definition.nonFlatShading) {
            writeByte(22)
        }

        if (definition.modelClipped) {
            writeByte(23)
        }

        if (definition.getAnimationId() != -1) {
            writeByte(24)
            writeShort(definition.getAnimationId())
        }

        if (definition.solid == 1) {
            writeByte(27)
        }

        writeByte(28)
        writeByte(definition.getDecorDisplacement())

        writeByte(29)
        writeByte(definition.ambient)

        writeByte(39)
        writeByte(definition.contrast / 25)


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

        if (definition.getCategory() != -1) {
            writeByte(61)
            writeShort(definition.getCategory())
        }

        if (definition.isRotated) {
            writeByte(62)
        }

        if (!definition.clipped) {
            writeByte(64)
        }

        writeByte(65)
        writeShort(definition.getModelSizeX())

        writeByte(66)
        writeShort(definition.getModelSizeZ())

        writeByte(67)
        writeShort(definition.getModelSizeY())

        if (definition.getMapSceneID() != -1) {
            writeByte(68)
            writeShort(definition.getMapSceneID())
        }

        if (definition.clipMask != 0) {
            writeByte(69)
            writeByte(definition.clipMask)
        }

        writeByte(70)
        writeShort(definition.getOffsetX())

        writeByte(71)
        writeShort(definition.getOffsetZ())

        writeByte(72)
        writeShort(definition.getOffsetY())

        if (definition.obstructive) {
            writeByte(73)
        }

        if (definition.isHollow) {
            writeByte(74)
        }

        if (definition.getSupportsItems() != -1) {
            writeByte(75)
            writeByte(definition.getSupportsItems())
        }

        if (definition.getAmbientSoundId() != -1) {
            writeByte(78)
            writeShort(definition.getAmbientSoundId())
            writeByte(definition.getSoundDistance())
            if (CacheManager.revisionIsOrAfter(220)) {
                writeByte(definition.getSoundRetain())
            }
        }

        if (definition.ambientSoundIds != null) {
            writeByte(79)
            writeShort(definition.getSoundMin())
            writeShort(definition.getSoundMax())
            writeByte(definition.getSoundDistance())
            if (CacheManager.revisionIsOrAfter(220)) {
                writeByte(definition.getSoundRetain())
            }

            writeByte(definition.ambientSoundIds!!.size)
            for (i in definition.ambientSoundIds!!.indices) {
                writeShort(definition.ambientSoundIds!![i])
            }
        }

        if (definition.getClipType() != -1) {
            writeByte(81)
            writeByte(definition.getClipType() / 256)
        }

        if (definition.getMapAreaId() != -1) {
            writeByte(82)
            writeShort(definition.getMapAreaId())
        }

        if (!definition.randomizeAnimStart) {
            writeByte(89)
        }

        if (definition.delayAnimationUpdate) {
            writeByte(90)
        }

        definition.writeTransforms(this, 77,92)
        definition.writeParameters(this)

        writeByte(0)
    }

}