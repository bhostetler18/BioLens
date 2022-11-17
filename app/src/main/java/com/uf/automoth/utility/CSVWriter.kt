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
        val row = values.joinToString(separator) { sanitize(it) }
        writer.write(row)
        writer.println()
    }

    private fun sanitize(string: String): String {
        // If desired, we can escape these instead in the future
        return string.replace("\n", "").replace(separator, "")
    }

    override fun close() {
        writer.close()
    }
}
