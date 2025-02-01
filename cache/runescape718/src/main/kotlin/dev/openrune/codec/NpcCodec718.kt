package dev.openrune.codec

import dev.openrune.buffer.Reader
import dev.openrune.buffer.Writer
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.data.ItemType
import dev.openrune.cache.filestore.definition.data.NpcType

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
    override fun NpcType.read(opcode: Int, buffer: Reader) {
        when (opcode) {
            1 -> {
                val length = buffer.readUnsignedByte()
                models = MutableList(length) { 0 }
                for (count in 0 until length) {
                    models!![count] = buffer.readBigSmart()
                    if (models!![count] == 65535) {
                        models!![count] = -1
                    }
                }
            }
            2 -> name = buffer.readString()
            12 -> size = buffer.readUnsignedByte()
            in 30..34 -> actions[-30 + opcode] = buffer.readString()
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            42 -> readColourPalette(buffer)
            60 -> {
                val length: Int = buffer.readUnsignedByte()
                chatheadModels = MutableList(length) { 0 }
                (0 until length).forEach {
                    chatheadModels!![it] = buffer.readBigSmart()
                }
            }
            93 -> isMinimapVisible = false
            95 -> combatLevel = buffer.readShort()
            97 -> widthScale = buffer.readShort()
            98 -> heightScale = buffer.readShort()
            99 -> hasRenderPriority = true
            100 -> ambient = buffer.readByte()
            101 -> contrast = 5 * buffer.readByte()
            102 -> {
                headIconArchiveIds = MutableList(0) { 0 }
                headIconSpriteIndex = MutableList(buffer.readShort()) { 0 }
            }
            103 -> rotation = buffer.readShort()
            106, 118 -> readTransforms(buffer, opcode == 118)
            107 -> isInteractable = false
            109 -> isInteractable = false
            111 -> setExtraProperty("animateIdle", false)
            113 -> {
                setExtraProperty("primaryShadowColour", buffer.readShort().toShort())
                setExtraProperty("secondaryShadowColour", buffer.readShort().toShort())
            }
            114 -> {
                setExtraProperty("primaryShadowModifier", buffer.readShort().toShort())
                setExtraProperty("secondaryShadowModifier", buffer.readShort().toShort())
            }
            119 -> setExtraProperty("walkMask", buffer.readByte().toByte())
            121 -> {
                val translations : Array<IntArray?> = arrayOfNulls(models!!.size)
                val length = buffer.readUnsignedByte()
                for (count in 0 until length) {
                    val index = buffer.readUnsignedByte()
                    translations[index] = intArrayOf(
                        buffer.readByte(),
                        buffer.readByte(),
                        buffer.readByte()
                    )
                }
                setExtraProperty("translations", translations)
            }
            122 -> setExtraProperty("hitbarSprite", buffer.readBigSmart())
            123 -> height = buffer.readShort()
            125 -> setExtraProperty("respawnDirection", buffer.readByte().toByte())
            127 -> setExtraProperty("renderAnimations", buffer.readShort())
            128 -> buffer.readUnsignedByte()
            134 -> {
                var idleSound = buffer.readShort()
                if (idleSound == 65535) {
                    idleSound = -1
                }
                var crawlSound = buffer.readShort()
                if (crawlSound == 65535) {
                    crawlSound = -1
                }
                var walkSound = buffer.readShort()
                if (walkSound == 65535) {
                    walkSound = -1
                }
                var runSound = buffer.readShort()
                if (runSound == 65535) {
                    runSound = -1
                }

                setExtraProperty("idleSound", idleSound)
                setExtraProperty("crawlSound", crawlSound)
                setExtraProperty("walkSound", walkSound)
                setExtraProperty("runSound" ,runSound)
                setExtraProperty("soundDistance", buffer.readUnsignedByte())

            }
            135 -> {
                setExtraProperty("primaryCursorOpcode", buffer.readUnsignedByte())
                setExtraProperty("primaryCursor", buffer.readShort())
            }
            136 -> {
                setExtraProperty("secondaryCursorOpcode", buffer.readUnsignedByte())
                setExtraProperty("secondaryCursor", buffer.readShort())
            }
            137 -> setExtraProperty("attackCursor", buffer.readShort())
            138 -> setExtraProperty("armyIcon", buffer.readBigSmart())
            139 -> setExtraProperty("spriteId", buffer.readBigSmart())
            140 -> setExtraProperty("ambientSoundVolume", buffer.readUnsignedByte())
            141 -> hasRenderPriority = true
            142 -> setExtraProperty("mapFunction", buffer.readShort())
            143 -> setExtraProperty("invisiblePriority", true)
            in 150..154 -> actions[opcode - 150] = buffer.readString()
            155 -> {
                setExtraProperty("hue", buffer.readByte().toByte())
                setExtraProperty("saturation", buffer.readByte().toByte())
                setExtraProperty("lightness", buffer.readByte().toByte())
                setExtraProperty("opacity", buffer.readByte().toByte())
            }
            158 -> setExtraProperty("mainOptionIndex", 1.toByte())
            159 -> setExtraProperty("mainOptionIndex", 0.toByte())
            160 -> {
                val length = buffer.readUnsignedByte()
                setExtraProperty("campaigns",IntArray(length) { buffer.readShort() })
            }
            162 -> setExtraProperty("aBoolean2883",true)
            163 -> setExtraProperty("anInt2803",buffer.readUnsignedByte())
            164 -> {
                setExtraProperty("anInt2844",buffer.readShort())
                setExtraProperty("anInt2852",buffer.readShort())
            }
            165 -> setExtraProperty("anInt2831",buffer.readUnsignedByte())
            168 -> setExtraProperty("anInt2862",buffer.readUnsignedByte())
            249 -> readParameters(buffer)
        }
    }

    private fun readColourPalette(buffer: Reader) {
        val length = buffer.readUnsignedByte()
        ByteArray(length) { buffer.readByte().toByte() }
    }

    override fun Writer.encode(definition: NpcType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = NpcType()
}