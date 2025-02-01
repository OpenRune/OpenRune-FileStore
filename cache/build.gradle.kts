allprojects {
    dependencies {
        implementation(project(":core:buffer"))
        implementation(project(":core:filesystem"))
        implementation(project(":filestore"))
        if (project.name != "common") {
            api(project(":cache:common"))
        }
    }
}