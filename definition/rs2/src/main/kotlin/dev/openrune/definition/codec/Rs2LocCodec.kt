package dev.openrune.definition.codec

import dev.openrune.definition.revisionIsOrAfter
import dev.openrune.definition.type.Rs2LocAnim
import dev.openrune.definition.type.Rs2LocModelGroup
import dev.openrune.definition.type.Rs2LocPointLight
import dev.openrune.definition.type.Rs2LocRandomSound
import dev.openrune.definition.type.Rs2LocType
import dev.openrune.definition.type.Rs2LocVfx
import dev.openrune.definition.util.readNullableLargeSmartCorrect
import dev.openrune.definition.util.readShortSmart
import dev.openrune.definition.util.readSmart
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import dev.openrune.definition.util.writeShortSmart
import dev.openrune.definition.util.writeSmart
import dev.openrune.definition.util.writeString
import dev.openrune.definition.writeParameterBody
import dev.openrune.definition.writeRecolourPairs
import dev.openrune.definition.writeTransformBody
import io.netty.buffer.ByteBuf

class Rs2LocCodec(private val revision: Int) : Rs2OrderedCodec<Rs2LocType> {

    private val shapedModels get() = revisionIsOrAfter(revision, 582)
    private val wideModels get() = revisionIsOrAfter(revision, 681)
    private val wideTransformCount get() = revisionIsOrAfter(revision, 910)
    private val skewToFitAngle get() = revisionIsOrAfter(revision, 596)

    private fun ByteBuf.readModel(): Int =
        if (wideModels) readNullableLargeSmartCorrect() ?: -1 else readUnsignedShort()

    private fun ByteBuf.writeModel(value: Int) {
        if (wideModels) writeNullableLargeSmartCorrect(value) else writeShort(value)
    }

    private fun ByteBuf.readModelGroups(): MutableList<Rs2LocModelGroup> {
        val count = readUnsignedByte().toInt()
        return MutableList(count) {
            if (shapedModels) {
                val shape = readByte().toInt()
                val models = readUnsignedByte().toInt()
                Rs2LocModelGroup(shape, MutableList(models) { readModel() })
            } else {
                val model = readUnsignedShort()
                Rs2LocModelGroup(readUnsignedByte().toInt(), mutableListOf(model))
            }
        }
    }

    private fun ByteBuf.writeModelGroups(groups: List<Rs2LocModelGroup>) {
        writeByte(groups.size)
        for (group in groups) {
            if (shapedModels) {
                writeByte(group.shape)
                writeByte(group.models.size)
                group.models.forEach { writeModel(it) }
            } else {
                writeShort(group.models.first())
                writeByte(group.shape)
            }
        }
    }

