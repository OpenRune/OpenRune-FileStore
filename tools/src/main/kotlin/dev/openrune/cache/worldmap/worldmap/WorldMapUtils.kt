package dev.openrune.cache.worldmap.worldmap

import io.netty.buffer.ByteBuf
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.UUID
import javax.imageio.ImageIO

/**
 * @author Kris | 14/08/2022
 */
internal fun ByteBuf.toImage(): BufferedImage {
    val array = ByteArray(readableBytes())
    readBytes(array)
    return ImageIO.read(ByteArrayInputStream(array))
}

/** True when [internalName] looks like text, not binary (e.g. compositetexture PNG decoded as details). */
fun WorldMapAreaDetails.hasValidNames(): Boolean = internalName.sanitizeWorldMapFileName().isSafeFileName()

fun WorldMapAreaDetails.exportBaseName(): String {
    val candidate = internalName.ifEmpty { displayName }.sanitizeWorldMapFileName()
    return if (candidate.isSafeFileName()) candidate else randomExportName(id)
}

fun randomExportName(areaId: Int = -1): String {
    val token = UUID.randomUUID().toString().replace("-", "").take(12)
    return if (areaId >= 0) "map_${areaId}_$token" else "map_$token"
}

private fun String.isSafeFileName(): Boolean {
    if (isEmpty() || length > 120) return false
    if (startsWith("\u0089PNG") || startsWith("PNG")) return false
    return all { it.code in 32..126 && it !in "<>:\"/\\|?*" }
}

fun String.sanitizeWorldMapFileName(): String {
    return filter { it.code in 32..126 }
        .replace(Regex("""[<>:"/\\|?*]"""), "_")
        .take(120)
        .trim()
}
