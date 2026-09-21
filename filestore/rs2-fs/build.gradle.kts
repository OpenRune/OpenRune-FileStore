dependencies {
    implementation(project(":definition"))
    implementation("org.openrs2:openrs2-cache:0.1.0")
    implementation("org.openrs2:openrs2-cache-550:0.1.0")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

tasks.test {
    useJUnitPlatform()
    // Opt in to Rs2AllBuildsSpriteScanTest with `-Dscan=true`; it is skipped otherwise.
    System.getProperty("scan")?.let { systemProperty("scan", it) }
    // Rs2AllBuildsSpriteScanTest prints per-build progress as it runs - stream it live
    // instead of buffering everything until the (long-running) test method finishes.
    testLogging {
        showStandardStreams = true
    }
}
