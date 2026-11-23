package dev.openrune.definition.codec

import dev.openrune.definition.util.readShortSmartSub
import dev.openrune.definition.util.readSmart
import dev.openrune.definition.util.writeSmart
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SmartEncodingTest {

    private fun roundTrip(value: Int): Int {
        val buffer = Unpooled.buffer()
        buffer.writeSmart(value)
        return buffer.readSmart()
    }

    @Test
    fun `write smart is the inverse of read smart`() {
        for (value in intArrayOf(0, 1, 127, 128, 129, 1000, 255, 256, 32766, 32767)) {
            assertEquals(value, roundTrip(value)) { "writeSmart/readSmart disagreed on $value" }
        }
    }

    @Test
    fun `values of 128 or more take the two byte branch`() {
        val buffer = Unpooled.buffer()
        buffer.writeSmart(1000)

        assertEquals(2, buffer.readableBytes())
        assertEquals(0x83, buffer.getUnsignedByte(0).toInt())
    }

    @Test
    fun `values below 128 stay a single byte`() {
        val buffer = Unpooled.buffer()
        buffer.writeSmart(127)

        assertEquals(1, buffer.readableBytes())
        assertEquals(127, buffer.getUnsignedByte(0).toInt())
    }

    @Test
    fun `the sub variant reader agrees with the same encoding`() {
        val buffer = Unpooled.buffer()
        buffer.writeSmart(1000 + 1)

        assertEquals(1000, buffer.readShortSmartSub())
    }
}
