@file:Suppress("DuplicatedCode", "unused")

package dev.openrune.definition.codec

import dev.openrune.definition.type.model.MeshDecodingOption
import dev.openrune.definition.type.model.MeshType
import dev.openrune.definition.type.model.ModelType
import dev.openrune.definition.type.model.ModelType.Companion.USES_FACE_TYPES_FLAG
import dev.openrune.definition.type.model.ModelType.Companion.USES_MATERIALS_FLAG
import dev.openrune.definition.type.model.ModelType.Companion.X_POS_FLAG
import dev.openrune.definition.type.model.ModelType.Companion.Y_POS_FLAG
import dev.openrune.definition.type.model.ModelType.Companion.Z_POS_FLAG
import dev.openrune.definition.type.model.ModelType.Companion.BILLBOARDS_FLAG
import dev.openrune.definition.type.model.ModelType.Companion.COMPLEX_TEXTURE_RANGE
import dev.openrune.definition.type.model.ModelType.Companion.CUBE_TEXTURE
import dev.openrune.definition.type.model.ModelType.Companion.CYLINDRICAL_TEXTURE
import dev.openrune.definition.type.model.ModelType.Companion.DEFAULT_VERSION
import dev.openrune.definition.type.model.ModelType.Companion.DOWNSCALE_FACTOR
import dev.openrune.definition.type.model.ModelType.Companion.FACE_TYPES_FLAG
import dev.openrune.definition.type.model.ModelType.Companion.PARTICLES_FLAG
import dev.openrune.definition.type.model.ModelType.Companion.SIMPLE_TEXTURE
import dev.openrune.definition.type.model.ModelType.Companion.SPHERICAL_TEXTURE
import dev.openrune.definition.type.model.ModelType.Companion.UNVERSIONED
import dev.openrune.definition.type.model.ModelType.Companion.VERSION_FLAG
import dev.openrune.definition.type.model.particles.EffectiveVertex
import dev.openrune.definition.type.model.particles.EmissiveTriangle
import dev.openrune.definition.type.model.particles.FaceBillboard
import io.netty.buffer.ByteBuf

class ModelCodec(val id: Int, private val options: List<MeshDecodingOption>) {

    fun read(buffer: ByteBuf): ModelType {
        val modelType = ModelType(id)
        val version = buffer.getByte(buffer.writerIndex() - 1).toInt()
        val extra = buffer.getByte(buffer.writerIndex() - 2).toInt()
        when {
            version == -3 && extra == -1 -> decode4(buffer,modelType)
            version == -2 && extra == -1 -> decode3(buffer,modelType)
            version == -1 && extra == -1 -> decode2(buffer,modelType)
            else -> decode1(buffer,modelType)
        }
        return modelType
    }

    private fun decode1(data: ByteBuf, def : ModelType) {
        def.type = MeshType.Unversioned
        val buf1 = data.duplicate()
        val buf2 = data.duplicate()
        val buf3 = data.duplicate()
        val buf4 = data.duplicate()
        val buf5 = data.duplicate()
        buf1.readerIndex(buf1.writerIndex() - 18)
        val vertexCount = buf1.readUnsignedShort()
        val triangleCount = buf1.readUnsignedShort()
        val textureTriangleCount = buf1.readUnsignedByte().toInt()
        val hasTextures = buf1.readUnsignedByte().toInt()
        val modelPriority = buf1.readUnsignedByte().toInt()
        val hasFaceAlphas = buf1.readUnsignedByte().toInt()
        val hasFaceSkins = buf1.readUnsignedByte().toInt()
        val hasVertexSkins = buf1.readUnsignedByte().toInt()
        val vertexXBufIndex = buf1.readUnsignedShort()
        val vertexYBufIndex = buf1.readUnsignedShort()

        @Suppress("UNUSED_VARIABLE")
        val vertexZBufIndex = buf1.readUnsignedShort()
        val triangleIndicesBufIndex = buf1.readUnsignedShort()

        var position = 0

        @Suppress("KotlinConstantConditions")
        val vertexFlagsOffset = position
        position += vertexCount
        val facesCompressTypeOffset = position
        position += triangleCount
        val facePrioritiesOffset = position
        if (modelPriority == 0xFF) {
            position += triangleCount
        }
        val faceSkinsOffset = position
        if (hasFaceSkins == 1) {
            position += triangleCount
        }
        val faceTypesOffset = position
        if (hasTextures == 1) {
            position += triangleCount
        }
        val vertexSkinsOffset = position
        if (hasVertexSkins == 1) {
            position += vertexCount
        }
        val faceAlphasOffset = position
        if (hasFaceAlphas == 1) {
            position += triangleCount
        }
        val faceIndicesOffset = position
        position += triangleIndicesBufIndex
        val faceColorsOffset = position
        position += triangleCount * 2
        val faceMappingsOffset = position
        position += textureTriangleCount * 6
        val vertexXOffsetOffset = position
        position += vertexXBufIndex
        val vertexYOffsetOffset = position
        position += vertexYBufIndex
        val vertexZOffsetOffset = position

        initializeUnversioned(
            vertexCount,
            triangleCount,
            textureTriangleCount,
            hasVertexSkins,
            hasTextures,
            modelPriority,
            hasFaceAlphas,
            hasFaceSkins,
            hasSkeletalBones = false,
            def
        )

        buf1.readerIndex(vertexFlagsOffset)
        buf2.readerIndex(vertexXOffsetOffset)
        buf3.readerIndex(vertexYOffsetOffset)
        buf4.readerIndex(vertexZOffsetOffset)
        buf5.readerIndex(vertexSkinsOffset)
        readVertexPositions(
            hasVertexSkins,
            hasSkeletalBones = false,
            buf1,
            buf2,
            buf3,
            buf4,
            buf5,
            def
        )
        buf1.readerIndex(faceColorsOffset)
        buf2.readerIndex(faceTypesOffset)
        buf3.readerIndex(facePrioritiesOffset)
        buf4.readerIndex(faceAlphasOffset)
        buf5.readerIndex(faceSkinsOffset)
        val (usesFaceTypes, usesMaterials) = readUnversionedTriangleInfo(
            options.toTypedArray(),
            hasTextures,
            modelPriority,
            hasFaceAlphas,
            hasFaceSkins,
            buf1,
            buf2,
            buf3,
            buf4,
            buf5,
            def
        )
        buf1.readerIndex(faceIndicesOffset)
        buf2.readerIndex(facesCompressTypeOffset)
        readTriangleVertices(
            buf1,
            buf2,
            def
        )
        buf1.readerIndex(faceMappingsOffset)
        readUnversionedTextureVertices(buf1,def)
        if (!options.contains(MeshDecodingOption.PreserveOriginalData)) {
            filterUnversionedTextures(
                usesFaceTypes,
                usesMaterials,
                def
            )
        }
    }

