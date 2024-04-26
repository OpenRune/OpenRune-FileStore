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

<details>
<summary><b>Gradle Kotlin DSL (build.gradle.kts)</b></summary>

```kotlin
dependencies {
    implementation("dev.openrune:filestore:1.2.4")
}
```

</details>

<details>
<summary><b>Gradle Groovy DSL (build.gradle)</b></summary>

```groovy
dependencies {
    implementation 'dev.openrune:filestore:1.2.4'
}
```

</details>

<details>
<summary><b>Maven (pom.xml)</b></summary>

```xml
<dependency>
    <groupId>dev.openrune</groupId>
    <artifactId>filestore</artifactId>
    <version>1.2.4</version>
</dependency>
```

</details>

## Usage

The Builder pattern is employed to configure and execute tasks within the OpenRune File Store framework. This approach allows for a fluent and intuitive setup process, catering to various tasks such as cache installation, building, and running a JS5 server.

### Tools

<details>
<summary><b>Dump TypeId</b></summary>

The `DumpTypeId` tool from the OpenRune File Store suite is crafted to facilitate the extraction and dumping of identifiers for Items, NPCs, and Objects from the game's cache. These identifiers are then written to language-specific files, either Kotlin or Java, according to the user's preference.

### Usage Instructions

1. **Initialization**: Begin by creating an instance of `DumpTypeId`, providing it with the necessary paths and configuration settings. The `cache` parameter specifies the location of your game's cache, while `rev` indicates the cache's revision number. The `outputPath` determines where the generated code files will be saved, and `packageName` defines the package name for these generated files.

2. **Configuration**: Optionally, you can further customize the behavior of `DumpTypeId` by specifying the desired output `language` and customizing the `fileNames` for different data types. The `language` parameter can be set to either `Language.KOTLIN` or `Language.JAVA`, depending on your project's needs. The `fileNames` list allows you to define custom names for the generated files, corresponding to items, NPCs, and objects, enhancing organization and readability.

```kotlin
import dev.openrune.cache.tools.DumpTypeId
import dev.openrune.cache.tools.Language
import java.nio.file.Path

// Initialize the DumpTypeId tool with your cache settings
val dumper = DumpTypeId(
    cache = Path.of("./path/to/cache"), // Path to your cache directory
    rev = 220,                          // Cache revision number
    outputPath = Path.of("./output/path"), // Directory where output files will be saved
    packageName = "com.example.generated"  // Package name for generated code files
)

// (Optional) Configure additional settings
dumper.init(
    language = Language.KOTLIN,             // Choose between Kotlin or Java output
    fileNames = listOf("items", "npcs", "objs", "objsNull") // Custom file names
)
```

</details>

### Task Types:

- **FRESH_INSTALL**: Performs a clean installation of the cache, followed by the execution of specified tasks.
- **BUILD**: Constructs your cache based on the tasks you define.
- **RUN_JS5**: Initiates a JS5 server to serve your cache.

Below is an example showcasing how to utilize the Builder pattern to configure a fresh cache installation with custom tasks:

```kotlin
// Define your custom tasks. In this example, we're packing item definitions and maps.
val tasks: Array<CacheTask> = arrayOf(
    PackItems(File("./custom/definitions/items/")), // Pack custom item definitions.
    PackMaps(File("./custom/maps/"), File("./data/cache/xteas.json")) // Pack custom maps using XTEA keys.
)

// Initialize the builder with your desired task type, cache revision, and cache directory.
val builder = Builder(type = TaskType.FRESH_INSTALL, revision = cacheBuildValue, cacheDir = File("./data/cache/"))

// Add your custom tasks and build the cache, then initialize it to apply the changes.
builder.extraTasks(*tasks).build().initialize()
```

This setup begins with a fresh installation, ensuring that any existing data is replaced with the newly defined configurations and contents. Replace `cacheBuildValue` with the actual revision number for your cache. The `extraTasks` method allows you to specify additional tasks, such as `PackItems` and `PackMaps` in this case, which are then executed as part of the build process.


### Running JS5:

To initiate the JS5 server, you'll need to use the `Builder` with the task type set to `RUN_JS5`. Ensure you configure the necessary options to tailor the server to your needs. Below is a basic example to get you started:

```kotlin
val js5Server = Builder(type = TaskType.RUN_JS5, revision = 220)
js5Server.build().initialize()
```

## Builder Options

Configuring the JS5 server requires setting specific options via the Builder to ensure it runs correctly and efficiently. Here's a rundown of the critical options:

- **cacheRevision(rev: Int)**: Sets the cache revision that the JS5 server will use to validate client connections. Using `-1` bypasses this check, allowing any client version to connect. It's crucial for maintaining compatibility and ensuring clients are up-to-date.

  ```kotlin
  .cacheRevision(24) // Use the cache version, replace with your current version.
  ```

- **js5Ports(ports: List<Int>)**: Defines the ports on which the JS5 server will listen. This can be a single port or multiple ports to accommodate various network setups and requirements.

  ```kotlin
  .js5Ports(listOf(43594)) // Specify custom JS5 ports. Adjust according to your network configuration.
  ```

