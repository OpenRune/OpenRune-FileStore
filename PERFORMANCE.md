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

## Read path pass

The decode pass above left `Cache.data` as about 96% of a definition load. That has now been
addressed, so the end-to-end figures moved again:

| Workload                                   | Decode pass | Read pass | Change   |
|--------------------------------------------|-------------|-----------|----------|
| all definitions (`OsrsCacheProvider.init`)  | 845 ms      | 93 ms     | **-89%** |
| objects (62 400)                            | 537 ms      | 26 ms     | **-95%** |
| gamevals, all 15 groups (178 618)           | 881 ms      | 65 ms     | **-93%** |
| items (33 971)                              | 144 ms      | 16 ms     | **-89%** |
| npcs (16 338)                               | 43 ms       | 8 ms      | **-81%** |
| varbits (19 086)                            | 41 ms       | 1 ms      | **-98%** |
| sprites (8 559)                             | 289 ms      | 172 ms    | -40%     |
| interfaces + components (26 407)            | 68 ms       | 55 ms     | -19%     |
| `Cache.data` for 62 400 object payloads     | 493 ms      | 0 ms      | **-100%**|

What changed:

- `FileCache.data` mapped a file id to its slot with `IntArray.indexOf`, a linear scan of the
  archive's file id table run once per definition — roughly 2 × 10^9 comparisons for the object
  archive. File ids are cumulative deltas, so each table is now classified once at load as identity,
  ascending or neither, and looked up by slot, binary search or scan accordingly.
- A one entry memo in front of the archive LRU, since a definition load reads one archive
  repeatedly and the `LinkedHashMap` lookup boxed its key every time.
- Index files are read whole at open, so an archive read no longer costs a seek and a read on the
  index file. The largest index file is under a megabyte.
- Sector reads use a positional `FileChannel` read rather than a seek followed by a read, halving
  the syscalls per chunk, and a short read is now detected instead of being left as zeroes.
- Two `RandomAccessFile.length()` syscalls were being made per sector chunk despite the length
  already being passed in.

`FileCache.close` also now closes the index 255 handle, which it leaked.

## Where the remaining time goes

`sprites` is the only workload still meaningfully above its codec figure: 172 ms end to end against
22 ms in `SpriteCodec`. The gap is 8 559 separate archive reads and gzip inflations, one per sprite
group, which is inherent to the layout rather than to the lookup path. It is also the noisiest
figure in the harness, moving between roughly 140 ms and 175 ms across runs.

## Memory

Held by the cache after a load: 18 MB — the decoded payloads for the archives just read, which the
bounded LRU keeps resident. Bounding that cache by bytes rather than by entry count was tried and
reverted: the config archives fit inside any budget worth setting, so it reclaimed nothing while
adding per-insert bookkeeping.

### Memory pass over the definitions themselves

Measured by `CacheMemoryBench` (`-Dbench=true`): the settled heap delta from loading one definition
type and holding only its map, per type, on OSRS 240.

| Type       | Count  | Before  | After   | Change |
|------------|--------|---------|---------|--------|
| objects    | 62 400 | 35.3 MB | 28.2 MB | -20%   |
| items      | 33 971 | 23.5 MB | 20.4 MB | -13%   |
| npcs       | 16 338 | 14.8 MB | 10.5 MB | -29%   |
| anims      | 14 468 | 12.9 MB | 8.2 MB  | -36%   |
| dbrows     | 16 790 | 10.3 MB | 9.3 MB  | -10%   |
| interfaces | 26 407 | 14.6 MB | 13.5 MB | -8%    |
| enums      | 5 872  | 4.0 MB  | 3.3 MB  | -18%   |
| structs    | 3 990  | 2.6 MB  | 1.9 MB  | -27%   |
| sprites    | 8 559  | 23.7 MB | 23.5 MB | 0      |
| varbits    | 19 086 | 1.6 MB  | 1.6 MB  | 0      |
| **total**  |        | **143 MB** | **120 MB** | **-16%** |

What changed — deduplication plus primitive-backed storage on the decode path, none of it API
changes:

- `readString`/`readStringCP` route through a small direct-mapped pool, so the tens of thousands of
  repeated op names, entity names and examine lines collapse to one instance each.
- `EntityOpsDefinition.Op` instances are pooled the same way (`Op.of`), including the default
  "Take" op every item used to allocate.
- Boxed integers above the JVM's -128..127 cache are pooled (`BoxedInts`) where the same values
  recur across maps and cells: enum keys and values, params, and db row cells.
