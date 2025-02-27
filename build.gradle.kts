plugins {
    kotlin("jvm") version "1.9.0"
    id("maven-publish")
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "idea")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    group = "dev.or2"
    version = "2.0.0"

    java.sourceCompatibility = JavaVersion.VERSION_11

    repositories {
        mavenCentral()
        maven("https://raw.githubusercontent.com/OpenRune/hosting/master")
        maven("https://jitpack.io")
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation("it.unimi.dsi:fastutil:8.5.13")
        implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
        implementation("org.slf4j:slf4j-api:2.1.0-alpha1")
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])

                artifactId = project.name

                pom {
                    name.set("OpenRune - ${project.name}")
                    description.set("Module ${project.name} of the OpenRune project.")
                    url.set("https://github.com/OpenRune")

                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://opensource.org/licenses/Apache-2.0")
                        }
                    }

                    developers {
                        developer {
                            id.set("openrune")
                            name.set("OpenRune Team")
                            email.set("contact@openrune.dev")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/OpenRune.git")
                        developerConnection.set("scm:git:ssh://github.com/OpenRune.git")
                        url.set("https://github.com/OpenRune")
                    }
                }
            }
        }

        repositories {
            maven {
                url = uri("K:/rsprot/repo3")
            }
        }
    }
}