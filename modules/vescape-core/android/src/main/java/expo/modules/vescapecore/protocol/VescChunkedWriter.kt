package expo.modules.vescapecore.protocol

/** ATT default MTU. A link that never negotiates higher has 20 bytes per write. */
internal const val MIN_ATT_MTU = 23

/**
 * Largest single write for a negotiated [mtu]. Only a minimum-MTU link is chunked: some BLE modules
 * negotiate nothing above 23 and drop the link when a larger frame arrives as a GATT long write.
 * Links above the minimum keep sending each frame whole.
 */
internal fun maxWriteBytesForMtu(mtu: Int): Int =
    if (mtu <= MIN_ATT_MTU) MIN_ATT_MTU - 3 else Int.MAX_VALUE

/**
 * Feeds [queue] to GATT one characteristic write at a time, splitting a frame larger than
 * [maxWriteBytes] into consecutive chunks. The queue's in-flight unit stays the whole frame, so its
 * next write (including an urgent neutral) waits for the last chunk and never lands inside a frame.
 * @parity /modules/vescape-core/ios/protocol/VescGattClient.swift `drainWriteQueue`
 */
internal class VescChunkedWriter(private val queue: VescWriteQueue) {
    @Volatile
    var maxWriteBytes: Int = Int.MAX_VALUE

    private var chunks: List<ByteArray> = emptyList()
    private var index = 0
    private var chunkInFlight = false

    /** Next chunk to write, or `null` while a chunk is outstanding or nothing is queued. */
    @Synchronized
    fun next(): ByteArray? {
        if (chunkInFlight) return null
        if (chunks.isEmpty()) {
            val write = queue.startNext() ?: return null
            chunks = split(write.bytes, maxWriteBytes)
            index = 0
        }
        chunkInFlight = true
        return chunks[index]
    }

    /**
     * Android refused to start the chunk from [next]. Before any chunk of the frame went out, the
     * frame returns to the queue so a newer remote input can still win; mid-frame, the same chunk is
     * retried, because restarting a half-sent frame would corrupt it.
     */
    @Synchronized
    fun refused() {
        chunkInFlight = false
        if (index == 0) {
            queue.retryInFlight()
            chunks = emptyList()
        }
    }

    /**
     * The GATT write callback for the outstanding chunk. A failed chunk abandons the rest of its frame;
     * the remaining bytes would only feed the controller's reassembler a broken frame.
     * @return `true` when this callback finished the frame.
     */
    @Synchronized
    fun written(success: Boolean): Boolean {
        if (!chunkInFlight) return false
        chunkInFlight = false
        if (success && index < chunks.lastIndex) {
            index++
            return false
        }
        queue.completeInFlight()
        chunks = emptyList()
        return true
    }

    /** Drop queued writes and forget the link's write size; the next link negotiates its own. */
    @Synchronized
    fun clear() {
        queue.clear()
        maxWriteBytes = Int.MAX_VALUE
        chunks = emptyList()
        index = 0
        chunkInFlight = false
    }

    private fun split(bytes: ByteArray, max: Int): List<ByteArray> =
        if (bytes.size <= max) listOf(bytes) else bytes.asList().chunked(max) { it.toByteArray() }
}