- Every decoded id, colour, transform and frame list is an `IntBackedList` — the declared field
  type stays `MutableList<Int>`, but the storage is a plain `IntArray`: four bytes per element
  instead of a boxed `Integer` and a reference. Equality follows the `List` contract, so it
  compares equal to any other list with the same values, and the TOML, merge and consumer code
  is untouched.

The pools are bounded, lock free and race tolerant — entries are immutable and equality-checked
before reuse, so a race costs a slot, never correctness. Load times are unchanged within noise.

What was left alone, deliberately: `sprites` is raw pixel data, and the remaining per-definition
cost of the config types is their fifty-plus declared scalar fields — the floor for the current
type shapes. Converting the fields to raw `IntArray` was considered and rejected: it would break
the public definition API for the same bytes the array-backed lists already save.

### Immutable types built through builders

A cache is full of copies — one object at four rotations is four ids with identical model, type
and colour lists, and thousands of doors share one op set. Sharing those safely needs genuine
immutability, so the types themselves became immutable and the mutability moved into builders
(this replaced an earlier freeze-at-the-manager pass):

- `ObjectType` is `val`-only (`id` excepted, which the load machinery assigns; `actions` excepted,
  which the TOML options hook replaces after construction). `ObjectCodec` decodes through
  `ObjectTypeBuilder`, `ObjectType.toBuilder()` gives tools an editable copy, and the
  `objectType { }` / `edit { }` DSL in `dev.openrune.cache.tools.dsl` wraps both.
- `EntityOpsDefinition` is immutable everywhere, assembled through `EntityOpsBuilder`. Every stock
  item shares one default "Take" op set, every op-less definition shares `EMPTY`, and equal op
  sets built anywhere collapse to one instance.
- The codec helper interfaces split into read/mutable pairs (`Recolourable`/`MutableRecolourable`
  and so on): immutable types implement the `val` side, builders and the still-mutable types the
  `var` side. They are unrelated hierarchies because Kotlin cannot override a `var MutableList`
  with a `val List`.

Because built instances can never change, `ObjectTypeBuilder.build()` shares equal lists through
`IntListPool` and `EntityOpsBuilder.build()` shares equal op sets — no freeze flags, no post-load
compaction pass, nothing for callers to remember.

| Type    | Before all passes | Now     | Change |
|---------|-------------------|---------|--------|
| objects | 35.3 MB           | 21.2 MB | -40%   |
| npcs    | 14.8 MB           | 9.5 MB  | -36%   |
| items   | 23.5 MB           | 17.3 MB | -26%   |

Ten-workload total: 143 MB → 109 MB (-24%). Object decode pays a few extra milliseconds for the
builder pass and content pooling (roughly 30 ms → 55 ms for all 62 400), which is what buys the
immutability and the sharing.

### Round-trip verification

`CodecByteRoundTripTest` locks the pooling down over every definition in a real cache:
`encode(decode(encode(x)))` must equal `encode(x)` byte for byte, for objects, npcs, items, anims,
enums, structs and db rows. Writing it surfaced five pre-existing encoder bugs, now fixed:

- `SoundData.writeSound` wrote a one byte sound id where the decoder reads two, and dropped the
  loop count entirely, so any encoded sequence with sounds could not be decoded again; the pre-220
  packed form also masked the loop count to zero.
- `ObjectCodec` wrote the sound-fade payload without its opcode 93 byte, corrupting the stream of
  any object with non-default fades.
- `ObjectCodec` divided `contrast` by 25 on encode while the decoder stores the raw byte, so small
  values collapsed to zero.
- `ObjectCodec` never wrote opcode 96 (`rasie`), silently dropping it on every pack.
- `SequenceCodec` wrote the decoder's internal 0x98967f sentinel as payload, growing
  `interleaveLeave` by one entry per round trip.

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

The decode pass was verified by re-encoding the full decode output and comparing SHA-256 digests
against the previous revision: 62 400 objects, 33 971 items, 16 338 npcs, 14 468 anims, 19 086
varbits, 5 725 varps, 5 872 enums, 3 990 structs, 16 790 db rows, 248 db tables, all 15 gameval
groups and 26 407 interface components — byte-identical, with zero encode failures.

The read pass added two test classes:

- `CacheByteRoundTripTest` packs payloads with the displee writer, reads them back with `FileCache`
  and asserts the bytes are unchanged. Two independent implementations, so it pins the archive
  lookup, the index tables and the sector walk. Covers sparse file ids, payloads spanning several
  sectors, every compression type, several indexes, and interleaved reads that thrash the caches.
- `CacheFileIndexTest` checks the new archive lookup agrees with a linear scan for every archive in
  a real cache — about 1.4 million lookups — and for ids that are absent.
