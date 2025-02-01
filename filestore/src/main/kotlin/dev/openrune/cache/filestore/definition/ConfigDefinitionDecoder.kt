package dev.openrune.cache.filestore.definition

import dev.openrune.cache.CONFIGS
import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec

abstract class ConfigDefinitionDecoder<T : Definition>(
    codec: DefinitionCodec<T>,
    private val archive: Int,
) : DefinitionDecoder<T>(CONFIGS, codec) {

    override fun getArchive(id: Int) = archive

    override fun getFile(id: Int) = id
}
