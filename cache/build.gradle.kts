allprojects {
    dependencies {
        implementation(project(":core:buffer"))
        implementation(project(":core:filesystem"))
        implementation(project(":filestore"))
        implementation(project(":definition"))
        if (project.name != "common") {
            api(project(":cache:common"))
        }
    }
}