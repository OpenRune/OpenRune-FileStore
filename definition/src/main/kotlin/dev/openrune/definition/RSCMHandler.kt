package dev.openrune.definition

import java.io.File

object RSCMHandler {

    var mappings: Map<String, Int> = emptyMap()
    var rscmTypes: MutableList<String> = emptyList<String>().toMutableList()

    fun load(mappingsDir: File) {

        if (!mappingsDir.exists()) {
            throw IllegalStateException("Missing Mapping Files")
        }

        val allMappings = mutableMapOf<String, Int>()

        mappingsDir.listFiles { _, name -> name.endsWith(".rscm") }?.forEach { file ->
            val type = file.nameWithoutExtension
            rscmTypes.add(type)
            val infoMap = file.readLines()
                .filter { it.isNotBlank() }
                .associate { line ->
                    val (key, value) = line.split(":").takeIf { it.size == 2 }
                        ?: throw IllegalArgumentException("Invalid line format: $line")
                    "${type}.${key}" to value.toInt()
                }

            allMappings.putAll(infoMap)
        }

        mappings = allMappings
    }

    fun getMapping(key: String): Int? {
        return mappings[key]
    }
}
