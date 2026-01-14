rootProject.name = "filestore"

include("filesystem")
include("definition", "definition:osrs", "definition:r718", "definition:rs3", "definition:opcode")
include("filestore", "filestore:r718-fs", "filestore:osrs-fs")

include("tools")
include("displee")

include("cache")