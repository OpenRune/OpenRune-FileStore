package dev.openrune.cache.spritecopy

import java.io.File

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val sourceDir = File(
            ""
        )

        val targetDir = File(
            ""
        )

        val tomlFile = File(targetDir, "jak.sprites.toml")

        // Ensure target directory exists
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val tomlBuilder = StringBuilder()

        sourceDir.listFiles { file ->
            file.isFile && file.extension.equals("png", ignoreCase = true)
        }?.forEach { file ->

            // Filename without extension (e.g. "123" from "123.png")
            val spriteName = "jak_"+file.nameWithoutExtension

            // Copy file to target directory
//        val s = file.name
            val s = spriteName + ".png"
            val destinationFile = File(targetDir, s)
            file.copyTo(destinationFile, overwrite = true)

            // Write TOML entry
            tomlBuilder.appendLine("[$spriteName]")
            tomlBuilder.appendLine("path = \"$s\"")
            tomlBuilder.appendLine()
        }

        // Write TOML file
        tomlFile.writeText(tomlBuilder.toString())

        println("Done. Copied PNGs and generated sprites.toml")
    }

}
