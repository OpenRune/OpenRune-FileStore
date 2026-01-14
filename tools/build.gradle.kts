dependencies {
    implementation(project(":filestore"))
    implementation(project(":filesystem"))
    implementation(project(":definition"))
    implementation(project(":definition:osrs"))
    implementation(project(":filestore:osrs-fs"))
    implementation("io.netty:netty-buffer:4.1.107.Final")
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    implementation("me.tongfei:progressbar:0.9.2")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("commons-io:commons-io:2.15.1")
    implementation(project(":cache"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    implementation("org.slf4j:slf4j-simple:2.0.3")
    implementation("cc.ekblad:4koma:1.2.2-openrune")
    implementation("me.filby:clientscript-compiler:0.0.2-openrune")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")

    testImplementation(project(":filesystem"))

}

tasks.test {
    useJUnitPlatform()
}