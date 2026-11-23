
dependencies {
    implementation("io.netty:netty-buffer:4.1.107.Final")
    implementation(project(":definition"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testImplementation("io.netty:netty-buffer:4.1.107.Final")
    testImplementation(project(":definition"))
}

tasks.test {
    useJUnitPlatform()
}
