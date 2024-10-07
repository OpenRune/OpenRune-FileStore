package dev.openrune.game.item

import net.runelite.cache.IndexType
import net.runelite.cache.ItemManager
import net.runelite.cache.SpriteManager
import net.runelite.cache.TextureManager
import net.runelite.cache.definitions.loaders.ModelLoader
import net.runelite.cache.definitions.providers.ModelProvider
import net.runelite.cache.fs.Archive
import net.runelite.cache.fs.Index
import net.runelite.cache.fs.Store
import net.runelite.cache.item.ItemSpriteFactory
import java.io.File

object ItemSpriteLoader {

    lateinit var itemManager: ItemManager

    fun init(cache : String) {

        val store = Store(File(cache))

        store.load()
        itemManager = ItemManager(store)
        itemManager.load()
        itemManager.link()

        val modelProvider = ModelProvider { modelId ->
            val models: Index = store.getIndex(IndexType.MODELS)
            val archive: Archive = models.getArchive(modelId)

            val data: ByteArray = archive.decompress(store.getStorage().loadArchive(archive))
            val inventoryModel = ModelLoader().load(modelId, data)
            inventoryModel
        }

        val spriteManager: SpriteManager = SpriteManager(store)
        spriteManager.load()

        val textureManager: TextureManager = TextureManager(store)
        textureManager.load()

        ItemSpriteBuilder.Companion.init(ItemSpriteFactory(itemManager,modelProvider,spriteManager,textureManager))
    }

}