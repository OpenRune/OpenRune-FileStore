@file:Suppress("DuplicatedCode")

package dev.openrune.cache.tools.worldmap

import dev.openrune.cache.tools.worldmap.providers.CacheProvider
import dev.openrune.cache.tools.worldmap.utils.Coordinate
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.util.readNullableLargeSmart
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import io.netty.buffer.ByteBuf
import kotlin.math.max
import kotlin.math.min
import org.openrs2.cache.Cache

/**
 * @author Kris | 13/08/2022
 */
data class WorldMapAreaBoundaries(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int) {
    val width: Int get() = maxX - minX + 1
    val height: Int get() = maxY - minY + 1
}

data class WorldMapArea(
    val internalName: String,
    val details: WorldMapAreaDetails,
    val data: WorldMapAreaData,
    // val texture: WorldMapCompositeTexture,
) {

    val boundaries: WorldMapAreaBoundaries by lazy {
        val minMapsquareXByMapsquare = data.mapsquares.minOfOrNull { it.geography.mapsquareDestinationX } ?: Int.MAX_VALUE
        val minMapsquareYByMapsquare = data.mapsquares.minOfOrNull { it.geography.mapsquareDestinationY } ?: Int.MAX_VALUE
        val minMapsquareXByZone = data.zones.minOfOrNull { it.geography.mapsquareDestinationX } ?: Int.MAX_VALUE
        val minMapsquareYByZone = data.zones.minOfOrNull { it.geography.mapsquareDestinationY } ?: Int.MAX_VALUE
        val minX = min(minMapsquareXByMapsquare, minMapsquareXByZone)
        val minY = min(minMapsquareYByMapsquare, minMapsquareYByZone)
        if (minX == Int.MAX_VALUE || minY == Int.MAX_VALUE) return@lazy WorldMapAreaBoundaries(0, 0, 0, 0)

        val maxMapsquareXByMapsquare = data.mapsquares.maxOfOrNull { it.geography.mapsquareDestinationX } ?: 0
        val maxMapsquareYByMapsquare = data.mapsquares.maxOfOrNull { it.geography.mapsquareDestinationY } ?: 0
        val maxMapsquareXByZone = data.zones.maxOfOrNull { it.geography.mapsquareDestinationX } ?: 0
        val maxMapsquareYByZone = data.zones.maxOfOrNull { it.geography.mapsquareDestinationY } ?: 0
        val maxX = max(maxMapsquareXByMapsquare, maxMapsquareXByZone)
        val maxY = max(maxMapsquareYByMapsquare, maxMapsquareYByZone)
        WorldMapAreaBoundaries(minX, minY, maxX, maxY)
    }

    override fun toString(): String {
        val builder = StringBuilder(1000)
        builder.append("WorldMapArea:").appendLine()
        builder.append("\tinternalName: $internalName").appendLine()
        val detailsLines = details.toString().lineSequence()
        for (line in detailsLines) {
            builder.append("\t\t$line").appendLine()
        }
        val dataLines = data.toString().lineSequence()
        for (line in dataLines) {
            builder.append("\t\t$line").appendLine()
        }
        return builder.toString()
    }

    companion object {
        fun decode(cache: CacheProvider, id : Int,internalName: String): WorldMapArea {
            require(cache.exists(WORLD_MAP_DATA_ARCHIVE, "details", internalName))
            require(cache.exists(WORLD_MAP_DATA_ARCHIVE, "compositemap", internalName))
            // val repository = WorldMapGroundRepository(LinkedHashMap())
            val detailsBuffer = cache.read(WORLD_MAP_DATA_ARCHIVE, "details", internalName)
            val details = WorldMapAreaDetails.decode(id, detailsBuffer)
            val compositeBuffer = cache.read(WORLD_MAP_DATA_ARCHIVE, "compositemap", internalName)
            val data = WorldMapAreaData.decode(cache, compositeBuffer)
            // val texture = WorldMapCompositeTexture.decode(cache, internalName)
            // data.readGroundImages(cache, repository)
            return WorldMapArea(internalName, details, data/*, texture*/)
        }

        private fun Cache.takeFile(archive: Int, id: Int): ByteBuf {
            val groups = list(archive)
            if (groups.asSequence().count() == 1) {
                return read(archive, 0, id)
            }
            val filesInFirstGroup = list(archive, id)
            require(filesInFirstGroup.asSequence().count() == 1)
            return read(archive, id, 0)
        }

        fun WorldMapAreaData.readGroundImages(cache: Cache, repository: WorldMapGroundRepository) {
            val groupIds = LinkedHashSet<Int>(mapsquares.size)
            for (mapsquare in mapsquares) {
                groupIds += mapsquare.data.groupId
            }
            for (zone in zones) {
                groupIds += zone.data.groupId
            }
            for (groupId in groupIds) {
                val groundBuffer = cache.takeFile(WORLD_MAP_GROUND_ARCHIVE, groupId)
                repository.images[groupId] = groundBuffer.toImage()
            }
        }
    }
}

