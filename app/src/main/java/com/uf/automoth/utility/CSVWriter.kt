package com.uf.automoth.utility

import java.io.Closeable
import java.io.File
import java.io.PrintWriter

class CSVWriter(
    file: File,
    private val separator: String = ","
) : Closeable {
    private val writer: PrintWriter = PrintWriter(file)

    fun writeLine(values: List<String>) {
        for (value in values) {
            writer.write(value)
            writer.write(separator)
        }
        writer.println()
    }

    override fun close() {
        writer.close()
    }
}
