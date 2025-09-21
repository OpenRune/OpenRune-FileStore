package dev.openrune.definition.codec

import dev.openrune.definition.util.readBigSmart
import dev.openrune.definition.util.readString
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.NpcType
import io.netty.buffer.ByteBuf

fun NpcType.getPrimaryShadowColour(): Int {
    return getIntProperty("primaryShadowColour")
}

fun NpcType.getSecondaryShadowColour(): Int {
    return getIntProperty("secondaryShadowColour")
}

fun NpcType.getPrimaryShadowModifier(): Int {
    return getIntProperty("primaryShadowModifier")
}

fun NpcType.getSecondaryShadowModifier(): Int {
    return getIntProperty("secondaryShadowModifier")
}

fun NpcType.getWalkMask(): Int {
    return getIntProperty("walkMask")
}

fun NpcType.getTranslations(): Array<IntArray?> {
    return getIntArray2DProperty("translations")
}

fun NpcType.getHitbarSprite(): Int {
    return getIntProperty("hitbarSprite")
}

fun NpcType.getRespawnDirection(): Int {
    return getIntProperty("respawnDirection")
}

fun NpcType.getRenderAnimations(): Int {
    return getIntProperty("renderAnimations")
}

fun NpcType.getIdleSound(): Int {
    return getIntProperty("idleSound")
}

fun NpcType.getCrawlSound(): Int {
    return getIntProperty("crawlSound")
}

fun NpcType.getWalkSound(): Int {
    return getIntProperty("walkSound")
}

fun NpcType.getRunSound(): Int {
    return getIntProperty("runSound")
}

fun NpcType.getSoundDistance(): Int {
    return getIntProperty("soundDistance")
}

fun NpcType.getPrimaryCursorOpcode(): Int {
    return getIntProperty("primaryCursorOpcode")
}

fun NpcType.getPrimaryCursor(): Int {
    return getIntProperty("primaryCursor")
}

fun NpcType.getSecondaryCursorOpcode(): Int {
    return getIntProperty("secondaryCursorOpcode")
}

fun NpcType.getSecondaryCursor(): Int {
    return getIntProperty("secondaryCursor")
}

fun NpcType.getAttackCursor(): Int {
    return getIntProperty("attackCursor")
}

fun NpcType.getArmyIcon(): Int {
    return getIntProperty("armyIcon")
}

fun NpcType.getSpriteId(): Int {
    return getIntProperty("spriteId")
}

fun NpcType.getAmbientSoundVolume(): Int {
    return getIntProperty("ambientSoundVolume")
}

fun NpcType.getMapFunction(): Int {
    return getIntProperty("mapFunction")
}

fun NpcType.isInvisiblePriority(): Boolean {
    return getBooleanProperty("invisiblePriority")
}

fun NpcType.getHue(): Int {
    return getIntProperty("hue")
}

fun NpcType.getIntProperty(): Int {
    return getIntProperty("saturation")
}

fun NpcType.getLightness(): Int {
    return getIntProperty("lightness")
}

fun NpcType.getOpacity(): Int {
    return getIntProperty("opacity")
}

fun NpcType.getMainOptionIndex(): Int {
    return getIntProperty("mainOptionIndex")
}

fun NpcType.getCampaigns(): IntArray {
    return getIntArrayProperty("campaigns")
}

fun NpcType.getABoolean2883(): Boolean {
    return getBooleanProperty("aBoolean2883")
}

fun NpcType.getAnInt2803(): Int {
    return getIntProperty("anInt2803")
}

fun NpcType.getAnInt2844(): Int {
    return getIntProperty("anInt2844")
}

fun NpcType.getAnInt2852(): Int {
    return getIntProperty("anInt2852")
}

fun NpcType.getAnInt2831(): Int {
    return getIntProperty("anInt2831")
}

fun NpcType.getAnInt2862(): Int {
    return getIntProperty("anInt2862")
}

class NpcCodec718 : DefinitionCodec<NpcType> {
    override fun NpcType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> {
                val length = buffer.readUnsignedByte().toInt()
                models = MutableList(length) { 0 }
                for (count in 0 until length) {
                    models!![count] = buffer.readBigSmart()
                    if (models!![count] == 65535) {
                        models!![count] = -1
                    }
                }
            }

            2 -> name = buffer.readString()
            12 -> size = buffer.readUnsignedByte().toInt()
            in 30..34 -> actions[-30 + opcode] = buffer.readString()
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            42 -> readColourPalette(buffer)
            60 -> {
                val length: Int = buffer.readUnsignedByte().toInt()
                chatheadModels = MutableList(length) { 0 }
                (0 until length).forEach {
                    chatheadModels!![it] = buffer.readBigSmart()
                }
            }

            93 -> isMinimapVisible = false
            95 -> combatLevel = buffer.readShort().toInt()
            97 -> widthScale = buffer.readShort().toInt()
            98 -> heightScale = buffer.readShort().toInt()
            99 -> renderPriority = 1
            100 -> ambient = buffer.readByte().toInt()
            101 -> contrast = buffer.readByte().toInt()
            102 -> {
                headIconGraphics = mutableListOf(0)
                headIconIndexes = mutableListOf(buffer.readUnsignedShort())
            }

