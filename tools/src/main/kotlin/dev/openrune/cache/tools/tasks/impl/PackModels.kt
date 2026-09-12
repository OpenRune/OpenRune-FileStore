package dev.openrune.cache.tools.tasks.impl

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.cache.MODELS
import dev.openrune.cache.tools.incremental.PackUnit
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.decompressGzipToBytes
import dev.openrune.cache.util.getFiles
import dev.openrune.filesystem.Cache
import java.io.File
import java.nio.file.Files

class PackModels(
    private val modelDirectory: File,
    private val rscmMappingPrefix: String = "models."
) : CacheTask() {
    override fun init(cache: Cache) {
        val modelFiles = getFiles(modelDirectory, "gz", "dat")
        if (modelFiles.isEmpty()) return

        val root = modelDirectory.absoluteFile
        val units = modelFiles.map { file ->
            PackUnit(key = file.absoluteFile.relativeTo(root).path.replace('\\', '/'), source = file)
        }

        incremental.run(
            task = this,
            scope = modelDirectory.absolutePath,
            label = "Packing Models",
            cache = cache,
            units = units,
        ) { packCache, unit ->
            packModel(packCache, unit.sources.single())
        }
    }

    private fun packModel(cache: Cache, file: File) {
        val name = file.nameWithoutExtension
        val id: Int? = if (name.matches(Regex("-?\\d+"))) {
            name.toInt()
        } else {
            ConstantProvider.getMapping(rscmMappingPrefix + name.lowercase().replace(" ", "_"))
        }

        val buffer = if (file.extension == "gz") {
            decompressGzipToBytes(file.toPath())
        } else {
            Files.readAllBytes(file.toPath())
        }

        if (id != null) {
            cache.write(MODELS, id, 0, buffer)
        } else {
            println("Unable to pack model ${file.name}")
        }
    }
}
