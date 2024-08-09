plugins {
    kotlin("plugin.serialization") version "1.9.0"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("com.github.jponge:lzma-java:1.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    repositories {
        maven {
            url = uri("K:/rsprot/repo")
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}