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
