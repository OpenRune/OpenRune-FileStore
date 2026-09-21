package dev.openrune

import dev.openrune.rs2.QuickChatDecoder

fun main() {
    val cache = Rs2Cache.loadRemoteFromRev(950)
    val quickChat = QuickChatDecoder()
    quickChat.load(cache)
    quickChat.printTree()
}