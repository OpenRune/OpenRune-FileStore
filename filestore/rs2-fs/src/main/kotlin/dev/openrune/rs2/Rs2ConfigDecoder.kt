package dev.openrune.rs2

import dev.openrune.Rs2Cache
import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec

abstract class Rs2ConfigDecoder<T : Definition>(
    private val location: Rs2ConfigLocation,
) : Rs2Decoder<T> {

    protected abstract fun codecFor(build: Int): DefinitionCodec<T>

    override fun ids(cache: Rs2Cache): List<Int> = cache.configIds(location)

    override fun read(cache: Rs2Cache, id: Int): T =
        cache.readConfig(location, id, codecFor(cache.build ?: Int.MAX_VALUE))
}
