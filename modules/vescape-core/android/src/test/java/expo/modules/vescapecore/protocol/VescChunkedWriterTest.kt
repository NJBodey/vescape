package expo.modules.vescapecore.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VescChunkedWriterTest {
    private val queue = VescWriteQueue()

    private fun writer(mtu: Int = MIN_ATT_MTU) = VescChunkedWriter(queue).apply {
        maxWriteBytes = maxWriteBytesForMtu(mtu)
    }

    private fun frame(size: Int, seed: Int = 0) = ByteArray(size) { (it + seed).toByte() }

    @Test
    fun minimumMtuSplitsConfigFrameIntoOrderedChunks() {
        val writer = writer()
        val config = frame(280)
        queue.enqueueNormal(config)

        val sent = mutableListOf<ByteArray>()
        while (true) {
            val chunk = writer.next() ?: break
            sent += chunk
            writer.written(success = true)
        }

        assertEquals(14, sent.size)
        assertTrue(sent.all { it.size <= MIN_ATT_MTU - 3 })
        assertArrayEquals(config, sent.reduce { acc, chunk -> acc + chunk })
    }

    @Test
    fun largerMtuKeepsFramesWhole() {
        val writer = writer(mtu = 247)
        val config = frame(280)
        queue.enqueueNormal(config)

        assertArrayEquals(config, writer.next())
        assertTrue(writer.written(success = true))
        assertNull(writer.next())
    }

    @Test
    fun urgentNeutralWaitsForLastChunkOfInFlightFrame() {
        val writer = writer()
        val config = frame(45)
        val neutral = byteArrayOf(9, 9, 9)
        queue.enqueueNormal(config)

        val first = writer.next()!!
        queue.replaceRemoteInput(neutral, urgent = true)
        assertNull("no second write while a chunk is outstanding", writer.next())
        assertFalse(writer.written(success = true))
        val second = writer.next()!!
        assertFalse(writer.written(success = true))
        val third = writer.next()!!
        assertTrue(writer.written(success = true))

        assertArrayEquals(config, first + second + third)
        assertArrayEquals(neutral, writer.next())
    }

    @Test
    fun refusedChunkMidFrameRetriesThatChunk() {
        val writer = writer()
        val config = frame(45)
        queue.enqueueNormal(config)

        val first = writer.next()!!
        writer.written(success = true)
        val second = writer.next()!!
        writer.refused()
        assertArrayEquals(second, writer.next())
        writer.written(success = true)
        val third = writer.next()!!

        assertArrayEquals(config, first + second + third)
    }

    @Test
    fun refusedFirstChunkLetsNewerRemoteInputWin() {
        val writer = writer()
        val staleTilt = byteArrayOf(1)
        val newerTilt = byteArrayOf(2)
        queue.replaceRemoteInput(staleTilt)

        assertArrayEquals(staleTilt, writer.next())
        writer.refused()
        queue.replaceRemoteInput(newerTilt)

        assertArrayEquals(newerTilt, writer.next())
    }

    @Test
    fun failedChunkDropsRestOfFrame() {
        val writer = writer()
        val config = frame(45)
        val poll = frame(6, seed = 100)
        queue.enqueueNormal(config)
        queue.enqueueNormal(poll)

        writer.next()
        assertTrue("a failed chunk ends its frame", writer.written(success = false))

        assertArrayEquals(poll, writer.next())
    }
}
