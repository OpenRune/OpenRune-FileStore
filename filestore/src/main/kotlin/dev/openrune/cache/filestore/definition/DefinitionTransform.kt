package dev.openrune.cache.filestore.definition

fun interface DefinitionTransform<T> {
    /**
     * Allows modifications to be made to the definition after loading.
     */
    fun changeValues(id: Int, definition: T)
}
