import dev.openrune.OsrsCacheProvider
import dev.openrune.Runescape718Store
import dev.openrune.cache.CacheManager
import dev.openrune.cache.filestore.definition.ModelDecoder
import dev.openrune.definition.type.*
import dev.openrune.filesystem.Cache
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

    val identityKittype: MutableMap<Int, IdentityKitType> = mutableMapOf()
    OsrsCacheProvider.IdentityKitDecoder().load(osrsCache,identityKittype)

    val inventoryType: MutableMap<Int, InventoryType> = mutableMapOf()
    OsrsCacheProvider.InventoryDecoder().load(osrsCache,inventoryType)

    val spotType: MutableMap<Int, SpotAnimType> = mutableMapOf()
    OsrsCacheProvider.SpotAnimDecoder().load(osrsCache,spotType)

    val varcClient: MutableMap<Int, VarClientType> = mutableMapOf()
    OsrsCacheProvider.VarClientDecoder().load(osrsCache,varcClient)

    cacheRunescape718.itemOffset = 60000
    cacheRunescape718.npcOffset = 60000

    CacheManager.init(cacheRunescape718)


    System.out.println("RS2 ITEM 995 - ${CacheManager.getItemOrDefault(995 + 60000).name}")
    System.out.println("RS2 TokHaar-Kal-Ket 23659 - ${CacheManager.getItemOrDefault(23659 + 60000).name} : ${CacheManager.getItemOrDefault(23659 + 60000).maleModel0}")

    //System.out.println("${CacheManager.getNpcOrDefault(15454 + 60000).name} : ${CacheManager.getNpcOrDefault(15454 + 60000).models}")

    val modelRs1 = ModelDecoder(osrsCache).getModel(1)
    println(modelRs1.toString())

    val modelRs2 = ModelDecoder(runescape718Cache).getModel(70260)
    println(modelRs2.toString())

    //val textures: MutableMap<Int, TextureType> = mutableMapOf()
    //OsrsCacheProvider.TextureDecoder().load(osrsCache,textures)
    //println("Texture 0 File ID: ${textures[0]!!.fileIds.first()}")
    //println("Texture 4 File ID: ${textures[4]!!.fileIds.first()}")

    //val sprites: MutableMap<Int, SpriteType> = mutableMapOf()
    //SpriteDecoder().load(osrsCache,sprites)

    //val sprites718: MutableMap<Int, SpriteType> = mutableMapOf()
    //SpriteDecoder().load(runescape718Cache,sprites718)

    ////SpriteType.dumpAllSprites(sprites = sprites718, saveLocation = Path.of("./sprites718"), spriteSaveMode = SpriteSaveMode.SPRITE_SHEET) { total, done ->
        //println("Processed $done out of $total sprites.")
    //}

    //SpriteType.dumpAllSprites(sprites = sprites, saveLocation = Path.of("./spritesOSRS"), spriteSaveMode = SpriteSaveMode.SPRITE_SHEET) { total, done ->
      //  println("Processed $done out of $total sprites.")
    //}



}
