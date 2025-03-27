package dev.openrune

import dev.openrune.cache.CacheManager
import dev.openrune.cache.tools.Builder
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.filesystem.Cache
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File


class ConfigTest {


    companion object {
        val REV = 225
        val cacheLocation = File("${System.getProperty("java.io.tmpdir")}/openrune/$REV")

        @BeforeAll
        @JvmStatic
        fun initializeCache() {
            cacheLocation.mkdirs()

            val zipLoc = File(cacheLocation,"cache.zip")

            if (!zipLoc.exists()) {
                DownloadOSRS.downloadCache(REV,zipLoc)
            }
            val cacheLoc = File(cacheLocation,"cache")
            if (cacheLoc.deleteRecursively()) {
                DownloadOSRS.unZip(zipLoc,cacheLocation.absolutePath)
                val resourceFile = File(ConfigTest::class.java.classLoader.getResource("tomls")!!.toURI())
                val tasks: MutableList<CacheTask> = listOf(
                    PackConfig(resourceFile)
                ).toMutableList()

                val builder = Builder(type = TaskType.BUILD, revision = REV, cacheLoc)
                builder.registerRSCM(File(ConfigTest::class.java.classLoader.getResource("mappings")!!.toURI()))
                builder.extraTasks(*tasks.toTypedArray()).build().initialize()
                CacheManager.init(OsrsCacheProvider(Cache.load(cacheLoc.toPath(), false), REV))
            }
        }
    }


    @Test
    fun `test Merlin Rune Portal Object`() {
        val objectId = 60005
        val objectDef = CacheManager.getObjectOrDefault(objectId)

        assertEquals(objectDef.name, "Merlin Rune Portal")
        assertEquals(objectDef.animationId, 2212)
        assertEquals(objectDef.sizeX, 2)
        assertEquals(objectDef.sizeY, 2)
        assertEquals(objectDef.actions, listOf("Teleport", null, null, null, null))
        assertEquals(objectDef.objectModels, listOf(63101))
    }

    @Test
    fun `test blood money item`() {
        val item = 31671
        val itemDef = CacheManager.getItemOrDefault(item)

        assertEquals(itemDef.examine, "Lovely money!")
        assertEquals(itemDef.name, "Blood Money")
    }

    @Test
    fun `test Custom Pank Object Inherit`() {
        val objectId = 69
        val objectDef = CacheManager.getObjectOrDefault(objectId)

        assertEquals(objectDef.name, "Custom Pank")
        assertEquals(objectDef.objectModels, listOf(1844))
    }
}
