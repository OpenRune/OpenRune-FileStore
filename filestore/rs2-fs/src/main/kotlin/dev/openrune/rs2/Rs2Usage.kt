package dev.openrune.rs2

import dev.openrune.Rs2Cache
import dev.openrune.definition.Rs2Recolourable
import dev.openrune.definition.codec.Rs2TextureCodec
import dev.openrune.definition.type.Rs2MapElementType
import dev.openrune.definition.type.Rs2NpcOverride
import io.netty.buffer.Unpooled

enum class Rs2Resource { SPRITE, MATERIAL, TEXTURE }

enum class Rs2Use(val resource: Rs2Resource) {
    MAP_SCENES(Rs2Resource.SPRITE),
    MAP_FUNCTIONS(Rs2Resource.SPRITE),
    NPC_MAP_ICONS(Rs2Resource.SPRITE),
    NPC_HEAD_ICONS(Rs2Resource.SPRITE),
    HITMARKS(Rs2Resource.SPRITE),
    HEADBARS(Rs2Resource.SPRITE),
    TEXTURE_SPRITES(Rs2Resource.SPRITE),

    MODEL_MATERIALS(Rs2Resource.MATERIAL),
    LOC_MATERIALS(Rs2Resource.MATERIAL),
    NPC_MATERIALS(Rs2Resource.MATERIAL),
    OBJ_MATERIALS(Rs2Resource.MATERIAL),
    SPOT_ANIM_MATERIALS(Rs2Resource.MATERIAL),
    IDENTITY_KIT_MATERIALS(Rs2Resource.MATERIAL),
    FLOOR_MATERIALS(Rs2Resource.MATERIAL),
    WATER_MATERIALS(Rs2Resource.MATERIAL),
    PARTICLE_MATERIALS(Rs2Resource.MATERIAL),
    BILLBOARD_MATERIALS(Rs2Resource.MATERIAL),
    ALL_MATERIALS(Rs2Resource.MATERIAL),

    MATERIAL_TEXTURES(Rs2Resource.TEXTURE),
    ;

    companion object {
        val SPRITES: List<Rs2Use> = entries.filter { it.resource == Rs2Resource.SPRITE }
        val MATERIALS: List<Rs2Use> = entries.filter { it.resource == Rs2Resource.MATERIAL && it != ALL_MATERIALS }
        val TEXTURES: List<Rs2Use> = entries.filter { it.resource == Rs2Resource.TEXTURE }
    }
}

class Rs2Usage(
    val byUse: Map<Rs2Use, Set<Int>>,
    val sprites: Set<Int>,
    val materials: Set<Int>,
    val textures: Set<Int>,
    val undecoded: Map<Rs2Use, Set<Int>> = emptyMap(),
) {
    fun of(resource: Rs2Resource): Set<Int> = when (resource) {
        Rs2Resource.SPRITE -> sprites
        Rs2Resource.MATERIAL -> materials
        Rs2Resource.TEXTURE -> textures
    }
}

object Rs2Usages {

    fun scan(cache: Rs2Cache, uses: Collection<Rs2Use>): Rs2Usage {
        val manager = Rs2CacheManager(cache)
        val byUse = LinkedHashMap<Rs2Use, Set<Int>>()
        val undecoded = LinkedHashMap<Rs2Use, Set<Int>>()
        uses.filter { it.resource != Rs2Resource.TEXTURE }.forEach { use ->
            val failed = LinkedHashSet<Int>()
            byUse[use] = idsFor(cache, manager, use, failed)
            if (failed.isNotEmpty()) undecoded[use] = failed
        }

        val sprites = collect(byUse, Rs2Resource.SPRITE)
        val materials = collect(byUse, Rs2Resource.MATERIAL)

        val textures = LinkedHashSet<Int>()
        if (Rs2Use.MATERIAL_TEXTURES in uses) {
            val failed = LinkedHashSet<Int>()
            textures += texturesOf(cache, materials, failed)
            byUse[Rs2Use.MATERIAL_TEXTURES] = textures
            if (failed.isNotEmpty()) undecoded[Rs2Use.MATERIAL_TEXTURES] = failed
        }

        return Rs2Usage(byUse, sprites, materials, textures, undecoded)
    }

