import dev.openrune.OsrsCacheProvider
import dev.openrune.Runescape742Store
import dev.openrune.cache.CacheManager
import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.filestore.definition.data.TextureType
import dev.openrune.cache.tools.animations.PossibleAnimations
import dev.openrune.decoder.TextureDecoder
import java.nio.file.Path

fun main() {
    //exmaple3()


    val osrsCache = Cache.load(Path.of("E:\\RSPS\\Illerai\\Illerai-Server\\data\\cache"), false)
    val cacheOSRS = OsrsCacheProvider(osrsCache,226)

    CacheManager.init(cacheOSRS)


    PossibleAnimations.dumpPossibleAnimations()

}

fun exmaple3() {



    val osrsCache = Cache.load(Path.of("E:\\RSPS\\Illerai\\Illerai-Server\\data\\cache"), false)
    val runescape742Cache = Cache.load(Path.of("C:\\Users\\Home\\Desktop\\Vorkath High Rev Data\\build"), false)

    val cacheOSRS = OsrsCacheProvider(osrsCache,226)
    val cacheRunescape742 = Runescape742Store(runescape742Cache,742)

    cacheRunescape742.itemOffset = 60000

    CacheManager.init(cacheOSRS,cacheRunescape742)



    System.out.println(CacheManager.getItemOrDefault(995).name)
    System.out.println(CacheManager.getItemOrDefault(3).name)
    System.out.println(CacheManager.getNpcOrDefault(65527).name)
    System.out.println(CacheManager.getNpcOrDefault(1).name)

    System.out.println(CacheManager.getItemOrDefault(995 + 60000).name)
    System.out.println(CacheManager.getItemOrDefault(995 + 60000).inventoryModel)
    System.out.println(CacheManager.getItemOrDefault(995 + 60000).id)

    val textures: MutableMap<Int, TextureType> = mutableMapOf()
    TextureDecoder().load(osrsCache,textures)
    println(textures[0]!!.fileIds.first())

    //System.out.println(CacheManager.getItem(20709 + 60000).getColourPalette().recolourPalette.contentToString())
}
