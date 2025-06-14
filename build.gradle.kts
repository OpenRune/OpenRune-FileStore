import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    id("maven-publish")
}

val buildDirectory = System.getenv("HOSTING_DIRECTORY") ?: "K:\\documents\\GitHub\\hosting\\"
val buildNumber = "2.1"



subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "idea")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    group = "dev.or2"
    version = buildNumber

    java.sourceCompatibility = JavaVersion.VERSION_11

    repositories {
        mavenCentral()
        maven("https://raw.githubusercontent.com/OpenRune/hosting/master")
        maven("https://jitpack.io")
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation("com.michael-bull.kotlin-inline-logger:kotlin-inline-logger:1.0.3")
    }


    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
        }
    }

    val sourcesJar by tasks.registering(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifact(sourcesJar.get())
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
                url = uri(buildDirectory)
            }
        }
    }
}

val filesystemVersion = buildNumber
val definitionVersion = buildNumber
val definitionOsrsVersion = buildNumber
val definitionR718Version = buildNumber
val filestoreVersion = buildNumber
val filestoreOsrsFsVersion = buildNumber
val filestoreR718FsVersion = buildNumber
val toolsVersion = buildNumber
val displeeVersion = buildNumber

val nettyBufferVersion = "4.2.0.RC2"

group = "dev.or2"
version = buildNumber

publishing {
    publications {
        repositories {
            maven {
                url = uri(buildDirectory)
            }
        }
        create<MavenPublication>("all") {
            artifactId = "all"

            pom {
                name.set("OpenRune - all")
                description.set("Aggregate module including all OpenRune dependencies.")
                url.set("https://github.com/OpenRune/openrune-aggregates")

                withXml {
                    asNode().appendNode("dependencies").apply {
                        val filesystem = appendNode("dependency")
                        filesystem.appendNode("groupId", "dev.or2")
                        filesystem.appendNode("artifactId", "filesystem")
                        filesystem.appendNode("version", filesystemVersion)
                        filesystem.appendNode("scope", "compile")

                        val definition = appendNode("dependency")
                        definition.appendNode("groupId", "dev.or2")
                        definition.appendNode("artifactId", "definition")
                        definition.appendNode("version", definitionVersion)
                        definition.appendNode("scope", "compile")

                        val definitionOSRS = appendNode("dependency")
                        definitionOSRS.appendNode("groupId", "dev.or2")
                        definitionOSRS.appendNode("artifactId", "osrs")
                        definitionOSRS.appendNode("version", definitionOsrsVersion)
                        definitionOSRS.appendNode("scope", "compile")

                        val definition718 = appendNode("dependency")
                        definition718.appendNode("groupId", "dev.or2")
                        definition718.appendNode("artifactId", "r718")
                        definition718.appendNode("version", definitionR718Version)
                        definition718.appendNode("scope", "compile")

                        val filestore = appendNode("dependency")
                        filestore.appendNode("groupId", "dev.or2")
                        filestore.appendNode("artifactId", "filestore")
                        filestore.appendNode("version", filestoreVersion)
                        filestore.appendNode("scope", "compile")

                        val filestoreOSRS = appendNode("dependency")
                        filestoreOSRS.appendNode("groupId", "dev.or2")
                        filestoreOSRS.appendNode("artifactId", "osrs-fs")
                        filestoreOSRS.appendNode("version", filestoreOsrsFsVersion)
                        filestoreOSRS.appendNode("scope", "compile")

                        val filestore718 = appendNode("dependency")
                        filestore718.appendNode("groupId", "dev.or2")
                        filestore718.appendNode("artifactId", "r718-fs")
                        filestore718.appendNode("version", filestoreR718FsVersion)
                        filestore718.appendNode("scope", "compile")

                        val tools = appendNode("dependency")
                        tools.appendNode("groupId", "dev.or2")
                        tools.appendNode("artifactId", "tools")
                        tools.appendNode("version", toolsVersion)
                        tools.appendNode("scope", "compile")

                        val displee = appendNode("dependency")
                        displee.appendNode("groupId", "dev.or2")
                        displee.appendNode("artifactId", "displee")
                        displee.appendNode("version", toolsVersion)
                        displee.appendNode("scope", "compile")
                    }
                }
            }
        }

        create<MavenPublication>("allOsrs") {
            artifactId = "all-osrs"

            pom {
                name.set("OpenRune - all-osrs")
                description.set("Aggregate module including all OSRS-related dependencies.")
                url.set("https://github.com/OpenRune/openrune-aggregates")

                withXml {
                    asNode().appendNode("dependencies").apply {
                        val definition = appendNode("dependency")
                        definition.appendNode("groupId", "dev.or2")
                        definition.appendNode("artifactId", "definition")
                        definition.appendNode("version", definitionVersion)
                        definition.appendNode("scope", "compile")

                        val definitionOSRS = appendNode("dependency")
                        definitionOSRS.appendNode("groupId", "dev.or2")
                        definitionOSRS.appendNode("artifactId", "osrs")
                        definitionOSRS.appendNode("version", definitionOsrsVersion)
                        definitionOSRS.appendNode("scope", "compile")

                        val buffer = appendNode("dependency")
                        buffer.appendNode("groupId", "io.netty")
                        buffer.appendNode("artifactId", "netty-buffer")
                        buffer.appendNode("version", nettyBufferVersion)
                        buffer.appendNode("scope", "compile")
                    }
                }
            }
        }
    }
}
