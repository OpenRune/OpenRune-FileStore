package dev.openrune

import dev.openrune.rs2.OpenRs2CacheArchive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.io.File
import java.io.FileNotFoundException

@EnabledIfSystemProperty(named = "achscan", matches = "true")
class Rs2AchievementScanTest {

    @Test
    fun `scan every runescape build for achievement decode failures`() {
        val caches = OpenRs2CacheArchive.allBuilds().filter { it.builds.first().major >= 400 }
        println("Scanning ${caches.size} builds (in build order)...")

        val results = caches.mapIndexed { index, entry ->
            val build = entry.builds.first().major
            val progress = "[${index + 1}/${caches.size}]"

            val status = try {
                Rs2Cache.loadRemote(scope = entry.scope, id = entry.id).use { cache ->
                    val ids = cache.achievementIds()
                    for (id in ids) cache.readAchievement(id)
                    Status.Ok(ids.size)
                }
            } catch (e: FileNotFoundException) {
                Status.Skipped
            } catch (e: Throwable) {
                Status.Failed("${e.javaClass.simpleName}: ${e.message}")
            }

            val label = when (status) {
                is Status.Ok -> "OK (${status.count} achievements)"
                Status.Skipped -> "SKIPPED - achievements not present yet"
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
        println("\n$ok/${results.size} builds decoded achievements successfully, $skipped skipped (not present), ${failed.size} failed.")
        if (failed.isNotEmpty()) {
            println("Failed builds: ${failed.joinToString(", ") { it.build.toString() }}")
        }
    }

    private fun writeReport(results: List<ScanResult>) {
        val file = File("build/achievement-scan", "report.md").apply { parentFile.mkdirs() }
        val ok = results.count { it.status is Status.Ok }
        val skipped = results.count { it.status is Status.Skipped }
        val failed = results.filter { it.status is Status.Failed }

        val lines = buildList {
            add("# RS2/RS3 achievement decode scan")
            add("")
            add("$ok/${results.size} builds decoded successfully, $skipped skipped (not present), ${failed.size} failed. Only failures are listed below.")
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
        class Ok(val count: Int) : Status()
        object Skipped : Status()
        class Failed(val reason: String) : Status()
    }

    private class ScanResult(val build: Int, val cacheId: Int, val status: Status)
}
