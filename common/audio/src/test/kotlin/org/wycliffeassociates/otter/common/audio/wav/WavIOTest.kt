package org.wycliffeassociates.otter.common.audio.wav

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class WavIOTest {
    private val testCues = listOf(
        WavCue(123_943_347, "marker 1"),
        WavCue(200_000_000, "marker 2"),
        WavCue(300_000_000, "marker 3 ")
    )

    private fun writeDataToFile(file: File, samplesToWrite: Int, cues: List<WavCue>): WavFile {
        val wav = WavFile(
            file,
            DEFAULT_CHANNELS,
            DEFAULT_SAMPLE_RATE,
            DEFAULT_BITS_PER_SAMPLE
        )
        for (cue in cues) {
            wav.metadata.addCue(cue.location, cue.label)
        }
        WavOutputStream(wav).use {
            for (i in 0 until samplesToWrite) {
                it.write(i)
            }
        }
        wav.update()
        return wav
    }

    @Test
    fun `writing a file produces proper header`() {
        val temp = File.createTempFile("testwav", "wav")
        temp.deleteOnExit()
        val audioSamples = 4
        val wav = writeDataToFile(temp, audioSamples, listOf())
        assertEquals("Audio size:", audioSamples, wav.totalAudioLength)
        assertEquals("Total size:", temp.length().toInt() - CHUNK_HEADER_SIZE, wav.totalDataLength)
        assertEquals("Metadata exists:", false, wav.hasMetadata)
        assertEquals("Metadata size:", 0, wav.metadata.totalSize)
        temp.delete()
    }

    @Test
    fun `writing a file with metadata produces proper header`() {
        val temp = File.createTempFile("testwav", "wav")
        temp.deleteOnExit()
        val audioSamples = 4
        val wav = writeDataToFile(temp, audioSamples, listOf(testCues[0], testCues[1]))
        val cues = wav.metadata.getCues()

        assertEquals("Cue count:", 2, cues.size)
        for (i in 0..1) {
            assertEquals("Cue $i label:", testCues[i].label, cues[i].label)
            assertEquals("Cue $i location:", testCues[i].location, cues[i].location)
        }

        assertEquals("Audio size:", 4, wav.totalAudioLength)
        assertEquals("Metadata exists:", true, wav.hasMetadata)
        assertEquals("Metadata size:", 112, wav.metadata.totalSize)
        assertEquals("Total size:", temp.length().toInt() - CHUNK_HEADER_SIZE, wav.totalDataLength)
        temp.delete()
    }

    @Test
    fun `add cue after initally writing file`() {
        val temp = File.createTempFile("testwav", "wav")
        temp.deleteOnExit()
        val audioSamples = 4
        val wav = writeDataToFile(temp, audioSamples, listOf(testCues[0], testCues[1]))
        wav.metadata.addCue(testCues[2].location, testCues[2].label)
        wav.update()
        val cues = wav.metadata.getCues()

        assertEquals("Cue count:", 3, cues.size)
        for (i in 0..2) {
            assertEquals("Cue $i label:", testCues[i].label, cues[i].label)
            assertEquals("Cue $i location:", testCues[i].location, cues[i].location)
        }

        assertEquals("Audio size:", audioSamples, wav.totalAudioLength)
        assertEquals("Metadata exists:", true, wav.hasMetadata)
        assertEquals("Metadata size:", 160, wav.metadata.totalSize)
        assertEquals("Total size:", temp.length().toInt() - CHUNK_HEADER_SIZE, wav.totalDataLength)
        temp.delete()
    }

    @Test
    fun `buffered wav produces equivalent file`() {
        val temp = File.createTempFile("testwav", "wav")
        val temp2 = File.createTempFile("test2wav", "wav")
        val wav = WavFile(
            temp,
            DEFAULT_CHANNELS,
            DEFAULT_SAMPLE_RATE,
            DEFAULT_BITS_PER_SAMPLE
        )
        val wav2 = WavFile(
            temp2,
            DEFAULT_CHANNELS,
            DEFAULT_SAMPLE_RATE,
            DEFAULT_BITS_PER_SAMPLE
        )

        val audioSamples = 700_000

        WavOutputStream(
            wav,
            append = false,
            buffered = false
        ).use {
            for (i in 1..audioSamples) {
                it.write(i)
            }
        }
        WavOutputStream(
            wav2,
            append = false,
            buffered = true
        ).use {
            for (i in 1..audioSamples) {
                it.write(i)
            }
        }

        FileInputStream(temp).use { ifs ->
            FileInputStream(temp2).use { ifs2 ->
                val array = ifs.readAllBytes()
                val array2 = ifs2.readAllBytes()
                assertEquals("file size is the same", array.size, array2.size)
                for (i in array.indices) {
                    assertEquals(array[i], array2[i])
                }
            }
        }
        temp.delete()
        temp2.delete()
    }

    @Test
    fun `writing byte array buffered wav produces equivalent file`() {
        val temp = File.createTempFile("testwav", "wav")
        val temp2 = File.createTempFile("test2wav", "wav")
        val wav = WavFile(
            temp,
            DEFAULT_CHANNELS,
            DEFAULT_SAMPLE_RATE,
            DEFAULT_BITS_PER_SAMPLE
        )
        val wav2 = WavFile(
            temp2,
            DEFAULT_CHANNELS,
            DEFAULT_SAMPLE_RATE,
            DEFAULT_BITS_PER_SAMPLE
        )

        val audioSamples = 700_000
        val byteArray = ByteArray(audioSamples)
        for (i in 0 until audioSamples) {
            byteArray[i] = (i % 255).toByte()
        }

        WavOutputStream(
            wav,
            append = false,
            buffered = false
        ).use {
            it.write(byteArray)
        }
        WavOutputStream(
            wav2,
            append = false,
            buffered = true
        ).use {
            it.write(byteArray)
        }

        FileInputStream(temp).use { ifs ->
            FileInputStream(temp2).use { ifs2 ->
                val array = ifs.readAllBytes()
                val array2 = ifs2.readAllBytes()
                assertEquals("file size is the same", array.size, array2.size)
                for (i in array.indices) {
                    assertEquals(array[i], array2[i])

                    // arrays will be offset by 44 because of the wav header
                    if (i > 44) {
                        assertEquals("read file matches written bytes", array[i], byteArray[i - 44])
                    }
                }
            }
        }
        temp.delete()
        temp2.delete()
    }
}