package dev.openrune.definition.codec

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.IgnoreOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.opcode.impl.DefinitionOpcodeListActions
import dev.openrune.definition.opcode.impl.DefinitionOpcodeParams
import dev.openrune.definition.opcode.impl.DefinitionOpcodeTransforms
import dev.openrune.definition.revisionIsOrAfter
import dev.openrune.definition.type.ObjectType
import dev.openrune.definition.util.*
import io.netty.buffer.ByteBuf
import java.util.stream.IntStream
import kotlin.streams.toList

class ObjectCodecNew(private val revision: Int) : OpcodeDefinitionCodec<ObjectType>() {

    override val definitionCodec = OpcodeList<ObjectType>().apply {
        // Opcode 1: objectModels and objectTypes (complex)
        add(DefinitionOpcode(1,
            decode = { buf, def, _ ->
                val length = buf.readUnsignedByte().toInt()
                if (length > 0) {
                    def.objectTypes = MutableList(length) { 0 }
                    def.objectModels = MutableList(length) { 0 }
                    (0 until length).forEach {
                        def.objectModels!![it] = buf.readUnsignedShort()
                        def.objectTypes!![it] = buf.readUnsignedByte().toInt()
                    }
                }
            },
            encode = { buf, def ->
                val models = def.objectModels
                val types = def.objectTypes
                if (models != null && types != null && models.isNotEmpty()) {
                    buf.writeByte(models.size)
                    for (i in models.indices) {
                        buf.writeShort(models[i])
                        buf.writeByte(types[i])
                    }
                }
            },
            shouldEncode = { it.objectModels != null && it.objectTypes != null && it.objectModels!!.isNotEmpty() }
        ))

        // Opcode 2: name
        add(DefinitionOpcode(2, OpcodeType.STRING, ObjectType::name))

        // Opcode 5: objectModels only (objectTypes = null)
        add(DefinitionOpcode(5,
            decode = { buf, def, _ ->
                val length = buf.readUnsignedByte().toInt()
                if (length > 0) {
                    def.objectTypes = null
                    def.objectModels = IntStream.range(0, length).map {
                        buf.readUnsignedShort()
                    }.toList().toMutableList()
                }
            },
            encode = { buf, def ->
                val models = def.objectModels
                if (models != null && def.objectTypes == null && models.isNotEmpty()) {
                    buf.writeByte(models.size)
                    models.forEach { buf.writeShort(it) }
                }
            },
            shouldEncode = { it.objectModels != null && it.objectTypes == null && it.objectModels!!.isNotEmpty() }
        ))

        add(DefinitionOpcode(14, OpcodeType.UBYTE, ObjectType::sizeX))
        add(DefinitionOpcode(15, OpcodeType.UBYTE, ObjectType::sizeY))
        add(DefinitionOpcode(17,
            decode = { _, def, _ ->
                def.solid = 0
                def.impenetrable = false
            },
            encode = { _, _ -> },
            shouldEncode = { it.solid == 0 && !it.impenetrable }
        ))
        add(DefinitionOpcode(18, ObjectType::impenetrable, setValue = false))
        add(DefinitionOpcode(19, OpcodeType.UBYTE, ObjectType::interactive))
        add(DefinitionOpcode(21, ObjectType::clipType, setValue = 0, encodeWhen = 0))
        add(DefinitionOpcode(22, ObjectType::nonFlatShading))
        add(DefinitionOpcode(23, ObjectType::modelClipped))

        add(DefinitionOpcode(24,
            decode = { buf, def, _ ->
                var animId = buf.readUnsignedShort()
                def.animationId = if (animId == 65535) -1 else animId
            },
            encode = { buf, def ->
                if (def.animationId != -1) {
                    buf.writeShort(def.animationId)
                }
            },
            shouldEncode = { it.animationId != -1 }
        ))

        add(DefinitionOpcode(27, ObjectType::solid, setValue = 1, encodeWhen = 1))
        add(DefinitionOpcode(28, OpcodeType.UBYTE, ObjectType::decorDisplacement))
        add(DefinitionOpcode(29, OpcodeType.BYTE, ObjectType::ambient))
        addAll(DefinitionOpcodeListActions(30..34, ObjectType::actions))
        add(DefinitionOpcode(39,
            decode = { buf, def, _ ->
                def.contrast = buf.readByte().toInt() * 25
            },
            encode = { buf, def ->
                buf.writeByte(def.contrast / 25)
            },
            shouldEncode = { true }
        ))

        add(DefinitionOpcode(40,
            decode = { buf, def, _ ->
                def.readColours(buf)
            },
            encode = { buf, def ->
                val original = def.originalColours
                val modified = def.modifiedColours
                if (original != null && modified != null) {
                    buf.writeByte(original.size)
                    for (i in original.indices) {
                        buf.writeShort(original[i])
                        buf.writeShort(modified[i])
                    }
                }
            },
            shouldEncode = { it.originalColours != null && it.modifiedColours != null }
        ))

        add(DefinitionOpcode(41,
            decode = { buf, def, _ ->
                def.readTextures(buf)
            },
            encode = { buf, def ->
                val original = def.originalTextureColours
                val modified = def.modifiedTextureColours
                if (original != null && modified != null) {
                    buf.writeByte(original.size)
                    for (i in original.indices) {
                        buf.writeShort(original[i])
                        buf.writeShort(modified[i])
                    }
                }
            },
            shouldEncode = { it.originalTextureColours != null && it.modifiedTextureColours != null }
        ))

        add(DefinitionOpcode(60,
            decode = { buf, def, _ ->
                def.mapAreaId = buf.readUnsignedShort()
            },
            encode = { _, _ -> },
            shouldEncode = { false }
        ))

        add(DefinitionOpcode(61, OpcodeType.USHORT, ObjectType::category))
        add(DefinitionOpcode(62, ObjectType::isRotated))
        add(DefinitionOpcode(64,
            decode = { _, def, _ ->
                def.clipped = false
            },
            encode = { _, _ -> },
            shouldEncode = { !it.clipped }
        ))

        add(DefinitionOpcode(65, OpcodeType.USHORT, ObjectType::modelSizeX))

        add(DefinitionOpcode(66, OpcodeType.USHORT, ObjectType::modelSizeZ))

        add(DefinitionOpcode(67, OpcodeType.USHORT, ObjectType::modelSizeY))

        add(DefinitionOpcode(68, OpcodeType.USHORT, ObjectType::mapSceneID))

        add(DefinitionOpcode(69, OpcodeType.BYTE, ObjectType::clipMask))

        add(DefinitionOpcode(70, OpcodeType.USHORT, ObjectType::offsetX))

        add(DefinitionOpcode(71, OpcodeType.USHORT, ObjectType::offsetZ))

        add(DefinitionOpcode(72, OpcodeType.USHORT, ObjectType::offsetY))

        add(DefinitionOpcode(73, ObjectType::obstructive))

        add(DefinitionOpcode(74, ObjectType::isHollow))

        add(DefinitionOpcode(75, OpcodeType.UBYTE, ObjectType::supportsItems))

        add(DefinitionOpcodeTransforms(IntRange(77, 92), ObjectType::transforms, ObjectType::varbit, ObjectType::varp))

        add(DefinitionOpcode(78,
            decode = { buf, def, _ ->
                def.ambientSoundId = buf.readUnsignedShort()
                def.soundDistance = buf.readUnsignedByte().toInt()
                if (revisionIsOrAfter(revision, 220)) {
                    def.soundRetain = buf.readUnsignedByte().toInt()
                }
            },
            encode = { buf, def ->
                if (def.ambientSoundId != -1) {
                    buf.writeShort(def.ambientSoundId)
                    buf.writeByte(def.soundDistance)
                    if (revisionIsOrAfter(revision, 220)) {
                        buf.writeByte(def.soundRetain)
                    }
                }
            },
            shouldEncode = { it.ambientSoundId != -1 }
        ))

        add(DefinitionOpcode(79,
            decode = { buf, def, _ ->
                def.soundMin = buf.readUnsignedShort()
                def.soundMax = buf.readUnsignedShort()
                def.soundDistance = buf.readUnsignedByte().toInt()
                if (revisionIsOrAfter(revision, 220)) {
                    def.soundRetain = buf.readUnsignedByte().toInt()
                }
                val length = buf.readUnsignedByte().toInt()
                def.ambientSoundIds = IntStream.range(0, length).map {
                    buf.readUnsignedShort()
                }.toList().toMutableList()
            },
            encode = { buf, def ->
                val soundIds = def.ambientSoundIds
                if (soundIds != null && soundIds.isNotEmpty()) {
                    buf.writeShort(def.soundMin)
                    buf.writeShort(def.soundMax)
                    buf.writeByte(def.soundDistance)
                    if (revisionIsOrAfter(revision, 220)) {
                        buf.writeByte(def.soundRetain)
                    }
                    buf.writeByte(soundIds.size)
                    soundIds.forEach { buf.writeShort(it) }
                }
            },
            shouldEncode = { it.ambientSoundIds != null && it.ambientSoundIds!!.isNotEmpty() }
        ))

        add(DefinitionOpcode(81,
            decode = { buf, def, _ ->
                def.clipType = buf.readUnsignedByte().toInt() * 256
            },
            encode = { buf, def ->
                if (def.clipType != -1) {
                    buf.writeByte(def.clipType / 256)
                }
            },
            shouldEncode = { it.clipType != -1 }
        ))

        add(DefinitionOpcode(82,
            decode = { buf, def, _ ->
                def.mapAreaId = buf.readUnsignedShort()
            },
            encode = { buf, def ->
                buf.writeShort(def.mapAreaId)
            },
            shouldEncode = { it.mapAreaId != -1 }
        ))

        add(DefinitionOpcode(89,
            decode = { _, def, _ ->
                def.randomizeAnimStart = true
            },
            encode = { _, _ -> },
            shouldEncode = { !it.randomizeAnimStart }
        ))

        add(DefinitionOpcode(90, ObjectType::delayAnimationUpdate))

        add(DefinitionOpcode(91, OpcodeType.UBYTE, ObjectType::soundDistanceFadeCurve))

        add(DefinitionOpcode(93,
            decode = { buf, def, _ ->
                def.soundFadeInCurve = buf.readUnsignedByte().toInt()
                def.soundFadeInDuration = buf.readUnsignedShort().toInt()
                def.soundFadeOutCurve = buf.readUnsignedByte().toInt()
                def.soundFadeOutDuration = buf.readUnsignedShort().toInt()
            },
            encode = { buf, def ->
                buf.writeByte(def.soundFadeInCurve)
                buf.writeShort(def.soundFadeInDuration)
                buf.writeByte(def.soundFadeOutCurve)
                buf.writeShort(def.soundFadeOutDuration)
            },
            shouldEncode = {
                val defaults = listOf(0, 0, 300, 300)
                val values = listOf(
                    it.soundFadeInCurve,
                    it.soundFadeOutCurve,
                    it.soundFadeInDuration,
                    it.soundFadeOutDuration
                )
                values.indices.any { idx -> values[idx] != defaults[idx] }
            }
        ))

        add(DefinitionOpcode(95, OpcodeType.UBYTE, ObjectType::soundVisibility))

        add(DefinitionOpcodeParams(249, ObjectType::params))
    }

    override fun createDefinition() = ObjectType()
}
