// The one module that depends on both games on purpose: osrs and rs2 stay independent of each other,
// and anything wanting RS3 content inside an OSRS cache comes here instead.
dependencies {
    implementation(project(":definition"))
    implementation(project(":definition:osrs"))
    implementation(project(":definition:rs2"))
    implementation(project(":filesystem"))
    implementation(project(":filestore"))
    implementation(project(":filestore:osrs-fs"))
    implementation("io.netty:netty-buffer:4.1.107.Final")
}
