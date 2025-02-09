package dev.openrune.definition.codec

import dev.openrune.buffer.*
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.revisionIsOrAfter
import dev.openrune.definition.type.ObjectType
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
import java.util.stream.IntStream
import kotlin.streams.toList

class ObjectCodec(private val revision: Int) : DefinitionCodec<ObjectType> {

    override fun dev.openrune.definition.type.ObjectType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> {
                val length: Int = buffer.readUnsignedByteRD()
                when {
                    length > 0 -> {
                        objectTypes = MutableList(length) { 0 }
                        objectModels = MutableList(length) { 0 }

                        (0 until length).forEach {
                            objectModels!![it] = buffer.readUnsignedShortRD()
                            objectTypes!![it] = buffer.readUnsignedByteRD()
                        }
                    }
                }
            }
            2 -> name = buffer.readStringRD()
            5 -> {
                val length: Int = buffer.readUnsignedByteRD()
                when {
                    length > 0 -> {
                        objectTypes = null
                        objectModels = IntStream.range(0, length).map {
                            buffer.readUnsignedShortRD()
                        }.toList().toMutableList()
                    }
                }
            }
            14 -> sizeX = buffer.readUnsignedByteRD()
            15 -> sizeY = buffer.readUnsignedByteRD()
            17 -> {
                solid = 0
                impenetrable = false
            }
            18 -> impenetrable = false
            19 -> interactive = buffer.readUnsignedByteRD()
            21 -> clipType = 0
            22 -> nonFlatShading = true
            23 -> modelClipped = true
            24 -> {
                animationId = buffer.readUnsignedShortRD()
                if (animationId == 65535) {
                    animationId = -1
                }
            }
            27 -> solid = 1
            28 -> decorDisplacement = buffer.readUnsignedByteRD()
            29 -> ambient = buffer.readByteRD()
            39 -> contrast = buffer.readByteRD()
            in 30..34 -> {
                actions[opcode - 30] = buffer.readStringRD()
            }
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            61 -> category = buffer.readUnsignedShortRD()
            62 -> isRotated = true
            64 -> clipped = false
            65 -> modelSizeX = buffer.readUnsignedShortRD()
            66 -> modelSizeZ = buffer.readUnsignedShortRD()
            67 -> modelSizeY = buffer.readUnsignedShortRD()
            68 -> mapSceneID = buffer.readUnsignedShortRD()
            69 -> clipMask = buffer.readByteRD()
            70 -> offsetX = buffer.readUnsignedShortRD()
            71 -> offsetZ = buffer.readUnsignedShortRD()
            72 -> offsetY = buffer.readUnsignedShortRD()
            73 -> obstructive = true
            74 -> isHollow = true
            75 -> supportsItems = buffer.readUnsignedByteRD()
            77, 92 -> readTransforms(buffer, opcode == 92)
            78 -> {
                ambientSoundId = buffer.readUnsignedShortRD()
                soundDistance = buffer.readUnsignedByteRD()
                if (revisionIsOrAfter(revision, 220)) {
                    soundRetain = buffer.readUnsignedByteRD()
                }
            }
            79 -> {
                soundMin = buffer.readUnsignedShortRD()
                soundMax = buffer.readUnsignedShortRD()
                soundDistance = buffer.readUnsignedByteRD()
                if (revisionIsOrAfter(revision, 220)) {
                    soundRetain = buffer.readUnsignedByteRD()
                }
                val length: Int = buffer.readUnsignedByteRD()
                ambientSoundIds = IntStream.range(0, length).map {
                    buffer.readUnsignedShortRD()
                }.toList().toMutableList()
            }
            81 -> clipType = (buffer.readUnsignedByteRD()) * 256
            60,82 -> mapAreaId = buffer.readUnsignedShortRD()
            89 -> randomizeAnimStart = true
            90 -> delayAnimationUpdate = true
            249 -> readParameters(buffer)
            else -> dev.openrune.definition.codec.ObjectCodec.logger.info { "Unable to decode Object [${opcode}]" }
        }
    }

    override fun Writer.encode(definition: dev.openrune.definition.type.ObjectType) {
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
            if (revisionIsOrAfter(revision, 220)) {
                writeByte(definition.soundRetain)
            }
        }

        if (definition.ambientSoundIds != null) {
            writeByte(79)
            writeShort(definition.soundMin)
            writeShort(definition.soundMax)
            writeByte(definition.soundDistance)
            if (revisionIsOrAfter(revision, 220)) {
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

    override fun createDefinition() = dev.openrune.definition.type.ObjectType()

    companion object {
        internal val logger = KotlinLogging.logger {}
    }
}