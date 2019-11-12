package org.wycliffeassociates.otter.common.persistence.zip

import java.io.BufferedReader
import java.io.File
import java.io.InputStream

interface IZipFileReader : AutoCloseable {
    fun bufferedReader(filepath: String): BufferedReader
    fun stream(filepath: String): InputStream
    fun exists(filepath: String): Boolean
    fun copyDirectory(source: String, destinationDirectory: File, filter: (String) -> Boolean = { _ -> true })
    fun list(directory: String): Sequence<String>
}