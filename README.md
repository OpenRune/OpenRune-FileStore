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

This structure includes essential attributes like the item's ID, name, and the model used in the inventory. Each task that involves packing from JSON will look for files with similar structures tailored to the specific game element being packed.

For detailed information, please refer to the [OpenRune FileStore Definitions Documentation](https://github.com/OpenRune/OpenRune-FileStore/wiki).
