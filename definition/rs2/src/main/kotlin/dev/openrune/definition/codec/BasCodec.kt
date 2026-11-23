package dev.openrune.definition.codec

import dev.openrune.definition.revisionIsOrAfter
import dev.openrune.definition.type.BasRandomReadyAnim
import dev.openrune.definition.type.BasType
import dev.openrune.definition.util.readNullableLargeSmartCorrect
import dev.openrune.definition.util.readUnsignedShortOrNull
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import io.netty.buffer.ByteBuf

class BasCodec(private val revision: Int) : Rs2OrderedCodec<BasType> {

    private val wideAnims get() = revisionIsOrAfter(revision, 681)
    private val previousReady get() = revisionIsOrAfter(revision, 916)

    private fun ByteBuf.readAnim(): Int =
        if (wideAnims) readNullableLargeSmartCorrect() ?: -1 else readUnsignedShortOrNull() ?: -1

    private fun ByteBuf.writeAnim(value: Int) {
        if (wideAnims) writeNullableLargeSmartCorrect(value) else writeShort(value)
    }

    override fun BasType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> {
                readyAnim = buffer.readAnim()
                readyAnimSecondary = buffer.readAnim()
            }

            2 -> crawlAnim = buffer.readAnim()
            3 -> crawlAnimBack = buffer.readAnim()
            4 -> crawlAnimLeft = buffer.readAnim()
            5 -> crawlAnimRight = buffer.readAnim()
            6 -> runAnim = buffer.readAnim()
            7 -> runAnimBack = buffer.readAnim()
            8 -> runAnimLeft = buffer.readAnim()
            9 -> runAnimRight = buffer.readAnim()
            26 -> hillRotate = mutableListOf(
                buffer.readUnsignedByte().toInt(),
                buffer.readUnsignedByte().toInt(),
            )

            27 -> unknown27 = mutableListOf(
                buffer.readUnsignedByte().toInt(),
                buffer.readShort().toInt(),
                buffer.readShort().toInt(),
                buffer.readShort().toInt(),
                buffer.readShort().toInt(),
                buffer.readShort().toInt(),
                buffer.readShort().toInt(),
            )

            28 -> {
                val count = buffer.readUnsignedByte().toInt()
                unknown28 = MutableList(count) { buffer.readUnsignedByte().toInt() }
            }

            29 -> turnSpeed = buffer.readUnsignedByte().toInt()
            30 -> turnAcceleration = buffer.readUnsignedShort()
            31 -> unknown31 = buffer.readUnsignedByte().toInt()
            32 -> unknown32 = buffer.readUnsignedShort()
            33 -> unknown33 = buffer.readShort().toInt()
            34 -> unknown34 = buffer.readUnsignedByte().toInt()
            35 -> unknown35 = buffer.readUnsignedShort()
            36 -> unknown36 = buffer.readShort().toInt()
            37 -> walkSpeed = buffer.readUnsignedByte().toInt()
            38 -> readyAnimLeft = buffer.readAnim()
            39 -> readyAnimRight = buffer.readAnim()
            40 -> walkAnimBack = buffer.readAnim()
            41 -> walkAnimLeft = buffer.readAnim()
            42 -> walkAnimRight = buffer.readAnim()
            43 -> unknown43 = buffer.readUnsignedShort()
            44 -> unknown44 = buffer.readUnsignedShort()
            45 -> overlayHeight = buffer.readUnsignedShort()
            46 -> crawlTurnLeft = buffer.readAnim()
            47 -> crawlTurnRight = buffer.readAnim()
            48 -> runTurnLeft = buffer.readAnim()
            49 -> runTurnRight = buffer.readAnim()
            50 -> walkTurnLeft = buffer.readAnim()
            51 -> walkTurnRight = buffer.readAnim()
            52 -> {
                val count = buffer.readUnsignedByte().toInt()
                randomReadyAnims = MutableList(count) {
                    val anim = buffer.readAnim()
                    val weight = buffer.readUnsignedByte().toInt()
                    val previous = if (previousReady) {
                        val previousCount = buffer.readUnsignedByte().toInt()
                        MutableList(previousCount) { buffer.readUnsignedByte().toInt() }
                    } else {
                        mutableListOf()
                    }
                    BasRandomReadyAnim(anim, weight, previous)
                }
            }

