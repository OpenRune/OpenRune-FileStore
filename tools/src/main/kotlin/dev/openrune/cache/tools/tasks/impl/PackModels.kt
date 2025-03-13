package dev.openrune.cache.tools.tasks.impl

import dev.openrune.definition.RSCMHandler
import dev.openrune.cache.MODELS
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.decompressGzipToBytes
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import dev.openrune.filesystem.Cache
import java.io.File
import java.nio.file.Files


class PackModels(
    private val modelDirectory: File,
    private val rscmMappingPrefix: String = "models."
) : CacheTask() {
    override fun init(cache: Cache) {
        val modelFiles = getFiles(modelDirectory, "gz", "dat")
        val modelSize = modelFiles.size
        val progressModels = progress("Packing Models", modelSize)

        if (modelSize > 0) {
            modelFiles.forEach { file ->
                val name = file.nameWithoutExtension
                val id: Int? = if (name.matches(Regex("-?\\d+"))) {
                    name.toInt()
                } else {
                    RSCMHandler.getMapping(rscmMappingPrefix + name.lowercase().replace(" ", "_"))
                }

                val buffer = if (file.extension == "gz") {
                    decompressGzipToBytes(file.toPath())
                } else {
                    Files.readAllBytes(file.toPath())
                }

                if (id != null) {
                    cache.write(MODELS, id, 0, buffer)
                } else {
                    println("Unable to pack model")
                }

                progressModels.step()
            }

            progressModels.close()
        }
    }
}