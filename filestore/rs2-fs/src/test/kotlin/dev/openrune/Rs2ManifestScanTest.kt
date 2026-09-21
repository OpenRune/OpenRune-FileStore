package dev.openrune

import com.google.gson.GsonBuilder
import dev.openrune.rs2.OpenRs2CacheArchive
import dev.openrune.rs2.Rs2ConfigGroup
import dev.openrune.rs2.Rs2Index
import dev.openrune.rs2.Rs2ManifestData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.io.File
import java.io.FileNotFoundException
import java.lang.reflect.Modifier

/**
 * Finds the earliest build each [Rs2Index] archive and [Rs2ConfigGroup] group
 * is observed present in, plus the last build with any XTEA keys, and writes
 * it to `src/main/resources/rs2-manifest.json` (loaded at runtime by
 * [dev.openrune.rs2.Rs2Manifest]). Stops early once everything's found.
 * Not part of the normal test suite - slow and network-heavy. Run explicitly:
 * `./gradlew :filestore:rs2-fs:test --tests dev.openrune.Rs2ManifestScanTest -Dmanifestscan=true --rerun-tasks`
 */
@EnabledIfSystemProperty(named = "manifestscan", matches = "true")
class Rs2ManifestScanTest {

    @Test
    fun `find when every archive and config group first appears`() {
        val caches = OpenRs2CacheArchive.allBuilds().filter { it.builds.first().major >= 400 }
        println("Scanning up to ${caches.size} builds (in build order)...")

        val xteaLastBuild = caches.filter { it.keys > 0 }.maxOfOrNull { it.builds.first().major }
        println("Last build with any XTEA keys: ${xteaLastBuild ?: "none observed"}")

        val archives = intConstants(Rs2Index::class.java)
        val configGroups = intConstants(Rs2ConfigGroup::class.java)

        val archiveFirstSeen = linkedMapOf<String, Int>()
        val configFirstSeen = linkedMapOf<String, Int>()

        for ((index, entry) in caches.withIndex()) {
            val build = entry.builds.first().major
            val progress = "[${index + 1}/${caches.size}]"

            try {
                Rs2Cache.loadRemote(scope = entry.scope, id = entry.id).use { cache ->
                    for ((name, id) in archives) {
                        if (name !in archiveFirstSeen && hasArchive(cache, id)) {
                            archiveFirstSeen[name] = build
                        }
                    }
                    for ((name, id) in configGroups) {
                        if (name !in configFirstSeen && hasGroup(cache, Rs2Index.CONFIG, id)) {
                            configFirstSeen[name] = build
                        }
                    }
                }
                println("$progress build $build (cache ${entry.id}): archives ${archiveFirstSeen.size}/${archives.size}, configs ${configFirstSeen.size}/${configGroups.size} found so far")
            } catch (e: Throwable) {
                println("$progress build $build (cache ${entry.id}): FAILED to open - ${e.javaClass.simpleName}: ${e.message}")
            }
            System.out.flush()

            if (archiveFirstSeen.size == archives.size && configFirstSeen.size == configGroups.size) {
                println("Everything accounted for - stopping early.")
                break
            }
        }

        writeManifest(archives, archiveFirstSeen, configGroups, configFirstSeen, xteaLastBuild)
    }

    private fun hasArchive(cache: Rs2Cache, archive: Int): Boolean = try {
        cache.cache.list(archive)
        true
    } catch (e: FileNotFoundException) {
        false
    }

    private fun hasGroup(cache: Rs2Cache, archive: Int, group: Int): Boolean = try {
        cache.cache.list(archive).asSequence().any { it.id == group }
    } catch (e: FileNotFoundException) {
        false
    }

    private fun intConstants(clazz: Class<*>): List<Pair<String, Int>> =
        clazz.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType }
            .map { it.name to it.getInt(null) }
            .sortedBy { it.second }

    private fun writeManifest(
        archives: List<Pair<String, Int>>,
        archiveFirstSeen: Map<String, Int>,
        configGroups: List<Pair<String, Int>>,
        configFirstSeen: Map<String, Int>,
        xteaLastBuild: Int?
    ) {
        val file = File("src/main/resources", "rs2-manifest.json").apply { parentFile.mkdirs() }

        val notObserved = archives.map { it.first }.filter { it !in archiveFirstSeen } +
            configGroups.map { it.first }.filter { it !in configFirstSeen }
        if (notObserved.isNotEmpty()) {
            println("Not observed in any scanned build (omitted from the manifest): ${notObserved.joinToString(", ")}")
        }

        val manifest = Rs2ManifestData(
            archives = archiveFirstSeen.toSortedMap(),
            configs = configFirstSeen.toSortedMap(),
            xteaLastBuild = xteaLastBuild
        )
        val json = GsonBuilder().setPrettyPrinting().create().toJson(manifest)
        file.writeText(json)
        println("\nManifest written to ${file.absolutePath}")
    }
}
