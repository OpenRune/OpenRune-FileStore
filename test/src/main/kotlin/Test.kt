import dev.openrune.OsrsCacheProvider
import dev.openrune.Runescape718Store
import dev.openrune.cache.CacheManager
import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.filestore.definition.data.TextureType
import dev.openrune.decoder.TextureDecoder
import java.nio.file.Path

fun main() {
    exmaple3()


    //val osrsCache = Cache.load(Path.of("E:\\RSPS\\Illerai\\Illerai-Server\\data\\cache"), false)
    //val cacheOSRS = OsrsCacheProvider(osrsCache,226)

    //CacheManager.init(cacheOSRS)


    //PossibleAnimations.dumpPossibleAnimations()

}

fun exmaple3() {



    val osrsCache = Cache.load(Path.of("E:\\RSPS\\Illerai\\Illerai-Server\\data\\cache"), false)
    val runescape718Cache = Cache.load(Path.of("C:\\Users\\Home\\Desktop\\cache-runescape-live-en-b719-2012-06-16-00-00-00-openrs2#303\\cache"), false)

    val cacheOSRS = OsrsCacheProvider(osrsCache,226)
    val cacheRunescape718 = Runescape718Store(runescape718Cache,718)

    cacheRunescape718.itemOffset = 60000
    cacheRunescape718.npcOffset = 60000

    CacheManager.init(cacheRunescape718)


    System.out.println(CacheManager.getItemOrDefault(995 + 60000).name)
    System.out.println(CacheManager.getNpcOrDefault(15454 + 60000).name)

    val textures: MutableMap<Int, TextureType> = mutableMapOf()
    TextureDecoder().load(osrsCache,textures)
    println(textures[0]!!.fileIds.first())

    //System.out.println(CacheManager.getItem(20709 + 60000).getColourPalette().recolourPalette.contentToString())
}
