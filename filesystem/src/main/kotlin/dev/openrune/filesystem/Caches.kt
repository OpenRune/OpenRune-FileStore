package dev.openrune.filesystem

fun Cache.nextFreeIndex(from: Int = 0): Int {
    var id = from
    while (exists(id)) id++
    return id
}

fun Cache.nextFreeArchive(index: Int, from: Int = 0): Int {
    if (!exists(index)) return from
    val used = archives(index).toHashSet()
    var id = from
    while (id in used) id++
    return id
}

fun Cache.nextFreeFile(index: Int, archive: Int, from: Int = 0): Int {
    if (!exists(index)) return from
    val used = files(index, archive).toHashSet()
    var id = from
    while (id in used) id++
    return id
}