    private fun decode2(data: ByteBuf, def : ModelType) {
        def.type = MeshType.Versioned
        val buf1 = data.duplicate()
        val buf2 = data.duplicate()
        val buf3 = data.duplicate()
        val buf4 = data.duplicate()
        val buf5 = data.duplicate()
        val buf6 = data.duplicate()
        val buf7 = data.duplicate()
        buf1.readerIndex(buf1.writerIndex() - 23)
        val vertexCount = buf1.readUnsignedShort()
        val triangleCount = buf1.readUnsignedShort()
        val textureTriangleCount = buf1.readUnsignedByte().toInt()
        val footerFlags = buf1.readUnsignedByte().toInt()
        val hasFaceTypes = footerFlags and FACE_TYPES_FLAG == FACE_TYPES_FLAG
        val hasParticleEffects = footerFlags and PARTICLES_FLAG == PARTICLES_FLAG
        val hasBillboards = footerFlags and BILLBOARDS_FLAG == BILLBOARDS_FLAG
        val hasVersion = footerFlags and VERSION_FLAG == VERSION_FLAG
        val version = if (hasVersion) {
            buf1.readerIndex(buf1.readerIndex() - 7)
            val version = buf1.readUnsignedByte().toInt()
            buf1.readerIndex(buf1.readerIndex() + 6)
            version
        } else {
            DEFAULT_VERSION
        }
        val modelPriority = buf1.readUnsignedByte().toInt()
        val hasFaceAlphas = buf1.readUnsignedByte().toInt()
        val hasFaceSkins = buf1.readUnsignedByte().toInt()
        val hasTextures = buf1.readUnsignedByte().toInt()
        val hasVertexSkins = buf1.readUnsignedByte().toInt()
        val modelVerticesX = buf1.readUnsignedShort()
        val modelVerticesY = buf1.readUnsignedShort()
        val modelVerticesZ = buf1.readUnsignedShort()
        val faceIndices = buf1.readUnsignedShort()
        val textureIndices = buf1.readUnsignedShort()

        var simpleTextureFaceCount = 0
        var complexTextureFaceCount = 0
        var cubeTextureFaceCount = 0
        if (textureTriangleCount > 0) {
            val textureRenderTypes = IntArray(textureTriangleCount)
            def.textureRenderTypes = textureRenderTypes
            buf1.readerIndex(0)
            for (index in 0 until textureTriangleCount) {
                textureRenderTypes[index] = buf1.readByte().toInt()
                val type = textureRenderTypes[index]
                if (type == SIMPLE_TEXTURE) {
                    simpleTextureFaceCount++
                }
                if (type == CUBE_TEXTURE) {
                    cubeTextureFaceCount++
                }
                if (type in COMPLEX_TEXTURE_RANGE) {
                    complexTextureFaceCount++
                }
            }
        }

        var offset = textureTriangleCount
        val vertexFlagsOffset = offset
        offset += vertexCount
        val faceTypesOffset = offset
        if (hasFaceTypes) {
            offset += triangleCount
        }
        val faceCompressTypeOffset = offset
        offset += triangleCount
        val facePrioritiesOffset = offset
        if (modelPriority == 0xFF) {
            offset += triangleCount
        }
        val faceSkinsOffset = offset
        if (hasFaceSkins == 1) {
            offset += triangleCount
        }
        val vertexSkinsOffset = offset
        if (hasVertexSkins == 1) {
            offset += vertexCount
        }
        val faceAlphasOffset = offset
        if (hasFaceAlphas == 1) {
            offset += triangleCount
        }
        val faceIndicesOffset = offset
        offset += faceIndices
        val faceMaterialsOffset = offset
        if (hasTextures == 1) {
            offset += triangleCount * 2
        }
        val faceTextureIndicesOffset = offset
        offset += textureIndices
        val faceColorsOffset = offset
        offset += 2 * triangleCount
        val vertexXOffsetOffset = offset
        offset += modelVerticesX
        val vertexYOffsetOffset = offset
        offset += modelVerticesY
        val vertexZOffsetOffset = offset
        offset += modelVerticesZ
        val simpleTexturesOffset = offset
        offset += 6 * simpleTextureFaceCount
        val complexTexturesOffset = offset
        offset += complexTextureFaceCount * 6
        var textureBytes = 6
        if (version == 14) {
            textureBytes = 7
        } else if (version >= 15) {
            textureBytes = 9
        }
        val texturesScaleOffset = offset
        offset += complexTextureFaceCount * textureBytes
        val texturesRotationOffset = offset
        offset += complexTextureFaceCount
        val texturesDirectionOffset = offset
        offset += complexTextureFaceCount
        val texturesTranslationOffset = offset
        offset += complexTextureFaceCount + (2 * cubeTextureFaceCount)
        val particlesOffset = offset

        initializeVersioned(
            version,
            vertexCount,
            triangleCount,
            textureTriangleCount,
            hasFaceTypes,
            hasVertexSkins,
            hasTextures,
            modelPriority,
            hasFaceAlphas,
            hasFaceSkins,
            complexTextureFaceCount,
            cubeTextureFaceCount,
            hasSkeletalBones = false,
            def
        )

        buf1.readerIndex(vertexFlagsOffset)
        buf2.readerIndex(vertexXOffsetOffset)
        buf3.readerIndex(vertexYOffsetOffset)
        buf4.readerIndex(vertexZOffsetOffset)
        buf5.readerIndex(vertexSkinsOffset)
        readVertexPositions(
            hasVertexSkins,
            hasSkeletalBones = false,
            buf1,
            buf2,
            buf3,
            buf4,
            buf5,
            def
        )

        buf1.readerIndex(faceColorsOffset)
        buf2.readerIndex(faceTypesOffset)
        buf3.readerIndex(facePrioritiesOffset)
        buf4.readerIndex(faceAlphasOffset)
        buf5.readerIndex(faceSkinsOffset)
        buf6.readerIndex(faceMaterialsOffset)
        buf7.readerIndex(faceTextureIndicesOffset)
        readVersionedTriangleInfo(
            hasTextures,
            modelPriority,
            hasFaceAlphas,
            hasFaceSkins,
            hasFaceTypes,
            buf1,
            buf2,
            buf3,
            buf4,
            buf5,
            buf6,
            buf7,
            def
        )
        buf1.readerIndex(faceIndicesOffset)
        buf2.readerIndex(faceCompressTypeOffset)
        readTriangleVertices(
            buf1,
            buf2,
            def
        )
        buf1.readerIndex(simpleTexturesOffset)
        buf2.readerIndex(complexTexturesOffset)
        buf3.readerIndex(texturesScaleOffset)
        buf4.readerIndex(texturesRotationOffset)
        buf5.readerIndex(texturesDirectionOffset)
        buf6.readerIndex(texturesTranslationOffset)
        readVersionedTextures(
            buf1,
            buf2,
            buf3,
            buf4,
            buf5,
            buf6,
            def
        )

        buf1.readerIndex(particlesOffset)
        if (hasParticleEffects) {
            decodeParticles(
                buf1,
                modelPriority,
                def
            )
        }
        if (hasBillboards) {
            decodeBillboards(buf1,def)
        }
        if (options.contains(MeshDecodingOption.ScaleVersionedMesh)) {
            if (version > DEFAULT_VERSION) {
                downscale(def = def)
            }
        }
    }

