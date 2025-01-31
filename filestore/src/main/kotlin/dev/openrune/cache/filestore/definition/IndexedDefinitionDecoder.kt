package dev.openrune.cache.filestore.definition

abstract class IndexedDefinitionDecoder<T : Definition>(
    index: Int,
    private val shift: Int,
    codec: DefinitionCodec<T>
) : DefinitionDecoder<T>(index, codec) {

    private val mask = (1 shl shift) - 1

    override fun getFile(id: Int) = id and mask
    override fun getArchive(id: Int) = id ushr shift
    override fun isRS2() = true
}
