package dev.openrune.codec

import dev.openrune.cache.CacheManager
import dev.openrune.cache.CacheManager.revisionIsOrAfter
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.data.ObjectType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.stream.IntStream
import kotlin.streams.toList

class ObjectCodec(private val revision: Int) : DefinitionCodec<ObjectType> {

    override fun ObjectType.read(opcode: Int, buffer: Reader) {
        when (opcode) {
            1 -> {
                val length: Int = buffer.readUnsignedByte()
                when {
                    length > 0 -> {
                        objectTypes = MutableList(length) { 0 }
                        objectModels = MutableList(length) { 0 }

                        (0 until length).forEach {
                            objectModels!![it] = buffer.readUnsignedShort()
                            objectTypes!![it] = buffer.readUnsignedByte()
                        }
                    }
                }
            }
            2 -> name = buffer.readString()
            5 -> {
                val length: Int = buffer.readUnsignedByte()
                when {
                    length > 0 -> {
                        objectTypes = null
                        objectModels = IntStream.range(0, length).map {
                            buffer.readUnsignedShort()
                        }.toList().toMutableList()
                    }
                }
            }
            14 -> sizeX = buffer.readUnsignedByte()
            15 -> sizeY = buffer.readUnsignedByte()
            17 -> {
                solid = 0
                impenetrable = false
            }
            18 -> impenetrable = false
            19 -> interactive = buffer.readUnsignedByte()
            21 -> clipType = 0
            22 -> nonFlatShading = true
            23 -> modelClipped = true
            24 -> {
                animationId = buffer.readUnsignedShort()
                if (animationId == 65535) {
                    animationId = -1
                }
            }
            27 -> solid = 1
            28 -> decorDisplacement = buffer.readUnsignedByte()
            29 -> ambient = buffer.readByte()
            39 -> contrast = buffer.readByte()
            in 30..34 -> {
                actions[opcode - 30] = buffer.readString()
            }
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            61 -> category = buffer.readUnsignedShort()
            62 -> isRotated = true
            64 -> clipped = false
            65 -> modelSizeX = buffer.readUnsignedShort()
            66 -> modelSizeZ = buffer.readUnsignedShort()
            67 -> modelSizeY = buffer.readUnsignedShort()
            68 -> mapSceneID = buffer.readUnsignedShort()
            69 -> clipMask = buffer.readByte()
            70 -> offsetX = buffer.readUnsignedShort()
            71 -> offsetZ = buffer.readUnsignedShort()
            72 -> offsetY = buffer.readUnsignedShort()
            73 -> obstructive = true
            74 -> isHollow = true
            75 -> supportsItems = buffer.readUnsignedByte()
            77, 92 -> readTransforms(buffer, opcode == 92)
            78 -> {
                ambientSoundId = buffer.readUnsignedShort()
                soundDistance = buffer.readUnsignedByte()
                if (revisionIsOrAfter(revision, 220)) {
                    soundRetain = buffer.readUnsignedByte()
                }
            }
            79 -> {
                soundMin = buffer.readUnsignedShort()
                soundMax = buffer.readUnsignedShort()
                soundDistance = buffer.readUnsignedByte()
                if (revisionIsOrAfter(revision, 220)) {
                    soundRetain = buffer.readUnsignedByte()
                }
                val length: Int = buffer.readUnsignedByte()
                ambientSoundIds = IntStream.range(0, length).map {
                    buffer.readUnsignedShort()
                }.toList().toMutableList()
            }
            81 -> clipType = (buffer.readUnsignedByte()) * 256
            60,82 -> mapAreaId = buffer.readUnsignedShort()
            89 -> randomizeAnimStart = true
            90 -> delayAnimationUpdate = true
            249 -> readParameters(buffer)
            else -> logger.info { "Unable to decode Object [${opcode}]" }
        }
    }

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

        if (definition.name != null) {
            writeByte(2)
            writeString(definition.name)
        }


        writeByte(14)
        writeByte(definition.sizeX)

        writeByte(15)
        writeByte(definition.sizeY)


        if (definition.solid == 0 && !definition.impenetrable) {
            writeByte(17)
        }

        if (!definition.impenetrable) {
            writeByte(18)
        }

        if (definition.interactive != -1) {
            writeByte(19)
            writeByte(definition.interactive)
        }

        if (definition.clipType == 0) {
            writeByte(21)
        }

        if (definition.nonFlatShading) {
            writeByte(22)
        }

        if (definition.modelClipped) {
            writeByte(23)
        }

        if (definition.animationId != -1) {
            writeByte(24)
            writeShort(definition.animationId)
        }

        if (definition.solid == 1) {
            writeByte(27)
        }

        writeByte(28)
        writeByte(definition.decorDisplacement)

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

        if (definition.category != -1) {
            writeByte(61)
            writeShort(definition.category)
        }

        if (definition.isRotated) {
            writeByte(62)
        }

        if (!definition.clipped) {
            writeByte(64)
        }

        writeByte(65)
        writeShort(definition.modelSizeX)

        writeByte(66)
        writeShort(definition.modelSizeZ)

        writeByte(67)
        writeShort(definition.modelSizeY)

        if (definition.mapSceneID != -1) {
            writeByte(68)
            writeShort(definition.mapSceneID)
        }

        if (definition.clipMask != 0) {
            writeByte(69)
            writeByte(definition.clipMask)
        }

        writeByte(70)
        writeShort(definition.offsetX)

        writeByte(71)
        writeShort(definition.offsetZ)

        writeByte(72)
        writeShort(definition.offsetY)

        if (definition.obstructive) {
            writeByte(73)
        }

        if (definition.isHollow) {
            writeByte(74)
        }

        if (definition.supportsItems != -1) {
            writeByte(75)
            writeByte(definition.supportsItems)
        }

        if (definition.ambientSoundId != -1) {
            writeByte(78)
            writeShort(definition.ambientSoundId)
            writeByte(definition.soundDistance)
            if (CacheManager.revisionIsOrAfter(revision, 220)) {
                writeByte(definition.soundRetain)
            }
        }

        if (definition.ambientSoundIds != null) {
            writeByte(79)
            writeShort(definition.soundMin)
            writeShort(definition.soundMax)
            writeByte(definition.soundDistance)
            if (CacheManager.revisionIsOrAfter(revision, 220)) {
                writeByte(definition.soundRetain)
            }

            writeByte(definition.ambientSoundIds!!.size)
            for (i in definition.ambientSoundIds!!.indices) {
                writeShort(definition.ambientSoundIds!![i])
            }
        }

        if (definition.clipType != -1) {
            writeByte(81)
            writeByte(definition.clipType / 256)
        }

        if (definition.mapAreaId != -1) {
            writeByte(82)
            writeShort(definition.mapAreaId)
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

    companion object {
        internal val logger = KotlinLogging.logger {}
    }
}