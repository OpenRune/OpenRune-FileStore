@file:Suppress("DuplicatedCode")

package dev.openrune.cache.worldmap.worldmap

import dev.openrune.cache.tools.worldmap.utils.Tuple5
import dev.openrune.cache.worldmap.worldmap.utils.WorldMapConstants.MAPSQUARE_SIZE
import dev.openrune.cache.worldmap.worldmap.utils.WorldMapConstants.MAX_LEVELS
import dev.openrune.cache.worldmap.worldmap.utils.WorldMapConstants.ZONE_SIZE
import dev.openrune.definition.util.readNullableLargeSmart
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import io.netty.buffer.ByteBuf
import kotlin.math.max
import kotlin.math.min

/**
 * @author Kris | 14/08/2022
 */
data class WorldMapDecorationObject(val objectId: Int, val shape: Int, val rotation: Int) {
    val isWall: Boolean get() = shape == WALL_STRAIGHT_SHAPE || shape == WALL_L_SHAPE || shape == WALL_SQUARE_CORNER_SHAPE || shape == WALL_DIAGONAL_SHAPE
    val isGroundDecoration: Boolean get() = shape == GROUND_DECOR_SHAPE
    val isCentrepiece: Boolean get() = shape == CENTREPIECE_STRAIGHT_SHAPE || shape == CENTREPIECE_DIAGONAL_SHAPE

