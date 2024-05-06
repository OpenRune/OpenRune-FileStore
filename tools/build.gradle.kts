import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    `maven-publish`
}

group = "dev.openrune"
version = "1.2.5"

dependencies {
    implementation(project(":filestore"))
    implementation("io.netty:netty-buffer:4.1.107.Final")
    implementation("dev.openrune:js5server:1.0.6")
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    implementation("cc.ekblad:4koma:1.1.0")
}