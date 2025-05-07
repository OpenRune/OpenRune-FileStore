
import cc.ekblad.toml.TomlMapper
import cc.ekblad.toml.decode
import cc.ekblad.toml.get
import cc.ekblad.toml.model.TomlValue
import cc.ekblad.toml.serialization.from
import cc.ekblad.toml.tomlMapper
import cc.ekblad.toml.util.InternalAPI
import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import dev.openrune.OsrsCacheProvider.Companion.CACHE_REVISION
import dev.openrune.cache.*
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.item.ItemSlotType
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig.Companion
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig.Companion.registerPackType
import dev.openrune.cache.tools.tasks.impl.defs.PackType
import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.Js5GameValGroup
import dev.openrune.definition.RSCMHandler
import dev.openrune.definition.codec.ItemCodec
import dev.openrune.definition.codec.ObjectCodec
import dev.openrune.definition.codec.StructCodec
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.type.NpcType
import dev.openrune.definition.type.ObjectType
import dev.openrune.definition.type.StructType
import dev.openrune.definition.util.toArray
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf


val packTypes = mutableMapOf<String, PackTypee>()



class PackTypee(
    val index: Int,
    val archive: Int,
    val codecClass: KClass<*>,
    val name: String,
    val gameValGroup: Js5GameValGroup? = null,
    val tomlMapper: TomlMapper,
    val kType: KType
) {
    val typeClass: KClass<*> = codecClass.supertypes.firstOrNull()
        ?.arguments?.firstOrNull()?.type?.classifier as? KClass<*>
        ?: throw IllegalArgumentException("Type class not found for codec $codecClass")
}


fun registerPackType(
    archive: Int, codec: KClass<*>,
    name: String,
    gameValGroup: Js5GameValGroup? = null,
    index: Int = CONFIGS,
    tomlMapper: TomlMapper,
    kType: KType,
) {
    val packType = PackTypee(index,archive, codec, name, gameValGroup,tomlMapper,kType)
    packTypes[packType.name] = packType
}

fun initPacks() {
    registerPackType(ITEM, ItemCodec::class, "item",Js5GameValGroup.OBJTYPES, tomlMapper = tomlMapper {
        addDecoder<ItemType> { content , def: ItemType ->
            val ops = arrayOfNulls<String>(5)
            ops[0] = (content["op1"] as TomlValue.String).value
            //println(ops.contentDeepToString())
        }

        mapping<ItemType>(
            "resizeXX" to "resizeX",
            "price" to "cost"
        )
    }, kType = typeOf<List<ItemType>>())

    registerPackType(OBJECT, ObjectCodec::class, "object",Js5GameValGroup.LOCTYPES, tomlMapper = tomlMapper {

    }, kType = typeOf<List<ObjectType>>())

    registerPackType(STRUCT, StructCodec::class, "struct", tomlMapper = tomlMapper {

    }, kType = typeOf<List<StructType>>())

}

@OptIn(InternalAPI::class)
fun main() {
    initPacks()

    val document = TomlValue.from(test)
    document.properties.forEach { prop ->
        println("==========${prop.key}==============")
        val codec: PackTypee? = packTypes[prop.key]
        codec?.let {
            val test2 : List<Definition> = codec.tomlMapper.decode(codec.kType, prop.value)

            test2.forEach {
                val constructor = codec.codecClass.constructors.first()
                val params = constructor.parameters.size
                val codecInstance = if (params == 0) {
                    constructor.call() as DefinitionCodec<*>
                } else {
                    constructor.call(CACHE_REVISION) as DefinitionCodec<*>
                }

               packDefinitions(codec, it, codecInstance)
            }
        }

    }
}

private fun <T : Definition> packDefinitions(
    packType: PackTypee,
    definition: Definition,
    codec: DefinitionCodec<T>
) {
    println(definition)
}

val test = """
                [[item]]
                id = "item.df"
                resizeXX = 443434
                resizeX = 343434
                op1 = "Testing 1"
                price = 34
                
                [[item]]
                id = 49
                cost = 69
                resizeXX = 1
                resizeX = 1
                op1 = "Testing 2"
                
                [[object]]
                id = 34
                
                       
            """.trimIndent()