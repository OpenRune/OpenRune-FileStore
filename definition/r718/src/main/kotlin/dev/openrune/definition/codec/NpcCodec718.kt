package dev.openrune.definition.codec

import dev.openrune.buffer.*
import io.netty.buffer.ByteBuf
import dev.openrune.buffer.Writer
import dev.openrune.buffer.readBigSmartRD
import dev.openrune.buffer.readStringRD
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.NpcType

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
                val length = buffer.readUnsignedByteRD()
                models = MutableList(length) { 0 }
                for (count in 0 until length) {
                    models!![count] = buffer.readBigSmartRD()
                    if (models!![count] == 65535) {
                        models!![count] = -1
                    }
                }
            }
            2 -> name = buffer.readStringRD()
            12 -> size = buffer.readUnsignedByteRD()
            in 30..34 -> actions[-30 + opcode] = buffer.readStringRD()
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            42 -> readColourPalette(buffer)
            60 -> {
                val length: Int = buffer.readUnsignedByteRD()
                chatheadModels = MutableList(length) { 0 }
                (0 until length).forEach {
                    chatheadModels!![it] = buffer.readBigSmartRD()
                }
            }
            93 -> isMinimapVisible = false
            95 -> combatLevel = buffer.readShortRD()
            97 -> widthScale = buffer.readShortRD()
            98 -> heightScale = buffer.readShortRD()
            99 -> hasRenderPriority = true
            100 -> ambient = buffer.readByteRD()
            101 -> contrast = 5 * buffer.readByteRD()
            102 -> {
                headIconArchiveIds = MutableList(0) { 0 }
                headIconSpriteIndex = MutableList(buffer.readShortRD()) { 0 }
            }
            103 -> rotation = buffer.readShortRD()
            106, 118 -> readTransforms(buffer, opcode == 118)
            107 -> isInteractable = false
            109 -> isInteractable = false
            111 -> setExtraProperty("animateIdle", false)
            113 -> {
                setExtraProperty("primaryShadowColour", buffer.readShortRD().toShort())
                setExtraProperty("secondaryShadowColour", buffer.readShortRD().toShort())
            }
            114 -> {
                setExtraProperty("primaryShadowModifier", buffer.readShortRD().toShort())
                setExtraProperty("secondaryShadowModifier", buffer.readShortRD().toShort())
            }
            119 -> setExtraProperty("walkMask", buffer.readByteRD().toByte())
            121 -> {
                val translations : Array<IntArray?> = arrayOfNulls(models!!.size)
                val length = buffer.readUnsignedByteRD()
                for (count in 0 until length) {
                    val index = buffer.readUnsignedByteRD()
                    translations[index] = intArrayOf(
                        buffer.readByteRD(),
                        buffer.readByteRD(),
                        buffer.readByteRD()
                    )
                }
                setExtraProperty("translations", translations)
            }
            122 -> setExtraProperty("hitbarSprite", buffer.readBigSmartRD())
            123 -> height = buffer.readShortRD()
            125 -> setExtraProperty("respawnDirection", buffer.readByteRD().toByte())
            127 -> setExtraProperty("renderAnimations", buffer.readShortRD())
            128 -> buffer.readUnsignedByteRD()
            134 -> {
                var idleSound = buffer.readShortRD()
                if (idleSound == 65535) {
                    idleSound = -1
                }
                var crawlSound = buffer.readShortRD()
                if (crawlSound == 65535) {
                    crawlSound = -1
                }
                var walkSound = buffer.readShortRD()
                if (walkSound == 65535) {
                    walkSound = -1
                }
                var runSound = buffer.readShortRD()
                if (runSound == 65535) {
                    runSound = -1
                }

                setExtraProperty("idleSound", idleSound)
                setExtraProperty("crawlSound", crawlSound)
                setExtraProperty("walkSound", walkSound)
                setExtraProperty("runSound" ,runSound)
                setExtraProperty("soundDistance", buffer.readUnsignedByteRD())

            }
            135 -> {
                setExtraProperty("primaryCursorOpcode", buffer.readUnsignedByteRD())
                setExtraProperty("primaryCursor", buffer.readShortRD())
            }
            136 -> {
                setExtraProperty("secondaryCursorOpcode", buffer.readUnsignedByteRD())
                setExtraProperty("secondaryCursor", buffer.readShortRD())
            }
            137 -> setExtraProperty("attackCursor", buffer.readShortRD())
            138 -> setExtraProperty("armyIcon", buffer.readBigSmartRD())
            139 -> setExtraProperty("spriteId", buffer.readBigSmartRD())
            140 -> setExtraProperty("ambientSoundVolume", buffer.readUnsignedByteRD())
            141 -> hasRenderPriority = true
            142 -> setExtraProperty("mapFunction", buffer.readShortRD())
            143 -> setExtraProperty("invisiblePriority", true)
            in 150..154 -> actions[opcode - 150] = buffer.readStringRD()
            155 -> {
                setExtraProperty("hue", buffer.readByteRD().toByte())
                setExtraProperty("saturation", buffer.readByteRD().toByte())
                setExtraProperty("lightness", buffer.readByteRD().toByte())
                setExtraProperty("opacity", buffer.readByteRD().toByte())
            }
            158 -> setExtraProperty("mainOptionIndex", 1.toByte())
            159 -> setExtraProperty("mainOptionIndex", 0.toByte())
            160 -> {
                val length = buffer.readUnsignedByteRD()
                setExtraProperty("campaigns",IntArray(length) { buffer.readShortRD() })
            }
            162 -> setExtraProperty("aBoolean2883",true)
            163 -> setExtraProperty("anInt2803",buffer.readUnsignedByteRD())
            164 -> {
                setExtraProperty("anInt2844",buffer.readShortRD())
                setExtraProperty("anInt2852",buffer.readShortRD())
            }
            165 -> setExtraProperty("anInt2831",buffer.readUnsignedByteRD())
            168 -> setExtraProperty("anInt2862",buffer.readUnsignedByteRD())
            249 -> readParameters(buffer)
        }
    }

    private fun readColourPalette(buffer: ByteBuf) {
        val length = buffer.readUnsignedByteRD()
        ByteArray(length) { buffer.readByteRD().toByte() }
    }

    override fun Writer.encode(definition: NpcType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = NpcType()
}