            103 -> rotation = buffer.readShort().toInt()
            106, 118 -> readTransforms(buffer, opcode == 118)
            107 -> isInteractable = false
            109 -> isInteractable = false
            111 -> setExtraProperty("animateIdle", false)
            113 -> {
                setExtraProperty("primaryShadowColour", buffer.readShort().toInt().toShort())
                setExtraProperty("secondaryShadowColour", buffer.readShort().toInt().toShort())
            }

            114 -> {
                setExtraProperty("primaryShadowModifier", buffer.readShort().toInt().toShort())
                setExtraProperty("secondaryShadowModifier", buffer.readShort().toInt().toShort())
            }

            119 -> setExtraProperty("walkMask", buffer.readByte().toInt().toByte())
            121 -> {
                val translations: Array<IntArray?> = arrayOfNulls(models!!.size)
                val length = buffer.readUnsignedByte().toInt()
                for (count in 0 until length) {
                    val index = buffer.readUnsignedByte().toInt()
                    translations[index] = intArrayOf(
                        buffer.readByte().toInt(),
                        buffer.readByte().toInt(),
                        buffer.readByte().toInt()
                    )
                }
                setExtraProperty("translations", translations)
            }

            122 -> setExtraProperty("hitbarSprite", buffer.readBigSmart())
            123 -> height = buffer.readShort().toInt()
            125 -> setExtraProperty("respawnDirection", buffer.readByte().toInt().toByte())
            127 -> setExtraProperty("renderAnimations", buffer.readShort().toInt())
            128 -> buffer.readUnsignedByte().toInt()
            134 -> {
                var idleSound = buffer.readShort().toInt()
                if (idleSound == 65535) {
                    idleSound = -1
                }
                var crawlSound = buffer.readShort().toInt()
                if (crawlSound == 65535) {
                    crawlSound = -1
                }
                var walkSound = buffer.readShort().toInt()
                if (walkSound == 65535) {
                    walkSound = -1
                }
                var runSound = buffer.readShort().toInt()
                if (runSound == 65535) {
                    runSound = -1
                }

                setExtraProperty("idleSound", idleSound)
                setExtraProperty("crawlSound", crawlSound)
                setExtraProperty("walkSound", walkSound)
                setExtraProperty("runSound", runSound)
                setExtraProperty("soundDistance", buffer.readUnsignedByte().toInt())

            }

            135 -> {
                setExtraProperty("primaryCursorOpcode", buffer.readUnsignedByte().toInt())
                setExtraProperty("primaryCursor", buffer.readShort().toInt())
            }

            136 -> {
                setExtraProperty("secondaryCursorOpcode", buffer.readUnsignedByte().toInt())
                setExtraProperty("secondaryCursor", buffer.readShort().toInt())
            }

            137 -> setExtraProperty("attackCursor", buffer.readShort().toInt())
            138 -> setExtraProperty("armyIcon", buffer.readBigSmart())
            139 -> setExtraProperty("spriteId", buffer.readBigSmart())
            140 -> setExtraProperty("ambientSoundVolume", buffer.readUnsignedByte().toInt())
            141 -> renderPriority = 1
            142 -> setExtraProperty("mapFunction", buffer.readShort().toInt())
            143 -> setExtraProperty("invisiblePriority", true)
            in 150..154 -> actions[opcode - 150] = buffer.readString()
            155 -> {
                setExtraProperty("hue", buffer.readByte().toInt().toByte())
                setExtraProperty("saturation", buffer.readByte().toInt().toByte())
                setExtraProperty("lightness", buffer.readByte().toInt().toByte())
                setExtraProperty("opacity", buffer.readByte().toInt().toByte())
            }

            158 -> setExtraProperty("mainOptionIndex", 1.toByte())
            159 -> setExtraProperty("mainOptionIndex", 0.toByte())
            160 -> {
                val length = buffer.readUnsignedByte().toInt()
                setExtraProperty("campaigns", IntArray(length) { buffer.readShort().toInt() })
            }

            162 -> setExtraProperty("aBoolean2883", true)
            163 -> setExtraProperty("anInt2803", buffer.readUnsignedByte().toInt())
            164 -> {
                setExtraProperty("anInt2844", buffer.readShort().toInt())
                setExtraProperty("anInt2852", buffer.readShort().toInt())
            }

            165 -> setExtraProperty("anInt2831", buffer.readUnsignedByte().toInt())
            168 -> setExtraProperty("anInt2862", buffer.readUnsignedByte().toInt())
            249 -> readParameters(buffer)
        }
    }

    private fun readColourPalette(buffer: ByteBuf) {
        val length = buffer.readUnsignedByte().toInt()
        ByteArray(length) { buffer.readByte().toInt().toByte() }
    }

    override fun ByteBuf.encode(definition: NpcType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = NpcType()
}