import dev.openrune.OsrsGameDataSource
import dev.openrune.Runescape717GameDataSource
import dev.openrune.cache.CacheManager
import dev.openrune.cache.filestore.Cache
import java.nio.file.Path

//Load a single cache this can be one we provide or custom
fun exmaple1() {
    CacheManager.init(OsrsGameDataSource(Cache.load(Path.of("E:\\RSPS\\Vorkath\\Vorkath-Server\\data\\cache-vorkath"), false),223))
    System.out.println(CacheManager.getItem(20709 + 30000).name)
}

//THis will combine 2 caches together eg
//if the id exist in bith caches it will use the first one loaded
fun exmaple2() {
    val cacheOSRS = OsrsGameDataSource(Cache.load(Path.of("E:\\RSPS\\Vorkath\\Vorkath-Server\\data\\cache-vorkath"), false),223)
    val cache742 = Runescape717GameDataSource(Cache.load(Path.of("C:\\Users\\Home\\Desktop\\Vorkath High Rev Data\\tmp\\cache\\cache-717"), false),717)

    CacheManager.init(cacheOSRS, cache742)
    System.out.println(CacheManager.getItem(28254).name)
    System.out.println(CacheManager.getItem(20709).name)
}


//THis will combine 2 caches together eg
//Also adds a offset eg [20709 will become [80709]]
fun exmaple3() {
    val cacheOSRS = OsrsGameDataSource(Cache.load(Path.of("E:\\RSPS\\Vorkath\\Vorkath-Server\\data\\cache-vorkath"), false),223)
    val cache742 = Runescape717GameDataSource(Cache.load(Path.of("C:\\Users\\Home\\Desktop\\Vorkath High Rev Data\\tmp\\cache\\cache-717"), false),717)

    cache742.itemOffset = 60000

    CacheManager.init(cacheOSRS, cache742)
    System.out.println(CacheManager.getItem(28254).name)
    System.out.println(CacheManager.getItem(20709 + cache742.itemOffset).name)
}




fun main(){
    exmaple3()

}