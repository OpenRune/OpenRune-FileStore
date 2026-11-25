dependencies {
    implementation(project(":filestore"))
    implementation(project(":filesystem"))
    implementation(project(":definition"))
    implementation(project(":definition:osrs"))
    implementation(project(":filestore:osrs-fs"))
    implementation(project(":definition:rs2"))
    implementation(project(":filestore:rs2-fs"))
    implementation(project(":filestore:merged-fs"))
    implementation("io.netty:netty-buffer:4.1.107.Final")
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    implementation("me.tongfei:progressbar:0.9.2")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("commons-io:commons-io:2.15.1")
    api("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation(project(":displee"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    // logback-classic: clientscript-compiler configureLogLevel() expects Logback LoggerContext (not slf4j-simple).
    implementation("ch.qos.logback:logback-classic:1.5.16")
    implementation("dev.or2:toml-rsconfig:1.1")

    implementation("me.filby:clientscript-compiler:0.0.8-openrune")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")

    testImplementation(project(":filesystem"))
    testImplementation(project(":definition:opcode"))

}

tasks.test {
    useJUnitPlatform()
    // Opt in to CacheDecodeBench with `-Dbench=true`; it is skipped otherwise.
    System.getProperty("bench")?.let { systemProperty("bench", it) }
}



tasks.register<JavaExec>("runPackCache") {
    group = "rs2"
    description = "Packs a fresh OSRS cache with the RS3 content listed in PackCache."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("dev.openrune.cache.tools.tasks.impl.rs2.PackCache")
    maxHeapSize = "6g"
}