data class WorldMapAreaData(
    val mapsquares: List<WorldMapMapsquare>,
    val zones: List<WorldMapZone>,
    val mapElements: List<WorldMapElement>,
) {

    override fun toString(): String {
        val builder = StringBuilder(1000)
        builder.append("WorldMapAreaData:").appendLine()
        builder.append("\tmapsquares:").appendLine()
        for (mapsquare in mapsquares) {
            val lines = mapsquare.toString().lineSequence()
            for (line in lines) {
                builder.append("\t\t$line").appendLine()
            }
        }
        builder.append("\tzones:").appendLine()
        for (zone in zones) {
            val lines = zone.toString().lineSequence()
            for (line in lines) {
                builder.append("\t\t$line").appendLine()
            }
        }
        builder.append("\tmapElements:").appendLine()
        for (label in mapElements) {
            val lines = label.toString().lineSequence()
            for (line in lines) {
                builder.append("\t\t$line").appendLine()
            }
        }
        return builder.toString()
    }

    fun encode(buffer: ByteBuf) {
        buffer.writeShort(mapsquares.size)
        for (mapsquare in mapsquares) {
            mapsquare.data.encode(buffer)
        }
        buffer.writeShort(zones.size)
        for (zone in zones) {
            zone.data.encode(buffer)
        }
        buffer.writeShort(mapElements.size)
        for (label in mapElements) {
            label.encode(buffer)
        }
    }

    companion object {
        fun decode(cache: CacheProvider, buffer: ByteBuf): WorldMapAreaData {
            val mapsquareCount = buffer.readUnsignedShort()
            val mapsquares = ArrayList<WorldMapMapsquare>(mapsquareCount)
            for (i in 0 until mapsquareCount) {
                val data = WorldMapMapsquareData.decode(buffer)
                val geographyBuffer = cache.read(WORLD_MAP_GEOGRAPHY_ARCHIVE, data.groupId, data.fileId)
                val geography = WorldMapMapsquareGeography.decode(geographyBuffer, data)
                mapsquares += WorldMapMapsquare(data, geography)
                geographyBuffer.release()
            }

            val zoneCount = buffer.readUnsignedShort()
            val zones = ArrayList<WorldMapZone>(zoneCount)
            for (i in 0 until zoneCount) {
                val data = WorldMapZoneData.decode(buffer)
                val geographyBuffer = cache.read(WORLD_MAP_GEOGRAPHY_ARCHIVE, data.groupId, data.fileId)
                val geography = WorldMapZoneGeography.decode(geographyBuffer, data)
                zones += WorldMapZone(data, geography)
                geographyBuffer.release()
            }

            val labelsCount = buffer.readUnsignedShort()
            val labels = ArrayList<WorldMapElement>(labelsCount)
            for (i in 0 until labelsCount) {
                labels += WorldMapElement.decode(buffer)
            }
            buffer.release()
            return WorldMapAreaData(mapsquares, zones, labels)
        }
    }
}

interface WorldMapData {
    val level: Int
    val levelsCount: Int
    val mapsquareDestinationX: Int
    val mapsquareDestinationY: Int
    val mapsquareSourceX: Int
    val mapsquareSourceY: Int
    val groupId: Int
    val fileId: Int
}

sealed interface WorldMapBlock {
    val data: WorldMapData
    val geography: WorldMapGeography
}

data class WorldMapMapsquare(
    override val data: WorldMapMapsquareData,
    override val geography: WorldMapMapsquareGeography,
) : WorldMapBlock {
    override fun toString(): String {
        val builder = StringBuilder(100)
        builder.append("WorldMapMapsquare:").appendLine()
        builder.append("\tdata:").appendLine()
        val dataLines = data.toString().lineSequence()
        for (line in dataLines) {
            builder.append("\t\t$line").appendLine()
        }

        builder.append("\tgeography:").appendLine()
        val geographyLines = geography.toString().lineSequence()
        for (line in geographyLines) {
            builder.append("\t\t$line").appendLine()
        }
        return builder.toString()
    }
}

