package dev.openrune.cache.filestore.definition

import dev.openrune.cache.CONFIGS

abstract class ConfigDefinitionDecoder<T : Definition>(
    codec: DefinitionCodec<T>,
    private val archive: Int,
) : DefinitionDecoder<T>(CONFIGS, codec) {

    override fun getArchive(id: Int) = archive

    override fun getFile(id: Int) = id
}
