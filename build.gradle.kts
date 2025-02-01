plugins {
    kotlin("jvm") version "1.9.0"
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "idea")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "dev.openrune"
    version = "1.3.5"

    java.sourceCompatibility = JavaVersion.VERSION_11

    repositories {
        mavenCentral()
        maven("https://raw.githubusercontent.com/OpenRune/hosting/master")
        maven("https://jitpack.io")
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation("it.unimi.dsi:fastutil:8.5.13")
        implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
        implementation("org.slf4j:slf4j-api:2.1.0-alpha1")
    }
}