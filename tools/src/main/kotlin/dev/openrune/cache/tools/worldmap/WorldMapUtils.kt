package dev.openrune.cache.tools.worldmap

import io.netty.buffer.ByteBuf
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/**
 * @author Kris | 14/08/2022
 */
internal fun ByteBuf.toImage(): BufferedImage {
    val array = ByteArray(readableBytes())
    readBytes(array)
    return ImageIO.read(ByteArrayInputStream(array))
}
