import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    `maven-publish`
}

group = "dev.openrune"
version = "1.2.4"

dependencies {
    implementation(project(":filestore"))
    implementation("io.netty:netty-buffer:5.0.0.Alpha2")
    implementation("dev.openrune:js5server:1.0.2")
    implementation("net.lingala.zip4j:zip4j:2.11.5")
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}