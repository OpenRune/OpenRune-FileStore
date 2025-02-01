package dev.openrune.cache.tools.animations

import com.google.gson.Gson
import dev.openrune.cache.CacheManager
import kotlin.system.measureTimeMillis

object PossibleAnimations {

    data class NpcAnimData(
        val id: Int,
        val name: String,
        val stanceAnims: List<Int>?,
        val walkAnims: List<Int>?,
        val attackBlockAnims: List<Int>?,
        val deathAnims: List<Int>?,
        val otherAnims: List<Int>?,
        val type: String? = null
    )

    @JvmStatic
    fun dumpPossibleAnimations() {

        val time = measureTimeMillis {
            val noHumanoidNpcs : Boolean = false

            // Get a map of all the animations available in the cache
            val anims = getAnimationMap()

            // List to store NPC data
            val npcs = mutableListOf<Npc>()
            CacheManager.getNpcs().forEach {
                val npc = Npc()
                npc.id = it.key
                npc.name = it!!.value.name
                npc.stanceAnimation = it!!.value.standAnim
                npc.walkAnimation = it!!.value.walkAnim
                npcs.add(npc)

            }


            val npcAnimDataList = mutableListOf<NpcAnimData>()

            // Process each NPC and find possible animations
            val npcIterator = npcs.iterator()
            while (npcIterator.hasNext()) {
                val npc = npcIterator.next()
                val possibleAnims = mutableListOf<Int>()

                // Find animations that are related to the NPC's stance or walk animation
                for (anim in anims.values) {
                    val iterator = anim.frameIds.iterator()
                    while (iterator.hasNext()) {
                        val id = iterator.next()
                        var found = false

                        // Check if the NPC's stance animation frames are similar to the current animation frames
                        if (npc.stanceAnimation != -1) {
                            for (i in anims[npc.stanceAnimation]?.frameIds ?: emptyList()) {
                                if (id in i - 1000..i + 1000 && !possibleAnims.contains(anim.id)) {
                                    possibleAnims.add(anim.id)
                                    found = true
                                    break
                                }
                            }
                        }
                        if (npc.walkAnimation != -1 && !found) {
                            for (i in anims[npc.walkAnimation]?.frameIds ?: emptyList()) {
                                if (id in i - 1000..i + 1000 && !possibleAnims.contains(anim.id)) {
                                    possibleAnims.add(anim.id)
                                    break
                                }
                            }
                        }
                    }
                }

                val attackBlockAnims = mutableListOf<Int>()
                val deathAnims = mutableListOf<Int>()
                val walkAnims = mutableListOf<Int>()
                val stanceAnims = mutableListOf<Int>()
                val otherAnims = mutableListOf<Int>()

                val iterator = possibleAnims.iterator()
                while (iterator.hasNext()) {
                    val i = iterator.next()
                    val a = anims[i]
                    var found = false
                    for (n in npcs) {
                        if (n.walkAnimation == i) {
                            walkAnims.add(i)
                            found = true
                            break
                        } else if (n.stanceAnimation == i) {
                            stanceAnims.add(i)
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        if (a!!.forcedPriority == 10 || a!!.lastFrameLength > a!!.secondLastFrameLength * 5) {
                            deathAnims.add(i)
                        } else if (a.forcedPriority == 6 || a.forcedPriority == 8) {
                            attackBlockAnims.add(i)
                        } else {
                            otherAnims.add(i)
                        }
                    }
                }

                // Add the processed NPC data to the list
                npcAnimDataList.add(
                    NpcAnimData(
                        npc.id,
                        npc.name,
                        if (noHumanoidNpcs && walkAnims.contains(819)) null else stanceAnims,
                        if (noHumanoidNpcs && walkAnims.contains(819)) null else walkAnims,
                        attackBlockAnims,
                        deathAnims,
                        otherAnims,
                        if (noHumanoidNpcs && walkAnims.contains(819)) "HUMANOID" else null
                    )
                )
            }
            println(Gson().newBuilder().setPrettyPrinting().create().toJson(npcAnimDataList))
            println("===================")
        }


        println("Taken: ${time}")


    }


    fun getAnimationMap(): HashMap<Int, Animation> {
        val anims = HashMap<Int, Animation>()

        CacheManager.getAnims().forEach {
            val anim = Animation()

            val definition = it.value
            anim.id = it.key

            if (definition.frameDelays != null && definition.frameDelays!!.size >= 2) {
                anim.lastFrameLength = definition.frameDelays!![definition.frameDelays!!.size - 1]
                anim.secondLastFrameLength = definition.frameDelays!![definition.frameDelays!!.size - 2]
            }
            definition.frameIDs?.forEach { anim.frameIds.add(it) }
            if (definition.priority != -1) {
                anim.forcedPriority = definition.priority
            }
            if (definition.forcedPriority != 5) {
                anim.forcedPriority = definition.forcedPriority
            }
            anims[anim.id] = anim
        }

        return anims
    }

}