package dev.openrune

import dev.openrune.rs2.OpenRs2CacheArchive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.io.File
import java.io.FileNotFoundException

/**
 * Decodes every quickchat category and phrase for every "runescape" build
 * the quickchat archive exists in, recording which builds succeed vs fail.
 * Builds where the archive doesn't exist yet are skipped, not failed. Not
 * part of the normal test suite - slow and network-heavy. Run explicitly:
 * `./gradlew :filestore:rs2-fs:test --tests dev.openrune.Rs2QuickChatScanTest -Dqcscan=true --rerun-tasks`
 */
@EnabledIfSystemProperty(named = "qcscan", matches = "true")
class Rs2QuickChatScanTest {

    @Test
    fun `scan every runescape build for quickchat decode failures`() {
        val caches = OpenRs2CacheArchive.allBuilds().filter { it.builds.first().major >= 400 }
        println("Scanning ${caches.size} builds (in build order)...")

        val results = caches.mapIndexed { index, entry ->
            val build = entry.builds.first().major
            val progress = "[${index + 1}/${caches.size}]"

            val status = try {
                Rs2Cache.loadRemote(scope = entry.scope, id = entry.id).use { cache ->
                    val categoryIds = cache.quickChatCategoryIds()
                    val phraseIds = cache.quickChatPhraseIds()

                    for (id in categoryIds) cache.readQuickChatCategory(id)
                    for (id in phraseIds) cache.readQuickChatPhrase(id)

                    Status.Ok(categoryIds.size, phraseIds.size)
                }
            } catch (e: FileNotFoundException) {
                Status.Skipped
            } catch (e: Throwable) {
                Status.Failed("${e.javaClass.simpleName}: ${e.message}")
            }

            val label = when (status) {
                is Status.Ok -> "OK (${status.categories} categories, ${status.phrases} phrases)"
                Status.Skipped -> "SKIPPED - quickchat archive not present yet"
                is Status.Failed -> "FAILED - ${status.reason}"
            }
            println("$progress build $build (cache ${entry.id}): $label")
            System.out.flush()

            ScanResult(build, entry.id, status)
        }

        writeReport(results)

        val ok = results.count { it.status is Status.Ok }
        val skipped = results.count { it.status is Status.Skipped }
        val failed = results.filter { it.status is Status.Failed }
        println("\n$ok/${results.size} builds decoded quickchat successfully, $skipped skipped (archive not present), ${failed.size} failed.")
        if (failed.isNotEmpty()) {
            println("Failed builds: ${failed.joinToString(", ") { it.build.toString() }}")
        }
    }

    private fun writeReport(results: List<ScanResult>) {
        val file = File("build/quickchat-scan", "report.md").apply { parentFile.mkdirs() }
        val ok = results.count { it.status is Status.Ok }
        val skipped = results.count { it.status is Status.Skipped }
        val failed = results.filter { it.status is Status.Failed }

        val lines = buildList {
            add("# RS2/RS3 quickchat decode scan")
            add("")
            add("$ok/${results.size} builds decoded successfully, $skipped skipped (archive not present), ${failed.size} failed. Only failures are listed below.")
            add("")
            add("| Build | Cache ID | Result |")
            add("|---|---|---|")
            for (r in failed) {
                add("| ${r.build} | ${r.cacheId} | FAILED: ${(r.status as Status.Failed).reason} |")
            }
        }

        file.writeText(lines.joinToString("\n"))
        println("\nReport written to ${file.absolutePath}")
    }

    private sealed class Status {
        class Ok(val categories: Int, val phrases: Int) : Status()
        object Skipped : Status()
        class Failed(val reason: String) : Status()
    }

    private class ScanResult(val build: Int, val cacheId: Int, val status: Status)
}
