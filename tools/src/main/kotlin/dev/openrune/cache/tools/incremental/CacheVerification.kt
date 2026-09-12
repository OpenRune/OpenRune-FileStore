package dev.openrune.cache.tools.incremental

/**
 * How a build decides whether the cache still matches what the previous build left behind.
 */
enum class CacheVerification {

    /**
     * Fingerprint the whole cache. Anything that touched it since the last build discards all state and
     * repacks. Cheap and strict; correct for a cache the packer owns end to end.
     */
    FINGERPRINT,

    /**
     * Compare each recorded output against the bytes actually in the cache, and repack the units whose
     * outputs no longer match.
     *
     * Use for a cache that is legitimately rewritten between builds — a server cache reseeded from a live
     * cache, then minified afterwards. Under [FINGERPRINT] such a cache never matches and repacks in full
     * every time. Here the reseeded entries that already hold the right bytes are skipped, while anything
     * the reseed overwrote or dropped — server-only definitions, most importantly — is detected and
     * repacked.
     *
     * Costs a read of every recorded output, so it is slower to decide than [FINGERPRINT] but far cheaper
     * than repacking everything.
     */
    OUTPUT_CRC,
}
