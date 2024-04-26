dependencies {
    implementation("it.unimi.dsi:fastutil:8.5.13")
    implementation("com.github.jponge:lzma-java:1.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    implementation("dev.openrune:js5server:1.0.2")

}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

