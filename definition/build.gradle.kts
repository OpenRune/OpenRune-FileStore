
dependencies {
    implementation("io.netty:netty-buffer:4.1.107.Final")
    implementation("dev.or2:toml-core:1.0")
}

tasks.withType<JavaCompile> {
    options.annotationProcessorPath = configurations.annotationProcessor.get()
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}