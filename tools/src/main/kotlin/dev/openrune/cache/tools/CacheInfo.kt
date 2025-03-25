package dev.openrune.cache.tools

data class CacheInfo(
    val id: Int,
    val game: String,
    val environment : String,
    val timestamp: String,
    val builds: List<CacheInfoBuilds>,
    val sources: List<String>,
    val size: Long
) {
    data class CacheInfoBuilds(
        val major: Int
    )

}