    companion object {
        const val GROUND_DECOR_SHAPE = 22
        const val WALL_STRAIGHT_SHAPE = 0
        const val WALL_L_SHAPE = 2
        const val WALL_SQUARE_CORNER_SHAPE = 3
        const val WALL_DIAGONAL_SHAPE = 9
        const val CENTREPIECE_STRAIGHT_SHAPE = 10
        const val CENTREPIECE_DIAGONAL_SHAPE = 11
    }
}
sealed class WorldMapGeography(
    open val underlays: Underlays,
    open val overlays: Overlays,
    open val shapes: Shapes,
    open val rotations: Rotations,
    open val decorations: Decorations,
) {

    fun getUnderlayId(x: Int, y: Int): Int {
        if (x < 0 || x >= MAPSQUARE_SIZE || y < 0 || y >= MAPSQUARE_SIZE) return -1
        return underlays[0][x][y] - 1
    }

    fun getOverlayId(z: Int, x: Int, y: Int): Int {
        if (x < 0 || x >= MAPSQUARE_SIZE || y < 0 || y >= MAPSQUARE_SIZE || z < 0 || z >= overlays.size) return -1
        return overlays[z][x][y] - 1
    }

    fun getOverlayShape(z: Int, x: Int, y: Int): Int {
        if (x < 0 || x >= MAPSQUARE_SIZE || y < 0 || y >= MAPSQUARE_SIZE || z < 0 || z >= overlays.size) return 0
        return shapes[z][x][y].toInt()
    }

    fun getOverlayRotation(z: Int, x: Int, y: Int): Int {
        if (x < 0 || x >= MAPSQUARE_SIZE || y < 0 || y >= MAPSQUARE_SIZE || z < 0 || z >= overlays.size) return 0
        return rotations[z][x][y].toInt()
    }

    fun getDecorations(z: Int, x: Int, y: Int): List<WorldMapDecorationObject> {
        if (x < 0 || x >= MAPSQUARE_SIZE || y < 0 || y >= MAPSQUARE_SIZE || z < 0 || z >= overlays.size) return emptyList()
        return decorations[z][x][y]
    }

    fun getLevelsCount(): Int {
        return overlays.size
    }

    abstract fun encode(buffer: ByteBuf)

    private fun isSimpleTile(x: Int, y: Int, maxOverlayLevel: Int, maxDecorationLevel: Int): Boolean {
        /* Decorative objects can only be encoded in full format. */
        if (maxDecorationLevel != -1) return false
        /* If there are no overlays, must be a simple format. */
        if (maxOverlayLevel == -1) return true
        /* Overlays that are on any floor besides 0 can only be encoded in full format. */
        if (maxOverlayLevel > 0) return false
        for (level in 0..maxOverlayLevel) {
            if (overlays[level][x][y].toInt() == 0) continue
            if (shapes[level][x][y].toInt() != 0 || rotations[level][x][y].toInt() != 0) return false
        }
        return true
    }

    fun encodeTile(buffer: ByteBuf, x: Int, y: Int, useG4s: Boolean = false) {
        val maxOverlayLevel = overlays.indices.maxOf { if (overlays[it][x][y].toInt() == 0) -1 else it }
        val maxDecorationLevel = decorations.indices.maxOf { if (decorations[it][x][y].isEmpty()) -1 else it }
        val isSimple = isSimpleTile(x, y, maxOverlayLevel, maxDecorationLevel)
        if (isSimple) {
            val overlay = overlays[0][x][y]
            if (overlay.toInt() != 0) {
                buffer.writeByte(SIMPLE_TILE or HAS_OVERLAY)
                buffer.writeShort(overlay.toInt())
                buffer.writeShort(underlays[0][x][y].toInt())
            } else {
                if (underlays[0][x][y].toInt() != 0) {
                    buffer.writeByte(SIMPLE_TILE)
                    buffer.writeShort(underlays[0][x][y].toInt())
                } else {
                    buffer.writeByte(0)
                }
            }
        } else {
            val hasOverlays = maxOverlayLevel != -1
            val hasDecorations = maxDecorationLevel != -1
            val maxLevels = max(maxOverlayLevel, maxDecorationLevel)
            var flag = 0
            if (hasOverlays) {
                flag = flag or HAS_OVERLAY
            }
            if (hasDecorations) {
                flag = flag or HAS_DECORATIVE_OBJECTS
            }
            flag = flag or (maxLevels shl 3)
            buffer.writeByte(flag)
            buffer.writeShort(underlays[0][x][y].toInt())
            if (hasOverlays) {
                buffer.writeByte(maxLevels.inc())
                for (level in 0..maxLevels) {
                    val overlayId = overlays[level][x][y].toInt()
                    buffer.writeShort(overlayId)
                    if (overlayId == 0) continue
                    val tileInfo = (shapes[level][x][y].toInt() shl 2) or (rotations[level][x][y].toInt() and 0x3)
                    buffer.writeByte(tileInfo)
                }
            }
            if (hasDecorations) {
                for (level in 0..maxLevels) {
                    val tileDecorations = decorations[level][x][y]
                    buffer.writeByte(tileDecorations.size)
                    for (decoration in tileDecorations) {
                        writeDecorationObjectId(buffer, decoration.objectId, useG4s)
                        val objectInfo = (decoration.shape shl 2) or (decoration.rotation and 0x3)
                        buffer.writeByte(objectInfo)
                    }
                }
            }
        }
    }

    companion object {
        private const val SIMPLE_TILE = 0x1
        private const val HAS_OVERLAY = 0x2
        private const val HAS_DECORATIVE_OBJECTS = 0x4

        fun writeDecorationObjectId(buffer: ByteBuf, objectId: Int, useG4s: Boolean) {
            if (useG4s) {
                buffer.writeInt(objectId)
            } else {
                buffer.writeNullableLargeSmartCorrect(if (objectId == -1) null else objectId)
            }
        }

        fun readDecorationObjectId(buffer: ByteBuf, useG4s: Boolean): Int {
            return if (useG4s) {
                buffer.readInt()
            } else {
                buffer.readNullableLargeSmart()
            }
        }

        fun constructGeography(levels: Int): Tuple5<Underlays, Overlays, Shapes, Rotations, Decorations> {
            val underlays = Array(1) {
                Array(MAPSQUARE_SIZE) {
                    ShortArray(MAPSQUARE_SIZE)
                }
            }
            val overlays = Array(levels) {
                Array(MAPSQUARE_SIZE) {
                    ShortArray(MAPSQUARE_SIZE)
                }
            }
            val shapes = Array(levels) {
                Array(MAPSQUARE_SIZE) {
                    ByteArray(MAPSQUARE_SIZE)
                }
            }
            val rotations = Array(levels) {
                Array(MAPSQUARE_SIZE) {
                    ByteArray(MAPSQUARE_SIZE)
                }
            }
            val decorations = Array(levels) {
                Array(MAPSQUARE_SIZE) {
                    Array(MAPSQUARE_SIZE) {
                        mutableListOf<WorldMapDecorationObject>()
                    }
                }
            }
            return Tuple5(underlays, overlays, shapes, rotations, decorations)
        }

        fun decodeTile(
            x: Int,
            y: Int,
            buffer: ByteBuf,
            overlays: Overlays,
            underlays: Underlays,
            shapes: Shapes,
            rotations: Rotations,
            decorations: Decorations,
            useG4s: Boolean = false,
        ) {
            val flag = buffer.readUnsignedByte().toInt()
            if (flag == 0) return
            if (flag and SIMPLE_TILE != 0) {
                decodeTileSimple(x, y, buffer, flag, overlays, underlays)
            } else {
                decodeTileFull(x, y, buffer, flag, overlays, underlays, shapes, rotations, decorations, useG4s)
            }
        }

        private fun decodeTileSimple(
            x: Int,
            y: Int,
            buffer: ByteBuf,
            flag: Int,
            overlays: Overlays,
            underlays: Underlays
        ) {
            val hasOverlay = flag and HAS_OVERLAY != 0
            if (hasOverlay) {
                overlays[0][x][y] = buffer.readShort()
            }
            underlays[0][x][y] = buffer.readShort()
        }

        private fun decodeTileFull(
            x: Int,
            y: Int,
            buffer: ByteBuf,
            flag: Int,
            overlays: Overlays,
            underlays: Underlays,
            shapes: Shapes,
            rotations: Rotations,
            decorations: Decorations,
            useG4s: Boolean = false,
        ) {
            val hasOverlays = flag and HAS_OVERLAY != 0
            val hasDecorations = flag and HAS_DECORATIVE_OBJECTS != 0

            underlays[0][x][y] = buffer.readShort()
            if (hasOverlays) {
                val levels = buffer.readUnsignedByte()
                for (level in 0 until levels) {
                    val overlayId = buffer.readShort()
                    if (overlayId.toInt() == 0) continue
                    overlays[level][x][y] = overlayId
                    val tileInfo = buffer.readUnsignedByte().toInt()
                    shapes[level][x][y] = (tileInfo shr 2).toByte()
                    rotations[level][x][y] = (tileInfo and 0x3).toByte()
                }
            }

            if (hasDecorations) {
                val levels = ((flag shr 3) and 0x3) + 1
                for (level in 0 until levels) {
                    val count = buffer.readUnsignedByte().toInt()
                    if (count == 0) continue
                    for (i in 0 until count) {
                        val objectId = readDecorationObjectId(buffer, useG4s)
                        val objectInfo = buffer.readUnsignedByte().toInt()
                        val shape = objectInfo shr 2
                        val rotation = objectInfo and 0x3
                        decorations[level][x][y] += WorldMapDecorationObject(objectId, shape, rotation)
                    }
                }
            }
        }
    }
}

