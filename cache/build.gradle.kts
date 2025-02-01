allprojects {
    dependencies {
        implementation(project(":filestore"))
        if (project.name != "common") {
            api(project(":cache:common"))
        }
    }
}