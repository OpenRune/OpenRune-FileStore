rootProject.name = "filestore"

include("filesystem")
include("definition", "definition:osrs", "definition:rs2", "definition:opcode")
include("filestore", "filestore:osrs-fs", "filestore:rs2-fs")

include("tools")
include("displee")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://raw.githubusercontent.com/OpenRune/hosting/master")
        maven("https://jitpack.io")
        maven("https://repo.openrs2.org/repository/openrs2")
    }
}