typealias Underlays = Array<Array<ShortArray>>
typealias Overlays = Array<Array<ShortArray>>
typealias Shapes = Array<Array<ByteArray>>
typealias Rotations = Array<Array<ByteArray>>
typealias Decorations = Array<Array<Array<MutableList<WorldMapDecorationObject>>>>

data class WorldMapMapsquareGeography(
    val mapsquareDestinationX: Int,
    val mapsquareDestinationY: Int,
    override val underlays: Underlays,
    override val overlays: Overlays,
    override val shapes: Shapes,
    override val rotations: Rotations,
    override val decorations: Decorations,
) : WorldMapGeography(underlays, overlays, shapes, rotations, decorations) {

    fun encode(buffer: ByteBuf, legacy: Boolean) {
        if (legacy) {
            buffer.writeByte(WorldMapAreaType.Mapsquare.typeId)
            buffer.writeByte(mapsquareDestinationX)
            buffer.writeByte(mapsquareDestinationY)
        }
        for (x in 0 until MAPSQUARE_SIZE) {
            for (y in 0 until MAPSQUARE_SIZE) {
                encodeTile(buffer, x, y, useG4s = !legacy)
            }
        }
    }

    override fun encode(buffer: ByteBuf) {
        encode(buffer, legacy = true)
    }

    override fun toString(): String {
        val builder = StringBuilder(1000)
        builder.append("WorldMapZoneGeography:").appendLine()
        builder.append("\tmapsquares:").appendLine()
        for (x in 0 until MAPSQUARE_SIZE) {
            for (y in 0 until MAPSQUARE_SIZE) {
                builder.append("\t\ttile: $x, $y").appendLine()
                for (level in underlays.indices) {
                    builder.append("\t\t\tunderlay[$level]: ${underlays[level][x][y]}").appendLine()
                }
                for (level in overlays.indices) {
                    builder.append("\t\t\toverlays[$level]: ${overlays[level][x][y]}").appendLine()
                }
                for (level in shapes.indices) {
                    builder.append("\t\t\tshapes[$level]: ${shapes[level][x][y]}").appendLine()
                }
                for (level in rotations.indices) {
                    builder.append("\t\t\trotations[$level]: ${rotations[level][x][y]}").appendLine()
                }
                for (level in decorations.indices) {
                    val decorations = decorations[level][x][y]
                    for (index in decorations.indices) {
                        builder.append("\t\t\tdecorations[$level][$index]: ${decorations[index]}").appendLine()
                    }
                }
            }
        }
        return builder.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WorldMapMapsquareGeography

        if (!underlays.contentDeepEquals(other.underlays)) return false
        if (!overlays.contentDeepEquals(other.overlays)) return false
        if (!shapes.contentDeepEquals(other.shapes)) return false
        if (!rotations.contentDeepEquals(other.rotations)) return false
        if (!decorations.contentDeepEquals(other.decorations)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = underlays.contentDeepHashCode()
        result = 31 * result + overlays.contentDeepHashCode()
        result = 31 * result + shapes.contentDeepHashCode()
        result = 31 * result + rotations.contentDeepHashCode()
        result = 31 * result + decorations.contentDeepHashCode()
        return result
    }

    companion object {
        fun decode(
            buffer: ByteBuf,
            data: WorldMapData,
            legacy: Boolean = true,
        ): WorldMapMapsquareGeography {
            val levels = min(data.levelsCount, MAX_LEVELS)
            val (underlays, overlays, shapes, rotations, decorations) = constructGeography(levels)
            val mapsquareDestinationX: Int
            val mapsquareDestinationY: Int
            if (legacy) {
                val typeId = buffer.readUnsignedByte().toInt()
                val type = WorldMapAreaType[typeId]
                require(type == WorldMapAreaType.Mapsquare)
                mapsquareDestinationX = buffer.readUnsignedByte().toInt()
                mapsquareDestinationY = buffer.readUnsignedByte().toInt()
                require(mapsquareDestinationX == data.mapsquareDestinationX) {
                    "Mismatching x coord: $mapsquareDestinationX, ${data.mapsquareDestinationX}"
                }
                require(mapsquareDestinationY == data.mapsquareDestinationY) {
                    "Mismatching y coord: $mapsquareDestinationY, ${data.mapsquareDestinationY}"
                }
            } else {
                mapsquareDestinationX = data.mapsquareDestinationX
                mapsquareDestinationY = data.mapsquareDestinationY
            }
            for (x in 0 until MAPSQUARE_SIZE) {
                for (y in 0 until MAPSQUARE_SIZE) {
                    decodeTile(x, y, buffer, overlays, underlays, shapes, rotations, decorations, useG4s = !legacy)
                }
            }
            return WorldMapMapsquareGeography(
                mapsquareDestinationX,
                mapsquareDestinationY,
                underlays,
                overlays,
                shapes,
                rotations,
                decorations
            )
        }
    }
}

data class WorldMapZoneGeography(
    val mapsquareDestinationX: Int,
    val mapsquareDestinationY: Int,
    val zoneDestinationX: Int,
    val zoneDestinationY: Int,
    override val underlays: Underlays,
    override val overlays: Overlays,
    override val shapes: Shapes,
    override val rotations: Rotations,
    override val decorations: Decorations,
) : WorldMapGeography(underlays, overlays, shapes, rotations, decorations) {
    private val minXInMapsquare: Int get() = zoneDestinationX shl 3
    private val minYInMapsquare: Int get() = zoneDestinationY shl 3

    fun encode(buffer: ByteBuf, legacy: Boolean) {
        if (legacy) {
            buffer.writeByte(WorldMapAreaType.Zone.typeId)
            buffer.writeByte(mapsquareDestinationX)
            buffer.writeByte(mapsquareDestinationY)
            buffer.writeByte(zoneDestinationX)
            buffer.writeByte(zoneDestinationY)
        }
        for (x in 0 until ZONE_SIZE) {
            for (y in 0 until ZONE_SIZE) {
                encodeTile(buffer, x + minXInMapsquare, y + minYInMapsquare, useG4s = !legacy)
            }
        }
    }

    override fun encode(buffer: ByteBuf) {
        encode(buffer, legacy = true)
    }

    override fun toString(): String {
        val builder = StringBuilder(1000)
        builder.append("WorldMapZoneGeography:").appendLine()
        builder.append("\tminXInMapsquare: $minXInMapsquare").appendLine()
        builder.append("\tminYInMapsquare: $minYInMapsquare").appendLine()
        builder.append("\tmapsquares:").appendLine()
        for (x in 0 until ZONE_SIZE) {
            for (y in 0 until ZONE_SIZE) {
                val tileX = x + minXInMapsquare
                val tileY = y + minYInMapsquare
                builder.append("\t\ttile: $tileX, $tileY").appendLine()
                for (level in underlays.indices) {
                    builder.append("\t\t\tunderlay[$level]: ${underlays[level][tileX][tileY]}").appendLine()
                }
                for (level in overlays.indices) {
                    builder.append("\t\t\toverlays[$level]: ${overlays[level][tileX][tileY]}").appendLine()
                }
                for (level in shapes.indices) {
                    builder.append("\t\t\tshapes[$level]: ${shapes[level][tileX][tileY]}").appendLine()
                }
                for (level in rotations.indices) {
                    builder.append("\t\t\trotations[$level]: ${rotations[level][tileX][tileY]}").appendLine()
                }
                for (level in decorations.indices) {
                    val decorations = decorations[level][x][y]
                    for (index in decorations.indices) {
                        builder.append("\t\t\tdecorations[$level][$index]: ${decorations[index]}").appendLine()
                    }
                }
            }
        }
        return builder.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WorldMapZoneGeography

        if (!underlays.contentDeepEquals(other.underlays)) return false
        if (!overlays.contentDeepEquals(other.overlays)) return false
        if (!shapes.contentDeepEquals(other.shapes)) return false
        if (!rotations.contentDeepEquals(other.rotations)) return false
        if (!decorations.contentDeepEquals(other.decorations)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = underlays.contentDeepHashCode()
        result = 31 * result + overlays.contentDeepHashCode()
        result = 31 * result + shapes.contentDeepHashCode()
        result = 31 * result + rotations.contentDeepHashCode()
        result = 31 * result + decorations.contentDeepHashCode()
        return result
    }

    companion object {
        fun decode(
            buffer: ByteBuf,
            data: WorldMapZoneData,
            legacy: Boolean = true,
        ): WorldMapZoneGeography {
            val levels = min(data.levelsCount, MAX_LEVELS)
            val (underlays, overlays, shapes, rotations, decorations) = constructGeography(levels)
            val mapsquareDestinationX: Int
            val mapsquareDestinationY: Int
            val zoneDestinationX: Int
            val zoneDestinationY: Int
            if (legacy) {
                val typeId = buffer.readUnsignedByte().toInt()
                val type = WorldMapAreaType[typeId]
                require(type == WorldMapAreaType.Zone)
                mapsquareDestinationX = buffer.readUnsignedByte().toInt()
                mapsquareDestinationY = buffer.readUnsignedByte().toInt()
                zoneDestinationX = buffer.readUnsignedByte().toInt()
                zoneDestinationY = buffer.readUnsignedByte().toInt()
                require(mapsquareDestinationX == data.mapsquareDestinationX) {
                    "Mismatching x coord: $mapsquareDestinationX, ${data.mapsquareDestinationX}"
                }
                require(mapsquareDestinationY == data.mapsquareDestinationY) {
                    "Mismatching y coord: $mapsquareDestinationY, ${data.mapsquareDestinationY}"
                }
                require(zoneDestinationX == data.zoneDestinationX) {
                    "Mismatching zone x coord: $zoneDestinationX, ${data.zoneDestinationX}"
                }
                require(zoneDestinationY == data.zoneDestinationY) {
                    "Mismatching zone y coord: $zoneDestinationY, ${data.zoneDestinationY}"
                }
            } else {
                mapsquareDestinationX = data.mapsquareDestinationX
                mapsquareDestinationY = data.mapsquareDestinationY
                zoneDestinationX = data.zoneDestinationX
                zoneDestinationY = data.zoneDestinationY
            }
            for (x in 0 until ZONE_SIZE) {
                for (y in 0 until ZONE_SIZE) {
                    decodeTile(
                        x + (zoneDestinationX shl 3),
                        y + (zoneDestinationY shl 3),
                        buffer,
                        overlays,
                        underlays,
                        shapes,
                        rotations,
                        decorations,
                        useG4s = !legacy,
                    )
                }
            }
            return WorldMapZoneGeography(
                mapsquareDestinationX,
                mapsquareDestinationY,
                zoneDestinationX,
                zoneDestinationY,
                underlays,
                overlays,
                shapes,
                rotations,
                decorations
            )
        }
    }
}
