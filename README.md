# OpenRune Filestore

## About

OpenRune File Store is an extensive suite of tools and utilities designed specifically for the Old School RuneScape (OSRS) community. This collection is tailored to enhance the gaming experience by providing efficient solutions for packing and updating data, accompanied by a robust JS5 file server. It serves as a vital resource for both developers and gamers within the OSRS ecosystem, streamlining the process of data management and distribution. Whether you're looking to optimize your development workflow or ensure seamless game updates, OpenRune File Store offers the reliability and flexibility needed to support the evolving demands of OSRS runescape content.

## Adding OpenRune File Store

Add OpenRune File Store to your project to get started with our powerful OSRS tools and utilities.

### Adding the OpenRune Repository

#### Gradle Kotlin DSL (build.gradle.kts)

```kotlin
repositories {
    maven("https://raw.githubusercontent.com/OpenRune/hosting/master")
}
```

#### Gradle Groovy DSL (build.gradle)

```groovy
repositories {
    maven { url 'https://raw.githubusercontent.com/OpenRune/hosting/master' }
}
```

#### Maven (pom.xml)

```xml
<repositories>
    <repository>
        <id>openrune-repo</id>
        <url>https://raw.githubusercontent.com/OpenRune/hosting/master</url>
    </repository>
</repositories>
```

### Adding the Dependency

Modules are currently published under the dev.or2 organization at the above maven remote. The all module will include all of the modules in OpenRune-Filestore. Please see the following for instructions on adding it to your dependency manager.
<details>
<summary><b>Gradle Kotlin DSL (build.gradle.kts)</b></summary>

```kotlin
dependencies {
    implementation("dev.or2:all:2.1.1")
}
```

</details>

<details>
<summary><b>Gradle Groovy DSL (build.gradle)</b></summary>

```groovy
dependencies {
    implementation 'dev.or2:all:2.1.1'
}
```

</details>

<details>
<summary><b>Maven (pom.xml)</b></summary>

```xml
<dependency>
    <groupId>dev.or2</groupId>
    <artifactId>all</artifactId>
    <version>2.1.1</version>
</dependency>
```

</details>

## Openrune Modules Overview

Openrune is structured as a multi-module project. Below is a breakdown of the key modules and their purposes.

### Definition Modules

These modules focus on decoding definition data from byte arrays.

- **`definition-base`**  
  Core implementations of definition classes, along with generic cross-revision codecs and utility methods.

- **`opcode-kotlin`**  
  A Kotlin reflection-based framework for constructing definition codecs and abstracting low-level buffer interactions.

- **`osrs`**  
  Definition codecs specifically for decoding OSRS formats.

- **`r718`**  
  Definition codecs for decoding RuneScape 718 revision formats.

- **`rs3`**  
  Definition codecs for RS3 formats.

---

### Cache and File Handling Modules

These modules provide systems for loading, reading, and decoding cache files:

- **`displee`**  
  A fork of Displee's cache library with internal fixes. This is a temporary dependency until the original is updated and published properly to Maven Central.

- **`filestore`**  
  A shared abstraction layer for loading caches via the `filesystem` module and decoding them using the definition modules.

- **`osrs-fs`**  
  An OSRS-specific implementation of the `filestore` system.

- **`r718-fs`**  
  A 718-specific implementation of the `filestore` system.

- **`filesystem`**  
  Low-level library. Handles reading cache files and extracting them into byte arrays for higher-level decoding.

- **`tools`**  
  Utility module providing tools for cache packing, downloading caches from OpenRS2, and performing modifications.

---

### Incremental Packing

`TaskType.BUILD` and `TaskType.SERVER_CACHE_BUILD` only repack inputs that actually changed since the last
build. It is on by default:

```kotlin
cacheTool {
    taskType = TaskType.BUILD
    revision(240)
    cache("data/cache")

    incremental = true                              // default
    incrementalDatabase("build/packstate.db")       // optional; defaults to <cache>/incremental/packstate.db
}
```

State is kept in a SQLite database under `<cache>/incremental/`. That directory is preserved by cache
cleanup — a fresh install wipes the cache around it, and seeding a server cache skips it, so the two caches
keep separate state. Point `incrementalDatabase` anywhere you like; whichever directory holds it is the one
protected from cleanup.

An input is repacked when:

