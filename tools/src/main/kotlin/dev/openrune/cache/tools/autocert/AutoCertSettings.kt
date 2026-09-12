package dev.openrune.cache.tools.autocert

import java.io.File

const val DEFAULT_CERT_TEMPLATE: String = "obj.cert_shark"

data class AutoCertSettings(
    val configDirectories: List<File>,
    val template: String = DEFAULT_CERT_TEMPLATE,
    val keyPrefix: String = "cert_",
    val table: String = "obj",
    val requireAdjacentIds: Boolean = true,
)

data class CertCandidate(
    val itemKey: String,
    val certKey: String,
    val source: File,
)