    fun texturesOf(
        cache: Rs2Cache,
        materials: Collection<Int>,
        failed: MutableSet<Int> = LinkedHashSet(),
    ): Set<Int> {
        val textures = LinkedHashSet<Int>()
        materials.forEach { id ->
            val material = decode(id, failed) { cache.readMaterial(id) }
            if (material != null) textures += material.textures
        }
        return textures
    }

    private inline fun <T> decode(id: Int, failed: MutableSet<Int>, read: () -> T): T? =
        failed.track(id, runCatching(read).getOrNull())

    private fun <T> MutableSet<Int>.track(id: Int, value: T?): T? {
        if (value == null) add(id)
        return value
    }

    private fun collect(byUse: Map<Rs2Use, Set<Int>>, resource: Rs2Resource): Set<Int> =
        byUse.entries.filter { it.key.resource == resource }
            .flatMapTo(LinkedHashSet()) { it.value }

    private fun idsFor(
        cache: Rs2Cache,
        manager: Rs2CacheManager,
        use: Rs2Use,
        failed: MutableSet<Int>,
    ): Set<Int> {
        val ids = LinkedHashSet<Int>()
        when (use) {
            Rs2Use.MAP_SCENES -> cache.mapSceneIconIds().forEach { id ->
                val icon = decode(id, failed) { cache.readMapSceneIcon(id) } ?: return@forEach
                ids.addId(icon.graphic)
            }

            Rs2Use.MAP_FUNCTIONS -> cache.mapElementIds().forEach { id ->
                val element = decode(id, failed) { cache.readMapElement(id) } ?: return@forEach
                ids.addElement(element)
            }

            Rs2Use.NPC_MAP_ICONS -> {
                val elements = LinkedHashSet<Int>()
                manager.npcIds().forEach { id -> failed.track(id, manager.npc(id))?.mapElement?.let(elements::add) }
                elements.forEach { element ->
                    val decoded = decode(element, failed) { cache.readMapElement(element) } ?: return@forEach
                    ids.addElement(decoded)
                }
            }

            Rs2Use.NPC_HEAD_ICONS -> manager.npcIds().forEach { id ->
                val npc = failed.track(id, manager.npc(id)) ?: return@forEach
                ids.addId(npc.headIconLegacy)
                npc.headIcons.values.forEach { ids.addId(it.graphic) }
            }

            Rs2Use.HITMARKS -> manager.hitmarkIds().forEach { id ->
                val hitmark = failed.track(id, manager.hitmark(id)) ?: return@forEach
                ids.addId(hitmark.classGraphic)
                ids.addId(hitmark.leftGraphic)
                ids.addId(hitmark.middleGraphic)
                ids.addId(hitmark.rightGraphic)
            }

            Rs2Use.HEADBARS -> manager.headbarIds().forEach { id ->
                val headbar = failed.track(id, manager.headbar(id)) ?: return@forEach
                ids.addId(headbar.full)
                ids.addId(headbar.empty)
                ids.addId(headbar.fullPlayerGroup)
                ids.addId(headbar.emptyPlayerGroup)
                ids.addId(headbar.fullPlayerGroupTeam)
                ids.addId(headbar.emptyPlayerGroupTeam)
                ids.addId(headbar.leftCapSprite)
                ids.addId(headbar.rightCapSprite)
            }

            Rs2Use.TEXTURE_SPRITES -> if (cache.hasIndex(Rs2Index.TEXTURES)) {
                val codec = Rs2TextureCodec()
                cache.groups(Rs2Index.TEXTURES).forEach { group ->
                    cache.files(Rs2Index.TEXTURES, group).forEach { file ->
                        val texture = decode(file, failed) {
                            codec.loadData(file, Unpooled.wrappedBuffer(cache.groupBytes(Rs2Index.TEXTURES, group, file)))
                        } ?: return@forEach
                        ids.addId(texture.sprite)
                    }
                }
            }

            Rs2Use.MODEL_MATERIALS -> cache.modelIds().forEach { id ->
                val model = decode(id, failed) { cache.readModel(id) } ?: return@forEach
                model.renders.forEach { ids.addId(it.material) }
            }

            Rs2Use.LOC_MATERIALS -> manager.locIds().forEach { id ->
                val loc = failed.track(id, manager.loc(id)) ?: return@forEach
                ids.addRetextures(loc)
                ids.addOverrides(loc.appearanceOverrides)
            }

            Rs2Use.NPC_MATERIALS -> manager.npcIds().forEach { id ->
                val npc = failed.track(id, manager.npc(id)) ?: return@forEach
                ids.addRetextures(npc)
                ids.addOverrides(npc.appearanceOverrides)
                npc.spotShadowTexture?.firstOrNull()?.let { ids.addId(it) }
            }

            Rs2Use.OBJ_MATERIALS -> manager.objIds().forEach { id ->
                failed.track(id, manager.obj(id))?.let { ids.addRetextures(it) }
            }

            Rs2Use.SPOT_ANIM_MATERIALS -> manager.spotAnimIds().forEach { id ->
                failed.track(id, manager.spotAnim(id))?.let { ids.addRetextures(it) }
            }

            Rs2Use.IDENTITY_KIT_MATERIALS -> manager.identityKitIds().forEach { id ->
                failed.track(id, manager.identityKit(id))?.let { ids.addRetextures(it) }
            }

            Rs2Use.FLOOR_MATERIALS -> {
                manager.floorOverlayIds().forEach { id -> ids.addId(failed.track(id, manager.floorOverlay(id))?.material) }
                manager.floorUnderlayIds().forEach { id -> ids.addId(failed.track(id, manager.floorUnderlay(id))?.material) }
            }

            Rs2Use.WATER_MATERIALS -> manager.waterIds().forEach { id ->
                val water = failed.track(id, manager.water(id)) ?: return@forEach
                ids.addId(water.foamMaterial)
                ids.addId(water.normalMapMaterial1)
                ids.addId(water.normalMapMaterial2)
                ids.addId(water.normalMapMaterial3)
                ids.addId(water.emissiveMapMaterial)
            }

            Rs2Use.PARTICLE_MATERIALS -> manager.particleEmitterIds().forEach { id ->
                ids.addId(failed.track(id, manager.particleEmitter(id))?.material)
            }

            Rs2Use.BILLBOARD_MATERIALS -> manager.billboardIds().forEach { id ->
                ids.addId(failed.track(id, manager.billboard(id))?.material)
            }

            Rs2Use.ALL_MATERIALS -> ids.addAll(cache.materialIds())

            Rs2Use.MATERIAL_TEXTURES -> Unit
        }
        return ids
    }

    private fun MutableSet<Int>.addId(id: Int?) {
        if (id != null && id >= 0) add(id)
    }

    private fun MutableSet<Int>.addElement(element: Rs2MapElementType) {
        addId(element.sprite)
        addId(element.mouseOverGraphic)
        addId(element.flashSprite)
    }

    private fun MutableSet<Int>.addOverrides(overrides: List<Rs2NpcOverride>) {
        overrides.forEach { override ->
            override.retextures.forEach { group ->
                group.entries.forEach { entry ->
                    addId(entry.source)
                    addId(entry.destination)
                }
            }
        }
    }

    private fun MutableSet<Int>.addRetextures(definition: Rs2Recolourable) {
        definition.originalTextureColours?.forEach { addId(it) }
        definition.modifiedTextureColours?.forEach { addId(it) }
    }
}