data class WorldMapMapsquareData(
    override val level: Int,
    override val levelsCount: Int,
    override val mapsquareSourceX: Int,
    override val mapsquareSourceY: Int,
    override val mapsquareDestinationX: Int,
    override val mapsquareDestinationY: Int,
    override val groupId: Int,
    override val fileId: Int,
) : WorldMapData {

    fun encode(buffer: ByteBuf) {
        buffer.writeByte(WorldMapAreaType.Mapsquare.typeId)
        buffer.writeByte(level)
        buffer.writeByte(levelsCount)
        buffer.writeShort(mapsquareSourceX)
        buffer.writeShort(mapsquareSourceY)
        buffer.writeShort(mapsquareDestinationX)
        buffer.writeShort(mapsquareDestinationY)
        buffer.writeNullableLargeSmartCorrect(if (groupId == -1) null else groupId)
        buffer.writeNullableLargeSmartCorrect(if (fileId == -1) null else fileId)
    }

    override fun toString(): String {
        val builder = StringBuilder(100)
        builder.append("WorldMapMapsquareData:").appendLine()
        builder.append("\tlevel: $level").appendLine()
        builder.append("\tlevelsCount: $levelsCount").appendLine()
        builder.append("\tmapsquareSourceX: $mapsquareSourceX").appendLine()
        builder.append("\tmapsquareSourceY: $mapsquareSourceY").appendLine()
        builder.append("\tmapsquareDestinationX: $mapsquareDestinationX").appendLine()
        builder.append("\tmapsquareDestinationY: $mapsquareDestinationY").appendLine()
        builder.append("\tgroupId: $groupId").appendLine()
        builder.append("\tfileId: $fileId").appendLine()
        return builder.toString()
    }

    companion object {
        fun decode(buffer: ByteBuf): WorldMapMapsquareData {
            val typeId = buffer.readUnsignedByte().toInt()
            val type = WorldMapAreaType[typeId]
            require(type == WorldMapAreaType.Mapsquare)
            val level = buffer.readUnsignedByte().toInt()
            val levelsCount = buffer.readUnsignedByte().toInt()
            val mapsquareSourceX = buffer.readUnsignedShort()
            val mapsquareSourceY = buffer.readUnsignedShort()
            val mapsquareDestinationX = buffer.readUnsignedShort()
            val mapsquareDestinationY = buffer.readUnsignedShort()
            val groupId = buffer.readNullableLargeSmart() ?: -1
            val fileId = buffer.readNullableLargeSmart() ?: -1
            return WorldMapMapsquareData(
                level,
                levelsCount,
                mapsquareSourceX,
                mapsquareSourceY,
                mapsquareDestinationX,
                mapsquareDestinationY,
                groupId,
                fileId
            )
        }
    }
}

data class WorldMapZone(
    override val data: WorldMapZoneData,
    override val geography: WorldMapZoneGeography,
) : WorldMapBlock {

    override fun toString(): String {
        val builder = StringBuilder(100)
        builder.append("WorldMapZone:").appendLine()
        builder.append("\tdata:").appendLine()
        val dataLines = data.toString().lineSequence()
        for (line in dataLines) {
            builder.append("\t\t$line").appendLine()
        }

        builder.append("\tgeography:").appendLine()
        val geographyLines = geography.toString().lineSequence()
        for (line in geographyLines) {
            builder.append("\t\t$line").appendLine()
        }
        return builder.toString()
    }
}

