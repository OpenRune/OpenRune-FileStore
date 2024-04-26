plugins {
    kotlin("jvm") version "1.9.0"
    `maven-publish`
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "idea")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "dev.openrune"
    version = "1.2.4"

    java.sourceCompatibility = JavaVersion.VERSION_11

    repositories {
        mavenCentral()
        maven("https://raw.githubusercontent.com/OpenRune/hosting/master")
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation("com.beust:klaxon:5.5")
        implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
        implementation("me.tongfei:progressbar:0.9.2")
        implementation("com.displee:rs-cache-library:7.1.0")
        implementation("org.slf4j:slf4j-api:2.1.0-alpha1")
        implementation("com.google.code.gson:gson:2.10.1")
        implementation("commons-io:commons-io:2.15.1")
    }

}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(project(":filestore").sourceSets["main"].output)
    from(project(":tools").sourceSets["main"].output)
    from(sourceSets.main.get().allSource)
}

tasks.named("jar", Jar::class) {
    from(project(":filestore").sourceSets["main"].output)
    from(project(":tools").sourceSets["main"].output)

    // Set a strategy to handle duplicate files
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

publishing {
    repositories {
        maven {
            url = uri("$buildDir/repo")
        }
        if (System.getenv("REPO_URL") != null) {
            maven {
                url = uri(System.getenv("REPO_URL"))
                credentials {
                    username = System.getenv("REPO_USERNAME")
                    password = System.getenv("REPO_PASSWORD")
                }
            }
        }
    }


    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}