- **supportPrefetch(state: Boolean)**: Toggles prefetching capabilities of the JS5 server. Enabling prefetching can lead to faster data loading at the cost of increased memory usage. Consider your server's resource availability when enabling this feature.

  ```kotlin
  .supportPrefetch(true) // Enable prefetching for improved performance.
  ```

- **cacheLocation(cacheLocation: File)**: Specifies the directory where the cache is located. This setting is essential for directing the JS5 server to the correct cache data.

  ```kotlin
  .cacheLocation(File("./path/to/your/cache")) // Set the cache location. Ensure the path is correct and accessible.
  ```

Each of these options plays a vital role in the configuration of your JS5 server, ensuring it operates as intended and provides the best possible experience for users.


### Builder Options:

<details>
<summary><b>Builder Options</b></summary>

The Builder provides several configuration options to tailor the setup of your OpenRune File Store project. Below are the available methods and their descriptions:

- **`extraTasks(vararg types: CacheTask)`**: Specifies additional tasks to be executed during the build process. Accepts a variable number of `CacheTask` instances. This is useful for adding custom processing or data manipulation tasks to your cache build.

    ```kotlin
    builder.extraTasks(PackItems(...), PackMaps(...))
    ```

- **`cacheLocation(cacheLocation: File)`**: Sets the directory where the cache will be located. This is where your cache files will be stored and accessed from.

    ```kotlin
    builder.cacheLocation(File("./path/to/cache"))
    ```

- **`cacheRevision(rev: Int)`**: Defines the revision number for the cache. This can be used to specify the version of the cache that you are building or updating.

    ```kotlin
    builder.cacheRevision(194)
    ```

- **`js5Ports(ports: List<Int>)`**: Configures the ports to be used by the JS5 server. This allows you to specify one or more ports for the server to listen on, accommodating various network configurations.

    ```kotlin
    builder.js5Ports(listOf(43594, 43595))
    ```

- **`supportPrefetch(state: Boolean)`**: Enables or disables prefetch support for the JS5 server. Setting this to `true` allows the server to prefetch data, potentially improving performance and efficiency.

    ```kotlin
    builder.supportPrefetch(true)
    ```

Each of these methods returns the Builder instance, allowing for a fluent interface where methods can be chained together to configure the build process succinctly.

Here's a comprehensive example combining all the options:

```kotlin
val builder = Builder(type = TaskType.FRESH_INSTALL, revision = cacheBuildValue, cacheDir = File("./data/cache/"))
builder.extraTasks(PackItems(...), PackMaps(...))
      .cacheLocation(File("./path/to/cache"))
      .cacheRevision(194)
      .js5Ports(listOf(43594, 43595))
      .supportPrefetch(true)
      .build()
      .initialize()
```

This setup ensures a customized build process tailored to your project's specific needs and environment.

</details>

## Tasks Reference

The OpenRune File Store library offers a variety of tasks to facilitate the management and manipulation of game data for Old School RuneScape (OSRS). These tasks are designed to pack various types of data into the cache, utilizing both custom and standard formats. Below is an overview of available tasks, their functionalities, and examples of how to use them.


| Task Name     | Description                                                  | Arguments                                            | Usage Example                                                                 |
|---------------|--------------------------------------------------------------|------------------------------------------------------|-------------------------------------------------------------------------------|
| `PackMaps`    | Packs map files, supporting both `.gz` and `.dat` formats. | `mapsDir`, `xteasLocation (optional)`, `xteaType (optional)` | `PackMaps(File("./custom/maps/"), File("./data/cache/xteas.json"),XteaType.RANDOM_KEYS)` |
| `PackModels`  | Packs model files in `.gz` format. | `modelDir`                                           | `PackModels(File("./custom/models/"))`                                        |
| `PackItems`   | Packs item definitions from JSON files into the cache. | `itemDir`                                            | `PackItems(File("./custom/definitions/items/"))`                              |
| `PackNpcs`    | Packs NPC definitions from JSON files. | `npcDir`                                             | `PackNpcs(File("./custom/definitions/npcs/"))`                                |
| `PackObjects` | Packs object definitions from JSON files. | `objectDir`                                          | `PackObjects(File("./custom/definitions/objects/"))`                          |


It's important to ensure that the provided directories contain valid files, as the library will recursively search through all files and subdirectories within them.

### JSON Structure for Definitions

The JSON files used for definitions (`PackItems`, `PackNpcs`, `PackObjects`) should follow a structured format that outlines the properties of each game element. Here's an example for an item definition in JSON:

```json
{
  "id": 29432,
  "name": "Shooting Star Teleport",
  "inventoryModel": 52876
}
```

This structure includes essential attributes like the item's ID, name, and the model used in the inventory. Each task that involves packing from JSON will look for files with similar structures tailored to the specific game element being packed.

For detailed information on the parameters and structure of JSON files for each definition type, please refer to the [OpenRune FileStore Definitions Documentation](https://github.com/OpenRune/OpenRune-FileStore/tree/main/src/main/kotlin/dev/openrune/cache/filestore/definition/decoder).
