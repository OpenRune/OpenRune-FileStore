package dev.openrune.decoder

import dev.openrune.cache.CONFIGS
import dev.openrune.cache.CacheManager
import dev.openrune.cache.STRUCT
import dev.openrune.cache.TEXTURES
import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.filestore.buffer.BufferReader
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.data.HealthBarType
import dev.openrune.cache.filestore.definition.data.HitSplatType
import dev.openrune.cache.filestore.definition.data.StructType
import dev.openrune.cache.filestore.definition.data.TextureType
import io.github.oshai.kotlinlogging.KotlinLogging
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import java.nio.BufferUnderflowException

class TextureDecoder : DefinitionDecoder<TextureType>(TEXTURES) {

    internal val logger = KotlinLogging.logger {}

    override fun getArchive(id: Int) = 0
    override fun isFlat() : Boolean = true
    override fun getFile(id: Int) = id
    override fun createDefinition(): TextureType = TextureType()

    override fun TextureType.read(opcode: Int, buffer: Reader) {
        averageRgb = buffer.readUnsignedShort()
        isTransparent = buffer.readUnsignedByte() == 1
        val count: Int = buffer.readUnsignedByte()

        if (count in 1..4) {
            fileIds = IntArray(count).toMutableList()
            for (index in 0 until count) {
                fileIds[index] = buffer.readUnsignedShort()
            }

            if (count > 1) {

                combineModes = IntArray(count -1).toMutableList()
                for (index in 0 until count - 1) {
                    combineModes[index] = buffer.readUnsignedShort()
                }

                field2440 = IntArray(count -1).toMutableList()
                for (index in 0 until count - 1) {
                    field2440[index] = buffer.readUnsignedShort()
                }

            }

            colourAdjustments = IntArray(count).toMutableList()
            for (index in 0 until count) {
                colourAdjustments[index] = buffer.readInt()
            }

            animationDirection = buffer.readUnsignedByte()
            animationSpeed = buffer.readUnsignedByte()
        }
    }

    override fun readLoop(definition: TextureType, buffer: Reader) {
        definition.read(0, buffer)
    }
}