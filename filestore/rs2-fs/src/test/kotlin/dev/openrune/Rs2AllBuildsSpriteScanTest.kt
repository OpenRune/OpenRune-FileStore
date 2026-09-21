package dev.openrune

import dev.openrune.rs2.OpenRs2CacheArchive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.io.File
import java.io.FileNotFoundException

/**
 * Attempts to read sprite archive 8 for every distinct "runescape" (main
 * game) build archive.openrs2.org has a validly archived cache for, and
 * records which builds succeed vs fail against this module's
 * [dev.openrune.rs2.OpenRs2ArchiveStore]/[Rs2Cache].
 *
 * Not part of the normal test suite - it makes a large number of real
 * network requests (one build per client revision, ever) and can take a
 * long time. Run it explicitly:
 *
 * ```
 * ./gradlew :filestore:rs2-fs:test --tests dev.openrune.Rs2AllBuildsSpriteScanTest -Dscan=true --rerun-tasks
 * ```
 *
 * Results are written to `build/sprite-scan/report.md` as well as printed to
 * stdout, since a scan this long is worth keeping a persisted copy of rather
 * than scrolling back through console output. See `README.md` in this module
 * for what a failure here usually means and where to look when the cache
 * format changes again.
 */
@EnabledIfSystemProperty(named = "scan", matches = "true")
class Rs2AllBuildsSpriteScanTest {

    @Test
    fun `scan every runescape build for sprite decode failures`() {
        // Builds below ~400 predate JS5 (flat .idx/.dat format); OpenRs2ArchiveStore
        // doesn't support that format at all, so there's nothing to learn from them.
        val caches = OpenRs2CacheArchive.allBuilds().filter { it.builds.first().major >= 400 }
        println("Scanning ${caches.size} builds (in build order)...")

        val results = caches.mapIndexed { index, entry ->
            val build = entry.builds.first().major

            val status = try {
                Rs2Cache.loadRemote(scope = entry.scope, id = entry.id).use { cache ->
                    val groups = cache.spriteGroups()
                    check(groups.isNotEmpty()) { "no sprite groups" }

                    for (group in listOf(groups.first(), groups.last()).distinct()) {
                        val frame = cache.readSprite(group = group).firstOrNull()
                        checkNotNull(frame) { "sprite $group decoded no frames" }
                        check(frame.width > 0 && frame.height > 0) { "sprite $group has no dimensions" }
                    }
                }
                Status.Ok
            } catch (e: FileNotFoundException) {
                // A specific resource archive.openrs2.org's public HTTP mirror doesn't
                // actually serve, even though our fetch chain correctly resolved which
                // resource to ask for. Confirmed (see README) to be a gap in what's
                // publicly reachable for some older caches, not a decode bug - so it
                // doesn't belong in the failure list, just noted separately.
                Status.Skipped("${e.javaClass.simpleName}: ${e.message}")
            } catch (e: Throwable) {
                Status.Failed("${e.javaClass.simpleName}: ${e.message}")
            }

            val result = ScanResult(build, entry.id, entry.diskStoreValid, status)
            val progress = "[${index + 1}/${caches.size}]"
            val validity = if (entry.diskStoreValid) "" else " (disk_store_valid=false)"
            val label = when (status) {
                is Status.Ok -> "OK"
                is Status.Skipped -> "SKIPPED - ${status.reason}"
                is Status.Failed -> "FAILED - ${status.reason}"
            }
            println("$progress build $build (cache ${entry.id})$validity: $label")
            System.out.flush()
            result
        }

        writeReport(results)

        val ok = results.count { it.status is Status.Ok }
        val skipped = results.filter { it.status is Status.Skipped }
        val failed = results.filter { it.status is Status.Failed }
        println("\n$ok/${results.size} builds decoded sprites successfully, ${skipped.size} skipped (not our bug), ${failed.size} failed.")
        if (failed.isNotEmpty()) {
            println("Failed builds: ${failed.joinToString(", ") { it.build.toString() }}")
        }
    }

    private fun writeReport(results: List<ScanResult>) {
        val file = File("build/sprite-scan", "report.md").apply { parentFile.mkdirs() }
        val ok = results.count { it.status is Status.Ok }
        val skipped = results.count { it.status is Status.Skipped }
        val failed = results.filter { it.status is Status.Failed }

        val lines = buildList {
            add("# RS2/RS3 sprite decode scan")
            add("")
            add(
                "$ok/${results.size} builds decoded sprite archive 8 successfully, " +
                    "$skipped skipped (resource genuinely missing from archive.openrs2.org's public " +
                    "mirror, not a decode bug), ${failed.size} failed. Only failures are listed below."
            )
            add("")
            add("| Build | Cache ID | Valid archive | Result |")
            add("|---|---|---|---|")
            for (r in failed) {
                add("| ${r.build} | ${r.cacheId} | ${if (r.diskStoreValid) "yes" else "no"} | FAILED: ${(r.status as Status.Failed).reason} |")
            }
        }

        file.writeText(lines.joinToString("\n"))
        println("\nReport written to ${file.absolutePath}")
    }

    private sealed class Status {
        object Ok : Status()
        class Skipped(val reason: String) : Status()
        class Failed(val reason: String) : Status()
    }

    private class ScanResult(val build: Int, val cacheId: Int, val diskStoreValid: Boolean, val status: Status)
}
