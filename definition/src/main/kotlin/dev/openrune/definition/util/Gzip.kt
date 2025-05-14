package dev.openrune.definition.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

fun decompressGzip(data: ByteArray): ByteArray {
    ByteArrayInputStream(data).use { byteStream ->
        GZIPInputStream(byteStream).use { gzipStream ->
            return gzipStream.readBytes()
        }
    }
}
fun applyGZCompression(input: ByteArray): ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    GZIPOutputStream(byteArrayOutputStream).use { gzipOutputStream ->
        gzipOutputStream.write(input)
    }

    return byteArrayOutputStream.toByteArray()
}