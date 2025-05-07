plugins {
    kotlin("plugin.serialization") version "1.9.0"
}
dependencies {
    implementation("io.netty:netty-buffer:4.1.107.Final")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
}

tasks.withType<JavaCompile> {
    options.annotationProcessorPath = configurations.annotationProcessor.get()
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}