    private fun decode3(data: ByteBuf, def: ModelType) {
        def.type = MeshType.UnversionedSkeletal
        val buf1 = data.duplicate()
        val buf2 = data.duplicate()
        val buf3 = data.duplicate()
        val buf4 = data.duplicate()
        val buf5 = data.duplicate()
        buf1.readerIndex(buf1.writerIndex() - 23)
        val vertexCount = buf1.readUnsignedShort()
        val triangleCount = buf1.readUnsignedShort()
        val textureTriangleCount = buf1.readUnsignedByte().toInt()
        val hasTextures = buf1.readUnsignedByte().toInt()
        val modelPriority = buf1.readUnsignedByte().toInt()
        val hasFaceAlphas = buf1.readUnsignedByte().toInt()
        val hasFaceSkins = buf1.readUnsignedByte().toInt()
        val hasVertexSkins = buf1.readUnsignedByte().toInt()
        val hasSkeletalBones = buf1.readUnsignedByte().toInt()
        val vertexXBufIndex = buf1.readUnsignedShort()
        val vertexYBufIndex = buf1.readUnsignedShort()

        @Suppress("UNUSED_VARIABLE")
        val vertexZBufIndex = buf1.readUnsignedShort()
        val triangleIndicesBufIndex = buf1.readUnsignedShort()
        val skeletalInfoOffset = buf1.readUnsignedShort()

        var position = 0

        @Suppress("KotlinConstantConditions")
        val vertexFlagsOffset = position
        position += vertexCount
        val facesCompressTypeOffset = position
        position += triangleCount
        val facePrioritiesOffset = position
        if (modelPriority == 0xFF) {
            position += triangleCount
        }
        val faceSkinsOffset = position
        if (hasFaceSkins == 1) {
            position += triangleCount
        }
        val faceTypesOffset = position
        if (hasTextures == 1) {
            position += triangleCount
        }
        val vertexSkinsOffset = position
        position += skeletalInfoOffset
        val faceAlphasOffset = position
        if (hasFaceAlphas == 1) {
            position += triangleCount
        }
        val faceIndicesOffset = position
        position += triangleIndicesBufIndex
        val faceColorsOffset = position
        position += triangleCount * 2
        val faceMappingsOffset = position
        position += textureTriangleCount * 6
        val vertexXOffsetOffset = position
        position += vertexXBufIndex
        val vertexYOffsetOffset = position
        position += vertexYBufIndex
        val vertexZOffsetOffset = position

        initializeUnversioned(
            vertexCount,
            triangleCount,
            textureTriangleCount,
            hasVertexSkins,
            hasTextures,
            modelPriority,
            hasFaceAlphas,
            hasFaceSkins,
            hasSkeletalBones = hasSkeletalBones == 1,
            def
        )

        buf1.readerIndex(vertexFlagsOffset)
        buf2.readerIndex(vertexXOffsetOffset)
        buf3.readerIndex(vertexYOffsetOffset)
        buf4.readerIndex(vertexZOffsetOffset)
        buf5.readerIndex(vertexSkinsOffset)
        readVertexPositions(
            hasVertexSkins,
            hasSkeletalBones = hasSkeletalBones == 1,
            buf1,
            buf2,
            buf3,
            buf4,
            buf5,
            def
        )
        buf1.readerIndex(faceColorsOffset)
        buf2.readerIndex(faceTypesOffset)
        buf3.readerIndex(facePrioritiesOffset)
        buf4.readerIndex(faceAlphasOffset)
        buf5.readerIndex(faceSkinsOffset)
        val (usesFaceTypes, usesMaterials) = readUnversionedTriangleInfo(
            options.toTypedArray(),
            hasTextures,
            modelPriority,
            hasFaceAlphas,
            hasFaceSkins,
            buf1,
            buf2,
            buf3,
            buf4,
            buf5,
            def
        )
        buf1.readerIndex(faceIndicesOffset)
        buf2.readerIndex(facesCompressTypeOffset)
        readTriangleVertices(
            buf1,
            buf2,
            def
        )
        buf1.readerIndex(faceMappingsOffset)
        readUnversionedTextureVertices(buf1,def)
        if (!options.contains(MeshDecodingOption.PreserveOriginalData)) {
            filterUnversionedTextures(
                usesFaceTypes,
                usesMaterials,
                def
            )
        }
    }

