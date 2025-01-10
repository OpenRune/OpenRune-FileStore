import dev.openrune.cache.CacheManager
import dev.openrune.cache.definition.OsrsGameDataSource
import dev.openrune.cache.filestore.Cache
import java.nio.file.Path


fun main() {
    CacheManager.init(OsrsGameDataSource(Cache.load(Path.of("C:\\Users\\Home\\Desktop\\cache-oldschool-live-en-b225-2024-10-09-10-45-06-openrs2#1943\\cache"),true),225))

    println("Npc: " + CacheManager.getNpc(23))

}