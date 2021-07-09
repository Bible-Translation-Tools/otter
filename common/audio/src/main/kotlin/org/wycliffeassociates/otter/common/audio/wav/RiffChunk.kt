package org.wycliffeassociates.otter.common.audio.wav

import java.nio.ByteBuffer

internal const val CHUNK_HEADER_SIZE = 8
internal const val CHUNK_LABEL_SIZE = 4
internal const val DWORD_SIZE = 4

// chunk data must be word aligned but the size might not account for padding
// therefore, if odd, the size we read must add one to include the padding
// https://sharkysoft.com/jwave/docs/javadocs/lava/riff/wave/doc-files/riffwave-frameset.htm
internal fun wordAlign(subchunkSize: Int) = subchunkSize + if (subchunkSize % 2 == 0) 0 else 1

interface RiffChunk {
    fun parse(chunk: ByteBuffer)
    fun toByteArray(): ByteArray
    val totalSize: Int
}