            53 -> unknown53 = false
            54 -> hillRotateLimit = mutableListOf(
                buffer.readUnsignedByte().toInt(),
                buffer.readUnsignedByte().toInt(),
            )

            55 -> unknown55 = mutableListOf(
                buffer.readUnsignedByte().toInt(),
                buffer.readUnsignedShort(),
            )

            56 -> unknown56 = mutableListOf(
                buffer.readUnsignedByte().toInt(),
                buffer.readShort().toInt(),
                buffer.readShort().toInt(),
                buffer.readShort().toInt(),
            )
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: BasType) {
        when (opcode) {
            1 -> {
                writeAnim(definition.readyAnim!!)
                writeAnim(definition.readyAnimSecondary!!)
            }

            2 -> writeAnim(definition.crawlAnim!!)
            3 -> writeAnim(definition.crawlAnimBack!!)
            4 -> writeAnim(definition.crawlAnimLeft!!)
            5 -> writeAnim(definition.crawlAnimRight!!)
            6 -> writeAnim(definition.runAnim!!)
            7 -> writeAnim(definition.runAnimBack!!)
            8 -> writeAnim(definition.runAnimLeft!!)
            9 -> writeAnim(definition.runAnimRight!!)
            26 -> {
                val it = definition.hillRotate!!
                writeByte(it[0])
                writeByte(it[1])
            }

            27 -> {
                val it = definition.unknown27!!
                writeByte(it[0])
                for (i in 1..6) writeShort(it[i])
            }

            28 -> {
                val it = definition.unknown28!!
                writeByte(it.size)
                it.forEach { value -> writeByte(value) }
            }

            29 -> writeByte(definition.turnSpeed!!)
            30 -> writeShort(definition.turnAcceleration!!)
            31 -> writeByte(definition.unknown31!!)
            32 -> writeShort(definition.unknown32!!)
            33 -> writeShort(definition.unknown33!!)
            34 -> writeByte(definition.unknown34!!)
            35 -> writeShort(definition.unknown35!!)
            36 -> writeShort(definition.unknown36!!)
            37 -> writeByte(definition.walkSpeed!!)
            38 -> writeAnim(definition.readyAnimLeft!!)
            39 -> writeAnim(definition.readyAnimRight!!)
            40 -> writeAnim(definition.walkAnimBack!!)
            41 -> writeAnim(definition.walkAnimLeft!!)
            42 -> writeAnim(definition.walkAnimRight!!)
            43 -> writeShort(definition.unknown43!!)
            44 -> writeShort(definition.unknown44!!)
            45 -> writeShort(definition.overlayHeight!!)
            46 -> writeAnim(definition.crawlTurnLeft!!)
            47 -> writeAnim(definition.crawlTurnRight!!)
            48 -> writeAnim(definition.runTurnLeft!!)
            49 -> writeAnim(definition.runTurnRight!!)
            50 -> writeAnim(definition.walkTurnLeft!!)
            51 -> writeAnim(definition.walkTurnRight!!)
            52 -> {
                val entries = definition.randomReadyAnims!!
                writeByte(entries.size)
                for (entry in entries) {
                    writeAnim(entry.anim)
                    writeByte(entry.weight)
                    if (previousReady) {
                        writeByte(entry.previousReady.size)
                        entry.previousReady.forEach { writeByte(it) }
                    }
                }
            }

            53 -> Unit
            54 -> {
                val it = definition.hillRotateLimit!!
                writeByte(it[0])
                writeByte(it[1])
            }

            55 -> {
                val it = definition.unknown55!!
                writeByte(it[0])
                writeShort(it[1])
            }

            56 -> {
                val it = definition.unknown56!!
                writeByte(it[0])
                writeShort(it[1])
                writeShort(it[2])
                writeShort(it[3])
            }
        }
    }

    override fun createDefinition() = BasType()
}