    private fun decode4(data: ByteBuf, def: ModelType) {
        def.type = MeshType.VersionedSkeletal
        val buf1 = data.duplicate()
        val buf2 = data.duplicate()
        val buf3 = data.duplicate()
        val buf4 = data.duplicate()
        val buf5 = data.duplicate()
        val buf6 = data.duplicate()
        val buf7 = data.duplicate()
        buf1.readerIndex(buf1.writerIndex() - 26)
        val vertexCount = buf1.readUnsignedShort()
        val triangleCount = buf1.readUnsignedShort()
        val textureTriangleCount = buf1.readUnsignedByte().toInt()
        val footerFlags = buf1.readUnsignedByte().toInt()
        val hasFaceTypes = footerFlags and FACE_TYPES_FLAG == 1
        val hasParticleEffects = footerFlags and PARTICLES_FLAG == 2
        val hasBillboards = footerFlags and BILLBOARDS_FLAG == 4
        val hasVersion = footerFlags and VERSION_FLAG == 8
        val version = if (hasVersion) {
            buf1.readerIndex(buf1.readerIndex() - 7)
            val version = buf1.readUnsignedByte().toInt()
            buf1.readerIndex(buf1.readerIndex() + 6)
            version
        } else {
            DEFAULT_VERSION
        }
        val modelPriority = buf1.readUnsignedByte().toInt()
        val hasFaceAlphas = buf1.readUnsignedByte().toInt()
        val hasFaceSkins = buf1.readUnsignedByte().toInt()
        val hasTextures = buf1.readUnsignedByte().toInt()
        val hasVertexSkins = buf1.readUnsignedByte().toInt()
        val hasSkeletalBones = buf1.readUnsignedByte().toInt()
        val modelVerticesX = buf1.readUnsignedShort()
        val modelVerticesY = buf1.readUnsignedShort()
        val modelVerticesZ = buf1.readUnsignedShort()
        val faceIndices = buf1.readUnsignedShort()
        val textureIndices = buf1.readUnsignedShort()
        val skeletalInfoOffset = buf1.readUnsignedShort()

        var simpleTextureFaceCount = 0
        var complexTextureFaceCount = 0
        var cubeTextureFaceCount = 0
        if (textureTriangleCount > 0) {
            val textureRenderTypes = IntArray(textureTriangleCount)
            def.textureRenderTypes = textureRenderTypes
            buf1.readerIndex(0)
            for (index in 0 until textureTriangleCount) {
                textureRenderTypes[index] = buf1.readByte().toInt()
                val type = textureRenderTypes[index]
                if (type == 0) {
                    simpleTextureFaceCount++
                }
                if (type == 2) {
                    cubeTextureFaceCount++
                }
                if (type in 1..3) {
                    complexTextureFaceCount++
                }
            }
        }

        var offset = textureTriangleCount
        val vertexFlagsOffset = offset
        offset += vertexCount
        val faceTypesOffset = offset
        if (hasFaceTypes) {
            offset += triangleCount
        }
        val faceCompressTypeOffset = offset
        offset += triangleCount
        val facePrioritiesOffset = offset
        if (modelPriority == 0xFF) {
            offset += triangleCount
        }
        val faceSkinsOffset = offset
        if (hasFaceSkins == 1) {
            offset += triangleCount
        }
        val vertexSkinsOffset = offset
        offset += skeletalInfoOffset
        val faceAlphasOffset = offset
        if (hasFaceAlphas == 1) {
            offset += triangleCount
        }
        val faceIndicesOffset = offset
        offset += faceIndices
        val faceMaterialsOffset = offset
        if (hasTextures == 1) {
            offset += triangleCount * 2
        }
        val faceTextureIndicesOffset = offset
        offset += textureIndices
        val faceColorsOffset = offset
        offset += 2 * triangleCount
        val vertexXOffsetOffset = offset
        offset += modelVerticesX
        val vertexYOffsetOffset = offset
        offset += modelVerticesY
        val vertexZOffsetOffset = offset
        offset += modelVerticesZ
        val simpleTexturesOffset = offset
        offset += 6 * simpleTextureFaceCount
        val complexTexturesOffset = offset
        offset += complexTextureFaceCount * 6
        var textureBytes = 6
        if (version == 14) {
            textureBytes = 7
        } else if (version >= 15) {
            textureBytes = 9
        }
        val texturesScaleOffset = offset
        offset += complexTextureFaceCount * textureBytes
        val texturesRotationOffset = offset
        offset += complexTextureFaceCount
        val texturesDirectionOffset = offset
        offset += complexTextureFaceCount
        val texturesTranslationOffset = offset
        offset += complexTextureFaceCount + (2 * cubeTextureFaceCount)
        val particlesOffset = offset

        initializeVersioned(
            version,
            vertexCount,
            triangleCount,
            textureTriangleCount,
            hasFaceTypes,
            hasVertexSkins,
            hasTextures,
            modelPriority,
            hasFaceAlphas,
            hasFaceSkins,
            complexTextureFaceCount,
            cubeTextureFaceCount,
            hasSkeletalBones = hasSkeletalBones == 1,
            def
        )

        buf1.readerIndex(vertexFlagsOffset)
        buf2.readerIndex(vertexXOffsetOffset)
        buf3.readerIndex(vertexYOffsetOffset)
        buf4.readerIndex(vertexZOffsetOffset)
        buf5.readerIndex(vertexSkinsOffset)
        readVertexPositions(
            hasVertexSkins,
            hasSkeletalBones = hasSkeletalBones == 1,
            buf1,
            buf2,
            buf3,
            buf4,
            buf5,
            def
        )

        buf1.readerIndex(faceColorsOffset)
        buf2.readerIndex(faceTypesOffset)
        buf3.readerIndex(facePrioritiesOffset)
        buf4.readerIndex(faceAlphasOffset)
        buf5.readerIndex(faceSkinsOffset)
        buf6.readerIndex(faceMaterialsOffset)
        buf7.readerIndex(faceTextureIndicesOffset)
        readVersionedTriangleInfo(
            hasTextures,
            modelPriority,
            hasFaceAlphas,
            hasFaceSkins,
            hasFaceTypes,
            buf1,
            buf2,
            buf3,
            buf4,
            buf5,
            buf6,
            buf7,
            def
        )
        buf1.readerIndex(faceIndicesOffset)
        buf2.readerIndex(faceCompressTypeOffset)
        readTriangleVertices(
            buf1,
            buf2,
            def
        )
        buf1.readerIndex(simpleTexturesOffset)
        buf2.readerIndex(complexTexturesOffset)
        buf3.readerIndex(texturesScaleOffset)
        buf4.readerIndex(texturesRotationOffset)
        buf5.readerIndex(texturesDirectionOffset)
        buf6.readerIndex(texturesTranslationOffset)
        readVersionedTextures(
            buf1,
            buf2,
            buf3,
            buf4,
            buf5,
            buf6,
            def
        )

        buf1.readerIndex(particlesOffset)
        if (hasParticleEffects) {
            decodeParticles(
                buf1,
                modelPriority,
                def
            )
        }
        if (hasBillboards) {
            decodeBillboards(buf1,def)
        }
        if (options.contains(MeshDecodingOption.ScaleVersionedMesh)) {
            if (version > DEFAULT_VERSION) {
                downscale(def)
            }
        }
    }

