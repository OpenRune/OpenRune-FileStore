package dev.openrune.cache.tools

import dev.openrune.cache.CacheManager
import dev.openrune.cache.NPC
import java.nio.file.Path

fun main() {
    CacheManager.init(Path.of("C:\\Users\\Home\\Desktop\\cache-oldschool-live-en-b225-2024-10-09-10-45-06-openrs2#1943\\cache"),225)
    println("Npc Size: " + CacheManager.npcSize())
    println("Npc: " + CacheManager.getNpc(23))
    println("Npc Name: " + CacheManager.getNpc(23)!!.getName())
}