- its source files changed,
- a gameval or RSCM constant it resolved now maps to a **different id** — this covers every quoted
  `"table.key"` in a config, whether it is the `id`, an `inherit` target, or a param key, as well as sprite
  manifest entries and model names. Renumbering `obj.coins` repacks every config that referenced it and
  nothing else,
- a `%token%` it expanded changed — local `[[tokenizedReplacement]]` blocks through the file's own hash,
  mapper-level global tokens through `PackConfig`'s token set and token file,
- a cache entry it read was rewritten earlier in the same build (config inheritance, varbit/varp checks),
- another input writing the same cache entry changed — sprite sets and map tile/loc pairs are packed whole,
- one of its outputs is missing from the cache,
- the cache revision changed, or the cache was modified outside the packer.

Deleting a source file removes the cache entries it produced, unless another input still writes them.

#### Config granularity

`PackConfig` works per `[[block]]`, not per file. A file holding a hundred definitions repacks only the one
you edited; adding or removing a block leaves its neighbours untouched, and a removed block's cache entry is
pruned.

A definition is identified by its type and id rather than by the file it lives in, so **moving a definition
between config files repacks nothing** — it is the same definition, unchanged. Move it *and* edit it and
only that definition repacks.

Blocks are also ordered by their `inherit` graph, so a definition is always packed after the definition it
inherits from regardless of file or position. Previously a child that happened to sort before its parent was
dropped entirely on a cold build, and merged against the previous build's parent afterwards.

For a one-off full repack without editing code, set `OPENRUNE_FULL_REBUILD=1`.

Supported by `PackConfig`, `PackSprites`, `PackModels`, `PackMaps` and `PackCs2`. The remaining tasks —
`PackIfType`, `PackDBTables` and `PackAutoCert` — still run in full on every build.

#### CS2

Neptune compiles the project as a whole and reports no per-script dependencies, so CS2 is one coarse unit:
any file added, edited or removed under the `sources`, `symbols` or `libraries` paths in `neptune.toml`
recompiles everything, and an untouched project skips compilation entirely. Generated output under
`excluded` is ignored so a build does not look like it changed its own inputs.

The fingerprint is taken *after* symbols are dumped from the cache, so a config or gameval change that
alters a symbol also triggers a recompile. Scripts deleted from the project have their cache entries
removed.

#### Progress reporting

Every task reports through a `CacheProgress`, swappable from the DSL:

```kotlin
cacheTool {
    progress = MyProgress()              // or SilentCacheProgress to report nothing
}
```

The default opens one bar per section, which means one per config directory — noisy when a build has many.
Implement `CacheProgress` to render them your way; `buildStarted`/`buildFinished` bracket the run, so a
single bar can be held open across every task. `summary(label, packed, skipped)` reports how each section
resolved, and returning `ProgressTracker.None` from `begin` hides a section entirely. Sections with nothing
to do never call `begin` at all, so an up-to-date build need not print anything.

#### Adding incremental support to a new task

Route the task's work through the engine instead of looping over files. Cache writes, cache reads, gameval
lookups and gameval registrations are then recorded automatically:

```kotlin
class PackThings(private val directory: File) : CacheTask() {
    override fun init(cache: Cache) {
        val units = getFiles(directory, "json").map { PackUnit(it.name, it) }

        incremental.run(
            task = this,
            scope = directory.absolutePath,
            label = "Packing Things",
            cache = cache,
            units = units,
        ) { packCache, unit ->
            packCache.write(INDEX, id, 0, encode(unit.sources.single()))
        }
    }
}
```

Pick the unit so packing it is self-contained: if several files contribute to one cache entry, they belong
to the same unit. Write through the `Cache` the lambda is given, not the one passed to `init`.

---

### Aggregate Modules

To simplify dependency management, Openrune provides aggregate POM modules:

- **`all`**  
  Includes **all** modules listed above.

- **`all-osrs`**  
  Includes only the definition and osrs modules.

Both aggregate POMs use `compile` scope, meaning their subprojects are transitively exposed when added as dependencies.

---

This structure includes essential attributes like the item's ID, name, and the model used in the inventory. Each task that involves packing from JSON will look for files with similar structures tailored to the specific game element being packed.

For detailed information, please refer to the [OpenRune FileStore Definitions Documentation](https://github.com/OpenRune/OpenRune-FileStore/wiki).
