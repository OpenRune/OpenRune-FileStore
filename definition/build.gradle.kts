plugins {
    kotlin("plugin.serialization") version "2.1.0-Beta1"
}
dependencies {
    implementation("io.netty:netty-buffer:4.1.107.Final")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.0-Beta1")
}

tasks.withType<JavaCompile> {
    options.annotationProcessorPath = configurations.annotationProcessor.get()
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}