    fun downscale(def: ModelType,factor: Int = DOWNSCALE_FACTOR) {
        if (def.vertexCount > 0) {
            val vertexPositionsX = requireNotNull(def.vertexPositionsX)
            val vertexPositionsY = requireNotNull(def.vertexPositionsY)
            val vertexPositionsZ = requireNotNull(def.vertexPositionsZ)
            for (i in 0 until def.vertexCount) {
                vertexPositionsX[i] = vertexPositionsX[i] shr factor
                vertexPositionsY[i] = vertexPositionsY[i] shr factor
                vertexPositionsZ[i] = vertexPositionsZ[i] shr factor
            }
        }
        if (def.textureTriangleCount <= 0) return
        val scaleZ = def.textureScaleZ ?: return
        val speed = requireNotNull(def.textureSpeed)
        val renderTypes = requireNotNull(def.textureRenderTypes)
        val scaleX = requireNotNull(def.textureScaleX)
        for (i in scaleZ.indices) {
            scaleZ[i] = scaleZ[i] shr factor
            speed[i] = speed[i] shr factor
            if (renderTypes[i] != 1) {
                scaleX[i] = scaleX[i] shr factor
            }
        }
    }

    private fun upscale(factor: Int = DOWNSCALE_FACTOR, def: ModelType) {
        if (def.vertexCount > 0) {
            val vertexPositionsX = requireNotNull(def.vertexPositionsX)
            val vertexPositionsY = requireNotNull(def.vertexPositionsY)
            val vertexPositionsZ = requireNotNull(def.vertexPositionsZ)
            for (i in 0 until def.vertexCount) {
                vertexPositionsX[i] = vertexPositionsX[i] shl factor
                vertexPositionsY[i] = vertexPositionsY[i] shl factor
                vertexPositionsZ[i] = vertexPositionsZ[i] shl factor
            }
        }
        if (def.textureTriangleCount <= 0) return
        val scaleZ = def.textureScaleZ ?: return
        val speed = requireNotNull(def.textureSpeed)
        val renderTypes = requireNotNull(def.textureRenderTypes)
        val scaleX = requireNotNull(def.textureScaleX)
        for (i in scaleZ.indices) {
            scaleZ[i] = scaleZ[i] shl factor
            speed[i] = speed[i] shl factor
            if (renderTypes[i] != 1) {
                scaleX[i] = scaleX[i] shl factor
            }
        }
    }

    private fun decodeBillboards(
        buf1: ByteBuf,
        def: ModelType
    ) {
        val count = buf1.readUnsignedByte().toInt()
        if (count > 0) {
            val faceBillboards = Array(count) {
                val id = buf1.readUnsignedShort()
                val face = buf1.readUnsignedShort()
                val skin = buf1.readUnsignedByte().toInt()
                val distance = buf1.readByte().toInt()
                FaceBillboard(id, face, skin, distance)
            }
            def.faceBillboards = faceBillboards
        }
    }

    private fun decodeParticles(
        buf1: ByteBuf,
        modelPriority: Int,
        def: ModelType
    ) {
        val numEmitters = buf1.readUnsignedByte().toInt()
        if (numEmitters > 0) {
            val triangleVertex1 = requireNotNull(def.triangleVertex1)
            val triangleVertex2 = requireNotNull(def.triangleVertex2)
            val triangleVertex3 = requireNotNull(def.triangleVertex3)

            def.emitters = Array(numEmitters) {
                val emitter = buf1.readUnsignedShort()
                val face = buf1.readUnsignedShort()
                val pri = if (modelPriority == 0xFF) {
                    val triangleRenderPriorities = requireNotNull(def.triangleRenderPriorities)
                    triangleRenderPriorities[face]
                } else {
                    modelPriority
                }
                EmissiveTriangle(
                    emitter,
                    face,
                    triangleVertex1[face],
                    triangleVertex2[face],
                    triangleVertex3[face],
                    pri
                )
            }
        }
        val numEffectors = buf1.readUnsignedByte().toInt()
        if (numEffectors > 0) {
            def.effectors = Array(numEffectors) {
                val effector = buf1.readUnsignedShort()
                val vertex = buf1.readUnsignedShort()
                EffectiveVertex(effector, vertex)
            }
        }
    }

