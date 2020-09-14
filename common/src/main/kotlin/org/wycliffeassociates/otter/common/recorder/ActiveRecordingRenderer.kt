package org.wycliffeassociates.otter.common.recorder

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import org.wycliffeassociates.otter.common.collections.FloatRingBuffer
import org.wycliffeassociates.otter.common.audio.wav.DEFAULT_SAMPLE_RATE
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

private const val DEFAULT_BUFFER_SIZE = 1024

class ActiveRecordingRenderer(
    stream: Observable<ByteArray>,
    recordingActive: Observable<Boolean>,
    width: Int,
    secondsOnScreen: Int
) {
    private val logger = LoggerFactory.getLogger(ActiveRecordingRenderer::class.java)

    private var isActive = AtomicBoolean(false)

    // double the width as for each pixel there will be a min and max value
    val floatBuffer = FloatRingBuffer(width * 2)
    private val pcmCompressor = PCMCompressor(floatBuffer, samplesToCompress(width, secondsOnScreen))
    val bb = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)

    init {
        recordingActive.subscribe(
            { isActive.set(it) },
            { e ->
                logger.error("Error in active recording listener", e)
            }
        )
        bb.order(ByteOrder.LITTLE_ENDIAN)
    }

    val activeRenderer = stream
        .subscribeOn(Schedulers.io())
        .subscribe(
            {
                bb.put(it)
                bb.position(0)
                while (bb.hasRemaining()) {
                    val short = bb.short
                    if (isActive.get()) {
                        pcmCompressor.add(short.toFloat())
                    }
                }
                bb.clear()
            },
            { e ->
                logger.error("Error in active renderer stream", e)
            }
        )

    private fun samplesToCompress(width: Int, secondsOnScreen: Int): Int {
        // TODO: get samplerate from wav file, don't assume 44.1khz
        return (DEFAULT_SAMPLE_RATE * secondsOnScreen) / width
    }
}
