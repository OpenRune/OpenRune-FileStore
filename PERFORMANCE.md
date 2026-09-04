# Cache read performance

Figures for the cache read path before and after the decode optimisation pass.

## Method

- Cache: `data/cache`, OSRS revision 240 (62 400 objects, 33 971 items, 16 338 npcs, 16 790 db rows).
- JDK 21, Gradle test JVM, Windows 11.
- Each figure is the **best of 4-5 in-process runs**. The first run of any workload is dominated by
  class loading and JIT warm-up and is discarded by taking the minimum.

Reproduce with:

```
./gradlew :tools:test --tests dev.openrune.CacheDecodeBench -Dbench=true --rerun-tasks
```

The harness (`tools/src/test/kotlin/dev/openrune/CacheDecodeBench.kt`) is skipped unless `-Dbench=true`
is set. It writes `openrune-decode-bench.txt` to the temp directory so two checkouts can be diffed.

## Codec only

Payloads are pulled out of the cache first, so the timed section is decode work only. This is the
part of the read path this repository controls.

| Codec  | Count  | Before | After | Change   |
|--------|--------|--------|-------|----------|
| dbrows | 16 790 | 204 ms | 4 ms  | **-98%** |
| enums  | 5 872  | 9 ms   | 1 ms  | **-89%** |
| sprites| 8 559  | 41 ms  | 25 ms | **-39%** |
| npcs   | 16 338 | 11 ms  | 8 ms  | -27%     |
| objects| 62 400 | 22 ms  | 17 ms | -23%     |
| items  | 33 971 | 18 ms  | 14 ms | -22%     |

## End to end

Includes `Cache.data`, so these are floored by cache I/O rather than by decode work.

| Workload                              | Before  | After  | Change   |
|---------------------------------------|---------|--------|----------|
| all definitions (`OsrsCacheProvider.init`) | 1171 ms | 911 ms | **-22%** |
| interfaces + components (26 407)      | 276 ms  | 59 ms  | **-79%** |
| db rows (16 790)                      | 271 ms  | 41 ms  | **-85%** |
| enums (5 872)                         | 16 ms   | 6 ms   | -63%     |
| sprites (8 559)                       | 217 ms  | 184 ms | -15%     |
| gamevals, all 15 groups (178 618)     | 831 ms  | 869 ms | ~0       |
| objects (62 400)                      | 491 ms  | 506 ms | ~0       |
| items (33 971)                        | 162 ms  | 159 ms | ~0       |
| npcs (16 338)                         | 44 ms   | 45 ms  | ~0       |

Resident heap after a definition load dropped from +115 MB to +105 MB.

Rows marked `~0` are I/O bound and move a few percent between runs in either direction; only their
codec-only figures above changed meaningfully.

## Where the remaining time goes

Splitting the object load:

| Stage                                 | Time   |
|---------------------------------------|--------|
| `Cache.data` for 62 400 object payloads | 478 ms |
| `ObjectCodec` over those payloads     | 17 ms  |

**About 96% of a definition load is now `Cache.data`, not decoding.** The cost is in `FileCache.data`:

```kotlin
val matchingIndex = files.getOrNull(index)?.getOrNull(archive)?.indexOf(file) ?: -1
```

`IntArray.indexOf` is a linear scan of the archive's file id table, run once per definition. For the
62 400-entry object archive that is roughly 2 × 10^9 comparisons, which matches the measured 478-530 ms.
File ids are stored as ascending cumulative deltas, so a binary search would remove nearly all of it.

That code lives in the `filesystem` module, which is slated for replacement, so it was left alone in
this pass. It is the single largest remaining win in the read path.

## What changed

Decode path:

- `CacheVarLiteral` id/char/name indexes are cached instead of rebuilt from the registry on every
  lookup (`byID` runs once per db-row cell, `byChar` twice per enum).
- `GameValList` gives gameval groups an id index, and `Interface` a child-id index, so
  `lookup`/`lookupAs` are constant time rather than a linear scan — the interface decode was
  quadratic in the component count.
- `ComponentDecoder` resolves the gameval once per group and skips reading payloads for unnamed
  components instead of decoding and discarding them.
- `EntityOpsDefinition` allocates its four backing lists on first touch rather than per definition,
  with non-allocating read views for callers that only inspect them.
- Codecs build lists in one pass instead of zero-filling and overwriting; `readString` decodes in one
  Latin-1 pass; sprite rasters are bulk-copied instead of read a byte at a time.
- `OpcodeList` resolves handlers through an opcode index.

Tooling:

- `CacheManager` bulk getters return read-only views instead of copying the table per call, and the
  `getXOrDefault` fallbacks are only constructed on a miss.
- `PackConfig` reuses one codec per pack type instead of building one per definition through Kotlin
  reflection, and caches merge field reflection per class.
- `PackIfType` reuses the decoder's gameval group instead of re-reading it per inherited interface.
- `RSCMProvider` parses each line's key once when writing gamevals instead of re-parsing the file per
  lookup.
- `SpriteSet.encode` indexes the palette by colour instead of scanning it per pixel.
- Hot regexes are compiled once rather than per call.

## Correctness

Every change was verified by re-encoding the full decode output and comparing SHA-256 digests against
the previous revision: 62 400 objects, 33 971 items, 16 338 npcs, 14 468 anims, 19 086 varbits,
5 725 varps, 5 872 enums, 3 990 structs, 16 790 db rows, 248 db tables, all 15 gameval groups and
26 407 interface components — byte-identical, with zero encode failures.