    private fun readVersionedTextures(
        buf1: ByteBuf,
        buf2: ByteBuf,
        buf3: ByteBuf,
        buf4: ByteBuf,
        buf5: ByteBuf,
        buf6: ByteBuf,
        def: ModelType
    ) {
        if (def.textureTriangleCount <= 0) return
        val textureRenderTypes = requireNotNull(def.textureRenderTypes)
        val textureTriangleVertex1 = requireNotNull(def.textureTriangleVertex1)
        val textureTriangleVertex2 = requireNotNull(def.textureTriangleVertex2)
        val textureTriangleVertex3 = requireNotNull(def.textureTriangleVertex3)
        for (index in 0 until def.textureTriangleCount) {
            val textureRenderType = textureRenderTypes[index] and 0xFF
            if (textureRenderType == SIMPLE_TEXTURE) {
                textureTriangleVertex1[index] = buf1.readUnsignedShort().toShort().toInt()
                textureTriangleVertex2[index] = buf1.readUnsignedShort().toShort().toInt()
                textureTriangleVertex3[index] = buf1.readUnsignedShort().toShort().toInt()
            }
            if (textureRenderType == CYLINDRICAL_TEXTURE) {
                val textureScaleX = requireNotNull(def.textureScaleX)
                val textureScaleY = requireNotNull(def.textureScaleY)
                val textureScaleZ = requireNotNull(def.textureScaleZ)
                val textureSpeed = requireNotNull(def.textureSpeed)
                val textureRotation = requireNotNull(def.textureRotation)
                val textureDirection = requireNotNull(def.textureDirection)
                textureTriangleVertex1[index] = buf2.readUnsignedShort().toShort().toInt()
                textureTriangleVertex2[index] = buf2.readUnsignedShort().toShort().toInt()
                textureTriangleVertex3[index] = buf2.readUnsignedShort().toShort().toInt()
                if (def.version < 15) {
                    textureScaleZ[index] = buf3.readUnsignedShort()
                    if (def.version < 14) {
                        textureSpeed[index] = buf3.readUnsignedShort()
                    } else {
                        textureSpeed[index] = buf3.readMedium()
                    }
                    textureScaleX[index] = buf3.readUnsignedShort()
                } else {
                    textureScaleZ[index] = buf3.readMedium()
                    textureSpeed[index] = buf3.readMedium()
                    textureScaleX[index] = buf3.readMedium()
                }
                textureRotation[index] = buf4.readByte().toInt()
                textureScaleY[index] = buf5.readByte().toInt()
                textureDirection[index] = buf6.readByte().toInt()
            }
            if (textureRenderType == CUBE_TEXTURE) {
                val textureScaleX = requireNotNull(def.textureScaleX)
                val textureScaleY = requireNotNull(def.textureScaleY)
                val textureScaleZ = requireNotNull(def.textureScaleZ)
                val textureSpeed = requireNotNull(def.textureSpeed)
                val textureRotation = requireNotNull(def.textureRotation)
                val textureDirection = requireNotNull(def.textureDirection)
                val textureTransU = requireNotNull(def.textureTransU)
                val textureTransV = requireNotNull(def.textureTransV)
                textureTriangleVertex1[index] = buf2.readUnsignedShort().toShort().toInt()
                textureTriangleVertex2[index] = buf2.readUnsignedShort().toShort().toInt()
                textureTriangleVertex3[index] = buf2.readUnsignedShort().toShort().toInt()
                if (def.version < 15) {
                    textureScaleZ[index] = buf3.readUnsignedShort()
                    if (def.version >= 14) {
                        textureSpeed[index] = buf3.readMedium()
                    } else {
                        textureSpeed[index] = buf3.readUnsignedShort()
                    }
                    textureScaleX[index] = buf3.readUnsignedShort()
                } else {
                    textureScaleZ[index] = buf3.readMedium()
                    textureSpeed[index] = buf3.readMedium()
                    textureScaleX[index] = buf3.readMedium()
                }
                textureRotation[index] = buf4.readByte().toInt()
                textureScaleY[index] = buf5.readByte().toInt()
                textureDirection[index] = buf6.readByte().toInt()
                textureTransU[index] = buf6.readByte().toInt()
                textureTransV[index] = buf6.readByte().toInt()
            }
            if (textureRenderType == SPHERICAL_TEXTURE) {
                val textureScaleX = requireNotNull(def.textureScaleX)
                val textureScaleY = requireNotNull(def.textureScaleY)
                val textureScaleZ = requireNotNull(def.textureScaleZ)
                val textureSpeed = requireNotNull(def.textureSpeed)
                val textureRotation = requireNotNull(def.textureRotation)
                val textureDirection = requireNotNull(def.textureDirection)
                textureTriangleVertex1[index] = buf2.readUnsignedShort().toShort().toInt()
                textureTriangleVertex2[index] = buf2.readUnsignedShort().toShort().toInt()
                textureTriangleVertex3[index] = buf2.readUnsignedShort().toShort().toInt()
                if (def.version < 15) {
                    textureScaleZ[index] = buf3.readUnsignedShort()
                    if (def.version < 14) {
                        textureSpeed[index] = buf3.readUnsignedShort()
                    } else {
                        textureSpeed[index] = buf3.readMedium()
                    }
                    textureScaleX[index] = buf3.readUnsignedShort()
                } else {
                    textureScaleZ[index] = buf3.readMedium()
                    textureSpeed[index] = buf3.readMedium()
                    textureScaleX[index] = buf3.readMedium()
                }
                textureRotation[index] = buf4.readByte().toInt()
                textureScaleY[index] = buf5.readByte().toInt()
                textureDirection[index] = buf6.readByte().toInt()
            }
        }
    }

    private fun readVersionedTriangleInfo(
        hasTextures: Int,
        modelPriority: Int,
        hasFaceAlphas: Int,
        hasFaceSkins: Int,
        hasFaceTypes: Boolean,
        buf1: ByteBuf,
        buf2: ByteBuf,
        buf3: ByteBuf,
        buf4: ByteBuf,
        buf5: ByteBuf,
        buf6: ByteBuf,
        buf7: ByteBuf,
        def: ModelType
    ) {
        if (def.triangleCount <= 0) return
        val triangleColors = requireNotNull(def.triangleColors)
        val textureCoordinates = def.textureCoordinates
        for (index in 0 until def.triangleCount) {
            triangleColors[index] = buf1.readUnsignedShort().toShort()
            if (hasFaceTypes) {
                val triangleRenderTypes = requireNotNull(def.triangleRenderTypes)
                triangleRenderTypes[index] = buf2.readByte().toInt()
            }
            if (modelPriority == 0xFF) {
                val triangleRenderPriorities = requireNotNull(def.triangleRenderPriorities)
                triangleRenderPriorities[index] = buf3.readByte().toInt()
            }
            if (hasFaceAlphas == 1) {
                val triangleAlphas = requireNotNull(def.triangleAlphas)
                triangleAlphas[index] = buf4.readByte().toInt()
            }
            if (hasFaceSkins == 1) {
                val triangleSkins = requireNotNull(def.triangleSkins)
                triangleSkins[index] = buf5.readUnsignedByte().toInt()
            }
            if (hasTextures == 1) {
                val triangleTextures = requireNotNull(def.triangleTextures)
                triangleTextures[index] = (buf6.readUnsignedShort() - 1).toShort().toInt()
            }
            if (textureCoordinates != null) {
                val triangleTextures = requireNotNull(def.triangleTextures)
                if (triangleTextures[index] == -1) {
                    textureCoordinates[index] = -1
                } else {
                    textureCoordinates[index] = (buf7.readUnsignedByte() - 1).toByte().toInt()
                }
            }
        }
    }

    private fun initializeVersioned(
        version: Int,
        vertexCount: Int,
        triangleCount: Int,
        textureTriangleCount: Int,
        hasFaceTypes: Boolean,
        hasVertexSkins: Int,
        hasTextures: Int,
        modelPriority: Int,
        hasFaceAlphas: Int,
        hasFaceSkins: Int,
        complexTextureFaceCount: Int,
        cubeTextureFaceCount: Int,
        hasSkeletalBones: Boolean,
        def: ModelType
    ) {
        def.version = version
        def.vertexCount = vertexCount
        def.triangleCount = triangleCount
        def.textureTriangleCount = textureTriangleCount
        def.vertexPositionsX = IntArray(vertexCount)
        def.vertexPositionsY = IntArray(vertexCount)
        def.vertexPositionsZ = IntArray(vertexCount)
        def.triangleVertex1 = IntArray(triangleCount)
        def.triangleVertex2 = IntArray(triangleCount)
        def.triangleVertex3 = IntArray(triangleCount)
        def.triangleColors = ShortArray(triangleCount)
        if (hasVertexSkins == 1) {
            def.vertexSkins = IntArray(vertexCount)
        }

        if (hasFaceTypes) {
            def.triangleRenderTypes = IntArray(triangleCount)
        }

        if (modelPriority == 0xFF) {
            def.triangleRenderPriorities = IntArray(triangleCount)
        } else {
            def.renderPriority = modelPriority.toByte().toInt()
        }

        if (hasFaceAlphas == 1) {
            def.triangleAlphas = IntArray(triangleCount)
        }

        if (hasFaceSkins == 1) {
            def.triangleSkins = IntArray(triangleCount)
        }

        if (hasTextures == 1) {
            def.triangleTextures = IntArray(triangleCount)
        }

        if (hasTextures == 1 && textureTriangleCount > 0) {
            def.textureCoordinates = IntArray(triangleCount)
        }

        if (textureTriangleCount > 0) {
            def.textureTriangleVertex1 = IntArray(textureTriangleCount)
            def.textureTriangleVertex2 = IntArray(textureTriangleCount)
            def.textureTriangleVertex3 = IntArray(textureTriangleCount)
            if (complexTextureFaceCount > 0) {
                def.textureScaleX = IntArray(complexTextureFaceCount)
                def.textureScaleY = IntArray(complexTextureFaceCount)
                def.textureScaleZ = IntArray(complexTextureFaceCount)
                def.textureRotation = IntArray(complexTextureFaceCount)
                def.textureDirection = IntArray(complexTextureFaceCount)
                def.textureSpeed = IntArray(complexTextureFaceCount)
            }
            if (cubeTextureFaceCount > 0) {
                def.textureTransU = IntArray(cubeTextureFaceCount)
                def.textureTransV = IntArray(cubeTextureFaceCount)
            }
        }
        if (hasSkeletalBones) {
            def.skeletalBones = arrayOfNulls(vertexCount)
            def.skeletalScales = arrayOfNulls(vertexCount)
        }
    }

