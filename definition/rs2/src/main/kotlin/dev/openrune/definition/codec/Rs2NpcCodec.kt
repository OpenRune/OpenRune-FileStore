package dev.openrune.definition.codec

import dev.openrune.definition.revisionIsOrAfter
import dev.openrune.definition.type.Rs2NpcHeadIcon
import dev.openrune.definition.type.Rs2NpcType
import dev.openrune.definition.util.readNullableLargeSmartCorrect
import dev.openrune.definition.util.readShortSmartSub
import dev.openrune.definition.util.readSmart
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.readUnsignedShortOrNull
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import dev.openrune.definition.util.writeSmart
import dev.openrune.definition.util.writeString
import dev.openrune.definition.writeParameterBody
import dev.openrune.definition.writeRecolourPairs
import dev.openrune.definition.writeTransformBody
import io.netty.buffer.ByteBuf

class Rs2NpcCodec(private val revision: Int) : Rs2OrderedCodec<Rs2NpcType> {

    private val packedRecolours get() = revisionIsOrAfter(revision, 465)
    private val wideModels get() = revisionIsOrAfter(revision, 681)
    private val filteredHeadIcons get() = revisionIsOrAfter(revision, 809)
    private val wideTransformCount get() = revisionIsOrAfter(revision, 910)

    private fun ByteBuf.readModel(): Int =
        if (wideModels) readNullableLargeSmartCorrect() ?: -1 else readUnsignedShortOrNull() ?: -1

    private fun ByteBuf.writeModel(value: Int) {
        if (wideModels) writeNullableLargeSmartCorrect(value) else writeShort(value)
    }

