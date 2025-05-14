package dev.openrune.cache.filestore.definition

import dev.openrune.cache.MODELS
import dev.openrune.definition.codec.ModelCodec
import dev.openrune.definition.type.model.MeshDecodingOption
import dev.openrune.definition.type.model.ModelType
import dev.openrune.filesystem.Cache
import dev.openrune.definition.util.decompressGzip
import io.netty.buffer.Unpooled

class ModelDecoder(val cache: Cache?, private val options: List<MeshDecodingOption> = emptyList()) {

    fun getModel(id: Int) : ModelType? {
        val codec = ModelCodec(id,options)
        if (cache == null) {
            throw IllegalStateException("Cache is not initialized.")
        }
        val data = cache.data(MODELS, id)
        if (data != null) {
            return codec.read(Unpooled.wrappedBuffer(data))
        } else {
            println("Model not found: $id")
            return null
        }

    }

    fun getModelFromGzip(data: ByteArray, id: Int): ModelType {
        val decompressedData = decompressGzip(data)
        val codec = ModelCodec(id, options)
        return codec.read(Unpooled.wrappedBuffer(decompressedData))
    }

    fun getModel(data: ByteArray, id: Int): ModelType {
        val codec = ModelCodec(id, options)
        return codec.read(Unpooled.wrappedBuffer(data))
    }


}