    private fun initializeUnversioned(
        vertexCount: Int,
        triangleCount: Int,
        textureTriangleCount: Int,
        hasVertexSkins: Int,
        hasTextures: Int,
        modelPriority: Int,
        hasFaceAlphas: Int,
        hasFaceSkins: Int,
        hasSkeletalBones: Boolean,
        def: ModelType
    ) {
        def.version = UNVERSIONED
        def.vertexCount = vertexCount
        def.triangleCount = triangleCount
        def.textureTriangleCount = textureTriangleCount
        def.vertexPositionsX = IntArray(vertexCount)
        def.vertexPositionsY = IntArray(vertexCount)
        def.vertexPositionsZ = IntArray(vertexCount)
        def.triangleVertex1 = IntArray(triangleCount)
        def.triangleVertex2 = IntArray(triangleCount)
        def.triangleVertex3 = IntArray(triangleCount)
        def.triangleColors = ShortArray(triangleCount)
        if (textureTriangleCount > 0) {
            def.textureRenderTypes = IntArray(textureTriangleCount)
            def.textureTriangleVertex1 = IntArray(textureTriangleCount)
            def.textureTriangleVertex2 = IntArray(textureTriangleCount)
            def.textureTriangleVertex3 = IntArray(textureTriangleCount)
        }
        if (hasVertexSkins == 1) {
            def.vertexSkins = IntArray(vertexCount)
        }
        if (hasTextures == 1) {
            def.triangleRenderTypes = IntArray(triangleCount)
            def.textureCoordinates = IntArray(triangleCount)
            def.triangleTextures = IntArray(triangleCount)
        }
        if (modelPriority == 0xFF) {
            def.triangleRenderPriorities = IntArray(triangleCount)
        } else {
            def.renderPriority = modelPriority.toByte().toInt()
        }
        if (hasFaceAlphas == 1) {
            def.triangleAlphas = IntArray(triangleCount)
        }

        if (hasFaceSkins == 1) {
            def.triangleSkins = IntArray(triangleCount)
        }
        if (hasSkeletalBones) {
            def.skeletalBones = arrayOfNulls(vertexCount)
            def.skeletalScales = arrayOfNulls(vertexCount)
        }
    }

    private fun readVertexPositions(
        hasVertexSkins: Int,
        hasSkeletalBones: Boolean,
        buf1: ByteBuf,
        buf2: ByteBuf,
        buf3: ByteBuf,
        buf4: ByteBuf,
        buf5: ByteBuf,
        def: ModelType
    ) {
        if (def.vertexCount <= 0) return
        val vertexPositionsX = requireNotNull(def.vertexPositionsX)
        val vertexPositionsY = requireNotNull(def.vertexPositionsY)
        val vertexPositionsZ = requireNotNull(def.vertexPositionsZ)
        var lastXOffset = 0
        var lastYOffset = 0
        var lastZOffset = 0
        for (index in 0 until def.vertexCount) {
            val pflag = buf1.readUnsignedByte().toInt()
            var xOffset = 0
            if (pflag and X_POS_FLAG != 0) {
                xOffset = buf2.readShortSmart()
            }
            var yOffset = 0
            if (pflag and Y_POS_FLAG != 0) {
                yOffset = buf3.readShortSmart()
            }
            var zOffset = 0
            if (pflag and Z_POS_FLAG != 0) {
                zOffset = buf4.readShortSmart()
            }
            vertexPositionsX[index] = xOffset + lastXOffset
            vertexPositionsY[index] = yOffset + lastYOffset
            vertexPositionsZ[index] = zOffset + lastZOffset
            lastXOffset = vertexPositionsX[index]
            lastYOffset = vertexPositionsY[index]
            lastZOffset = vertexPositionsZ[index]
            if (hasVertexSkins == 1) {
                val vertexSkins = requireNotNull(def.vertexSkins)
                vertexSkins[index] = buf5.readUnsignedByte().toInt()
            }
        }

        if (hasSkeletalBones) {
            val skeletalBones = requireNotNull(def.skeletalBones)
            val skeletalScales = requireNotNull(def.skeletalScales)
            for (index in 0 until def.vertexCount) {
                val count = buf5.readUnsignedByte().toInt()
                val bones = IntArray(count)
                val scales = IntArray(count)
                skeletalBones[index] = bones
                skeletalScales[index] = scales
                for (i in 0 until count) {
                    bones[i] = buf5.readUnsignedByte().toInt()
                    scales[i] = buf5.readUnsignedByte().toInt()
                }
            }
        }
    }

