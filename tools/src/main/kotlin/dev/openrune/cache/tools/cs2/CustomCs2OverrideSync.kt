package dev.openrune.cache.tools.cs2

import com.github.michaelbull.logging.InlineLogger
import java.io.File

class CustomCs2OverrideSync(
    private val cs2Dir: File,
    private val revision: Int
) {

    private val logger = InlineLogger()

    private val customDir = File(cs2Dir, "custom")

    private val scriptsDir = File(cs2Dir, "script")

    private val backupRoot = File(cs2Dir, ".backup")

    companion object {
        private val SCRIPT_HEADER = Regex("""(?m)^\[(clientscript|proc),([^\]]+)]""")
        private val SCRIPT_BLOCK = Regex("""(?ms)^\[(clientscript|proc),([^\]]+)].*?(?=^\[(?:clientscript|proc),|\z)""")
    }

    fun sync() {
        val overrides = collectCustomOverrides()
        removeOverriddenScripts(overrides)
        restoreMissingScripts(overrides)
    }

    private fun collectCustomOverrides(): Set<String> {
        val result = mutableSetOf<String>()
        customDir.walkTopDown().filter { it.isFile && it.extension.equals("cs2", true) }.forEach { file ->
            val text = runCatching {
                file.readText()
            }.getOrNull() ?: return@forEach

            SCRIPT_HEADER.findAll(text).forEach {
                result += it.value
            }
        }
        return result
    }

    private fun removeOverriddenScripts(overrides: Set<String>) {
        overrides.forEach { header ->
            val file = File(scriptsDir, "$header.cs2")

            if (!file.exists()) {
                return@forEach
            }

            backup(file)

            if (!file.delete()) {
                return@forEach
            }

        }
    }

    private fun restoreMissingScripts(overrides: Set<String>) {
        if (!backupRoot.exists()) {
            return
        }

        backupRoot.walkTopDown().filter { it.isFile && it.extension.equals("cs2", true) }.forEach { backup ->
            val text = runCatching {
                backup.readText()
            }.getOrNull() ?: return@forEach

            val scripts = splitScripts(text)

            val restore = scripts.filter {
                val header = SCRIPT_HEADER.find(it)?.value
                header !in overrides
            }

            if (restore.isEmpty()) {
                return@forEach
            }

            val relative = backup.relativeTo(backupRoot)

            val target = File(scriptsDir, relative.path)

            val existing = if (target.exists()) target.readText() else ""
            val existingHeaders = SCRIPT_HEADER.findAll(existing).map { it.value }.toSet()

            val toAppend = restore.filter {
                val header = SCRIPT_HEADER.find(it)?.value
                header !in existingHeaders
            }

            if (toAppend.isEmpty()) {
                return@forEach
            }

            target.parentFile.mkdirs()

            val builder = StringBuilder()

            if (existing.isNotBlank()) {
                builder.append(existing.trimEnd())
                builder.append("\n\n")
            }

            builder.append(toAppend.joinToString("\n\n"))

            target.writeText(builder.toString())

            logger.info {
                "Restored ${toAppend.size} script(s) into ${target.absolutePath}"
            }
        }
    }

    private fun backup(file: File) {
        val relative = scriptsDir.toPath().relativize(file.toPath())

        val backup = File(backupRoot, relative.toString())

        backup.parentFile.mkdirs()

        if (!backup.exists()) {
            file.copyTo(backup, overwrite = false)
        }
    }

    private fun splitScripts(text: String): List<String> {
        return SCRIPT_BLOCK.findAll(text).map { it.value.trim() }.toList()
    }
}