    override fun Rs2NpcType.read(opcode: Int, buffer: ByteBuf) {
        check(packedRecolours) { "RS3 npcs before revision 465 are not supported (got $revision)" }
        when {
            opcode == 1 -> {
                val count = buffer.readUnsignedByte().toInt()
                models = MutableList(count) { buffer.readModel() }
            }

            opcode == 2 -> name = buffer.readString()
            opcode == 3 -> description = buffer.readString()
            opcode == 12 -> size = buffer.readUnsignedByte().toInt()
            opcode == 13 -> readyAnim = buffer.readUnsignedShort()
            opcode == 14 -> walkAnim = buffer.readUnsignedShort()
            opcode == 15 -> turnLeftAnim = buffer.readUnsignedShort()
            opcode == 16 -> if (revisionIsOrAfter(revision, 300)) {
                turnRightAnim = buffer.readUnsignedShort()
            } else {
                hasAnim = true
            }

            opcode == 17 -> walkAnimSet = MutableList(4) { buffer.readUnsignedShort() }
            opcode == 18 -> category = buffer.readUnsignedShort()
            opcode == 26 -> wanderRange = buffer.readUnsignedShort()
            opcode == 27 -> maxRange = buffer.readUnsignedShort()
            opcode in 30..34 -> ops[opcode] = buffer.readString()
            opcode == 39 -> contrastLegacy = buffer.readByte().toInt()
            opcode == 40 -> readColours(buffer)
            opcode == 41 -> readTextures(buffer)
            opcode == 42 -> {
                val count = buffer.readUnsignedByte().toInt()
                recolourPalette = MutableList(count) { buffer.readByte().toInt() }
            }

            opcode == 44 -> recolourMask = buffer.readUnsignedShort()
            opcode == 45 -> textureMask = buffer.readUnsignedShort()
            opcode == 60 -> {
                val count = buffer.readUnsignedByte().toInt()
                heads = MutableList(count) { buffer.readModel() }
            }

            opcode == 93 -> minimap = false
            opcode == 95 -> visLevel = buffer.readUnsignedShort()
            opcode == 97 -> resizeHorizontal = buffer.readUnsignedShort()
            opcode == 98 -> resizeVertical = buffer.readUnsignedShort()
            opcode == 99 -> alwaysOnTop = true
            opcode == 100 -> ambient = buffer.readByte().toInt()
            opcode == 101 -> contrast = buffer.readByte().toInt()
            opcode == 102 -> if (!filteredHeadIcons) {
                headIconLegacy = buffer.readUnsignedShort()
            } else {
                val filter = buffer.readUnsignedByte().toInt()
                for (slot in 0 until 8) {
                    if ((filter and (1 shl slot)) != 0) {
                        headIcons[slot] = Rs2NpcHeadIcon(
                            buffer.readNullableLargeSmartCorrect() ?: -1,
                            buffer.readShortSmartSub(),
                        )
                    }
                }
            }

            opcode == 103 -> turnSpeed = buffer.readUnsignedShort()
            opcode == 106 || opcode == 118 ->
                readTransforms(buffer, isLast = opcode == 118, extendedTransforms = wideTransformCount)

            opcode == 107 -> active = false
            opcode == 109 -> walkSmoothing = false
            opcode == 111 -> spotShadow = false
            opcode == 113 -> spotShadowColour = MutableList(2) { buffer.readUnsignedShort() }
            opcode == 114 -> spotShadowTrans = MutableList(2) { buffer.readByte().toInt() }
            opcode == 115 -> unknown115 = MutableList(2) { buffer.readUnsignedByte().toInt() }
            opcode == 119 -> unknown119 = buffer.readByte().toInt()
            opcode == 120 -> unknown120 = mutableListOf(
                buffer.readUnsignedShort(),
                buffer.readUnsignedShort(),
                buffer.readUnsignedShort(),
                buffer.readUnsignedByte().toInt(),
            )

            opcode == 121 -> {
                val count = buffer.readUnsignedByte().toInt()
                repeat(count) {
                    val slot = buffer.readUnsignedByte().toInt()
                    modelOffsets[slot] = MutableList(3) { buffer.readByte().toInt() }
                }
            }

            opcode == 122 -> unknown122 = buffer.readUnsignedShort()
            opcode == 123 -> overlayHeight = buffer.readUnsignedShort()
            opcode == 125 -> respawnDirection = buffer.readByte().toInt()
            opcode == 127 -> bas = buffer.readUnsignedShort()
            opcode == 128 -> defaultMoveMode = buffer.readByte().toInt()
            opcode == 134 -> bgSound = mutableListOf(
                buffer.readUnsignedShortOrNull() ?: -1,
                buffer.readUnsignedShortOrNull() ?: -1,
                buffer.readUnsignedShortOrNull() ?: -1,
                buffer.readUnsignedShortOrNull() ?: -1,
                buffer.readUnsignedByte().toInt(),
            )

            opcode == 135 -> cursorLegacy1 =
                mutableListOf(buffer.readUnsignedByte().toInt(), buffer.readUnsignedShort())

            opcode == 136 -> cursorLegacy2 =
                mutableListOf(buffer.readUnsignedByte().toInt(), buffer.readUnsignedShort())

            opcode == 137 -> cursorAttack = buffer.readUnsignedShort()
            opcode == 138 -> coverMarker = buffer.readNullableLargeSmartCorrect() ?: -1
            opcode == 139 -> unknown139 = buffer.readNullableLargeSmartCorrect() ?: -1
            opcode == 140 -> bgSoundVolume = buffer.readUnsignedByte().toInt()
            opcode == 141 -> familiar = true
            opcode == 142 -> mapElement = buffer.readUnsignedShort()
            opcode == 143 -> alwaysOnBottom = true
            opcode in 150..154 -> membersOps[opcode] = buffer.readString()
            opcode == 155 -> tint = MutableList(4) { buffer.readByte().toInt() }
            opcode == 158 -> reprioritiseAttackOp = true
            opcode == 159 -> reprioritiseAttackOp = false
            opcode == 160 -> {
                val count = buffer.readUnsignedByte().toInt()
                quests = MutableList(count) { buffer.readUnsignedShort() }
            }

            opcode == 162 -> bgSoundVorbis = true
            opcode == 163 -> roughBounding = buffer.readUnsignedByte().toInt()
            opcode == 164 -> bgSoundRate = MutableList(2) { buffer.readUnsignedShort() }
            opcode == 165 -> pickSizeShift = buffer.readUnsignedByte().toInt()
            opcode == 168 -> bgSoundDropoffRange = buffer.readUnsignedByte().toInt()
            opcode == 169 -> antiMacro = false
            opcode in 170..175 -> cursors[opcode] = buffer.readUnsignedShortOrNull() ?: -1
            opcode == 178 -> castsShadows = false
            opcode == 179 -> customBounding = MutableList(6) { buffer.readSmart() }
            opcode == 180 -> fadeInDuration = buffer.readUnsignedByte().toInt()
            opcode == 181 ->
                spotShadowTexture = mutableListOf(buffer.readUnsignedShort(), buffer.readUnsignedByte().toInt())

            opcode == 182 -> transmogFakeNpc = true
            opcode == 184 -> highlightOverride = buffer.readUnsignedByte().toInt()
            opcode == 185 -> fastPicking = false
            opcode == 186 || opcode == 189 ->
                appearanceOverrides += buffer.readNpcOverride(mediumVarBit = opcode == 189)

            opcode == 187 || opcode == 188 -> {
                readTransforms(buffer, isLast = opcode == 188, extendedTransforms = true, mediumVarBit = true)
                transformsWideVarBit = true
            }

            opcode == 249 -> readParameters(buffer)
            opcode == 252 -> animationStateMachine = buffer.readUnsignedShort()
            opcode == 253 -> priorityOffset = buffer.readUnsignedByte().toInt()
            isLegacyColourSlot(opcode) -> readLegacyColourSlot(opcode, buffer)
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: Rs2NpcType) {
        when {
            opcode == 1 -> {
                val models = definition.models!!
                writeByte(models.size)
                models.forEach { writeModel(it) }
            }

            opcode == 2 -> writeString(definition.name!!)
            opcode == 3 -> writeString(definition.description!!)
            opcode == 12 -> writeByte(definition.size!!)
            opcode == 13 -> writeShort(definition.readyAnim!!)
            opcode == 14 -> writeShort(definition.walkAnim!!)
            opcode == 15 -> writeShort(definition.turnLeftAnim!!)
            opcode == 16 -> if (revisionIsOrAfter(revision, 300)) writeShort(definition.turnRightAnim!!) else Unit
            opcode == 17 -> definition.walkAnimSet!!.forEach { writeShort(it) }
            opcode == 18 -> writeShort(definition.category!!)
            opcode == 26 -> writeShort(definition.wanderRange!!)
            opcode == 27 -> writeShort(definition.maxRange!!)
            opcode in 30..34 -> writeString(definition.ops.getValue(opcode))
            opcode == 39 -> writeByte(definition.contrastLegacy!!)
            opcode == 40 -> writeRecolourPairs(definition.originalColours, definition.modifiedColours)
            opcode == 41 -> writeRecolourPairs(definition.originalTextureColours, definition.modifiedTextureColours)
            opcode == 42 -> {
                val palette = definition.recolourPalette!!
                writeByte(palette.size)
                palette.forEach { writeByte(it) }
            }

            opcode == 44 -> writeShort(definition.recolourMask!!)
            opcode == 45 -> writeShort(definition.textureMask!!)
            opcode == 60 -> {
                val heads = definition.heads!!
                writeByte(heads.size)
                heads.forEach { writeModel(it) }
            }

            opcode == 93 -> Unit
            opcode == 95 -> writeShort(definition.visLevel!!)
            opcode == 97 -> writeShort(definition.resizeHorizontal!!)
            opcode == 98 -> writeShort(definition.resizeVertical!!)
            opcode == 99 -> Unit
            opcode == 100 -> writeByte(definition.ambient!!)
            opcode == 101 -> writeByte(definition.contrast!!)
            opcode == 102 -> if (!filteredHeadIcons) {
                writeShort(definition.headIconLegacy!!)
            } else {
                var filter = 0
                definition.headIcons.keys.forEach { filter = filter or (1 shl it) }
                writeByte(filter)
                for (slot in definition.headIcons.keys.sorted()) {
                    val icon = definition.headIcons.getValue(slot)
                    writeNullableLargeSmartCorrect(icon.graphic)
                    writeSmart(icon.index + 1)
                }
            }

            opcode == 103 -> writeShort(definition.turnSpeed!!)
            opcode == 106 || opcode == 118 -> writeTransformBody(
                this,
                definition.multiVarBit,
                definition.multiVarp,
                definition.multiDefault,
                definition.transforms,
                hasDefault = opcode == 118,
                extendedTransforms = wideTransformCount,
            )

            opcode == 107 -> Unit
            opcode == 109 -> Unit
            opcode == 111 -> Unit
            opcode == 113 -> definition.spotShadowColour!!.forEach { writeShort(it) }
            opcode == 114 -> definition.spotShadowTrans!!.forEach { writeByte(it) }
            opcode == 115 -> definition.unknown115!!.forEach { writeByte(it) }
            opcode == 119 -> writeByte(definition.unknown119!!)
            opcode == 120 -> {
                val values = definition.unknown120!!
                writeShort(values[0])
                writeShort(values[1])
                writeShort(values[2])
                writeByte(values[3])
            }

            opcode == 121 -> {
                writeByte(definition.modelOffsets.size)
                for (slot in definition.modelOffsets.keys.sorted()) {
                    writeByte(slot)
                    definition.modelOffsets.getValue(slot).forEach { writeByte(it) }
                }
            }

            opcode == 122 -> writeShort(definition.unknown122!!)
            opcode == 123 -> writeShort(definition.overlayHeight!!)
            opcode == 125 -> writeByte(definition.respawnDirection!!)
            opcode == 127 -> writeShort(definition.bas!!)
            opcode == 128 -> writeByte(definition.defaultMoveMode!!)
            opcode == 134 -> {
                val sound = definition.bgSound!!
                for (i in 0..3) writeShort(sound[i])
                writeByte(sound[4])
            }

            opcode == 135 -> {
                val cursor = definition.cursorLegacy1!!
                writeByte(cursor[0])
                writeShort(cursor[1])
            }

            opcode == 136 -> {
                val cursor = definition.cursorLegacy2!!
                writeByte(cursor[0])
                writeShort(cursor[1])
            }

            opcode == 137 -> writeShort(definition.cursorAttack!!)
            opcode == 138 -> writeNullableLargeSmartCorrect(definition.coverMarker!!)
            opcode == 139 -> writeNullableLargeSmartCorrect(definition.unknown139!!)
            opcode == 140 -> writeByte(definition.bgSoundVolume!!)
            opcode == 141 -> Unit
            opcode == 142 -> writeShort(definition.mapElement!!)
            opcode == 143 -> Unit
            opcode in 150..154 -> writeString(definition.membersOps.getValue(opcode))
            opcode == 155 -> definition.tint!!.forEach { writeByte(it) }
            opcode == 158 || opcode == 159 -> Unit
            opcode == 160 -> {
                val quests = definition.quests!!
                writeByte(quests.size)
                quests.forEach { writeShort(it) }
            }

            opcode == 162 -> Unit
            opcode == 163 -> writeByte(definition.roughBounding!!)
            opcode == 164 -> definition.bgSoundRate!!.forEach { writeShort(it) }
            opcode == 165 -> writeByte(definition.pickSizeShift!!)
            opcode == 168 -> writeByte(definition.bgSoundDropoffRange!!)
            opcode == 169 -> Unit
            opcode in 170..175 -> writeShort(definition.cursors.getValue(opcode))
            opcode == 178 -> Unit
            opcode == 179 -> definition.customBounding!!.forEach { writeSmart(it) }
            opcode == 180 -> writeByte(definition.fadeInDuration!!)
            opcode == 181 -> {
                val shadow = definition.spotShadowTexture!!
                writeShort(shadow[0])
                writeByte(shadow[1])
            }

            opcode == 182 -> Unit
            opcode == 184 -> writeByte(definition.highlightOverride!!)
            opcode == 185 -> Unit
            opcode == 186 || opcode == 189 -> writeNpcOverride(
                definition.appearanceOverrides.filter { it.mediumVarBit == (opcode == 189) }[occurrence],
            )
            opcode == 187 || opcode == 188 -> writeTransformBody(
                this,
                definition.multiVarBit,
                definition.multiVarp,
                definition.multiDefault,
                definition.transforms,
                hasDefault = opcode == 188,
                extendedTransforms = true,
                mediumVarBit = true,
            )

            opcode == 249 -> writeParameterBody(this, definition.params)
            opcode == 252 -> writeShort(definition.animationStateMachine!!)
            opcode == 253 -> writeByte(definition.priorityOffset!!)
            definition.isLegacyColourSlot(opcode) -> writeShort(definition.legacyColourSlots.getValue(opcode))
        }
    }

    override fun createDefinition() = Rs2NpcType()
}
