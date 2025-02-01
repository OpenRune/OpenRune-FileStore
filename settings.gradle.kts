rootProject.name = "filestore"

include("core:buffer", "core:filesystem")
include("definition", "definition:osrs", "definition:r718")
include("filestore", "filestore:r718-fs", "filestore:osrs-fs")

include("tools", "test")