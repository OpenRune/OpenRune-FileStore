
dependencies {
    implementation("io.netty:netty-buffer:4.1.107.Final")
    implementation("dev.or2:toml-core:1.1")
}

tasks.withType<JavaCompile> {
    options.annotationProcessorPath = configurations.annotationProcessor.get()
}