    override fun Rs2LocType.read(opcode: Int, buffer: ByteBuf) {
        when {
            opcode == 1 -> models = buffer.readModelGroups()
            opcode == 2 -> name = buffer.readString()
            opcode == 3 -> description = buffer.readString()
            opcode == 5 -> {
                modelsHd = buffer.readModelGroups()
                modelsSd = buffer.readModelGroups()
            }

            opcode == 14 -> width = buffer.readUnsignedByte().toInt()
            opcode == 15 -> length = buffer.readUnsignedByte().toInt()
            opcode == 17 -> blockWalk = false
            opcode == 18 -> blockRange = false
            opcode == 19 -> active = buffer.readUnsignedByte().toInt()
            opcode == 21 -> hillSkew = true
            opcode == 22 -> shareLight = true
            opcode == 23 -> occlude = true
            opcode == 24 -> anim = buffer.readNullableLargeSmartCorrect() ?: -1
            opcode == 25 -> hasAlpha = true
            opcode == 27 -> blockWalk = true
            opcode == 28 -> wallWidth = buffer.readUnsignedByte().toInt()
            opcode == 29 -> ambient = buffer.readByte().toInt()
            opcode in 30..34 -> ops[opcode] = buffer.readString()
            opcode == 39 -> contrast = buffer.readByte().toInt()
            opcode == 40 -> readColours(buffer)
            opcode == 41 -> readTextures(buffer)
            opcode == 42 -> {
                val count = buffer.readUnsignedByte().toInt()
                recolourPalette = MutableList(count) { buffer.readByte().toInt() }
            }

            opcode == 44 -> recolourMask = buffer.readUnsignedShort()
            opcode == 45 -> textureMask = buffer.readUnsignedShort()
            opcode == 60 -> mapFunction = buffer.readUnsignedShort()
            opcode == 61 -> category = buffer.readUnsignedShort()
            opcode == 62 -> mirror = true
            opcode == 64 -> shadow = false
            opcode == 65 -> resizeX = buffer.readUnsignedShort()
            opcode == 66 -> resizeY = buffer.readUnsignedShort()
            opcode == 67 -> resizeZ = buffer.readUnsignedShort()
            opcode == 68 -> mapScene = buffer.readUnsignedShort()
            opcode == 69 -> forceApproach = buffer.readByte().toInt()
            opcode == 70 -> offsetX = buffer.readShort().toInt()
            opcode == 71 -> offsetY = buffer.readShort().toInt()
            opcode == 72 -> offsetZ = buffer.readShort().toInt()
            opcode == 73 -> forceDecor = true
            opcode == 74 -> breakRouteFinding = true
            opcode == 75 -> raiseObject = buffer.readUnsignedByte().toInt()
            opcode == 77 || opcode == 92 -> readTransforms(
                buffer,
                isLast = opcode == 92,
                extendedTransforms = wideTransformCount,
                smartIds = wideModels,
            )

            opcode == 78 -> bgSound =
                mutableListOf(buffer.readUnsignedShort(), buffer.readUnsignedByte().toInt())

            opcode == 79 -> {
                val sound = Rs2LocRandomSound(
                    buffer.readUnsignedShort(),
                    buffer.readUnsignedShort(),
                    buffer.readUnsignedByte().toInt(),
                )
                val count = buffer.readUnsignedByte().toInt()
                sound.sounds = MutableList(count) { buffer.readUnsignedShort() }
                randomSound = sound
            }

            opcode == 81 -> hillSkewTree = buffer.readUnsignedByte().toInt()
            opcode == 82 -> isTexture = true
            opcode == 88 -> hardShadow = false
            opcode == 89 -> randomAnimFrame = false
            opcode == 90 -> unknown90 = true
            opcode == 91 -> members = true
            opcode == 93 -> hillRotate = buffer.readUnsignedShort()
            opcode == 94 -> hillCeilingSkew = true
            opcode == 95 -> hillSkewToFit = if (skewToFitAngle) buffer.readUnsignedShort() else -1
            opcode == 96 -> unknown96 = true
            opcode == 97 -> msiRotate = true
            opcode == 98 -> hasParticles = true
            opcode == 99 -> cursorLegacy1 =
                mutableListOf(buffer.readUnsignedByte().toInt(), buffer.readUnsignedShort())

            opcode == 100 -> cursorLegacy2 =
                mutableListOf(buffer.readUnsignedByte().toInt(), buffer.readUnsignedShort())

            opcode == 101 -> msiAngle = buffer.readUnsignedByte().toInt()
            opcode == 102 -> msi = buffer.readUnsignedShort()
            opcode == 103 -> occlude = false
            opcode == 104 -> bgSoundVolume = buffer.readUnsignedByte().toInt()
            opcode == 105 -> msiMirror = true
            opcode == 106 -> {
                val count = buffer.readUnsignedByte().toInt()
                anims = MutableList(count) {
                    Rs2LocAnim(
                        buffer.readNullableLargeSmartCorrect() ?: -1,
                        buffer.readUnsignedByte().toInt(),
                    )
                }
            }

            opcode == 107 -> mapElement = buffer.readUnsignedShort()
            opcode == 108 -> unknown108 = true
            opcode == 109 -> unknown109 = true
            opcode == 110 -> unknown110 = true
            opcode == 111 -> unknown111 = true
            opcode in 150..154 -> membersOps[opcode] = buffer.readString()
            opcode == 160 -> {
                val count = buffer.readUnsignedByte().toInt()
                quests = MutableList(count) { buffer.readUnsignedShort() }
            }

            opcode == 162 -> hillRotateWide = buffer.readInt()
            opcode == 163 -> tint = MutableList(4) { buffer.readByte().toInt() }
            opcode == 164 -> postOffsetX = buffer.readShort().toInt()
            opcode == 165 -> postOffsetY = buffer.readShort().toInt()
            opcode == 166 -> postOffsetZ = buffer.readShort().toInt()
            opcode == 167 -> raiseObjectGroundDecor = buffer.readUnsignedShort()
            opcode == 168 -> bgSoundVorbis = true
            opcode == 169 -> randomSoundVorbis = true
            opcode == 170 -> unknown170 = buffer.readSmart()
            opcode == 171 -> unknown171 = buffer.readSmart()
            opcode == 173 -> bgSoundRate =
                mutableListOf(buffer.readUnsignedShort(), buffer.readUnsignedShort())

            opcode == 177 -> unknown177 = true
            opcode == 178 -> bgSoundDropoffRange = buffer.readUnsignedByte().toInt()
            opcode == 179 -> unknown179 = true
            opcode == 186 -> raiseEntities = buffer.readUnsignedByte().toInt()
            opcode == 188 -> findable = true
            opcode == 189 -> antiMacro = true
            opcode in 190..195 -> cursors[opcode] = buffer.readUnsignedShort()
            opcode == 196 -> minimumLodLevelOverride = buffer.readUnsignedByte().toInt()
            opcode == 197 -> indoorsOverride = buffer.readUnsignedByte().toInt()
            opcode == 198 -> runetek5Only = true
            opcode == 199 -> castsShadows = false
            opcode == 200 -> highDetailOnly = true
            opcode == 201 -> customBounding = MutableList(6) { buffer.readShortSmart() }
            opcode == 202 -> highlightOverride = buffer.readSmart()
            opcode == 203 -> fastPicking = false
            opcode == 204 -> {
                val count = buffer.readUnsignedByte().toInt()
                vfx = MutableList(count) {
                    Rs2LocVfx(
                        buffer.readUnsignedShort(),
                        buffer.readUnsignedByte().toInt(),
                        MutableList(6) { buffer.readFloat() },
                    )
                }
            }

            opcode == 205 || opcode == 209 ->
                appearanceOverrides += buffer.readNpcOverride(mediumVarBit = opcode == 209)

            opcode == 206 -> {
                buffer.readUnsignedShort()
                val count = buffer.readUnsignedByte().toInt()
                pointLights = MutableList(count) {
                    Rs2LocPointLight(
                        flags = buffer.readUnsignedByte().toInt(),
                        x = buffer.readFloat(),
                        y = buffer.readFloat(),
                        z = buffer.readFloat(),
                        radius = buffer.readUnsignedByte().toInt(),
                        intensity = buffer.readFloat(),
                        colour = buffer.readUnsignedMedium(),
                        light = buffer.readUnsignedShort(),
                        phase = buffer.readShort().toInt(),
                        attenuationFalloff = buffer.readFloat(),
                        unknown = buffer.readFloat(),
                        shadowFactor = buffer.readFloat(),
                    )
                }
            }

            opcode == 207 || opcode == 208 -> {
                readTransforms(
                    buffer,
                    isLast = opcode == 208,
                    extendedTransforms = true,
                    mediumVarBit = true,
                    smartIds = true,
                )
                transformsMediumVarBit = true
            }

            opcode == 249 -> readParameters(buffer)
            opcode == 250 -> bgSoundShape = buffer.readUnsignedByte().toInt()
            opcode == 251 -> bgSoundDistanceFiltered = buffer.readUnsignedByte().toInt()
            opcode == 252 -> bgSoundDistanceFilterParams = MutableList(3) { buffer.readUnsignedShort() }
            opcode == 253 -> randomSoundShape = buffer.readUnsignedByte().toInt()
            opcode == 254 -> randomSoundDistanceFiltered = buffer.readUnsignedByte().toInt()
            opcode == 255 -> randomSoundDistanceFilterParams = MutableList(3) { buffer.readUnsignedShort() }
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: Rs2LocType) {
        when {
            opcode == 1 -> writeModelGroups(definition.models)
            opcode == 2 -> writeString(definition.name!!)
            opcode == 3 -> writeString(definition.description!!)
            opcode == 5 -> {
                writeModelGroups(definition.modelsHd)
                writeModelGroups(definition.modelsSd)
            }

            opcode == 14 -> writeByte(definition.width!!)
            opcode == 15 -> writeByte(definition.length!!)
            opcode == 17 -> Unit
            opcode == 18 -> Unit
            opcode == 19 -> writeByte(definition.active!!)
            opcode == 21 -> Unit
            opcode == 22 -> Unit
            opcode == 23 -> Unit
            opcode == 24 -> writeNullableLargeSmartCorrect(definition.anim!!)
            opcode == 25 -> Unit
            opcode == 27 -> Unit
            opcode == 28 -> writeByte(definition.wallWidth!!)
            opcode == 29 -> writeByte(definition.ambient!!)
            opcode in 30..34 -> writeString(definition.ops.getValue(opcode))
            opcode == 39 -> writeByte(definition.contrast!!)
            opcode == 40 -> writeRecolourPairs(definition.originalColours, definition.modifiedColours)
            opcode == 41 -> writeRecolourPairs(definition.originalTextureColours, definition.modifiedTextureColours)
            opcode == 42 -> {
                val palette = definition.recolourPalette!!
                writeByte(palette.size)
                palette.forEach { writeByte(it) }
            }

            opcode == 44 -> writeShort(definition.recolourMask!!)
            opcode == 45 -> writeShort(definition.textureMask!!)
            opcode == 60 -> writeShort(definition.mapFunction!!)
            opcode == 61 -> writeShort(definition.category!!)
            opcode == 62 -> Unit
            opcode == 64 -> Unit
            opcode == 65 -> writeShort(definition.resizeX!!)
            opcode == 66 -> writeShort(definition.resizeY!!)
            opcode == 67 -> writeShort(definition.resizeZ!!)
            opcode == 68 -> writeShort(definition.mapScene!!)
            opcode == 69 -> writeByte(definition.forceApproach!!)
            opcode == 70 -> writeShort(definition.offsetX!!)
            opcode == 71 -> writeShort(definition.offsetY!!)
            opcode == 72 -> writeShort(definition.offsetZ!!)
            opcode == 73 -> Unit
            opcode == 74 -> Unit
            opcode == 75 -> writeByte(definition.raiseObject!!)
            opcode == 77 || opcode == 92 -> writeTransformBody(
                this,
                definition.multiVarBit,
                definition.multiVarp,
                definition.multiDefault,
                definition.transforms,
                hasDefault = opcode == 92,
                extendedTransforms = wideTransformCount,
                smartIds = wideModels,
            )

            opcode == 78 -> {
                val sound = definition.bgSound!!
                writeShort(sound[0])
                writeByte(sound[1])
            }

            opcode == 79 -> {
                val sound = definition.randomSound!!
                writeShort(sound.minDelay)
                writeShort(sound.maxDelay)
                writeByte(sound.volume)
                writeByte(sound.sounds.size)
                sound.sounds.forEach { writeShort(it) }
            }

            opcode == 81 -> writeByte(definition.hillSkewTree!!)
            opcode == 82 -> Unit
            opcode == 88 -> Unit
            opcode == 89 -> Unit
            opcode == 90 -> Unit
            opcode == 91 -> Unit
            opcode == 93 -> writeShort(definition.hillRotate!!)
            opcode == 94 -> Unit
            opcode == 95 -> if (skewToFitAngle) writeShort(definition.hillSkewToFit!!) else Unit
            opcode == 96 -> Unit
            opcode == 97 -> Unit
            opcode == 98 -> Unit
            opcode == 99 -> {
                val cursor = definition.cursorLegacy1!!
                writeByte(cursor[0])
                writeShort(cursor[1])
            }

            opcode == 100 -> {
                val cursor = definition.cursorLegacy2!!
                writeByte(cursor[0])
                writeShort(cursor[1])
            }

            opcode == 101 -> writeByte(definition.msiAngle!!)
            opcode == 102 -> writeShort(definition.msi!!)
            opcode == 103 -> Unit
            opcode == 104 -> writeByte(definition.bgSoundVolume!!)
            opcode == 105 -> Unit
            opcode == 106 -> {
                writeByte(definition.anims.size)
                definition.anims.forEach {
                    writeNullableLargeSmartCorrect(it.anim)
                    writeByte(it.weight)
                }
            }

            opcode == 107 -> writeShort(definition.mapElement!!)
            opcode == 108 -> Unit
            opcode == 109 -> Unit
            opcode == 110 -> Unit
            opcode == 111 -> Unit
            opcode in 150..154 -> writeString(definition.membersOps.getValue(opcode))
            opcode == 160 -> {
                val quests = definition.quests!!
                writeByte(quests.size)
                quests.forEach { writeShort(it) }
            }

            opcode == 162 -> writeInt(definition.hillRotateWide!!)
            opcode == 163 -> definition.tint!!.forEach { writeByte(it) }
            opcode == 164 -> writeShort(definition.postOffsetX!!)
            opcode == 165 -> writeShort(definition.postOffsetY!!)
            opcode == 166 -> writeShort(definition.postOffsetZ!!)
            opcode == 167 -> writeShort(definition.raiseObjectGroundDecor!!)
            opcode == 168 -> Unit
            opcode == 169 -> Unit
            opcode == 170 -> writeSmart(definition.unknown170!!)
            opcode == 171 -> writeSmart(definition.unknown171!!)
            opcode == 173 -> definition.bgSoundRate!!.forEach { writeShort(it) }
            opcode == 177 -> Unit
            opcode == 178 -> writeByte(definition.bgSoundDropoffRange!!)
            opcode == 179 -> Unit
            opcode == 186 -> writeByte(definition.raiseEntities!!)
            opcode == 188 -> Unit
            opcode == 189 -> Unit
            opcode in 190..195 -> writeShort(definition.cursors.getValue(opcode))
            opcode == 196 -> writeByte(definition.minimumLodLevelOverride!!)
            opcode == 197 -> writeByte(definition.indoorsOverride!!)
            opcode == 198 -> Unit
            opcode == 199 -> Unit
            opcode == 200 -> Unit
            opcode == 201 -> definition.customBounding!!.forEach { writeShortSmart(it) }
            opcode == 202 -> writeSmart(definition.highlightOverride!!)
            opcode == 203 -> Unit
            opcode == 204 -> {
                writeByte(definition.vfx.size)
                definition.vfx.forEach {
                    writeShort(it.vfx)
                    writeByte(it.slot)
                    it.transform.forEach { value -> writeFloat(value) }
                }
            }

            opcode == 205 || opcode == 209 -> writeNpcOverride(
                definition.appearanceOverrides.filter { it.mediumVarBit == (opcode == 209) }[occurrence],
            )

            opcode == 206 -> {
                writeShort(0)
                writeByte(definition.pointLights.size)
                definition.pointLights.forEach {
                    writeByte(it.flags)
                    writeFloat(it.x)
                    writeFloat(it.y)
                    writeFloat(it.z)
                    writeByte(it.radius)
                    writeFloat(it.intensity)
                    writeMedium(it.colour)
                    writeShort(it.light)
                    writeShort(it.phase)
                    writeFloat(it.attenuationFalloff)
                    writeFloat(it.unknown)
                    writeFloat(it.shadowFactor)
                }
            }

            opcode == 207 || opcode == 208 -> writeTransformBody(
                this,
                definition.multiVarBit,
                definition.multiVarp,
                definition.multiDefault,
                definition.transforms,
                hasDefault = opcode == 208,
                extendedTransforms = true,
                mediumVarBit = true,
                smartIds = true,
            )

            opcode == 249 -> writeParameterBody(this, definition.params)
            opcode == 250 -> writeByte(definition.bgSoundShape!!)
            opcode == 251 -> writeByte(definition.bgSoundDistanceFiltered!!)
            opcode == 252 -> definition.bgSoundDistanceFilterParams!!.forEach { writeShort(it) }
            opcode == 253 -> writeByte(definition.randomSoundShape!!)
            opcode == 254 -> writeByte(definition.randomSoundDistanceFiltered!!)
            opcode == 255 -> definition.randomSoundDistanceFilterParams!!.forEach { writeShort(it) }
        }
    }

    override fun createDefinition() = Rs2LocType()
}