    private fun readUnversionedTriangleInfo(
        options: Array<out MeshDecodingOption>,
        hasTextures: Int,
        modelPriority: Int,
        hasFaceAlphas: Int,
        hasFaceSkins: Int,
        buf1: ByteBuf,
        buf2: ByteBuf,
        buf3: ByteBuf,
        buf4: ByteBuf,
        buf5: ByteBuf,
        def: ModelType
    ): Pair<Boolean, Boolean> {
        if (def.triangleCount <= 0) return Pair(first = false, second = false)
        var usesFaceTypes = false
        var usesMaterials = false
        val triangleColors = requireNotNull(def.triangleColors)
        val preserveData = options.contains(MeshDecodingOption.PreserveOriginalData)
        for (index in 0 until def.triangleCount) {
            triangleColors[index] = buf1.readUnsignedShort().toShort()
            if (hasTextures == 1) {
                val triangleRenderTypes = requireNotNull(def.triangleRenderTypes)
                val textureCoordinates = requireNotNull(def.textureCoordinates)
                val triangleTextures = requireNotNull(def.triangleTextures)
                val flag = buf2.readUnsignedByte().toInt()
                if (flag and USES_FACE_TYPES_FLAG == 1) {
                    triangleRenderTypes[index] = 1
                    usesFaceTypes = true
                } else {
                    triangleRenderTypes[index] = 0
                }
                if (flag and USES_MATERIALS_FLAG == 2) {
                    textureCoordinates[index] = flag shr 2
                    triangleTextures[index] = triangleColors[index].toInt()
                    if (!preserveData) {
                        triangleColors[index] = 127.toShort()
                    }
                    if (triangleTextures[index] != -1) {
                        usesMaterials = true
                    }
                } else {
                    textureCoordinates[index] = -1
                    triangleTextures[index] = -1
                }
            }
            if (modelPriority == 0xFF) {
                val triangleRenderPriorities = requireNotNull(def.triangleRenderPriorities)
                triangleRenderPriorities[index] = buf3.readByte().toInt()
            }
            if (hasFaceAlphas == 1) {
                val triangleAlphas = requireNotNull(def.triangleAlphas)
                triangleAlphas[index] = buf4.readByte().toInt()
            }
            if (hasFaceSkins == 1) {
                val triangleSkins = requireNotNull(def.triangleSkins)
                triangleSkins[index] = buf5.readUnsignedByte().toInt()
            }
        }
        return Pair(first = usesFaceTypes, second = usesMaterials)
    }

    private fun readTriangleVertices(
        buf1: ByteBuf,
        buf2: ByteBuf,
        def: ModelType
    ) {
        if (def.triangleCount <= 0) return
        val triangleVertex1 = requireNotNull(def.triangleVertex1)
        val triangleVertex2 = requireNotNull(def.triangleVertex2)
        val triangleVertex3 = requireNotNull(def.triangleVertex3)
        var vertex1 = 0
        var vertex2 = 0
        var vertex3 = 0
        var offset = 0
        for (index in 0 until def.triangleCount) {
            when (buf2.readUnsignedByte().toInt()) {
                1 -> {
                    vertex1 = (buf1.readShortSmart() + offset).toShort().toInt()
                    offset = vertex1
                    vertex2 = (buf1.readShortSmart() + offset).toShort().toInt()
                    offset = vertex2
                    vertex3 = (buf1.readShortSmart() + offset).toShort().toInt()
                    offset = vertex3
                    triangleVertex1[index] = vertex1
                    triangleVertex2[index] = vertex2
                    triangleVertex3[index] = vertex3
                }
                2 -> {
                    vertex2 = vertex3
                    vertex3 = (buf1.readShortSmart() + offset).toShort().toInt()
                    triangleVertex1[index] = vertex1
                    offset = vertex3
                    triangleVertex2[index] = vertex2
                    triangleVertex3[index] = vertex3
                }
                3 -> {
                    vertex1 = vertex3
                    vertex3 = (buf1.readShortSmart() + offset).toShort().toInt()
                    triangleVertex1[index] = vertex1
                    offset = vertex3
                    triangleVertex2[index] = vertex2
                    triangleVertex3[index] = vertex3
                }
                4 -> {
                    val pos1 = vertex1
                    vertex1 = vertex2
                    vertex3 = (buf1.readShortSmart() + offset).toShort().toInt()
                    vertex2 = pos1
                    triangleVertex1[index] = vertex1
                    offset = vertex3
                    triangleVertex2[index] = vertex2
                    triangleVertex3[index] = vertex3
                }
            }
        }
    }

    private fun readUnversionedTextureVertices(
        buf1: ByteBuf,
        def: ModelType
    ) {
        if (def.textureTriangleCount <= 0) return
        val textureRenderTypes = requireNotNull(def.textureRenderTypes)
        val textureTriangleVertex1 = requireNotNull(def.textureTriangleVertex1)
        val textureTriangleVertex2 = requireNotNull(def.textureTriangleVertex2)
        val textureTriangleVertex3 = requireNotNull(def.textureTriangleVertex3)
        for (index in 0 until def.textureTriangleCount) {
            textureRenderTypes[index] = 0
            textureTriangleVertex1[index] = buf1.readUnsignedShort().toShort().toInt()
            textureTriangleVertex2[index] = buf1.readUnsignedShort().toShort().toInt()
            textureTriangleVertex3[index] = buf1.readUnsignedShort().toShort().toInt()
        }
    }

    private fun filterUnversionedTextures(
        usesFaceTypes: Boolean,
        usesMaterials: Boolean,
        def: ModelType
    ) {
        val textureCoordinates = def.textureCoordinates
        if (textureCoordinates != null) {
            var usesMapping = false
            require(def.triangleCount > 0)
            val triangleVertex1 = requireNotNull(def.triangleVertex1)
            val triangleVertex2 = requireNotNull(def.triangleVertex2)
            val triangleVertex3 = requireNotNull(def.triangleVertex3)
            for (index in 0 until def.triangleCount) {
                val texture = textureCoordinates[index] and 0xFF
                if (texture != 0xFF) {
                    val textureTriangleVertex1 = requireNotNull(def.textureTriangleVertex1)
                    val textureTriangleVertex2 = requireNotNull(def.textureTriangleVertex2)
                    val textureTriangleVertex3 = requireNotNull(def.textureTriangleVertex3)
                    if (triangleVertex1[index] != textureTriangleVertex1[texture] and 0xFFFF ||
                        triangleVertex2[index] != textureTriangleVertex2[texture] and 0xFFFF ||
                        triangleVertex3[index] != textureTriangleVertex3[texture] and 0xFFFF
                    ) {
                        usesMapping = true
                    } else {
                        textureCoordinates[index] = -1
                    }
                }
            }
            if (!usesMapping) {
                def.textureCoordinates = null
            }
        }
        if (!usesFaceTypes) {
            def.triangleRenderTypes = null
        }
        if (!usesMaterials) {
            def.triangleTextures = null
        }
    }

    private fun ByteBuf._readUnsignedByte(): Int {
        return readByte().toInt() and 0xff
    }

    private fun ByteBuf.readShortSmart(): Int {
        val peek = _readUnsignedByte()
        return if (peek < 128) peek - 64 else (peek shl 8 or _readUnsignedByte()) - 49152
    }
}