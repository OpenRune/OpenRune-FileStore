import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    `maven-publish`
}

group = "dev.openrune"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {

    // https://mvnrepository.com/artifact/me.tongfei/progressbar
    implementation("me.tongfei:progressbar:0.9.2")

    // https://mvnrepository.com/artifact/com.displee/rs-cache-library
    implementation("com.displee:rs-cache-library:7.1.0")
// https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:2.1.0-alpha1")

    implementation("com.github.jponge:lzma-java:1.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("it.unimi.dsi:fastutil:8.5.13")

    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.9"
    jvmTarget = "11"
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    repositories {
        maven {
            url = uri("$buildDir/repo")
        }
        if (System.getenv("REPO_URL") != null) {
            maven {
                url = uri(System.getenv("REPO_URL"))
                credentials {
                    username = System.getenv("REPO_USERNAME")
                    password = System.getenv("REPO_PASSWORD")
                }
            }
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}