data class WorldMapZoneData(
    override val level: Int,
    override val levelsCount: Int,
    override val mapsquareSourceX: Int,
    override val mapsquareSourceY: Int,
    val zoneSourceX: Int,
    val zoneSourceY: Int,
    override val mapsquareDestinationX: Int,
    override val mapsquareDestinationY: Int,
    val zoneDestinationX: Int,
    val zoneDestinationY: Int,
    override val groupId: Int,
    override val fileId: Int,
) : WorldMapData {

    fun encode(buffer: ByteBuf) {
        buffer.writeByte(WorldMapAreaType.Zone.typeId)
        buffer.writeByte(level)
        buffer.writeByte(levelsCount)
        buffer.writeShort(mapsquareSourceX)
        buffer.writeShort(mapsquareSourceY)
        buffer.writeByte(zoneSourceX)
        buffer.writeByte(zoneSourceY)
        buffer.writeShort(mapsquareDestinationX)
        buffer.writeShort(mapsquareDestinationY)
        buffer.writeByte(zoneDestinationX)
        buffer.writeByte(zoneDestinationY)
        buffer.writeNullableLargeSmartCorrect(if (groupId == -1) null else groupId)
        buffer.writeNullableLargeSmartCorrect(if (fileId == -1) null else fileId)
    }

    override fun toString(): String {
        val builder = StringBuilder(100)
        builder.append("WorldMapZoneData:").appendLine()
        builder.append("\tlevel: $level").appendLine()
        builder.append("\tlevelsCount: $levelsCount").appendLine()
        builder.append("\tmapsquareSourceX: $mapsquareSourceX").appendLine()
        builder.append("\tmapsquareSourceY: $mapsquareSourceY").appendLine()
        builder.append("\tzoneSourceX: $zoneSourceX").appendLine()
        builder.append("\tzoneSourceY: $zoneSourceY").appendLine()
        builder.append("\tmapsquareDestinationX: $mapsquareDestinationX").appendLine()
        builder.append("\tmapsquareDestinationY: $mapsquareDestinationY").appendLine()
        builder.append("\tzoneDestinationX: $zoneDestinationX").appendLine()
        builder.append("\tzoneDestinationY: $zoneDestinationY").appendLine()
        builder.append("\tgroupId: $groupId").appendLine()
        builder.append("\tfileId: $fileId").appendLine()
        return builder.toString()
    }

    companion object {
        fun decode(buffer: ByteBuf): WorldMapZoneData {
            val typeId = buffer.readUnsignedByte().toInt()
            val type = WorldMapAreaType[typeId]
            require(type == WorldMapAreaType.Zone)
            val level = buffer.readUnsignedByte().toInt()
            val levelsCount = buffer.readUnsignedByte().toInt()
            val mapsquareSourceX = buffer.readUnsignedShort()
            val mapsquareSourceY = buffer.readUnsignedShort()
            val zoneSourceX = buffer.readUnsignedByte().toInt()
            val zoneSourceY = buffer.readUnsignedByte().toInt()
            val mapsquareDestinationX = buffer.readUnsignedShort()
            val mapsquareDestinationY = buffer.readUnsignedShort()
            val zoneDestinationX = buffer.readUnsignedByte().toInt()
            val zoneDestinationY = buffer.readUnsignedByte().toInt()
            val groupId = buffer.readNullableLargeSmart() ?: -1
            val fileId = buffer.readNullableLargeSmart() ?: -1
            return WorldMapZoneData(
                level,
                levelsCount,
                mapsquareSourceX,
                mapsquareSourceY,
                zoneSourceX,
                zoneSourceY,
                mapsquareDestinationX,
                mapsquareDestinationY,
                zoneDestinationX,
                zoneDestinationY,
                groupId,
                fileId
            )
        }
    }
}

data class WorldMapElement(
    val elementId: Int,
    val location: Coordinate,
    val members: Boolean
) {

    constructor(name: String, location: Coordinate) : this(ConstantProvider.getMapping(name), location, false)

    fun encode(buffer: ByteBuf) {
        buffer.writeNullableLargeSmartCorrect(if (elementId == -1) null else elementId)
        buffer.writeInt(location.packedCoord)
        buffer.writeBoolean(members)
    }

    override fun toString(): String {
        val builder = StringBuilder(100)
        builder.append("WorldMapLabel:").appendLine()
        builder.append("\telementId: $elementId").appendLine()
        builder.append("\tlocation: $location").appendLine()
        builder.append("\tmembers: $members").appendLine()
        return builder.toString()
    }

    companion object {
        fun decode(buffer: ByteBuf): WorldMapElement {
            val elementId = buffer.readNullableLargeSmart() ?: -1
            val location = Coordinate(buffer.readInt())
            val members = buffer.readUnsignedByte().toInt() == 1
            return WorldMapElement(elementId, location, members)
        }
    }
}

enum class WorldMapAreaType(val typeId: Int) {
    Mapsquare(0),
    Zone(1);

    companion object {
        operator fun get(typeId: Int): WorldMapAreaType {
            return values().single { it.typeId == typeId }
        }
    }
}
